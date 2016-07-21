/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.globalbedo.bbdr;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.auxdata.AerosolClimatology;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.inversion.util.ModisTileGeoCoding;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.meris.radiometry.MerisRadiometryCorrectionOp;
import org.esa.beam.meris.radiometry.equalization.ReprocessingVersion;
import org.esa.beam.util.BitSetter;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.*;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;

/**
 * GlobAlbedo Level 2 processor for BBDR and kernel parameter computation: special use case of single pixel input
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l2.single")
public class GlobalbedoLevel2SinglePixel extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    @Parameter(defaultValue = "")
    private String tile;

    @Parameter(defaultValue = ".")
    private String bbdrDir;

    private String bbdrFileName;
    private float latitude;
    private float longitude;

    private int w;
    private int h;

    private String sourceProductName;

    @Override
    public void initialize() throws OperatorException {
        sourceProductName = ((File) sourceProduct.getProductReader().getInput()).getName();
        if (!isInputInvalid(sourceProductName)) {
            throw new OperatorException("Input product invalid!");
        }

        w = sourceProduct.getSceneRasterWidth();
        h = sourceProduct.getSceneRasterHeight();
        if (w != 1 || h != 1) {
            throw new OperatorException("Products with more than one pixel are not supported in single-pixel mode!");
        }

        Product bbdrInputProduct = null;
        if (sourceProduct.getStartTime() == null) {
            try {
                ProductData.UTC productTime = getProductTime(sensor, sourceProductName);
                sourceProduct.setStartTime(productTime);
                sourceProduct.setEndTime(productTime);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        // lat/lon bands must have been included in source csv files!
        Band latBand = sourceProduct.getBand("latitude");
        Band lonBand = sourceProduct.getBand("longitude");

        BbdrMasterOp bbdrOp;
        switch (sensor.getInstrument()) {
            case "MERIS":
                sourceProduct.setProductType("MER_RR_L1b");
                try {
                    if (sourceProduct.getFlagCodingGroup().getNodeCount() == 0) {
                        final FlagCoding merisL1bFlagCoding = createMerisL1bFlagCoding("l1_flags");
                        sourceProduct.getFlagCodingGroup().add(merisL1bFlagCoding);
                        sourceProduct.getBand("l1_flags").setSampleCoding(merisL1bFlagCoding);
                    }
                    bbdrInputProduct = prepareMerisBbdrInputProduct();
                } catch (Exception e) {
                    // todo
                    e.printStackTrace();
                }
                bbdrOp = new BbdrMerisOp();
                break;
            case "VGT":
                sourceProduct.setProductType("VGT_L1b");
                try {
                    final FlagCoding vgtL1bFlagCoding = createVgtL1bFlagCoding("SM");
                    sourceProduct.getFlagCodingGroup().add(vgtL1bFlagCoding);
                    sourceProduct.getBand("SM").setSampleCoding(vgtL1bFlagCoding);
                    bbdrInputProduct = prepareVgtBbdrInputProduct();
                } catch (Exception e) {
                    // todo
                    e.printStackTrace();
                }
                bbdrOp = new BbdrVgtOp();
                break;
            default:
                throw new OperatorException("Sensor " + sensor.getInstrument() + " not supported.");
        }

        final Tile latTile = getSourceTile(latBand, new Rectangle(0, 0, 1, 1));
        final Tile lonTile = getSourceTile(lonBand, new Rectangle(0, 0, 1, 1));
        latitude = latTile.getSampleFloat(0, 0);
        longitude = lonTile.getSampleFloat(0, 0);

        float elevation = 0.0f;
        try {
            elevation = BbdrUtils.getDem().getElevation(new GeoPos(latitude, longitude));
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Could not derive elevation from DEM - set to 0.0");
        }
        final Band elevationBand = bbdrInputProduct.addBand("elevation", ProductData.TYPE_FLOAT32);
        elevationBand.setRasterData(ProductData.createInstance(new float[]{elevation}));

        bbdrInputProduct.addBand("aot", ProductData.TYPE_FLOAT32).
                setSourceImage(ConstantDescriptor.create((float) w, (float) h, new Float[]{0.15f}, null)); // dummy
        bbdrInputProduct.addBand("aot_err", ProductData.TYPE_FLOAT32).
                setSourceImage(ConstantDescriptor.create(1.0f, 1.0f, new Float[]{0.0f}, null)); // dummy

        bbdrOp.setSourceProduct(bbdrInputProduct);

        bbdrOp.setParameterDefaultValues();
        bbdrOp.setParameter("sensor", sensor);
        bbdrOp.setParameter("singlePixelMode", true);
        bbdrOp.setParameter("useAotClimatology", true);
        bbdrOp.setParameter("aotClimatologyValue", getAotClimatologyValue());
        Product bbdrProduct = bbdrOp.getTargetProduct();

        ProductUtils.copyBand("latitude", sourceProduct, bbdrProduct, true);
        ProductUtils.copyBand("longitude", sourceProduct, bbdrProduct, true);

        setBbdrFilename(bbdrProduct);

        setTargetProduct(bbdrProduct);

        getTargetProduct().setProductType(sourceProduct.getProductType() + "_BBDR");
        getTargetProduct().setStartTime(sourceProduct.getStartTime());
        getTargetProduct().setEndTime(sourceProduct.getEndTime());

        writeCsvProduct(getTargetProduct(), bbdrFileName);
    }

    /**
     * returns product time derived from product name
     *
     * @param sensor - the sensor
     * @param productName - the source product name
     * @return product time
     * @throws ParseException
     */
    static ProductData.UTC getProductTime(Sensor sensor, String productName) throws ParseException {
        // subset_0_of_subset_saudiarabia_of_V2KRNP____20140511F025_1x1
        // subset_0_of_MER_RR__1PNMAP20060511_095146_000003522047_00337_21934_0001_1x1

        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        String dateTime = getDateTimeString(sensor, productName);

        final int year = Integer.parseInt(dateTime.substring(0, 4));
        final int month = Integer.parseInt(dateTime.substring(4, 6));
        final int day = Integer.parseInt(dateTime.substring(6, 8));
        if (sensor == Sensor.MERIS) {
            final int hour = Integer.parseInt(dateTime.substring(9, 11));
            final int min = Integer.parseInt(dateTime.substring(11, 13));
            final int sec = Integer.parseInt(dateTime.substring(13, 15));
            cal.set(year, month - 1, day, hour, min, sec);
        } else  {
            cal.set(year, month - 1, day, 0, 0, 0);
        }

        return ProductData.UTC.create(cal.getTime(), 0);
    }

    /**
     * returns datetime string derived from product name:
     * 'yyyyMMdd_hhmmss' for MERIS
     * 'yyMMdd' for VGT
     *
     * @param sensor - the sensor
     * @param productName - the source product name
     * @return datetime string
     */
    static String getDateTimeString(Sensor sensor, String productName) {
        if (sensor == Sensor.MERIS) {
            final int offset = productName.indexOf("MER_RR__1");
            return productName.substring(offset + 14, offset + 29);
        } else if (sensor == Sensor.VGT) {
            final int offset = productName.indexOf("V2KRNP____");
            return productName.substring(offset + 10, offset + 18);
        } else {
            return null;
        }
    }

    private boolean isInputInvalid(String productName) {
        if (sensor == Sensor.MERIS) {
            return productName.contains("MER_RR__1");
        } else
            return sensor == Sensor.VGT && productName.contains("V2KRNP____");
    }

    private float getAotClimatologyValue() {
        final String dateTimeString = getDateTimeString(sensor, sourceProductName);
        final String monthString = dateTimeString.substring(4, 6);
        final int month = Integer.parseInt(monthString);

        final Product aotProduct = AerosolClimatology.getInstance().getAotProduct();

        if (aotProduct != null) {
            final Band aotBand = aotProduct.getBand(BbdrConstants.AEROSOL_CLIMATOLOGY_MONTHLY_BAND_GROUP_NAME + month);
            final int aotProductWidth = aotProduct.getSceneRasterWidth();
            final int aotProductHeight = aotProduct.getSceneRasterHeight();
            final Tile aotTile = getSourceTile(aotBand, new Rectangle(aotProductWidth, aotProductHeight));
            final GeoCoding geoCoding = aotProduct.getGeoCoding();
            final PixelPos pixelPos = geoCoding.getPixelPos(new GeoPos(latitude, longitude), null);
            return aotTile.getSampleFloat((int) pixelPos.x, (int) pixelPos.y);
        } else {
            getLogger().log(Level.INFO, "No AOT product available - will use constant AOT value of 0.15.");
            return BbdrConstants.AOT_CONST_VALUE;
        }
    }

    private void setBbdrFilename(Product bbdrProduct) {
        final String modisTile = AlbedoInversionUtils.getModisTileFromLatLon(latitude, longitude);
        final ModisTileGeoCoding sinusoidalTileGeocoding = IOUtils.getSinusoidalTileGeocoding(modisTile);
        bbdrProduct.setGeoCoding(sinusoidalTileGeocoding);
        PixelPos pixelPos = sinusoidalTileGeocoding.getPixelPos(new GeoPos(latitude, longitude), null);
        final int pixelPosX = (int) pixelPos.getX();
        final int pixelPosY = (int) pixelPos.getY();

        final String sourceFileName = ((File) sourceProduct.getProductReader().getInput()).getName();
        bbdrProduct.setName(FileUtils.getFilenameWithoutExtension(sourceFileName));

        final String dateString = getCurrentDate();
        bbdrFileName = bbdrProduct.getName() + "_BBDR_" + modisTile + "_" +
                String.format("%04d", pixelPosX) + "_" + String.format("%04d", pixelPosY) + "_v" + dateString;
    }

    private Product prepareMerisBbdrInputProduct() throws Exception {
        int index = 0;
        for (Band srcBand : sourceProduct.getBands()) {
            final String srcName = srcBand.getName();
            if (srcName.startsWith("radiance_")) {
                srcBand.setSpectralBandIndex(index);
                srcBand.setSpectralWavelength(BbdrConstants.MERIS_WAVELENGHTS[index]);
                srcBand.setSolarFlux(EnvisatConstants.MERIS_SOLAR_FLUXES[index]);
                index++;
            }
        }

        Product radiometryProduct;
        MerisRadiometryCorrectionOp merisRadiometryCorrectionOp = new MerisRadiometryCorrectionOp();
        merisRadiometryCorrectionOp.setSourceProduct(sourceProduct);
        merisRadiometryCorrectionOp.setParameterDefaultValues();
        merisRadiometryCorrectionOp.setParameter("doRadToRefl", true);
        merisRadiometryCorrectionOp.setParameter("doEqualization", true);
        merisRadiometryCorrectionOp.setParameter("reproVersion", ReprocessingVersion.REPROCESSING_3);
        radiometryProduct = merisRadiometryCorrectionOp.getTargetProduct();

        // copy all non-radiance bands from sourceProduct and
        // copy reflectance bands from reflProduct
        for (Band srcBand : radiometryProduct.getBands()) {
            final String srcName = srcBand.getName();
            if (srcName.startsWith("reflec_")) {
                final String reflectanceName = "reflectance_" + srcName.split("_")[1];
                srcBand.setName(reflectanceName);
            }
        }

        return radiometryProduct;
    }

    private Product prepareVgtBbdrInputProduct() {
        Product vgtBbdrInputProduct = new Product(sourceProduct.getName(),
                                                  sourceProduct.getProductType(),
                                                  w, h);

        int index = 0;
        for (Band srcBand : sourceProduct.getBands()) {
            ProductUtils.copyBand(srcBand.getName(), sourceProduct, vgtBbdrInputProduct, true);
            final String srcName = srcBand.getName();
            if (srcName.startsWith("B") || srcName.equals("MIR")) {
                srcBand.setSpectralBandIndex(index);
                srcBand.setSpectralWavelength(BbdrConstants.VGT_WAVELENGHTS[index]);
                index++;
            }
        }

        return vgtBbdrInputProduct;
    }

    private static FlagCoding createMerisL1bFlagCoding(String flagIdentifier) {
        FlagCoding flagCoding = new FlagCoding(flagIdentifier);
        flagCoding.addFlag("COSMETIC", BitSetter.setFlag(0, 0), null);
        flagCoding.addFlag("DUPLICATED", BitSetter.setFlag(0, 1), null);
        flagCoding.addFlag("GLINT_RISK", BitSetter.setFlag(0, 2), null);
        flagCoding.addFlag("SUSPECT", BitSetter.setFlag(0, 3), null);
        flagCoding.addFlag("LAND_OCEAN", BitSetter.setFlag(0, 4), null);
        flagCoding.addFlag("BRIGHT", BitSetter.setFlag(0, 5), null);
        flagCoding.addFlag("COASTLINE", BitSetter.setFlag(0, 6), null);
        flagCoding.addFlag("INVALID", BitSetter.setFlag(0, 7), null);
        return flagCoding;
    }

    private FlagCoding createVgtL1bFlagCoding(String flagIdentifier) {
        FlagCoding flagCoding = new FlagCoding(flagIdentifier);
        flagCoding.addFlag("B0_GOOD", BitSetter.setFlag(0, 7), null);
        flagCoding.addFlag("B2_GOOD", BitSetter.setFlag(0, 6), null);
        flagCoding.addFlag("B3_GOOD", BitSetter.setFlag(0, 5), null);
        flagCoding.addFlag("MIR_GOOD", BitSetter.setFlag(0, 4), null);
        flagCoding.addFlag("LAND", BitSetter.setFlag(0, 3), null);
        flagCoding.addFlag("ICE_SNOW", BitSetter.setFlag(0, 2), null);
        flagCoding.addFlag("CLOUD_2", BitSetter.setFlag(0, 1), null);
        flagCoding.addFlag("CLOUD_1", BitSetter.setFlag(0, 0), null);
        return flagCoding;
    }

    private void writeCsvProduct(Product product, String productName) {
        File dir = new File(bbdrDir);
        dir.mkdirs();
        File file = new File(dir, productName + ".csv");
        WriteOp writeOp = new WriteOp(product, file, "CSV");
        writeOp.writeProduct(ProgressMonitor.NULL);
    }

    private static String getCurrentDate() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyyMMdd");//dd/MM/yyyy
        Date now = new Date();
        return sdfDate.format(now);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel2SinglePixel.class);
        }
    }
}

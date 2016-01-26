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
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.meris.radiometry.MerisRadiometryCorrectionOp;
import org.esa.beam.meris.radiometry.equalization.ReprocessingVersion;
import org.esa.beam.util.BitSetter;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.*;
import java.io.File;
import java.text.ParseException;
import java.util.logging.Logger;

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

    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();
        Product bbdrInputProduct = null;
//        ((File)sourceProduct.getProductReader().getInput()).getAbsolutePath()
        if (sourceProduct.getStartTime() == null) {
            try {
                // todo: these are test times. get correct start/stop from product name!!
                sourceProduct.setStartTime(ProductData.UTC.parse("11-May-2006 09:51:46"));
                sourceProduct.setEndTime(ProductData.UTC.parse("11-May-2006 09:51:46"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        Band latBand = null;
        Band lonBand = null;

        BbdrMasterOp bbdrOp;
        switch (sensor.getInstrument()) {
            case "MERIS":
                latBand = sourceProduct.getBand("latitude");
                lonBand = sourceProduct.getBand("longitude");

                sourceProduct.setProductType("MER_RR_L1b");
                if (sourceProduct.getFlagCodingGroup().getNodeCount() == 0) {
                    final FlagCoding merisL1bFlagCoding = createMerisL1bFlagCoding("l1_flags");
                    sourceProduct.getFlagCodingGroup().add(merisL1bFlagCoding);
                    sourceProduct.getBand("l1_flags").setSampleCoding(merisL1bFlagCoding);
                }
                try {
                    bbdrInputProduct = prepareMerisBbdrInputProduct();
                } catch (Exception e) {
                    // todo
                    e.printStackTrace();
                }
                bbdrOp = new BbdrMerisOp();
                break;
            case "VGT":
                // todo: apply radiometric correction as in GaMasterOp
                bbdrInputProduct = prepareVgtBbdrInputProduct();
                bbdrOp = new BbdrVgtOp();
                break;
            default:
                throw new OperatorException("Sensor " + sensor.getInstrument() + " not supported.");
        }

        final Tile latTile = getSourceTile(latBand, new Rectangle(0, 0, 1, 1));
        final Tile lonTile = getSourceTile(lonBand, new Rectangle(0, 0, 1, 1));
        latitude = latTile.getSampleFloat(0, 0);
        longitude = lonTile.getSampleFloat(0, 0);

        bbdrOp.setSourceProduct(bbdrInputProduct);

        bbdrOp.setParameterDefaultValues();
        bbdrOp.setParameter("sensor", sensor);
        bbdrOp.setParameter("singlePixelMode", true);
        bbdrOp.setParameter("useAotClimatology", true);
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

    private void setBbdrFilename(Product bbdrProduct) {
        final String modisTile = BbdrUtils.getModisTileFromLatLon(latitude, longitude);
        final CrsGeoCoding sinusoidalTileGeocoding = IOUtils.getSinusoidalTileGeocoding(modisTile);
        bbdrProduct.setGeoCoding(sinusoidalTileGeocoding);
        PixelPos pixelPos = sinusoidalTileGeocoding.getPixelPos(new GeoPos(latitude, longitude), null);
        final int pixelPosX = (int) pixelPos.getX();
        final int pixelPosY = (int) pixelPos.getY();

        final String sourceFileName = ((File) sourceProduct.getProductReader().getInput()).getName();
        bbdrProduct.setName(FileUtils.getFilenameWithoutExtension(sourceFileName));

        bbdrFileName = bbdrProduct.getName() + "_BBDR_" + modisTile + "_" +
                String.format("%04d", pixelPosX) + "_" + String.format("%04d", pixelPosY);
    }

    private Product prepareVgtBbdrInputProduct() {
        // todo
        return null;
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

        final float elevation = BbdrUtils.getDem().getElevation(new GeoPos(latitude, longitude));
        final Band elevationBand = radiometryProduct.addBand("elevation", ProductData.TYPE_FLOAT32);
        elevationBand.setRasterData(ProductData.createInstance(new float[]{elevation}));

        final int w = radiometryProduct.getSceneRasterWidth();
        final int h = radiometryProduct.getSceneRasterHeight();
        if (w != 1 || h != 1) {
            throw new OperatorException("Products with more than one pixel are not supported in single-pixel mode!");
        }

        radiometryProduct.addBand("aot", ProductData.TYPE_FLOAT32).
                setSourceImage(ConstantDescriptor.create((float) w, (float) h, new Float[]{0.15f}, null)); // dummy
        radiometryProduct.addBand("aot_err", ProductData.TYPE_FLOAT32).
                setSourceImage(ConstantDescriptor.create(1.0f, 1.0f, new Float[]{0.0f}, null)); // dummy

        return radiometryProduct;
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


    private void writeCsvProduct(Product product, String productName) {
        File dir = new File(bbdrDir);
        dir.mkdirs();
        File file = new File(dir, productName + ".csv");
        WriteOp writeOp = new WriteOp(product, file, "CSV");
        writeOp.writeProduct(ProgressMonitor.NULL);
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel2SinglePixel.class);
        }
    }
}

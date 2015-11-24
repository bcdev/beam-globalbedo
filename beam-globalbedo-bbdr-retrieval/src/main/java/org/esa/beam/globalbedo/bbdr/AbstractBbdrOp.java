/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.PixelOperator;
import org.esa.beam.framework.gpf.pointop.ProductConfigurer;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.landcover.StatusPostProcessOp;
import org.esa.beam.util.ProductUtils;

import java.awt.*;

/**
 * Computes SDR/BBDR and kernel parameters
 * // todo: make this an abstract class for BBDR computation. Implement for specific sensors.
 *
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
@OperatorMetadata(alias = "ga.bbdr",
        description = "Computes BBDRs and kernel parameters",
        authors = "Marco Zuehlke, Olaf Danne",
        version = "1.1",
        copyright = "(C) 2015 by Brockmann Consult")
public abstract class AbstractBbdrOp extends PixelOperator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(description = "Sensor")
    private Sensor sensor;

    @Parameter(defaultValue = "false")
    private boolean sdrOnly;

    @Parameter(defaultValue = "true")
    private boolean doUclCloudDetection;

    @Parameter
    private String landExpression;

    private static final int SRC_LAND_MASK = 0;
    private static final int SRC_SNOW_MASK = 1;
    private static final int SRC_VZA = 2;
//    private static final int SRC_VAA = 3;
//    private static final int SRC_SZA = 4;
//    private static final int SRC_SAA = 5;
//    private static final int SRC_DEM = 6;
//    private static final int SRC_AOT = 7;
//    private static final int SRC_AOT_ERR = 8;
//    private static final int SRC_OZO = 9;
//    private static final int SRC_WVP = 10;
    private static final int SRC_TOA_RFL = 11;
    protected int SRC_TOA_VAR;
    protected int SRC_STATUS;

    private static final int TRG_ERRORS = 3;
    private static final int TRG_KERN = 9;
    private static final int TRG_NDVI = 15;
    private static final int TRG_VZA = 17;
    private static final int TRG_SZA = 18;
    private static final int TRG_RAA = 19;
    private static final int TRG_DEM = 20;
    private static final int TRG_SNOW = 21;
    private static final int TRG_AOD = 22;

    private static final int TRG_AODERR = 23;

    protected BbdrAuxdata aux;

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        aux = BbdrAuxdata.getInstance(sourceProduct, sensor);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();
        if (sdrOnly) {
            addSdrBands(targetProduct);
            addSdrErrorBands(targetProduct);
            addNdviBand(targetProduct);

            // copy flag coding and flag images
            ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

            // add status band
            addStatusBand(targetProduct);

            targetProduct.setAutoGrouping("sdr_error:sdr");
        } else {
            String[] bandNames = {
                    "BB_VIS", "BB_NIR", "BB_SW",
                    "sig_BB_VIS_VIS", "sig_BB_VIS_NIR", "sig_BB_VIS_SW",
                    "sig_BB_NIR_NIR", "sig_BB_NIR_SW", "sig_BB_SW_SW",
                    "Kvol_BRDF_VIS", "Kvol_BRDF_NIR", "Kvol_BRDF_SW",
                    "Kgeo_BRDF_VIS", "Kgeo_BRDF_NIR", "Kgeo_BRDF_SW",
                    "AOD550", "sig_AOD550",
                    "NDVI", "sig_NDVI",
                    "VZA", "SZA", "RAA", "DEM",
            };
            for (String bandName : bandNames) {
                Band band = targetProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
                band.setNoDataValue(Float.NaN);
                band.setNoDataValueUsed(true);
            }
            targetProduct.addBand("snow_mask", ProductData.TYPE_INT8);

            // copy flag coding and flag images
            ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        }
    }

    private void addSdrBands(Product targetProduct) {
        for (int i=0; i<sensor.getToaBandNames().length; i++) {
            Band srcBand = sourceProduct.getBand(sensor.getToaBandNames()[i]);
            Band band = targetProduct.addBand(sensor.getSdrBandNames()[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
            ProductUtils.copySpectralBandProperties(srcBand, band);
        }
    }

    private void addSdrErrorBands(Product targetProduct) {
        for (int i=0; i<sensor.getToaBandNames().length; i++) {
            Band srcBand = sourceProduct.getBand(sensor.getToaBandNames()[i]);
            Band band = targetProduct.addBand(sensor.getSdrErrorBandNames()[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
            ProductUtils.copySpectralBandProperties(srcBand, band);
        }
    }

    private void addNdviBand(Product targetProduct) {
        Band ndvi = targetProduct.addBand("ndvi", ProductData.TYPE_FLOAT32);
        ndvi.setNoDataValue(Float.NaN);
        ndvi.setNoDataValueUsed(true);
        Band aod = targetProduct.addBand("aod", ProductData.TYPE_FLOAT32);
        aod.setNoDataValue(Float.NaN);
        aod.setNoDataValueUsed(true);
    }

    private void addStatusBand(Product targetProduct) {
        Band statusBand = targetProduct.addBand("status", ProductData.TYPE_INT8);
        statusBand.setNoDataValue(StatusPostProcessOp.STATUS_INVALID);
        statusBand.setNoDataValueUsed(true);

        final IndexCoding indexCoding = new IndexCoding("status");
        ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[7];
        indexCoding.addIndex("land", StatusPostProcessOp.STATUS_LAND, "");
        points[0] = new ColorPaletteDef.Point(StatusPostProcessOp.STATUS_LAND, Color.GREEN, "land");

        indexCoding.addIndex("water", StatusPostProcessOp.STATUS_WATER, "");
        points[1] = new ColorPaletteDef.Point(StatusPostProcessOp.STATUS_WATER, Color.BLUE, "water");

        indexCoding.addIndex("snow", StatusPostProcessOp.STATUS_SNOW, "");
        points[2] = new ColorPaletteDef.Point(StatusPostProcessOp.STATUS_SNOW, Color.YELLOW, "snow");

        indexCoding.addIndex("cloud", StatusPostProcessOp.STATUS_CLOUD, "");
        points[3] = new ColorPaletteDef.Point(StatusPostProcessOp.STATUS_CLOUD, Color.WHITE, "cloud");

        indexCoding.addIndex("cloud_shadow", StatusPostProcessOp.STATUS_CLOUD_SHADOW, "");
        points[4] = new ColorPaletteDef.Point(StatusPostProcessOp.STATUS_CLOUD_SHADOW, Color.GRAY, "cloud_shadow");

        indexCoding.addIndex("cloud_buffer", StatusPostProcessOp.STATUS_CLOUD_BUFFER, "");
        points[5] = new ColorPaletteDef.Point(StatusPostProcessOp.STATUS_CLOUD_BUFFER, Color.RED, "cloud_buffer");

        indexCoding.addIndex("ucl_cloud", StatusPostProcessOp.STATUS_UCL_CLOUD, "");
        points[6] = new ColorPaletteDef.Point(StatusPostProcessOp.STATUS_UCL_CLOUD, Color.ORANGE, "ucl_cloud");

        targetProduct.getIndexCodingGroup().add(indexCoding);
        statusBand.setSampleCoding(indexCoding);
        statusBand.setImageInfo(new ImageInfo(new ColorPaletteDef(points, points.length)));
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) {

        final String commonLandExpr;
        if (landExpression != null && !landExpression.isEmpty()) {
            commonLandExpr = landExpression;
        } else {
            commonLandExpr = sensor.getLandExpr();
        }
        final String snowMaskExpression = "cloud_classif_flags.F_CLEAR_SNOW";

        BandMathsOp.BandDescriptor bdSnow = new BandMathsOp.BandDescriptor();
        bdSnow.name = "snow_mask";
        bdSnow.expression = snowMaskExpression;
        bdSnow.type = ProductData.TYPESTRING_INT8;

        BandMathsOp snowOp = new BandMathsOp();
        snowOp.setParameterDefaultValues();
        snowOp.setSourceProduct(sourceProduct);
        snowOp.setTargetBandDescriptors(bdSnow);
        Product snowMaskProduct = snowOp.getTargetProduct();

        configurator.defineSample(SRC_SNOW_MASK, snowMaskProduct.getBandAt(0).getName(), snowMaskProduct);

        int ancillaryIndex = SRC_VZA;
        for (int i=0; i<sensor.getAncillaryBandNames().length; i++) {
            configurator.defineSample(ancillaryIndex++, sensor.getAncillaryBandNames()[i]);
        }

        BandMathsOp.BandDescriptor bdLand = new BandMathsOp.BandDescriptor();
        bdLand.name = "land_mask";
        bdLand.expression = commonLandExpr;
        bdLand.type = ProductData.TYPESTRING_INT8;

        BandMathsOp landOp = new BandMathsOp();
        landOp.setParameterDefaultValues();
        landOp.setSourceProduct(sourceProduct);
        landOp.setTargetBandDescriptors(bdLand);
        Product landMaskProduct = landOp.getTargetProduct();

        configurator.defineSample(SRC_LAND_MASK, landMaskProduct.getBandAt(0).getName(), landMaskProduct);

        for (int i = 0; i < sensor.getToaBandNames().length; i++) {
            configurator.defineSample(SRC_TOA_RFL + i, sensor.getToaBandNames()[i], sourceProduct);
        }
        SRC_TOA_VAR = SRC_TOA_RFL + sensor.getToaBandNames().length;

        ImageVarianceOp imageVarianceOp = new ImageVarianceOp();
        imageVarianceOp.setParameterDefaultValues();
        imageVarianceOp.setSourceProduct(sourceProduct);
        imageVarianceOp.setParameter("sensor", sensor);
        Product varianceProduct = imageVarianceOp.getTargetProduct();

        for (int i = 0; i < sensor.getToaBandNames().length; i++) {
            configurator.defineSample(SRC_TOA_VAR + i, sensor.getToaBandNames()[i], varianceProduct);
        }
        if (sdrOnly) {
            SRC_STATUS = SRC_TOA_RFL + sensor.getToaBandNames().length * 2;

            String statusExpression = sensor.getL1InvalidExpr() + " ? 0 : (cloud_classif_flags.F_CLOUD ? 4 :" +
                    "((cloud_classif_flags.F_CLEAR_SNOW) ? 3 :" +
                    "((cloud_classif_flags.F_WATER) ? 2 : 1)))";
            BandMathsOp.BandDescriptor statusBd = new BandMathsOp.BandDescriptor();
            statusBd.name = "status";
            statusBd.expression = statusExpression;
            statusBd.type = ProductData.TYPESTRING_INT8;

            BandMathsOp bandMathsOp = new BandMathsOp();
            bandMathsOp.setParameterDefaultValues();
            bandMathsOp.setTargetBandDescriptors(statusBd);
            bandMathsOp.setSourceProduct(sourceProduct);
            Product statusProduct = bandMathsOp.getTargetProduct();

            configurator.defineSample(SRC_STATUS, "status", statusProduct);
        }

    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) {
        if (sdrOnly) {
            int index = 0;
            for (int i = 0; i < sensor.getSdrBandNames().length; i++) {
                configurator.defineSample(index++, sensor.getSdrBandNames()[i]);
            }
            for (int i = 0; i < sensor.getSdrErrorBandNames().length; i++) {
                configurator.defineSample(index++, sensor.getSdrErrorBandNames()[i]);
            }
            configurator.defineSample(index++, "ndvi");
            configurator.defineSample(index++, "aod");
            configurator.defineSample(index, "status");
        } else {
            configurator.defineSample(0, "BB_VIS");
            configurator.defineSample(1, "BB_NIR");
            configurator.defineSample(2, "BB_SW");

            configurator.defineSample(TRG_ERRORS, "sig_BB_VIS_VIS");
            configurator.defineSample(TRG_ERRORS + 1, "sig_BB_VIS_NIR");
            configurator.defineSample(TRG_ERRORS + 2, "sig_BB_VIS_SW");
            configurator.defineSample(TRG_ERRORS + 3, "sig_BB_NIR_NIR");
            configurator.defineSample(TRG_ERRORS + 4, "sig_BB_NIR_SW");
            configurator.defineSample(TRG_ERRORS + 5, "sig_BB_SW_SW");

            configurator.defineSample(TRG_KERN, "Kvol_BRDF_VIS");
            configurator.defineSample(TRG_KERN + 1, "Kgeo_BRDF_VIS");
            configurator.defineSample(TRG_KERN + 2, "Kvol_BRDF_NIR");
            configurator.defineSample(TRG_KERN + 3, "Kgeo_BRDF_NIR");
            configurator.defineSample(TRG_KERN + 4, "Kvol_BRDF_SW");
            configurator.defineSample(TRG_KERN + 5, "Kgeo_BRDF_SW");

            configurator.defineSample(TRG_NDVI, "NDVI");
            configurator.defineSample(TRG_NDVI + 1, "sig_NDVI");

            configurator.defineSample(TRG_VZA, "VZA");
            configurator.defineSample(TRG_SZA, "SZA");
            configurator.defineSample(TRG_RAA, "RAA");
            configurator.defineSample(TRG_DEM, "DEM");
            configurator.defineSample(TRG_SNOW, "snow_mask");
            configurator.defineSample(TRG_AOD, "AOD550");
            configurator.defineSample(TRG_AODERR, "sig_AOD550");
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AbstractBbdrOp.class);
        }
    }
}

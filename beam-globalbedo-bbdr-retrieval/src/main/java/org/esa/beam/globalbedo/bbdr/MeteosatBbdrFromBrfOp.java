/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;

import java.util.HashMap;

import static java.lang.StrictMath.toRadians;

/**
 * Computes BBDRs for METEOSAT MVIRI/SEVIRI as agreed with PL, JPM, AL:
 *  - uses geometry computation taken from Eumetsat C code
 *  - broadband BRF is directly 'BB SW'. No BB VIS NIR.
 *  - kernels: same as for AVHRR, neglect Nsky and coupling
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.bbdr.meteosat",
                  description = "Computes BBDRs for Meteosat MVIRI/SEVIRI/GMS/GOES",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2011 by Brockmann Consult")
public class MeteosatBbdrFromBrfOp extends PixelOperator {

    protected static final int SRC_SPECTRAL_BRF = 0;
    protected static final int SRC_SIGMA_SPECTRAL_BRF = 1;
    protected static final int SRC_BROADBAND_BRF = 2;
    protected static final int SRC_SIGMA_BROADBAND_BRF = 3;
    protected static final int SRC_LAT = 4;
    protected static final int SRC_LON = 5;
//    protected static final int SRC_WATERMASK = 6;
    protected static final int SRC_AVHRR_MSSL_FLAG = 7;

    protected static final int TRG_BB_VIS = 0;
    protected static final int TRG_BB_NIR = 1;
    protected static final int TRG_BB_SW = 2;
    protected static final int TRG_sig_BB_VIS_VIS = 3;
    protected static final int TRG_sig_BB_VIS_NIR = 4;
    protected static final int TRG_sig_BB_VIS_SW = 5;
    protected static final int TRG_sig_BB_NIR_NIR = 6;
    protected static final int TRG_sig_BB_NIR_SW = 7;
    protected static final int TRG_sig_BB_SW_SW = 8;
    protected static final int TRG_VZA = 9;
    protected static final int TRG_SZA = 10;
    protected static final int TRG_RAA = 11;
    protected static final int TRG_SNOW = 12;
    protected static final int TRG_KERN = 13;
    protected static final int TRG_AVHRR_MSSL_SNAP = 19;

    public static final double METEOSAT_DEG_LAT = 0.0;

    private static final double SAT_DEG_LON = 0.0;
    private static final double SCAN_TIME = 12.0;

    @Parameter(description = "Sensor", valueSet = {"MVIRI", "SEVIRI", "GMS", "GOES_E", "GOES_W"}, defaultValue = "MVIRI")
    protected MeteosatSensor sensor;

    @SourceProduct(alias = "brf", description = "The BRF source product")
    protected Product sourceProduct;

    @SourceProduct(alias = "avhrrmask", description = "AVHRR mask product provided by SK")
    private Product avhrrMaskProduct;


    private int year;
    private int doy;

//    private Product waterMaskProduct;

    @Override
    protected void prepareInputs() throws OperatorException {
        final double[] inputParmsFromFilename = extractDoyYearFromFilename(sourceProduct.getName());
        doy = (int) inputParmsFromFilename[0];
        year = (int) inputParmsFromFilename[1];

//        HashMap<String, Object> waterParameters = new HashMap<>();
//        waterParameters.put("resolution", 150);
//        waterParameters.put("subSamplingFactorX", 1);
//        waterParameters.put("subSamplingFactorY", 1);
//        waterMaskProduct = GPF.createProduct("LandWaterMask", waterParameters, sourceProduct);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

//        final int waterFraction = sourceSamples[SRC_WATERMASK].getInt();
//        if (waterFraction > 0) {
//              // Only compute over land. Ignore clouds.
//            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
//            return;
//        }

        double broadbandBrf = sourceSamples[SRC_BROADBAND_BRF].getDouble();
        double sigmaBroadbandBrf = sourceSamples[SRC_SIGMA_BROADBAND_BRF].getDouble();

        final int avhrrMsslFlag = sourceSamples[SRC_AVHRR_MSSL_FLAG].getInt();

        if (BbdrUtils.isBrfInputInvalid(broadbandBrf, sigmaBroadbandBrf)) {
            // not meaningful values
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            computeAvhrrMsslSnapFlag(avhrrMsslFlag, targetSamples);
            return;
        }

        final boolean isCloud = AvhrrMsslFlag.isCloud(avhrrMsslFlag);
        final boolean isSnow = AvhrrMsslFlag.isSnow(avhrrMsslFlag);
        final boolean isClearLand = AvhrrMsslFlag.isClearLand(avhrrMsslFlag);
        final boolean isSea = !isClearLand && !isCloud && !isSnow;
        if (isSea || isCloud) {
            // only compute over clear land
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            computeAvhrrMsslSnapFlag(avhrrMsslFlag, targetSamples);
            return;
        }

        computeAvhrrMsslSnapFlag(avhrrMsslFlag, targetSamples);

        final double degLat = sourceSamples[SRC_LAT].getDouble();
        final double degLon = getDegLon(sourceSamples[SRC_LON]);

        final double sza = MeteosatGeometry.computeSunAngles(degLat, degLon, SAT_DEG_LON, doy, year, SCAN_TIME).getZenith();
        final double vza = MeteosatGeometry.computeViewAngles(METEOSAT_DEG_LAT, SAT_DEG_LON, degLat, degLon, sensor).getZenith();
        final double saa = MeteosatGeometry.computeSunAngles(degLat, degLon, SAT_DEG_LON, doy, year, SCAN_TIME).getAzimuth();
        final double vaa = MeteosatGeometry.computeViewAngles(METEOSAT_DEG_LAT, SAT_DEG_LON, degLat, degLon, sensor).getAzimuth();
        final double phi = Math.abs(saa - vaa);

        // todo: clarify if it is correct to just set all VIS and NIR to 0.0
        targetSamples[TRG_BB_VIS].set(0.0);
        targetSamples[TRG_BB_NIR].set(0.0);
        targetSamples[TRG_BB_SW].set(broadbandBrf);
        targetSamples[TRG_sig_BB_VIS_VIS].set(0.0);
        targetSamples[TRG_sig_BB_VIS_NIR].set(0.0);
        targetSamples[TRG_sig_BB_VIS_SW].set(0.0);
        targetSamples[TRG_sig_BB_NIR_NIR].set(0.0);
        targetSamples[TRG_sig_BB_NIR_SW].set(0.0);
        targetSamples[TRG_sig_BB_SW_SW].set(sigmaBroadbandBrf);
        targetSamples[TRG_VZA].set(vza);
        targetSamples[TRG_SZA].set(sza);
        targetSamples[TRG_RAA].set(phi);

        // set VIS and NIR to the same as SW (preliminary suggestion by SK, 20160603):
        targetSamples[TRG_BB_VIS].set(broadbandBrf);
        targetSamples[TRG_BB_NIR].set(broadbandBrf);
        targetSamples[TRG_sig_BB_VIS_VIS].set(sigmaBroadbandBrf);
        targetSamples[TRG_sig_BB_VIS_NIR].set(sigmaBroadbandBrf*sigmaBroadbandBrf);
        targetSamples[TRG_sig_BB_VIS_SW].set(sigmaBroadbandBrf*sigmaBroadbandBrf);
        targetSamples[TRG_sig_BB_NIR_NIR].set(sigmaBroadbandBrf);
        targetSamples[TRG_sig_BB_NIR_SW].set(sigmaBroadbandBrf*sigmaBroadbandBrf);

        // calculation of kernels (kvol, kgeo)

        final double vzaRad = toRadians(vza);
        final double szaRad = toRadians(sza);
        final double phiRad = toRadians(phi);

        final double[] kernels = BbdrUtils.computeConstantKernels(vzaRad, szaRad, phiRad);
        final double kvol = kernels[0];
        final double kgeo = kernels[1];

        for (int i_bb = 0; i_bb < BbdrConstants.N_SPC; i_bb++) {
            // for AVHRR, neglect Nsky, coupling terms and weighting (PL, 20160520)
            targetSamples[TRG_KERN + (i_bb * 2)].set(kvol);
            targetSamples[TRG_KERN + (i_bb * 2) + 1].set(kgeo);
        }

        // we take snow from MSSL AVHRR mask
        targetSamples[TRG_SNOW].set(isSnow ? 1 : 0);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();

        for (String bbBandName : BbdrConstants.BB_BAND_NAMES) {
            targetProduct.addBand(bbBandName, ProductData.TYPE_FLOAT32);
        }
        for (String bbSigmaBandName : BbdrConstants.BB_SIGMA_BAND_NAMES) {
            targetProduct.addBand(bbSigmaBandName, ProductData.TYPE_FLOAT32);
        }
        for (String bbKernelBandName : BbdrConstants.BB_KERNEL_BAND_NAMES) {
            targetProduct.addBand(bbKernelBandName, ProductData.TYPE_FLOAT32);
        }
        targetProduct.addBand("VZA", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("SZA", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("RAA", ProductData.TYPE_FLOAT32);

        for (Band band : targetProduct.getBands()) {
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }

        targetProduct.addBand("snow_mask", ProductData.TYPE_INT8);

        // set up also an AVHRR MSSL mask flag band
        final Band avhrrMsslMaskFlagBand = targetProduct.addBand("AVHRR_MSSL_FLAG_snap", ProductData.TYPE_INT8);
        FlagCoding avhrrMsslMaskFlagCoding = AvhrrMsslFlag.createAvhrrMsslMaskFlagCoding("AVHRR_MSSL_FLAG_snap_flag");
        avhrrMsslMaskFlagBand.setSampleCoding(avhrrMsslMaskFlagCoding);
        targetProduct.getFlagCodingGroup().add(avhrrMsslMaskFlagCoding);
        AvhrrMsslFlag.setupAvhrrMsslBitmasks(0, targetProduct);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {
        configurator.defineSample(SRC_SPECTRAL_BRF, "spectral_brf", sourceProduct);
        configurator.defineSample(SRC_SIGMA_SPECTRAL_BRF, "spectral_brf_err", sourceProduct);
        configurator.defineSample(SRC_BROADBAND_BRF, "broadband_brf", sourceProduct);
        configurator.defineSample(SRC_SIGMA_BROADBAND_BRF, "broadband_brf_err", sourceProduct);
        configurator.defineSample(SRC_LAT, "lat", sourceProduct);
        configurator.defineSample(SRC_LON, "lon", sourceProduct);
//        configurator.defineSample(SRC_WATERMASK, "land_water_fraction", waterMaskProduct);

        configurator.defineSample(SRC_AVHRR_MSSL_FLAG, "mask", avhrrMaskProduct);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        configurator.defineSample(TRG_BB_VIS, "BB_VIS");
        configurator.defineSample(TRG_BB_NIR, "BB_NIR");
        configurator.defineSample(TRG_BB_SW, "BB_SW");

        configurator.defineSample(TRG_sig_BB_VIS_VIS, "sig_BB_VIS_VIS");
        configurator.defineSample(TRG_sig_BB_VIS_NIR, "sig_BB_VIS_NIR");
        configurator.defineSample(TRG_sig_BB_VIS_SW, "sig_BB_VIS_SW");
        configurator.defineSample(TRG_sig_BB_NIR_NIR, "sig_BB_NIR_NIR");
        configurator.defineSample(TRG_sig_BB_NIR_SW, "sig_BB_NIR_SW");
        configurator.defineSample(TRG_sig_BB_SW_SW, "sig_BB_SW_SW");

        configurator.defineSample(TRG_VZA, "VZA");
        configurator.defineSample(TRG_SZA, "SZA");
        configurator.defineSample(TRG_RAA, "RAA");
        configurator.defineSample(TRG_SNOW, "snow_mask");

        configurator.defineSample(TRG_KERN, "Kvol_BRDF_VIS");
        configurator.defineSample(TRG_KERN + 1, "Kgeo_BRDF_VIS");
        configurator.defineSample(TRG_KERN + 2, "Kvol_BRDF_NIR");
        configurator.defineSample(TRG_KERN + 3, "Kgeo_BRDF_NIR");
        configurator.defineSample(TRG_KERN + 4, "Kvol_BRDF_SW");
        configurator.defineSample(TRG_KERN + 5, "Kgeo_BRDF_SW");

        configurator.defineSample(TRG_AVHRR_MSSL_SNAP, "AVHRR_MSSL_FLAG_snap");
    }

    private void computeAvhrrMsslSnapFlag(int srcValue, WritableSample[] targetSamples) {
        targetSamples[TRG_AVHRR_MSSL_SNAP].set(AvhrrMsslFlag.CLEAR_LAND_BIT_INDEX, AvhrrMsslFlag.isClearLand(srcValue));
        targetSamples[TRG_AVHRR_MSSL_SNAP].set(AvhrrMsslFlag.CLOUD_BIT_INDEX, AvhrrMsslFlag.isCloud(srcValue));
        targetSamples[TRG_AVHRR_MSSL_SNAP].set(AvhrrMsslFlag.SNOW_BIT_INDEX, AvhrrMsslFlag.isSnow(srcValue));
    }


    /**
     * Extracts parameters from source product name which are required for BRF --> BBDR conversion
     *
     * @param sourceProductName - source product name
     *
     * @return double[] input parameters
     */
    static double[] extractDoyYearFromFilename(String sourceProductName) {
        // filenames are like: W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET7+MVIRI_C_BRF_EUMP_20050501000000_h18v04
        // or W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET8+SEVIRI_HRVIS_000_C_BRF_EUMP_20060701000000_h18v06

        int year;
        int doy;
        if (sourceProductName.contains("BRF_EUMP_")) {
            // e.g. BRF_EUMP_20050501
            final int dateSubstringStart = sourceProductName.indexOf("BRF_EUMP_") + 9;
            year = Integer.parseInt(sourceProductName.substring(dateSubstringStart, dateSubstringStart+4));
            int month = Integer.parseInt(sourceProductName.substring(dateSubstringStart+4, dateSubstringStart+6));
            int day = Integer.parseInt(sourceProductName.substring(dateSubstringStart+6, dateSubstringStart+8));
            doy = MeteosatGeometry.getDoyFromYearMonthDay(year, month -1, day);
        } else {
            throw  new OperatorException("Source product '" + sourceProductName + "' not supported - invalid name.");
        }

        return new double[]{doy, year};
    }

    private double getDegLon(Sample sourceSample) {
        double degLon = sourceSample.getDouble();

        if (sourceProduct.getName().contains("VIRI_057_C_BRF") || sourceProduct.getName().contains("VIRI_HRVIS_057_C_BRF")) {
            // shift longitude according to shift of satDegLon from 57 to 0 for angle computation
            degLon -= 57.0;
        }
        if (sourceProduct.getName().contains("VIRI_063_C_BRF") || sourceProduct.getName().contains("VIRI_HRVIS_063_C_BRF")) {
            // shift longitude according to shift of satDegLon from 63 to 0 for angle computation
            degLon -= 63.0;
        }
        if (sensor == MeteosatSensor.GOES_E) {
            // shift longitude according to shift of satDegLon from -75 to 0 for angle computation
            degLon += 75.0;
        }
        if (sensor == MeteosatSensor.GOES_W) {
            // shift longitude according to shift of satDegLon from -135 to 0 for angle computation
            degLon += 135.0;
        }
        if (sensor == MeteosatSensor.GMS) {
            // shift longitude according to shift of satDegLon from 140 to 0 for angle computation
            degLon -= 140.0;
        }
        return degLon;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MeteosatBbdrFromBrfOp.class);
        }
    }
}

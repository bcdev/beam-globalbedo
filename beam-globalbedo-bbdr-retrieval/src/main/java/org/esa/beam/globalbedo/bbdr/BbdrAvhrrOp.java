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
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.util.BitSetter;

import static java.lang.StrictMath.toRadians;

/**
 * Generates a GlobAlbedo conform BBDR product from AVHRR-LTDR BRF product provided by JRC (NG)
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.bbdr.avhrr",
        description = "Computes BBDRs and kernel parameters for AVHRR",
        authors = "Marco Zuehlke, Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")
public class BbdrAvhrrOp extends PixelOperator {

    protected static final int SRC_BRF_1 = 0;
    protected static final int SRC_BRF_2 = 1;
    protected static final int SRC_SIGMA_BRF_1 = 2;
    protected static final int SRC_SIGMA_BRF_2 = 3;
    protected static final int SRC_TS = 4;
    protected static final int SRC_TV = 5;
    protected static final int SRC_PHI = 6;
    protected static final int SRC_LDTR_FLAG = 7;
    protected static final int SRC_AVHRR_MSSL_FLAG = 8;

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
    protected static final int TRG_LTDR_SNAP = 19;
    protected static final int TRG_AVHRR_MSSL_SNAP = 20;

    // two new parameters to ensure support for both new AVHRR BRF v5 (201707)as well as previous version
    @Parameter(defaultValue = "REL_PHI", valueSet = {"REL_PHI", "PHI"},
            description = "Name of relative sensor azimuth angle (was changed from 'PHI' to 'REL_PHI' in v5, 201707)")
    private String avhrrBrfPhiBandName;

    @Parameter(defaultValue = "LTDR_FLAG", valueSet = {"LTDR_FLAG", "LDTR_FLAG"},
            description = "Name of LTDR flag band (typo in band name was corrected in v5, 201707)")
    private String avhrrLtdrFlagBandName;


    @SourceProduct(alias = "brf", description = "The BRF source product")
    protected Product sourceProduct;

    @SourceProduct(description = "Prior product", optional = true)
    private Product priorProduct;

    @SourceProduct(alias = "avhrrmask", description = "AVHRR mask product provided by SK")
    private Product avhrrMaskProduct;


    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final double brf1 = sourceSamples[SRC_BRF_1].getDouble();
        final double brf2 = sourceSamples[SRC_BRF_2].getDouble();
        final double sigmaBrf1 = sourceSamples[SRC_SIGMA_BRF_1].getDouble();
        final double sigmaBrf2 = sourceSamples[SRC_SIGMA_BRF_2].getDouble();
        final int ldtrFlag = sourceSamples[SRC_LDTR_FLAG].getInt();
        final int avhrrMsslFlag = sourceSamples[SRC_AVHRR_MSSL_FLAG].getInt();

        if (BbdrUtils.isBrfInputInvalid(brf1, sigmaBrf1) || BbdrUtils.isBrfInputInvalid(brf2, sigmaBrf2)) {
            // not meaningful values
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            computeLtdrSnapFlag(ldtrFlag, targetSamples);
            computeAvhrrMsslSnapFlag(avhrrMsslFlag, targetSamples);
            return;
        }

        // decode ldtrFlag flag and extract cloud info, see emails from Mirko Marioni, 20160513:
//        final boolean isCloud = BitSetter.isFlagSet(ldtrFlag, 1);
        final boolean isCloud = AvhrrMsslFlag.isCloud(avhrrMsslFlag);
        final boolean isCloudShadow = BitSetter.isFlagSet(ldtrFlag, 2);
        final boolean isSnow = AvhrrMsslFlag.isSnow(avhrrMsslFlag);
        final boolean isClearLand = AvhrrMsslFlag.isClearLand(avhrrMsslFlag);
//        final boolean isSea = BitSetter.isFlagSet(ldtrFlag, 3);
        final boolean isSea = !isClearLand && !isCloud && !isSnow;
        final boolean isBrf1Invalid = BitSetter.isFlagSet(ldtrFlag, 8);    // NG/MM, Nov 2016
        final boolean isBrf2Invalid = BitSetter.isFlagSet(ldtrFlag, 9);    // NG/MM, Nov 2016
//        if (isSea || isCloud || isCloudShadow || isBrf1Invalid || isBrf2Invalid) {
        if (isSea || isCloud || isBrf1Invalid || isBrf2Invalid) {
            // only compute over clear land
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            computeLtdrSnapFlag(ldtrFlag, targetSamples);
            computeAvhrrMsslSnapFlag(avhrrMsslFlag, targetSamples);
            return;
        }

        computeLtdrSnapFlag(ldtrFlag, targetSamples);
        computeAvhrrMsslSnapFlag(avhrrMsslFlag, targetSamples);

        final double vza = sourceSamples[SRC_TV].getDouble();
        final double sza = sourceSamples[SRC_TS].getDouble();
        final double phi = sourceSamples[SRC_PHI].getDouble();

        // calculation of kernels (kvol, kgeo)

        final double vzaRad = toRadians(vza);
        final double szaRad = toRadians(sza);
        final double phiRad = toRadians(phi);

        final double[] kernels = BbdrUtils.computeConstantKernels(vzaRad, szaRad, phiRad);
        final double kvol = kernels[0];
        final double kgeo = kernels[1];

        // for conversion to broadband, use Liang coefficients (S.Liang, 2000, eq. (7)):
        double[] bb = new double[BbdrConstants.N_SPC];
        double[] sigmaBb = new double[BbdrConstants.N_SPC];
//        for (int i = 0; i < sigmaBb.length; i++) {
//            final double a = BbdrConstants.AVHRR_LIANG_COEFFS[i][0];
//            final double b = BbdrConstants.AVHRR_LIANG_COEFFS[i][1];
//            final double c = BbdrConstants.AVHRR_LIANG_COEFFS[i][2];
//            final double d = BbdrConstants.AVHRR_LIANG_COEFFS[i][3];
//            final double e = BbdrConstants.AVHRR_LIANG_COEFFS[i][4];
//            final double f = BbdrConstants.AVHRR_LIANG_COEFFS[i][5];
//            bb[i] = a * brf1 * brf1 + b * brf2 * brf2 + c * brf1 * brf2 + d * brf1 + e * brf2 + f;
//            sigmaBb[i] = Math.abs(2.0 * a * brf1 + c * brf2 + d) * Math.abs(sigmaBrf1) +
//                    Math.abs(2.0 * b * brf2 + c * brf1 + e) * Math.abs(sigmaBrf2);
//        }

        // now use instead Said new coeffs for AVHRR, 20170725: bb := a*brf1 + b*brf2 + c:
//        bb[0] = 0.6028 * brf1 + 0.0232;
//        bb[1] = 0.1312 * brf1 + 0.6955 * brf2 + 0.1452;
//        bb[2] = 0.3754 * brf1 + 0.3334 * brf2 + 0.086;
//        sigmaBb[0] = 0.6028 * Math.abs(sigmaBrf1);
//        sigmaBb[1] = 0.1312 * Math.abs(sigmaBrf1) + 0.6955 * Math.abs(sigmaBrf2);
//        sigmaBb[2] = 0.3754 * Math.abs(sigmaBrf1) + 0.3334 * Math.abs(sigmaBrf2);

        // and we change again to new coefficients for AVHRR (SK 20170727, avhrr_modis_bb_coefficients.xls):
        for (int i = 0; i < sigmaBb.length; i++) {
            final double a = BbdrConstants.AVHRR_SAID_COEFFS[i][0];
            final double b = BbdrConstants.AVHRR_SAID_COEFFS[i][1];
            final double c = BbdrConstants.AVHRR_SAID_COEFFS[i][2];
            final double d = BbdrConstants.AVHRR_SAID_COEFFS[i][3];
            final double e = BbdrConstants.AVHRR_SAID_COEFFS[i][4];
            final double f = BbdrConstants.AVHRR_SAID_COEFFS[i][5];
            bb[i] = a * brf1 * brf1 + b * brf2 * brf2 + c * brf1 * brf2 + d * brf1 + e * brf2 + f;
            sigmaBb[i] = Math.abs(2.0 * a * brf1 + c * brf2 + d) * Math.abs(sigmaBrf1) +
                    Math.abs(2.0 * b * brf2 + c * brf1 + e) * Math.abs(sigmaBrf2);
        }

        final double sigmaBbVisVis = sigmaBb[0];
        final double sigmaBbVisNir = Math.sqrt(sigmaBb[0] * sigmaBb[1]);
        final double sigmaBbVisSw = Math.sqrt(sigmaBb[0] * sigmaBb[2]);
        final double sigmaBbNirNir = sigmaBb[1];
        final double sigmaBbNirSw = Math.sqrt(sigmaBb[1] * sigmaBb[2]);
        final double sigmaBbSwSw = sigmaBb[2];

        targetSamples[TRG_BB_VIS].set(bb[0]);
        targetSamples[TRG_BB_NIR].set(bb[1]);
        targetSamples[TRG_BB_SW].set(bb[2]);
        targetSamples[TRG_sig_BB_VIS_VIS].set(sigmaBbVisVis);
        targetSamples[TRG_sig_BB_VIS_NIR].set(sigmaBbVisNir);
        targetSamples[TRG_sig_BB_VIS_SW].set(sigmaBbVisSw);
        targetSamples[TRG_sig_BB_NIR_NIR].set(sigmaBbNirNir);
        targetSamples[TRG_sig_BB_NIR_SW].set(sigmaBbNirSw);
        targetSamples[TRG_sig_BB_SW_SW].set(sigmaBbSwSw);
        targetSamples[TRG_VZA].set(vza);
        targetSamples[TRG_SZA].set(sza);
        targetSamples[TRG_RAA].set(phi);
        targetSamples[TRG_SNOW].set(isSnow ? 1 : 0);

        for (int i_bb = 0; i_bb < BbdrConstants.N_SPC; i_bb++) {
            // for AVHRR, neglect Nsky, coupling terms and weighting (PL, 20160520)
            targetSamples[TRG_KERN + (i_bb * 2)].set(kvol);
            targetSamples[TRG_KERN + (i_bb * 2) + 1].set(kgeo);
        }
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
//        ProductUtils.copyBand("QA", sourceProduct, targetProduct, true);
        // new AVHRR BRF products from JRC, Oct 2016 (note the misspelling LDTR instead of LTDR!!):
//        ProductUtils.copyBand("LDTR_FLAG", sourceProduct, targetProduct, true);

        // set up also an LTDR flag band for SNAP
        final Band ltdrFlagSnapBand = targetProduct.addBand("LTDR_FLAG_snap", ProductData.TYPE_INT16);
        FlagCoding ltdrFlagCoding = AvhrrLtdrFlag.createLtdrFlagCoding("LTDR_FLAG_snap_flag");
        ltdrFlagSnapBand.setSampleCoding(ltdrFlagCoding);
        targetProduct.getFlagCodingGroup().add(ltdrFlagCoding);
        int bitmaskIndex = AvhrrLtdrFlag.setupLtdrBitmasks(0, targetProduct);

        // set up also an AVHRR MSSL mask flag band
        final Band avhrrMsslMaskFlagBand = targetProduct.addBand("AVHRR_MSSL_FLAG_snap", ProductData.TYPE_INT8);
        FlagCoding avhrrMsslMaskFlagCoding = AvhrrMsslFlag.createAvhrrMsslMaskFlagCoding("AVHRR_MSSL_FLAG_snap_flag");
        avhrrMsslMaskFlagBand.setSampleCoding(avhrrMsslMaskFlagCoding);
        targetProduct.getFlagCodingGroup().add(avhrrMsslMaskFlagCoding);
        AvhrrMsslFlag.setupAvhrrMsslBitmasks(bitmaskIndex, targetProduct);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {
        configurator.defineSample(SRC_BRF_1, "BRF_BAND_1", sourceProduct);
        configurator.defineSample(SRC_BRF_2, "BRF_BAND_2", sourceProduct);
        configurator.defineSample(SRC_SIGMA_BRF_1, "SIGMA_BRF_BAND_1", sourceProduct);
        configurator.defineSample(SRC_SIGMA_BRF_2, "SIGMA_BRF_BAND_2", sourceProduct);
        configurator.defineSample(SRC_TS, "TS", sourceProduct);
        configurator.defineSample(SRC_TV, "TV", sourceProduct);
        configurator.defineSample(SRC_PHI, avhrrBrfPhiBandName, sourceProduct);
        configurator.defineSample(SRC_LDTR_FLAG, avhrrLtdrFlagBandName, sourceProduct);

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

        configurator.defineSample(TRG_KERN, "Kvol_BRDF_VIS");
        configurator.defineSample(TRG_KERN + 1, "Kgeo_BRDF_VIS");
        configurator.defineSample(TRG_KERN + 2, "Kvol_BRDF_NIR");
        configurator.defineSample(TRG_KERN + 3, "Kgeo_BRDF_NIR");
        configurator.defineSample(TRG_KERN + 4, "Kvol_BRDF_SW");
        configurator.defineSample(TRG_KERN + 5, "Kgeo_BRDF_SW");

        configurator.defineSample(TRG_LTDR_SNAP, "LTDR_FLAG_snap");
        configurator.defineSample(TRG_AVHRR_MSSL_SNAP, "AVHRR_MSSL_FLAG_snap");

        configurator.defineSample(TRG_SNOW, "snow_mask");

    }

    private void computeLtdrSnapFlag(int srcValue, WritableSample[] targetSamples) {
        targetSamples[TRG_LTDR_SNAP].set(AvhrrLtdrFlag.SPARE_BIT_INDEX, AvhrrLtdrFlag.isSpare(srcValue));
        targetSamples[TRG_LTDR_SNAP].set(AvhrrLtdrFlag.CLOUD_BIT_INDEX, AvhrrLtdrFlag.isCloud(srcValue));
        targetSamples[TRG_LTDR_SNAP].set(AvhrrLtdrFlag.CLOUD_SHADOW_BIT_INDEX, AvhrrLtdrFlag.isCloudShadow(srcValue));
        targetSamples[TRG_LTDR_SNAP].set(AvhrrLtdrFlag.WATER_BIT_INDEX, AvhrrLtdrFlag.isWater(srcValue));
        targetSamples[TRG_LTDR_SNAP].set(AvhrrLtdrFlag.GLINT_BIT_INDEX, AvhrrLtdrFlag.isGlint(srcValue));
        targetSamples[TRG_LTDR_SNAP].set(AvhrrLtdrFlag.DARK_VEGETATION_BIT_INDEX, AvhrrLtdrFlag.isDarkVegetation(srcValue));
        targetSamples[TRG_LTDR_SNAP].set(AvhrrLtdrFlag.NIGHT_BIT_INDEX, AvhrrLtdrFlag.isNight(srcValue));
        targetSamples[TRG_LTDR_SNAP].set(AvhrrLtdrFlag.UNKNOWN_BIT_INDEX, AvhrrLtdrFlag.isUnknown(srcValue));
        targetSamples[TRG_LTDR_SNAP].set(AvhrrLtdrFlag.CHANNEL_1_INVALID_BIT_INDEX, AvhrrLtdrFlag.isChannel1Invalid(srcValue));
        targetSamples[TRG_LTDR_SNAP].set(AvhrrLtdrFlag.CHANNEL_2_INVALID_BIT_INDEX, AvhrrLtdrFlag.isChannel2Invalid(srcValue));
        targetSamples[TRG_LTDR_SNAP].set(AvhrrLtdrFlag.BRDF_CORR_ISSUES_BIT_INDEX, AvhrrLtdrFlag.isBrdfCorrIssues(srcValue));
        targetSamples[TRG_LTDR_SNAP].set(AvhrrLtdrFlag.POLAR_BIT_INDEX, AvhrrLtdrFlag.isPolar(srcValue));

    }

    private void computeAvhrrMsslSnapFlag(int srcValue, WritableSample[] targetSamples) {
        targetSamples[TRG_AVHRR_MSSL_SNAP].set(AvhrrMsslFlag.CLEAR_LAND_BIT_INDEX, AvhrrMsslFlag.isClearLand(srcValue));
        targetSamples[TRG_AVHRR_MSSL_SNAP].set(AvhrrMsslFlag.CLOUD_BIT_INDEX, AvhrrMsslFlag.isCloud(srcValue));
        targetSamples[TRG_AVHRR_MSSL_SNAP].set(AvhrrMsslFlag.SNOW_BIT_INDEX, AvhrrMsslFlag.isSnow(srcValue));
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BbdrAvhrrOp.class);
        }
    }
}

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
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.util.BitSetter;
import org.esa.beam.util.ProductUtils;

import static java.lang.Math.pow;
import static java.lang.StrictMath.toRadians;
import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS;

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
    protected static final int TRG_KERN = 12;
    protected static final int TRG_LTDR_SNAP = 18;

    public static final int[][] SRC_PRIOR_MEAN = new int[NUM_ALBEDO_PARAMETERS][NUM_ALBEDO_PARAMETERS];

    public static final int[][] SRC_PRIOR_SD = new int[NUM_ALBEDO_PARAMETERS][NUM_ALBEDO_PARAMETERS];

    public static final int SOURCE_SAMPLE_OFFSET = 0;  // this value must be >= number of bands in a source product
    public static final int PRIOR_OFFSET = (int) pow(NUM_ALBEDO_PARAMETERS, 2.0);
    public static final int SRC_PRIOR_NSAMPLES = 2 * PRIOR_OFFSET;

    public static final int SRC_PRIOR_MASK = SOURCE_SAMPLE_OFFSET + 2 * PRIOR_OFFSET + 1;


    @Parameter(defaultValue = "true", description = "Use prior information")
    private boolean usePrior;

    @Parameter(defaultValue = "6", description = "Prior version (MODIS collection)")
    private int priorVersion;

    @Parameter(defaultValue = "30.0", description = "Prior scale factor")
    private double priorScaleFactor;

    @Parameter(defaultValue = "BRDF_Albedo_Parameters_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    private String priorMeanBandNamePrefix;

    @Parameter(defaultValue = "BRDF_Albedo_Parameters_", description = "Prefix of prior SD band (default fits to the latest prior version)")
    private String priorSdBandNamePrefix;

    @Parameter(defaultValue = "7", description = "Prior broad bands start index (no longer needed for Collection 6 priors)")
    private int priorBandStartIndex;

    @Parameter(defaultValue = "BRDF_Albedo_Parameters_nir_wns", description = "Prior NSamples band name (default fits to the latest prior version)")
    private String priorNSamplesBandName;

    @Parameter(defaultValue = "snowFraction", description = "Prior data mask band name (default fits to the latest prior version)")
    private String priorDataMaskBandName;


    @SourceProduct
    protected Product sourceProduct;

    @SourceProduct(description = "Prior product", optional = true)
    private Product priorProduct;


    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final double brf1 = sourceSamples[SRC_BRF_1].getDouble();
        final double brf2 = sourceSamples[SRC_BRF_2].getDouble();
        final double sigmaBrf1 = sourceSamples[SRC_SIGMA_BRF_1].getDouble();
        final double sigmaBrf2 = sourceSamples[SRC_SIGMA_BRF_2].getDouble();
        final int ldtrFlag = sourceSamples[SRC_LDTR_FLAG].getInt();

        if (BbdrUtils.isBrfInputInvalid(brf1, sigmaBrf1) || BbdrUtils.isBrfInputInvalid(brf2, sigmaBrf2)) {
            // not meaningful values
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            computeLtdrSnapFlag(ldtrFlag, targetSamples);
            return;
        }

        // decode ldtrFlag flag and extract cloud info, see emails from Mirko Marioni, 20160513:
        final boolean isCloud = BitSetter.isFlagSet(ldtrFlag, 1);
        final boolean isCloudShadow = BitSetter.isFlagSet(ldtrFlag, 2);
        final boolean isSea = BitSetter.isFlagSet(ldtrFlag, 3);
        final boolean isBrf1Invalid = BitSetter.isFlagSet(ldtrFlag, 8);    // NG/MM, Nov 2016
        final boolean isBrf2Invalid = BitSetter.isFlagSet(ldtrFlag, 9);    // NG/MM, Nov 2016
        if (isSea || isCloud || isCloudShadow || isBrf1Invalid || isBrf2Invalid) {
            // only compute over clear land
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            computeLtdrSnapFlag(ldtrFlag, targetSamples);
            return;
        }

        computeLtdrSnapFlag(ldtrFlag, targetSamples);

        // JRC swapped the angles in input products, so correct here. todo: remove this when JRC delivered correct data
//        final double vza = sourceSamples[SRC_TS].getDouble();
//        final double sza = sourceSamples[SRC_TV].getDouble();
        final double vza = sourceSamples[SRC_TV].getDouble();  // done now
        final double sza = sourceSamples[SRC_TS].getDouble();
        final double phi = sourceSamples[SRC_PHI].getDouble();

        // calculation of kernels (kvol, kgeo)

        final double vzaRad = toRadians(vza);
        final double szaRad = toRadians(sza);
        final double phiRad = toRadians(phi);

        final double[] kernels = BbdrUtils.computeConstantKernels(vzaRad, szaRad, phiRad);
        final double kvol = kernels[0];
        final double kgeo = kernels[1];

        // todo: new outlier detection from PL
//        final double
//
//        if (isOutlierFilter(x, y, brf1, priorParms, priorSd, kvol, kgeo) ||
//                isOutlierFilter(x, y, brf2, priorParms, priorSd, kvol, kgeo)) {
//            // not meaningful values
//            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
//            computeLtdrSnapFlag(ldtrFlag, targetSamples);
//            return;
//        }




        // for conversion to broadband, use Liang coefficients (S.Liang, 2000, eq. (7)):
        double[] bb = new double[BbdrConstants.N_SPC];
        double[] sigmaBb = new double[BbdrConstants.N_SPC];
        for (int i = 0; i < sigmaBb.length; i++) {
            final double a = BbdrConstants.AVHRR_LIANG_COEFFS[i][0];
            final double b = BbdrConstants.AVHRR_LIANG_COEFFS[i][1];
            final double c = BbdrConstants.AVHRR_LIANG_COEFFS[i][2];
            final double d = BbdrConstants.AVHRR_LIANG_COEFFS[i][3];
            final double e = BbdrConstants.AVHRR_LIANG_COEFFS[i][4];
            final double f = BbdrConstants.AVHRR_LIANG_COEFFS[i][5];
            bb[i] = a * brf1 * brf1 + b * brf2 * brf2 + c * brf1 * brf2 + d * brf1 + e * brf2 + f;
            sigmaBb[i] = Math.abs(2.0 * a * brf1 + c * brf2 + d) * Math.abs(sigmaBrf1) +
                    Math.abs(2.0 * b * brf2 + c * brf1 + e) * Math.abs(sigmaBrf2);
        }

        final double sigmaBbVisVis = sigmaBb[0];
        final double sigmaBbVisNir = Math.sqrt(sigmaBb[0]*sigmaBb[1]);
        final double sigmaBbVisSw = Math.sqrt(sigmaBb[0]*sigmaBb[2]);
        final double sigmaBbNirNir = sigmaBb[1];
        final double sigmaBbNirSw = Math.sqrt(sigmaBb[1]*sigmaBb[2]);
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
        ProductUtils.copyBand("QA", sourceProduct, targetProduct, true);
        // new AVHRR BRF products from JRC, Oct 2016 (note the misspelling LDTR instead of LTDR!!):
        ProductUtils.copyBand("LDTR_FLAG", sourceProduct, targetProduct, true);

        // set up also an LTDR flag band for SNAP
        final Band ltdrFlagSnapBand = targetProduct.addBand("LTDR_FLAG_snap", ProductData.TYPE_INT16);
        FlagCoding flagCoding = AvhrrLtdrFlag.createLtdrFlagCoding("LTDR_FLAG_snap_flag");
        ltdrFlagSnapBand.setSampleCoding(flagCoding);
        targetProduct.getFlagCodingGroup().add(flagCoding);
        AvhrrLtdrFlag.setupLtdrBitmasks(targetProduct);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {
        configurator.defineSample(SRC_BRF_1, "BRF_BAND_1", sourceProduct);
        configurator.defineSample(SRC_BRF_2, "BRF_BAND_2", sourceProduct);
        configurator.defineSample(SRC_SIGMA_BRF_1, "SIGMA_BRF_BAND_1", sourceProduct);
        configurator.defineSample(SRC_SIGMA_BRF_2, "SIGMA_BRF_BAND_2", sourceProduct);
        configurator.defineSample(SRC_TS, "TS", sourceProduct);
        configurator.defineSample(SRC_TV, "TV", sourceProduct);
        configurator.defineSample(SRC_PHI, "PHI", sourceProduct);
//        configurator.defineSample(SRC_QA, "QA", sourceProduct);
        // new AVHRR BRF products from JRC, Oct 2016 (note the misspelling LDTR instead of LTDR!!):
        configurator.defineSample(SRC_LDTR_FLAG, "LDTR_FLAG", sourceProduct);

        // prior product:
        // we have:
        // 3x3 mean, 3x3 SD, Nsamples, mask
        if (usePrior) {
            configurePriorSourceSamples(configurator);
        }
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

    }

    /**
     * New outlier detection for AVHRR as proposed by PL (Jan 2017)
     *
     * @param x - pixel x
     * @param y - pixel y
     * @param obs - BB value in either VIS, NIR or SW
     * @param priorParms - [f0, f1, f2] in either VIS, NIR or SW
     * @param priorSd - [sd0, sd1, sd2] in either VIS, NIR or SW
     * @param kvol - kvol kernel
     * @param kgeo - kgeo kernel
     *
     * @return boolean
     */
    boolean isOutlierFilter(int x, int y, double obs, double[] priorParms, double[] priorSd, double kvol, double kgeo) {
//        ==================
//        outlier pseudocode
//        ==================
//
//
//        cloud filtering
//        ------------------
//                assume we have obs avhrr
//
//        prior params = [f0,f1,f2]
//        prior sd        = [sd0,sd1,sd2]
//
//
//        fwd kernels = [1, k1, k2]
//        for the AVHRR angles
//
//        operations:
//        =========
//
//        # dot product -> scalar output
//        obs prediction = (prior params) dot (fwd kernels)
//
//
//        # scaled difference
//        delta = (  (obs prediction) - (obs avhrr)) / (prior sd)
//
//
//        We can decide how many SD to use for filtering, e.g. zthresh = 4? to
//        be conservative
//
//
//        comments
//                -----------------
//                so you will have a delta for each channel.
//
//
//        If |delta| > zthresh:
//
//        its probably an outlier ....
//
//        if sgn(delta) +ve in both channels, its probably a cloud
//        if sgn(delta) -ve in both channels, its probably a cloud shadow
//        if sgn(delta) are different ... probably its an outlier but I don't
//        know what it is ...
//
//
//        ==================
//        alternative outlier pseudocode
//                ==================
//        use a huber norm, which will be more robust to outliers ...

        final double obsPrediction = priorParms[0] + priorParms[1]*kvol + priorParms[2]*kgeo;
        final double sdMean = (priorSd[0] + priorSd[1] + priorSd[2])/3.0;   // todo: clarify with PL
        final double delta = (obsPrediction - obs)/sdMean;

        return false;
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

    private void configurePriorSourceSamples(SampleConfigurer configurator) {
        for (int i = 0; i < NUM_ALBEDO_PARAMETERS; i++) {
            if (priorVersion == 6) {
                // collection 6 , from Oct 2016
                for (int j = 0; j < NUM_ALBEDO_PARAMETERS; j++) {
                    // priorMeanBandNamePrefix = priorSdBandNamePrefix = 'BRDF_Albedo_Parameters_'
                    // BRDF_Albedo_Parameters_vis_f0_avr <--> MEAN_VIS_f0
                    // BRDF_Albedo_Parameters_nir_f0_avr <--> MEAN_NIR_f0
                    // BRDF_Albedo_Parameters_shortwave_f0_avr <--> MEAN_SW_f0
                    // same for f1, f2
                    final String meanBandName = priorMeanBandNamePrefix +
                            AlbedoInversionConstants.PRIOR_6_WAVE_BANDS[i] + "_f" + j + "_avr";
                    final int meanIndex = SRC_PRIOR_MEAN[i][j];
                    configurator.defineSample(meanIndex, meanBandName, priorProduct);

                    // BRDF_Albedo_Parameters_vis_f0_sd <--> sqrt (Cov_VIS_f0_VIS_f0)
                    // BRDF_Albedo_Parameters_nir_f0_sd <--> sqrt (Cov_NIR_f0_NIR_f0)
                    // BRDF_Albedo_Parameters_shortwave_f0_sd <--> sqrt (Cov_SW_f0_SW_f0)
                    // same for f1, f2
                    final String sdMeanBandName = priorMeanBandNamePrefix +
                            AlbedoInversionConstants.PRIOR_6_WAVE_BANDS[i] + "_f" + j + "_sd";
                    final int sdIndex = SRC_PRIOR_SD[i][j];
                    configurator.defineSample(sdIndex, sdMeanBandName, priorProduct);
                }
                configurator.defineSample(SRC_PRIOR_NSAMPLES, priorNSamplesBandName, priorProduct);
                configurator.defineSample(SRC_PRIOR_MASK, priorDataMaskBandName, priorProduct);
            } else {
                // collection 5 , before Oct 2016
                for (int j = 0; j < NUM_ALBEDO_PARAMETERS; j++) {
                    final String indexString = Integer.toString(priorBandStartIndex + i);
//                    final String meanBandName = "MEAN__BAND________" + i + "_PARAMETER_F" + j;
                    // 2014, e.g. MEAN:_BAND_7_PARAMETER_F1
//                    final String meanBandName = priorMeanBandNamePrefix + indexString + "_PARAMETER_F" + j;
                    // Oct. 2015 version, e.g. Mean_VIS_f0
                    final String meanBandName = priorMeanBandNamePrefix + AlbedoInversionConstants.BBDR_WAVE_BANDS[i] + "_f" + j;
                    final int meanIndex = SRC_PRIOR_MEAN[i][j];
                    configurator.defineSample(meanIndex, meanBandName, priorProduct);

//                    final String sdMeanBandName = "SD_MEAN__BAND________" + i + "_PARAMETER_F" + j;
                    // 2014, e.g. SD:_BAND_7_PARAMETER_F1
//                    final String sdMeanBandName = priorSdBandNamePrefix + indexString + "_PARAMETER_F" + j;
                    // Oct. 2015 version:
                    // SD:_BAND_7_PARAMETER_F0 --> now Cov_VIS_f0_VIS_f0
                    // SD:_BAND_7_PARAMETER_F1 --> now Cov_VIS_f1_VIS_f1
                    // SD:_BAND_7_PARAMETER_F2 --> now Cov_VIS_f2_VIS_f2
                    // SD:_BAND_8_PARAMETER_F0 --> now Cov_NIR_f0_NIR_f0
                    // SD:_BAND_8_PARAMETER_F1 --> now Cov_NIR_f1_NIR_f1
                    // SD:_BAND_8_PARAMETER_F2 --> now Cov_NIR_f2_NIR_f2
                    // SD:_BAND_9_PARAMETER_F0 --> now Cov_SW_f0_SW_f0
                    // SD:_BAND_9_PARAMETER_F1 --> now Cov_SW_f1_SW_f1
                    // SD:_BAND_9_PARAMETER_F2 --> now Cov_SW_f2_SW_f2
                    final String sdMeanBandName = priorSdBandNamePrefix +
                            AlbedoInversionConstants.BBDR_WAVE_BANDS[i] + "_f" + j + "_" +
                            AlbedoInversionConstants.BBDR_WAVE_BANDS[i] + "_f" + j;
                    final int sdIndex = SRC_PRIOR_SD[i][j];
                    configurator.defineSample(sdIndex, sdMeanBandName, priorProduct);
                }
//            configurator.defineSample(SRC_PRIOR_NSAMPLES, PRIOR_NSAMPLES_NAME, priorProduct);
                configurator.defineSample(SRC_PRIOR_NSAMPLES, priorNSamplesBandName, priorProduct);
//            configurator.defineSample(SRC_PRIOR_MASK, PRIOR_MASK_NAME, priorProduct);
                configurator.defineSample(SRC_PRIOR_MASK, priorDataMaskBandName, priorProduct);
            }
        }
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BbdrAvhrrOp.class);
        }
    }
}

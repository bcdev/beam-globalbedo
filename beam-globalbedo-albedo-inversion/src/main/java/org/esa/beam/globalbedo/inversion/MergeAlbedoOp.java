package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;

/**
 * Operator for merging Albedo Snow/NoSnow products.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.albedo.mergealbedo",
        description = "Merges Albedo Snow/NoSnow products. Can be applied to tiles or mosaics.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2017 by Brockmann Consult")
public class MergeAlbedoOp extends PixelOperator {

    // source samples:
    private String[] dhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] dhrAlphaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] dhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrAlphaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private String relEntropyBandName;
    private String weightedNumberOfSamplesBandName;
    private String goodnessOfFitBandName;
    private String dataMaskBandName;
    private String snowFractionBandName;
    private String szaBandName;

    private static int[][] SRC_DHR = new int[2][AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[][] SRC_DHR_ALPHA = new int[2][AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[][] SRC_DHR_SIGMA = new int[2][AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[][] SRC_BHR = new int[2][AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[][] SRC_BHR_ALPHA = new int[2][AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[][] SRC_BHR_SIGMA = new int[2][AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] SRC_WEIGHTED_NUM_SAMPLES = new int[2];
    private static int[] SRC_REL_ENTROPY = new int[2];
    private static int[] SRC_GOODNESS_OF_FIT = new int[2];
    private static int[] SRC_DATA_MASK = new int[2];
    private static int[] SRC_SZA = new int[2];
    private static int[] SRC_PRIOR_INFO_SNOW_FRACTION = new int[2];
    private static int[] SRC_PRIOR_INFO_LAND_WATER_MASK = new int[2];

    private static int[] TRG_DHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] TRG_DHR_ALPHA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] TRG_DHR_SIGMA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] TRG_BHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] TRG_BHR_ALPHA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] TRG_BHR_SIGMA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int TRG_WEIGHTED_NUM_SAMPLES = 18;
    private static final int TRG_SNOW_FRACTION = 19;
    private static final int TRG_REL_ENTROPY = 20;
    private static final int TRG_GOODNESS_OF_FIT = 21;
    private static final int TRG_DATA_MASK = 22;
    private static final int TRG_SZA = 23;

    @SourceProduct(description = "Albedo Snow product")
    private Product snowProduct;

    @SourceProduct(description = "Albedo NoSnow product")
    private Product noSnowProduct;

    @SourceProduct(description = "Prior Info NoSnow product", optional = true)
    private Product priorInfoNoSnowProduct;

    @SourceProduct(description = "Prior Info Snow product", optional = true)
    private Product priorInfoSnowProduct;


    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final double nSamplesNoSnowDataValue = sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[0]].getDouble();
        final double nSamplesNoSnow = AlbedoInversionUtils.isValid(nSamplesNoSnowDataValue) ? nSamplesNoSnowDataValue : 0.0;
        final double nSamplesSnowDataValue = sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[1]].getDouble();
        final double nSamplesSnow = AlbedoInversionUtils.isValid(nSamplesSnowDataValue) ? nSamplesSnowDataValue : 0.0;
        final double totalNSamples = nSamplesSnow + nSamplesNoSnow;

        double proportionNsamplesSnow;
        double proportionNsamplesNoSnow;

//        if (x == 80 && y == 250) {
//            System.out.println("x = " + x);
//        }

        final double priorLandWaterMaskDataValue = sourceSamples[SRC_PRIOR_INFO_LAND_WATER_MASK[0]].getDouble();
        if (priorLandWaterMaskDataValue != 1.0) {         // activate when products are ready
            setMergedBandsToNoData(targetSamples);
        } else {
            if (nSamplesNoSnow > 0 && nSamplesSnow > 0) {
                proportionNsamplesNoSnow = nSamplesNoSnow / totalNSamples;
                proportionNsamplesSnow = nSamplesSnow / totalNSamples;
                setMergedBands(sourceSamples, targetSamples, proportionNsamplesSnow, proportionNsamplesNoSnow);
            } else if (nSamplesNoSnow > 0) {
                setMergedBandsToNoSnowBands(sourceSamples, targetSamples);
            } else if (nSamplesSnow > 0) {
                setMergedBandsToSnowBands(sourceSamples, targetSamples);
            } else {
                setMergedBandsToPrior(sourceSamples, targetSamples);
            }
        }
    }

    private void setMergedBandsToNoSnowBands(Sample[] sourceSamples, WritableSample[] targetSamples) {
        setMergedBands(sourceSamples, targetSamples, 0.0, 1.0);
    }

    private void setMergedBandsToSnowBands(Sample[] sourceSamples, WritableSample[] targetSamples) {
        setMergedBands(sourceSamples, targetSamples, 1.0, 0.0);
    }

    private void setMergedBandsToPrior(Sample[] sourceSamples, WritableSample[] targetSamples) {
        setMergedBands(sourceSamples, targetSamples, 0.0, 0.0);
    }

    private void setMergedBands(Sample[] sourceSamples, WritableSample[] targetSamples, double proportionNsamplesSnow,
                                double proportionNsamplesNoSnow) {

        // sza from one of the data sets where available
        final double szaNoSnowDataValue = sourceSamples[SRC_SZA[0]].getDouble();
        final double szaSnowDataValue = sourceSamples[SRC_SZA[1]].getDouble();
        if (AlbedoInversionUtils.isValid(szaNoSnowDataValue)) {
            targetSamples[TRG_SZA].set(szaNoSnowDataValue);
        } else if (AlbedoInversionUtils.isValid(szaSnowDataValue)) {
            targetSamples[TRG_SZA].set(szaSnowDataValue);
        } else {
            targetSamples[TRG_SZA].set(AlbedoInversionConstants.NO_DATA_VALUE);
        }

        double priorSnowFractionNoSnowDataValue = 0.0;
        double priorSnowFractionSnowDataValue = 0.0;
        if (priorInfoNoSnowProduct != null && priorInfoSnowProduct != null) {
            priorSnowFractionNoSnowDataValue = sourceSamples[SRC_PRIOR_INFO_SNOW_FRACTION[0]].getDouble();
            priorSnowFractionSnowDataValue = sourceSamples[SRC_PRIOR_INFO_SNOW_FRACTION[1]].getDouble();
        }

        // BHR/DHR, alphas, sigmas
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetSamples[TRG_BHR[i]].
                    set(getWeightedMergeValue(sourceSamples[SRC_BHR[0][i]].getDouble(),
                                              sourceSamples[SRC_BHR[1][i]].getDouble(),
                                              proportionNsamplesNoSnow, proportionNsamplesSnow,
                                              priorSnowFractionNoSnowDataValue,
                                              priorSnowFractionSnowDataValue,
                                              szaNoSnowDataValue));
            targetSamples[TRG_DHR[i]].
                    set(getWeightedMergeValue(sourceSamples[SRC_DHR[0][i]].getDouble(),
                                              sourceSamples[SRC_DHR[1][i]].getDouble(),
                                              proportionNsamplesNoSnow, proportionNsamplesSnow,
                                              priorSnowFractionNoSnowDataValue,
                                              priorSnowFractionSnowDataValue,
                                              szaNoSnowDataValue));
            targetSamples[TRG_BHR_ALPHA[i]].
                    set(getWeightedMergeValue(sourceSamples[SRC_BHR_ALPHA[0][i]].getDouble(),
                                              sourceSamples[SRC_BHR_ALPHA[1][i]].getDouble(),
                                              proportionNsamplesNoSnow, proportionNsamplesSnow,
                                              priorSnowFractionNoSnowDataValue,
                                              priorSnowFractionSnowDataValue,
                                              szaNoSnowDataValue));
            targetSamples[TRG_DHR_ALPHA[i]].
                    set(getWeightedMergeValue(sourceSamples[SRC_DHR_ALPHA[0][i]].getDouble(),
                                              sourceSamples[SRC_DHR_ALPHA[1][i]].getDouble(),
                                              proportionNsamplesNoSnow, proportionNsamplesSnow,
                                              priorSnowFractionNoSnowDataValue,
                                              priorSnowFractionSnowDataValue,
                                              szaNoSnowDataValue));
            targetSamples[TRG_BHR_SIGMA[i]].
                    set(getWeightedMergeValue(sourceSamples[SRC_BHR_SIGMA[0][i]].getDouble(),
                                              sourceSamples[SRC_BHR_SIGMA[1][i]].getDouble(),
                                              proportionNsamplesNoSnow, proportionNsamplesSnow,
                                              priorSnowFractionNoSnowDataValue,
                                              priorSnowFractionSnowDataValue,
                                              szaNoSnowDataValue));
            targetSamples[TRG_DHR_SIGMA[i]].
                    set(getWeightedMergeValue(sourceSamples[SRC_DHR_SIGMA[0][i]].getDouble(),
                                              sourceSamples[SRC_DHR_SIGMA[1][i]].getDouble(),
                                              proportionNsamplesNoSnow, proportionNsamplesSnow,
                                              priorSnowFractionNoSnowDataValue,
                                              priorSnowFractionSnowDataValue,
                                              szaNoSnowDataValue));
        }

        // rel. entropy
        targetSamples[TRG_REL_ENTROPY].
                set(getWeightedMergeValue(sourceSamples[SRC_REL_ENTROPY[0]].getDouble(),
                                          sourceSamples[SRC_REL_ENTROPY[1]].getDouble(),
                                          proportionNsamplesNoSnow, proportionNsamplesSnow,
                                          priorSnowFractionNoSnowDataValue,
                                          priorSnowFractionSnowDataValue,
                                          szaNoSnowDataValue));

        // WNSamples
        targetSamples[TRG_WEIGHTED_NUM_SAMPLES].
                set(getWeightedMergeValue(sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[0]].getDouble(),
                                          sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[1]].getDouble(),
                                          proportionNsamplesNoSnow, proportionNsamplesSnow,
                                          priorSnowFractionNoSnowDataValue,
                                          priorSnowFractionSnowDataValue,
                                          szaNoSnowDataValue));

        // GoF
        targetSamples[TRG_GOODNESS_OF_FIT].
                set(getWeightedMergeValue(sourceSamples[SRC_GOODNESS_OF_FIT[0]].getDouble(),
                                          sourceSamples[SRC_GOODNESS_OF_FIT[1]].getDouble(),
                                          proportionNsamplesNoSnow, proportionNsamplesSnow,
                                          priorSnowFractionNoSnowDataValue,
                                          priorSnowFractionSnowDataValue,
                                          szaNoSnowDataValue));

        // data mask: 1.0 or 0.0
        final double maskNoSnowDataValue = sourceSamples[SRC_DATA_MASK[0]].getDouble();
        final double maskNoSnow = AlbedoInversionUtils.isValid(maskNoSnowDataValue) ? maskNoSnowDataValue : 0.0;
        final double maskSnowDataValue = sourceSamples[SRC_DATA_MASK[1]].getDouble();
        final double maskSnow = AlbedoInversionUtils.isValid(maskSnowDataValue) ? maskSnowDataValue : 0.0;
        final double maskMerged = Math.max(maskNoSnow, maskSnow);
        targetSamples[TRG_DATA_MASK].set(maskMerged);

        // fix of issue in WNSamples in snow/nosnow files if still present (20170922):
        if (maskMerged == 0.0) {
            targetSamples[TRG_WEIGHTED_NUM_SAMPLES].set(AlbedoInversionConstants.NO_DATA_VALUE);
        }

        // snow fraction is just proportionNsamplesSnow
        final double snowFraction = maskMerged > 0.0 && proportionNsamplesSnow > 0.0 ?
                proportionNsamplesSnow : AlbedoInversionConstants.NO_DATA_VALUE;

        targetSamples[TRG_SNOW_FRACTION].set(snowFraction);

    }

    private void setMergedBandsToNoData(WritableSample[] targetSamples) {
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetSamples[TRG_BHR[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            targetSamples[TRG_DHR[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            targetSamples[TRG_BHR_ALPHA[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            targetSamples[TRG_DHR_ALPHA[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            targetSamples[TRG_BHR_SIGMA[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            targetSamples[TRG_DHR_SIGMA[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
        }

        targetSamples[TRG_REL_ENTROPY].set(AlbedoInversionConstants.NO_DATA_VALUE);
        targetSamples[TRG_WEIGHTED_NUM_SAMPLES].set(AlbedoInversionConstants.NO_DATA_VALUE);
        targetSamples[TRG_GOODNESS_OF_FIT].set(AlbedoInversionConstants.NO_DATA_VALUE);
        targetSamples[TRG_DATA_MASK].set(0.0);
        targetSamples[TRG_WEIGHTED_NUM_SAMPLES].set(AlbedoInversionConstants.NO_DATA_VALUE);
        targetSamples[TRG_SNOW_FRACTION].set(AlbedoInversionConstants.NO_DATA_VALUE);
        targetSamples[TRG_SZA].set(AlbedoInversionConstants.NO_DATA_VALUE);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) throws OperatorException {

        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        dhrBandNames = IOUtils.getAlbedoDhrBandNames();
        int index = 0;
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_DHR[0][i] = index;
            SRC_DHR[1][i] = index + 100;
            index++;
            targetProduct.addBand(dhrBandNames[i], ProductData.TYPE_FLOAT32);
        }

        dhrAlphaBandNames = IOUtils.getAlbedoDhrAlphaBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_DHR_ALPHA[0][i] = index;
            SRC_DHR_ALPHA[1][i] = index + 100;
            index++;
            targetProduct.addBand(dhrAlphaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        dhrSigmaBandNames = IOUtils.getAlbedoDhrSigmaBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_DHR_SIGMA[0][i] = index;
            SRC_DHR_SIGMA[1][i] = index + 100;
            index++;
            targetProduct.addBand(dhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        bhrBandNames = IOUtils.getAlbedoBhrBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_BHR[0][i] = index;
            SRC_BHR[1][i] = index + 100;
            index++;
            targetProduct.addBand(bhrBandNames[i], ProductData.TYPE_FLOAT32);
        }

        bhrAlphaBandNames = IOUtils.getAlbedoBhrAlphaBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_BHR_ALPHA[0][i] = index;
            SRC_BHR_ALPHA[1][i] = index + 100;
            index++;
            targetProduct.addBand(bhrAlphaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        bhrSigmaBandNames = IOUtils.getAlbedoBhrSigmaBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_BHR_SIGMA[0][i] = index;
            SRC_BHR_SIGMA[1][i] = index + 100;
            index++;
            targetProduct.addBand(bhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        weightedNumberOfSamplesBandName = AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME;
        SRC_WEIGHTED_NUM_SAMPLES[0] = index;
        SRC_WEIGHTED_NUM_SAMPLES[1] = index + 100;
        index++;
        targetProduct.addBand(weightedNumberOfSamplesBandName, ProductData.TYPE_FLOAT32);

        SRC_REL_ENTROPY[0] = index;
        SRC_REL_ENTROPY[1] = index + 100;
        index++;
        relEntropyBandName = AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME;
        targetProduct.addBand(relEntropyBandName, ProductData.TYPE_FLOAT32);

        SRC_GOODNESS_OF_FIT[0] = index;
        SRC_GOODNESS_OF_FIT[1] = index + 100;
        index++;
        goodnessOfFitBandName = AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME;
        targetProduct.addBand(goodnessOfFitBandName, ProductData.TYPE_FLOAT32);

        SRC_DATA_MASK[0] = index;
        SRC_DATA_MASK[1] = index + 100;
        index++;
        dataMaskBandName = AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME;
        targetProduct.addBand(dataMaskBandName, ProductData.TYPE_FLOAT32);

        snowFractionBandName = AlbedoInversionConstants.ALB_SNOW_FRACTION_BAND_NAME;
        targetProduct.addBand(snowFractionBandName, ProductData.TYPE_FLOAT32);

        SRC_SZA[0] = index;
        SRC_SZA[1] = index + 100;
        index++;
        szaBandName = AlbedoInversionConstants.ALB_SZA_BAND_NAME;
        targetProduct.addBand(szaBandName, ProductData.TYPE_FLOAT32);

        SRC_PRIOR_INFO_SNOW_FRACTION[0] = index;
        SRC_PRIOR_INFO_SNOW_FRACTION[1] = index + 100;
        index++;

        SRC_PRIOR_INFO_LAND_WATER_MASK[0] = index;
        SRC_PRIOR_INFO_LAND_WATER_MASK[1] = index + 100;

        for (Band b : targetProduct.getBands()) {
            b.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
            b.setNoDataValueUsed(true);
        }

    }


    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {

        // source products:
        // for a number of source products, we have:
        // 3x DHR, 3x BHR, 3x DHR_sigma, 3x BHR_sigma, weightedNumSamples, relEntropy, goodnessOfFit,
        // snowFraction, datamask, SZA

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            configurator.defineSample(SRC_DHR[0][i], dhrBandNames[i], noSnowProduct);
            configurator.defineSample(SRC_DHR[1][i], dhrBandNames[i], snowProduct);
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            configurator.defineSample(SRC_BHR[0][i], bhrBandNames[i], noSnowProduct);
            configurator.defineSample(SRC_BHR[1][i], bhrBandNames[i], snowProduct);
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            configurator.defineSample(SRC_DHR_SIGMA[0][i], dhrSigmaBandNames[i], noSnowProduct);
            configurator.defineSample(SRC_DHR_SIGMA[1][i], dhrSigmaBandNames[i], snowProduct);
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            configurator.defineSample(SRC_BHR_SIGMA[0][i], bhrSigmaBandNames[i], noSnowProduct);
            configurator.defineSample(SRC_BHR_SIGMA[1][i], bhrSigmaBandNames[i], snowProduct);
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            configurator.defineSample(SRC_DHR_ALPHA[0][i], dhrAlphaBandNames[i], noSnowProduct);
            configurator.defineSample(SRC_DHR_ALPHA[1][i], dhrAlphaBandNames[i], snowProduct);
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            configurator.defineSample(SRC_BHR_ALPHA[0][i], bhrAlphaBandNames[i], noSnowProduct);
            configurator.defineSample(SRC_BHR_ALPHA[1][i], bhrAlphaBandNames[i], snowProduct);
        }

        configurator.defineSample(SRC_WEIGHTED_NUM_SAMPLES[0], weightedNumberOfSamplesBandName, noSnowProduct);
        configurator.defineSample(SRC_WEIGHTED_NUM_SAMPLES[1], weightedNumberOfSamplesBandName, snowProduct);
        configurator.defineSample(SRC_REL_ENTROPY[0], relEntropyBandName, noSnowProduct);
        configurator.defineSample(SRC_REL_ENTROPY[1], relEntropyBandName, snowProduct);
        configurator.defineSample(SRC_GOODNESS_OF_FIT[0], goodnessOfFitBandName, noSnowProduct);
        configurator.defineSample(SRC_GOODNESS_OF_FIT[1], goodnessOfFitBandName, snowProduct);
        configurator.defineSample(SRC_DATA_MASK[0], dataMaskBandName, noSnowProduct);
        configurator.defineSample(SRC_DATA_MASK[1], dataMaskBandName, snowProduct);
        configurator.defineSample(SRC_SZA[0], szaBandName, noSnowProduct);
        configurator.defineSample(SRC_SZA[1], szaBandName, snowProduct);

        if (priorInfoNoSnowProduct != null && priorInfoSnowProduct != null) {
            configurator.defineSample(SRC_PRIOR_INFO_SNOW_FRACTION[0],
                                      AlbedoInversionConstants.PRIOR_INFO_SNOW_FRACTION_BAND_NAME, priorInfoNoSnowProduct);
            configurator.defineSample(SRC_PRIOR_INFO_SNOW_FRACTION[1],
                                      AlbedoInversionConstants.PRIOR_INFO_SNOW_FRACTION_BAND_NAME, priorInfoSnowProduct);
            configurator.defineSample(SRC_PRIOR_INFO_LAND_WATER_MASK[0],
                                      AlbedoInversionConstants.PRIOR_INFO_LAND_WATER_MASK_BAND_NAME, priorInfoNoSnowProduct);
            configurator.defineSample(SRC_PRIOR_INFO_LAND_WATER_MASK[1],
                                      AlbedoInversionConstants.PRIOR_INFO_LAND_WATER_MASK_BAND_NAME, priorInfoSnowProduct);
        }
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        int index = 0;
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            TRG_DHR[i] = index;
            configurator.defineSample(TRG_DHR[i], dhrBandNames[i]);
            index++;
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            TRG_DHR_ALPHA[i] = index;
            configurator.defineSample(TRG_DHR_ALPHA[i], dhrAlphaBandNames[i]);
            index++;
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            TRG_DHR_SIGMA[i] = index;
            configurator.defineSample(TRG_DHR_SIGMA[i], dhrSigmaBandNames[i]);
            index++;
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            TRG_BHR[i] = index;
            configurator.defineSample(TRG_BHR[i], bhrBandNames[i]);
            index++;
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            TRG_BHR_ALPHA[i] = index;
            configurator.defineSample(TRG_BHR_ALPHA[i], bhrAlphaBandNames[i]);
            index++;
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            TRG_BHR_SIGMA[i] = index;
            configurator.defineSample(TRG_BHR_SIGMA[i], bhrSigmaBandNames[i]);
            index++;
        }

        configurator.defineSample(index++, weightedNumberOfSamplesBandName);
        configurator.defineSample(index++, snowFractionBandName);
        configurator.defineSample(index++, relEntropyBandName);
        configurator.defineSample(index++, goodnessOfFitBandName);
        configurator.defineSample(index++, dataMaskBandName);
        configurator.defineSample(index, szaBandName);
    }

    static double getWeightedMergeValue(double noSnowValue, double snowValue,
                                        double proportionNsamplesNoSnow, double proportionNsamplesSnow,
                                        double priorSnowFractionNoSnowDataValue,
                                        double priorSnowFractionSnowDataValue,
                                        double sza) {
        final boolean noSnowValid = AlbedoInversionUtils.isValid(noSnowValue);
        final boolean snowValid = AlbedoInversionUtils.isValid(snowValue);

        if (!noSnowValid && !snowValid) {
            return AlbedoInversionConstants.NO_DATA_VALUE;
        } else {
            double mergeValue;
            if (proportionNsamplesNoSnow > 0.0 && proportionNsamplesSnow > 0.0) {
                // snow and nosnow samples available
                mergeValue = noSnowValue * proportionNsamplesNoSnow + snowValue * proportionNsamplesSnow;
            } else if (proportionNsamplesNoSnow > 0.0) {
                // real nosnow sample available
                mergeValue = noSnowValue;
            } else if (proportionNsamplesSnow > 0.0) {
                // real snow sample available
                mergeValue = snowValue;
            } else {
                // prior info only
                if (noSnowValid && snowValid) {
//                    if (Math.abs(sza) > 88.0) {
//                        // we should be in high polar regions here
//                        // todo: find corresponding priors for this pixel and consider their snowFraction values
//                        mergeValue = snowValue;
//                    } else {
//                        // some guess but might also be wrong
//                        mergeValue = 0.5 * (noSnowValue + snowValue);
//                    }
                    // TEST Dec 2017:
                    final double priorSnowFraction =
                            Math.max(priorSnowFractionNoSnowDataValue, priorSnowFractionSnowDataValue);
                    if (Double.isNaN(priorSnowFraction)) {
                        mergeValue = noSnowValue;
                    } else {
                        mergeValue = (1.0 - priorSnowFraction) * noSnowValue + priorSnowFraction * snowValue;
                    }
                } else {
                    mergeValue = noSnowValid ? noSnowValue : snowValue;
                }
            }
            return mergeValue;
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MergeAlbedoOp.class);
        }
    }

}

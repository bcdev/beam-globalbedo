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
        description = "Merges Albedo Snow/NoSnow products.",
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

    private static int[] TRG_DHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] TRG_DHR_ALPHA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] TRG_DHR_SIGMA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] TRG_BHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] TRG_BHR_ALPHA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] TRG_BHR_SIGMA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int TRG_WEIGHTED_NUM_SAMPLES = 18;
    private static final int TRG_DATA_MASK = 19;
    //    private static final int TRG_REL_ENTROPY = 19;
//    private static final int TRG_GOODNESS_OF_FIT = 20;
    private static final int TRG_SNOW_FRACTION = 20;
//    private static final int TRG_SZA = 23;

    @SourceProduct(description = "Albedo Snow product")
    private Product snowProduct;

    @SourceProduct(description = "Albedo NoSnow product")
    private Product noSnowProduct;


    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final double nSamplesNoSnowDataValue = sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[0]].getDouble();
        final double nSamplesNoSnow = AlbedoInversionUtils.isValid(nSamplesNoSnowDataValue) ? nSamplesNoSnowDataValue : 0.0;
        final double nSamplesSnowDataValue = sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[1]].getDouble();
        final double nSamplesSnow = AlbedoInversionUtils.isValid(nSamplesSnowDataValue) ? nSamplesSnowDataValue : 0.0;
        final double totalNSamples = nSamplesSnow + nSamplesNoSnow;

        double proportionNsamplesSnow;
        double proportionNsamplesNoSnow;

        if (totalNSamples > 0.0) {
            if (nSamplesNoSnow > 0 && nSamplesSnow > 0) {
                proportionNsamplesNoSnow = nSamplesNoSnow / totalNSamples;
                proportionNsamplesSnow = nSamplesSnow / totalNSamples;
                setMergedBands(sourceSamples, targetSamples, proportionNsamplesSnow, proportionNsamplesNoSnow);
            } else if (nSamplesNoSnow > 0) {
                setMergedBandsToNoSnowBands(sourceSamples, targetSamples);
            } else if (nSamplesSnow > 0) {
                setMergedBandsToSnowBands(sourceSamples, targetSamples);
            }
        } else {
            setMergedBandsToZero(sourceSamples, targetSamples);
            targetSamples[TRG_WEIGHTED_NUM_SAMPLES].set(AlbedoInversionConstants.NO_DATA_VALUE);
        }
    }

    private void setMergedBandsToNoSnowBands(Sample[] sourceSamples, WritableSample[] targetSamples) {
        setMergedBands(sourceSamples, targetSamples, 0.0, 1.0);
    }

    private void setMergedBandsToSnowBands(Sample[] sourceSamples, WritableSample[] targetSamples) {
        setMergedBands(sourceSamples, targetSamples, 1.0, 0.0);
    }

    private void setMergedBandsToZero(Sample[] sourceSamples, WritableSample[] targetSamples) {
        setMergedBands(sourceSamples, targetSamples, 0.0, 0.0);
    }

    private void setMergedBands(Sample[] sourceSamples, WritableSample[] targetSamples, double proportionNsamplesSnow,
                                double proportionNsamplesNoSnow) {

        double[][] bhr = new double[2][AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[][] dhr = new double[2][AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[][] bhrAlpha = new double[2][AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[][] dhrAlpha = new double[2][AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[][] bhrSigma = new double[2][AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[][] dhrSigma = new double[2][AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            // BHR
            bhr[0][i] = sourceSamples[SRC_BHR[0][i]].getDouble();
            bhr[1][i] = sourceSamples[SRC_BHR[1][i]].getDouble();
            final double bhrNoSnow = AlbedoInversionUtils.isValid(bhr[0][i]) ? bhr[0][i] : 0.0;
            final double bhrSnow = AlbedoInversionUtils.isValid(bhr[1][i]) ? bhr[1][i] : 0.0;

            if (bhrNoSnow == 0.0 && bhrSnow == 0.0) {
                targetSamples[TRG_BHR[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            } else {
                final double bhrResult = bhrNoSnow * proportionNsamplesNoSnow + bhrSnow * proportionNsamplesSnow;
                targetSamples[TRG_BHR[i]].set(bhrResult);
            }

            // DHR
            dhr[0][i] = sourceSamples[SRC_DHR[0][i]].getDouble();
            dhr[1][i] = sourceSamples[SRC_DHR[1][i]].getDouble();
            final double dhrNoSnow = AlbedoInversionUtils.isValid(dhr[0][i]) ? dhr[0][i] : 0.0;
            final double dhrSnow = AlbedoInversionUtils.isValid(dhr[1][i]) ? dhr[1][i] : 0.0;

            if (dhrNoSnow == 0.0 && dhrSnow == 0.0) {
                targetSamples[TRG_DHR[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            } else {
                final double dhrResult = dhrNoSnow * proportionNsamplesNoSnow + dhrSnow * proportionNsamplesSnow;
                targetSamples[TRG_DHR[i]].set(dhrResult);
            }

            // BHR Alpha
            bhrAlpha[0][i] = sourceSamples[SRC_BHR_ALPHA[0][i]].getDouble();
            bhrAlpha[1][i] = sourceSamples[SRC_BHR_ALPHA[1][i]].getDouble();
            final double bhrAlphaNoSnow = AlbedoInversionUtils.isValid(bhrAlpha[0][i]) ? bhrAlpha[0][i] : 0.0;
            final double bhrAlphaSnow = AlbedoInversionUtils.isValid(bhrAlpha[1][i]) ? bhrAlpha[1][i] : 0.0;

            if (bhrAlphaNoSnow == 0.0 && bhrAlphaSnow == 0.0) {
                targetSamples[TRG_BHR_ALPHA[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            } else {
                final double bhrResult = bhrAlphaNoSnow * proportionNsamplesNoSnow + bhrAlphaSnow * proportionNsamplesSnow;
                targetSamples[TRG_BHR_ALPHA[i]].set(bhrResult);
            }

            // DHR Alpha
            dhrAlpha[0][i] = sourceSamples[SRC_BHR_ALPHA[0][i]].getDouble();
            dhrAlpha[1][i] = sourceSamples[SRC_BHR_ALPHA[1][i]].getDouble();
            final double dhrAlphaNoSnow = AlbedoInversionUtils.isValid(dhrAlpha[0][i]) ? dhrAlpha[0][i] : 0.0;
            final double dhrAlphaSnow = AlbedoInversionUtils.isValid(dhrAlpha[1][i]) ? dhrAlpha[1][i] : 0.0;

            if (dhrAlphaNoSnow == 0.0 && dhrAlphaSnow == 0.0) {
                targetSamples[TRG_DHR_ALPHA[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            } else {
                final double dhrResult = dhrAlphaNoSnow * proportionNsamplesNoSnow + dhrAlphaSnow * proportionNsamplesSnow;
                targetSamples[TRG_DHR_ALPHA[i]].set(dhrResult);
            }

            // BHR Sigma
            bhrSigma[0][i] = sourceSamples[SRC_BHR_SIGMA[0][i]].getDouble();
            bhrSigma[1][i] = sourceSamples[SRC_BHR_SIGMA[1][i]].getDouble();
            final double bhrSigmaNoSnow = AlbedoInversionUtils.isValid(bhrSigma[0][i]) ? bhrSigma[0][i] : 0.0;
            final double bhrSigmaSnow = AlbedoInversionUtils.isValid(bhrSigma[1][i]) ? bhrSigma[1][i] : 0.0;

            if (bhrSigmaNoSnow == 0.0 && bhrSigmaSnow == 0.0) {
                targetSamples[TRG_BHR_SIGMA[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            } else {
                final double bhrResult = bhrSigmaNoSnow * proportionNsamplesNoSnow + bhrSigmaSnow * proportionNsamplesSnow;
                targetSamples[TRG_BHR_SIGMA[i]].set(bhrResult);
            }

            // DHR Sigma
            dhrSigma[0][i] = sourceSamples[SRC_DHR_SIGMA[0][i]].getDouble();
            dhrSigma[1][i] = sourceSamples[SRC_DHR_SIGMA[1][i]].getDouble();
            final double dhrSigmaNoSnow = AlbedoInversionUtils.isValid(dhrSigma[0][i]) ? dhrSigma[0][i] : 0.0;
            final double dhrSigmaSnow = AlbedoInversionUtils.isValid(dhrSigma[1][i]) ? dhrSigma[1][i] : 0.0;

            if (dhrSigmaNoSnow == 0.0 && dhrSigmaSnow == 0.0) {
                targetSamples[TRG_DHR_SIGMA[i]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            } else {
                final double dhrResult = dhrSigmaNoSnow * proportionNsamplesNoSnow + dhrSigmaSnow * proportionNsamplesSnow;
                targetSamples[TRG_DHR_SIGMA[i]].set(dhrResult);
            }
        }

//        final double relEntNoSnowDataValue = sourceSamples[SRC_REL_ENTROPY[0]].getDouble();
//        final double relEntNoSnow = AlbedoInversionUtils.isValid(relEntNoSnowDataValue) ? relEntNoSnowDataValue : 0.0;
//        final double relEntSnowDataValue = sourceSamples[SRC_REL_ENTROPY[1]].getDouble();
//        final double relEntSnow = AlbedoInversionUtils.isValid(relEntSnowDataValue) ? relEntSnowDataValue : 0.0;
//        final double relEntropyResult = relEntSnow * proportionNsamplesSnow + relEntNoSnow * proportionNsamplesNoSnow;
//        if (relEntNoSnow == 0.0 && relEntSnow == 0.0) {
//            targetSamples[TRG_REL_ENTROPY].set(AlbedoInversionConstants.NO_DATA_VALUE);
//        } else {
//            targetSamples[TRG_REL_ENTROPY].set(relEntropyResult);
//        }

        final double wnsNoSnowDataValue = sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[0]].getDouble();
        final double wnsNoSnow = AlbedoInversionUtils.isValid(wnsNoSnowDataValue) ? wnsNoSnowDataValue : 0.0;
        final double wnsSnowDataValue = sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[1]].getDouble();
        final double wnsSnow = AlbedoInversionUtils.isValid(wnsSnowDataValue) ? wnsSnowDataValue : 0.0;
        final double wnsResult = wnsSnow * proportionNsamplesSnow + wnsNoSnow * proportionNsamplesNoSnow;
        if (wnsNoSnow == 0.0 && wnsSnow == 0.0) {
            targetSamples[TRG_WEIGHTED_NUM_SAMPLES].set(AlbedoInversionConstants.NO_DATA_VALUE);
        } else {
            targetSamples[TRG_WEIGHTED_NUM_SAMPLES].set(wnsResult);
        }

//        final double gofNoSnowDataValue = sourceSamples[SRC_GOODNESS_OF_FIT[0]].getDouble();
//        final double gofNoSnow = AlbedoInversionUtils.isValid(gofNoSnowDataValue) ? gofNoSnowDataValue : 0.0;
//        final double gofSnowDataValue = sourceSamples[SRC_GOODNESS_OF_FIT[1]].getDouble();
//        final double gofSnow = AlbedoInversionUtils.isValid(gofSnowDataValue) ? gofSnowDataValue : 0.0;
//        final double gofResult = gofSnow * proportionNsamplesSnow + gofNoSnow * proportionNsamplesNoSnow;
//        if (gofNoSnow == 0.0 && gofSnow == 0.0) {
//            targetSamples[TRG_GOODNESS_OF_FIT].set(AlbedoInversionConstants.NO_DATA_VALUE);
//        } else {
//            targetSamples[TRG_GOODNESS_OF_FIT].set(gofResult);
//        }

        final double maskNoSnowDataValue = sourceSamples[SRC_DATA_MASK[0]].getDouble();
        final double maskNoSnow = AlbedoInversionUtils.isValid(maskNoSnowDataValue) ? maskNoSnowDataValue : 0.0;
        final double maskSnowDataValue = sourceSamples[SRC_DATA_MASK[1]].getDouble();
        final double maskSnow = AlbedoInversionUtils.isValid(maskSnowDataValue) ? maskSnowDataValue : 0.0;
        final double maskResult = Math.max(maskNoSnow, maskSnow);
        targetSamples[TRG_DATA_MASK].set(maskResult);

        targetSamples[TRG_SNOW_FRACTION].set(proportionNsamplesSnow);
//        targetSamples[TRG_SZA].set(sourceSamples[SRC_SZA[0]].getDouble());
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

//        SRC_REL_ENTROPY[0] = index;
//        SRC_REL_ENTROPY[1] = index + 100;
//        index++;
//        relEntropyBandName = AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME;
//        targetProduct.addBand(relEntropyBandName, ProductData.TYPE_FLOAT32);

//        SRC_GOODNESS_OF_FIT[0] = index;
//        SRC_GOODNESS_OF_FIT[1] = index + 100;
//        index++;
//        goodnessOfFitBandName = AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME;
//        targetProduct.addBand(goodnessOfFitBandName, ProductData.TYPE_FLOAT32);

        SRC_DATA_MASK[0] = index;
        SRC_DATA_MASK[1] = index + 100;
//        index++;
        dataMaskBandName = AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME;
        targetProduct.addBand(dataMaskBandName, ProductData.TYPE_FLOAT32);

        targetProduct.addBand("snow_fraction", ProductData.TYPE_FLOAT32);

//        SRC_SZA[0] = index;
//        SRC_SZA[1] = index + 100;
//        szaBandName = AlbedoInversionConstants.ALB_SZA_BAND_NAME;
//        targetProduct.addBand(szaBandName, ProductData.TYPE_FLOAT32);

        for (Band b : targetProduct.getBands()) {
            b.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
            b.setNoDataValueUsed(true);
        }
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {

        // source products:
        // for a number of source products, we have:
        // 3x DHR, 3x BHR, 3x DHR_sigma, 3x BHR_sigma, weightedNumSamples, relEntropy, goodnessOfFit,#
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
//        configurator.defineSample(SRC_REL_ENTROPY[0], relEntropyBandName, noSnowProduct);
//        configurator.defineSample(SRC_REL_ENTROPY[1], relEntropyBandName, snowProduct);
//        configurator.defineSample(SRC_GOODNESS_OF_FIT[0], goodnessOfFitBandName, noSnowProduct);
//        configurator.defineSample(SRC_GOODNESS_OF_FIT[1], goodnessOfFitBandName, snowProduct);
        configurator.defineSample(SRC_DATA_MASK[0], dataMaskBandName, noSnowProduct);
        configurator.defineSample(SRC_DATA_MASK[1], dataMaskBandName, snowProduct);
//        configurator.defineSample(SRC_SZA[0], szaBandName, noSnowProduct);
//        configurator.defineSample(SRC_SZA[1], szaBandName, snowProduct);
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
//        configurator.defineSample(index++, relEntropyBandName);
//        configurator.defineSample(index++, goodnessOfFitBandName);
        configurator.defineSample(index++, dataMaskBandName);
        configurator.defineSample(index, "snow_fraction");
//        configurator.defineSample(index, szaBandName);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MergeAlbedoOp.class);
        }
    }

}

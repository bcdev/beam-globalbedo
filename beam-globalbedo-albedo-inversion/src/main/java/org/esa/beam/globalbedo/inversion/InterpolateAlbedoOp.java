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
 * Operator for interpolating two albedo products.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.albedo.interpolatealbedo",
        description = "Interpolates two albedo products. E.g. for leap days: 2004366 = 0.5*(2004365 + 2005001) .",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2017 by Brockmann Consult")
class InterpolateAlbedoOp extends PixelOperator {

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
    private static int[] SRC_SNOW_FRACTION = new int[2];

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
    private Product firstAlbedoProduct;

    @SourceProduct(description = "Albedo NoSnow product")
    private Product secondAlbedoProduct;


    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        setInterpolatedBands(sourceSamples, targetSamples);
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

        SRC_SNOW_FRACTION[0] = index;
        SRC_SNOW_FRACTION[1] = index + 100;

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
            configurator.defineSample(SRC_DHR[0][i], dhrBandNames[i], secondAlbedoProduct);
            configurator.defineSample(SRC_DHR[1][i], dhrBandNames[i], firstAlbedoProduct);
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            configurator.defineSample(SRC_BHR[0][i], bhrBandNames[i], secondAlbedoProduct);
            configurator.defineSample(SRC_BHR[1][i], bhrBandNames[i], firstAlbedoProduct);
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            configurator.defineSample(SRC_DHR_SIGMA[0][i], dhrSigmaBandNames[i], secondAlbedoProduct);
            configurator.defineSample(SRC_DHR_SIGMA[1][i], dhrSigmaBandNames[i], firstAlbedoProduct);
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            configurator.defineSample(SRC_BHR_SIGMA[0][i], bhrSigmaBandNames[i], secondAlbedoProduct);
            configurator.defineSample(SRC_BHR_SIGMA[1][i], bhrSigmaBandNames[i], firstAlbedoProduct);
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            configurator.defineSample(SRC_DHR_ALPHA[0][i], dhrAlphaBandNames[i], secondAlbedoProduct);
            configurator.defineSample(SRC_DHR_ALPHA[1][i], dhrAlphaBandNames[i], firstAlbedoProduct);
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            configurator.defineSample(SRC_BHR_ALPHA[0][i], bhrAlphaBandNames[i], secondAlbedoProduct);
            configurator.defineSample(SRC_BHR_ALPHA[1][i], bhrAlphaBandNames[i], firstAlbedoProduct);
        }

        configurator.defineSample(SRC_WEIGHTED_NUM_SAMPLES[0], weightedNumberOfSamplesBandName, secondAlbedoProduct);
        configurator.defineSample(SRC_WEIGHTED_NUM_SAMPLES[1], weightedNumberOfSamplesBandName, firstAlbedoProduct);
        configurator.defineSample(SRC_REL_ENTROPY[0], relEntropyBandName, secondAlbedoProduct);
        configurator.defineSample(SRC_REL_ENTROPY[1], relEntropyBandName, firstAlbedoProduct);
        configurator.defineSample(SRC_GOODNESS_OF_FIT[0], goodnessOfFitBandName, secondAlbedoProduct);
        configurator.defineSample(SRC_GOODNESS_OF_FIT[1], goodnessOfFitBandName, firstAlbedoProduct);
        configurator.defineSample(SRC_DATA_MASK[0], dataMaskBandName, secondAlbedoProduct);
        configurator.defineSample(SRC_DATA_MASK[1], dataMaskBandName, firstAlbedoProduct);
        configurator.defineSample(SRC_SZA[0], szaBandName, secondAlbedoProduct);
        configurator.defineSample(SRC_SZA[1], szaBandName, firstAlbedoProduct);
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

    private static double getInterpolatedValue(double firstValue, double secondValue) {
        final boolean firstValid = AlbedoInversionUtils.isValid(firstValue);
        final boolean secondValid = AlbedoInversionUtils.isValid(secondValue);

        // prior info only
        if (firstValid && secondValid) {
            return 0.5 * (firstValue + secondValue);
        } else if (firstValid) {
            return firstValue;
        } else if (secondValid) {
            return secondValue;
        } else {
            return AlbedoInversionConstants.NO_DATA_VALUE;
        }
    }

    private static void setInterpolatedBands(Sample[] sourceSamples, WritableSample[] targetSamples) {

        // BHR/DHR, alphas, sigmas
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetSamples[TRG_BHR[i]].
                    set(getInterpolatedValue(sourceSamples[SRC_BHR[0][i]].getDouble(),
                            sourceSamples[SRC_BHR[1][i]].getDouble()));
            targetSamples[TRG_DHR[i]].
                    set(getInterpolatedValue(sourceSamples[SRC_DHR[0][i]].getDouble(),
                            sourceSamples[SRC_DHR[1][i]].getDouble()));
            targetSamples[TRG_BHR_ALPHA[i]].
                    set(getInterpolatedValue(sourceSamples[SRC_BHR_ALPHA[0][i]].getDouble(),
                            sourceSamples[SRC_BHR_ALPHA[1][i]].getDouble()));
            targetSamples[TRG_DHR_ALPHA[i]].
                    set(getInterpolatedValue(sourceSamples[SRC_DHR_ALPHA[0][i]].getDouble(),
                            sourceSamples[SRC_DHR_ALPHA[1][i]].getDouble()));
            targetSamples[TRG_BHR_SIGMA[i]].
                    set(getInterpolatedValue(sourceSamples[SRC_BHR_SIGMA[0][i]].getDouble(),
                            sourceSamples[SRC_BHR_SIGMA[1][i]].getDouble()));
            targetSamples[TRG_DHR_SIGMA[i]].
                    set(getInterpolatedValue(sourceSamples[SRC_DHR_SIGMA[0][i]].getDouble(),
                            sourceSamples[SRC_DHR_SIGMA[1][i]].getDouble()));
        }

        // rel. entropy
        targetSamples[TRG_REL_ENTROPY].
                set(getInterpolatedValue(sourceSamples[SRC_REL_ENTROPY[0]].getDouble(),
                        sourceSamples[SRC_REL_ENTROPY[1]].getDouble()));

        // WNSamples
        targetSamples[TRG_WEIGHTED_NUM_SAMPLES].
                set(getInterpolatedValue(sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[0]].getDouble(),
                        sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[1]].getDouble()));

        // GoF
        targetSamples[TRG_GOODNESS_OF_FIT].
                set(getInterpolatedValue(sourceSamples[SRC_GOODNESS_OF_FIT[0]].getDouble(),
                        sourceSamples[SRC_GOODNESS_OF_FIT[1]].getDouble()));

        // SZA
        targetSamples[TRG_SZA].
                set(getInterpolatedValue(sourceSamples[SRC_SZA[0]].getDouble(),
                        sourceSamples[SRC_SZA[1]].getDouble()));

        // WNS
        targetSamples[TRG_WEIGHTED_NUM_SAMPLES].
                set(getInterpolatedValue(sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[0]].getDouble(),
                        sourceSamples[SRC_WEIGHTED_NUM_SAMPLES[1]].getDouble()));

        // Snow fraction
        targetSamples[TRG_SNOW_FRACTION].
                set(getInterpolatedValue(sourceSamples[SRC_SNOW_FRACTION[0]].getDouble(),
                        sourceSamples[SRC_SNOW_FRACTION[1]].getDouble()));

        // data mask: 1.0 or 0.0
        targetSamples[TRG_DATA_MASK].
                set(Math.max(sourceSamples[SRC_SNOW_FRACTION[0]].getDouble(),
                        sourceSamples[SRC_SNOW_FRACTION[1]].getDouble()));
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(InterpolateAlbedoOp.class);
        }
    }

}

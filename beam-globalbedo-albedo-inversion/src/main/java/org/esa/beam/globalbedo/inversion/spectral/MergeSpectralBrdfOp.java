package org.esa.beam.globalbedo.inversion.spectral;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.pow;

/**
 * Operator for merging spectral BRDF Snow/NoSnow products.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.albedo.mergebrdf.spectral",
        description = "Merges spectral BRDF Snow/NoSnow products.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")
public class MergeSpectralBrdfOp extends PixelOperator {

    private static final int SRC_SNOW_ENTROPY = 0;

    private static final int SRC_NOSNOW_ENTROPY = 0;
    private static final int SRC_SNOW_REL_ENTROPY = 1;
    private static final int SRC_NOSNOW_REL_ENTROPY = 1;
    private static final int SRC_SNOW_WEIGHTED_NUM_SAMPLES = 2;
    private static final int SRC_NOSNOW_WEIGHTED_NUM_SAMPLES = 2;
    private static final int SRC_SNOW_DAYS_CLOSEST_SAMPLE = 3;
    private static final int SRC_NOSNOW_DAYS_CLOSEST_SAMPLE = 3;
    private static final int SRC_SNOW_GOODNESS_OF_FIT = 4;
    private static final int SRC_NOSNOW_GOODNESS_OF_FIT = 4;

    private static final int sourceSampleOffset = 300;

    private static final int TRG_ENTROPY = 0;
    private static final int TRG_REL_ENTROPY = 1;
    private static final int TRG_WEIGHTED_NUM_SAMPLES = 2;
    private static final int TRG_DAYS_CLOSEST_SAMPLE = 3;
    private static final int TRG_GOODNESS_OF_FIT = 4;
    private static final int TRG_PROPORTION_NSAMPLES = 5;

    private String[] parameterBandNames;
    private String[][] uncertaintyBandNames;

    private String entropyBandName;
    private String relEntropyBandName;
    private String weightedNumberOfSamplesBandName;
    private String daysToTheClosestSampleBandName;
    private String goodnessOfFitBandName;
    private String proportionNsamplesBandName;

    private int[] srcSnowParams;
    private int[] srcSnowUncertainties;
    private int[] srcNoSnowParams;
    private int[] srcNoSnowUncertainties;

    private int[] trgParameters;
    private int[] trgUncertainties;

    private Map<Integer, String> spectralWaveBandsMap = new HashMap<>();


    @SourceProduct(description = "BRDF Snow product")
    private Product snowProduct;

    @SourceProduct(description = "BRDF NoSnow product")
    private Product noSnowProduct;

    @Parameter(defaultValue = "7", description = "Number of spectral bands (7 for standard MODIS spectral mapping")
    private int numSdrBands;

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        setupSpectralWaveBandsMap(numSdrBands);

        srcSnowParams = new int[3 * numSdrBands];
        srcNoSnowParams = new int[3 * numSdrBands];

        final int urMatrixOffset = ((int) pow(3 * numSdrBands, 2.0) + 3 * numSdrBands) / 2;
        srcSnowUncertainties = new int[urMatrixOffset];
        srcNoSnowUncertainties = new int[urMatrixOffset];

        trgParameters = new int[3 * numSdrBands];
        trgUncertainties = new int[urMatrixOffset];

        parameterBandNames = new String[3 * numSdrBands];
        uncertaintyBandNames = new String[3 * numSdrBands][3 * numSdrBands];
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final double nSamplesSnowDataValue = sourceSamples[srcSnowParams.length + srcSnowUncertainties.length +
                SRC_SNOW_WEIGHTED_NUM_SAMPLES].getDouble();
        final double nSamplesSnow = AlbedoInversionUtils.isValid(nSamplesSnowDataValue) ? nSamplesSnowDataValue : 0.0;

        final double nSamplesNoSnowDataValue = sourceSamples[sourceSampleOffset + srcNoSnowParams.length +
                srcNoSnowUncertainties.length + SRC_NOSNOW_WEIGHTED_NUM_SAMPLES].getDouble();
        final double nSamplesNoSnow = AlbedoInversionUtils.isValid(nSamplesNoSnowDataValue) ? nSamplesNoSnowDataValue : 0.0;

        final double totalNSamples = nSamplesSnow + nSamplesNoSnow;


        final double entropySnowDataValue = sourceSamples[srcSnowParams.length + srcSnowUncertainties.length +
                SRC_SNOW_ENTROPY].getDouble();
        final double entropySnow = AlbedoInversionUtils.isValid(entropySnowDataValue) ? entropySnowDataValue : 0.0;
        final double entropyNoSnowDataValue = sourceSamples[sourceSampleOffset + srcNoSnowParams.length +
                srcNoSnowUncertainties.length + SRC_NOSNOW_ENTROPY].getDouble();
        final double entropyNoSnow = AlbedoInversionUtils.isValid(entropyNoSnowDataValue) ? entropyNoSnowDataValue : 0.0;

        double proportionNsamplesSnow;
        double proportionNsamplesNoSnow;

        if (totalNSamples > 0.0 && (!areSnowSamplesZero() || !areNoSnowSamplesZero())) {
            proportionNsamplesNoSnow = nSamplesNoSnow / totalNSamples;
            proportionNsamplesSnow = nSamplesSnow / totalNSamples;
            setMergedBands(sourceSamples, targetSamples, proportionNsamplesSnow, proportionNsamplesNoSnow);
            targetSamples[trgParameters.length + trgUncertainties.length + TRG_PROPORTION_NSAMPLES].set(
                    proportionNsamplesSnow);
        } else if (entropySnow != 0.0 && entropyNoSnow != 0.0) {
            if (entropySnow <= entropyNoSnow) {
                setMergedBandsToSnowBands(sourceSamples, targetSamples);
                targetSamples[trgParameters.length + trgUncertainties.length + TRG_PROPORTION_NSAMPLES].set(1.0);
            } else {
                setMergedBandsToNoSnowBands(sourceSamples, targetSamples);
                targetSamples[trgParameters.length + trgUncertainties.length + TRG_PROPORTION_NSAMPLES].set(0.0);
            }
        } else {
            setMergedBandsToZero(sourceSamples, targetSamples);
            targetSamples[TRG_PROPORTION_NSAMPLES].set(AlbedoInversionConstants.NO_DATA_VALUE);
        }
    }

    private boolean areSnowSamplesZero() {
        for (int SRC_SNOW_PARAMETER : srcSnowParams) {
            if (SRC_SNOW_PARAMETER > 0.0) {
                return false;
            }
        }
        return true;
    }

    private boolean areNoSnowSamplesZero() {
        for (int SRC_NOSNOW_PARAMETER : srcNoSnowParams) {
            if (SRC_NOSNOW_PARAMETER > 0.0) {
                return false;
            }
        }
        return true;
    }

    private void setMergedBandsToSnowBands(Sample[] sourceSamples, WritableSample[] targetSamples) {
        setMergedBands(sourceSamples, targetSamples, 1.0, 0.0);
    }

    private void setMergedBandsToZero(Sample[] sourceSamples, WritableSample[] targetSamples) {
        setMergedBands(sourceSamples, targetSamples, 0.0, 0.0);
    }

    private void setMergedBandsToNoSnowBands(Sample[] sourceSamples, WritableSample[] targetSamples) {
        setMergedBands(sourceSamples, targetSamples, 0.0, 1.0);
    }

    private void setMergedBands(Sample[] sourceSamples, WritableSample[] targetSamples, double proportionNsamplesSnow,
                                double proportionNsamplesNoSnow) {

        // parameters
        int index = 0;
        for (int i = 0; i < 3 * numSdrBands; i++) {
            final double sampleParameterSnowDataValue = sourceSamples[index].getDouble();
            final double sampleParameterSnow =
                    AlbedoInversionUtils.isValid(sampleParameterSnowDataValue) ? sampleParameterSnowDataValue : 0.0;
            final double sampleParameterNoSnowDataValue = sourceSamples[sourceSampleOffset + index].getDouble();
            final double sampleParameterNoSnow =
                    AlbedoInversionUtils.isValid(sampleParameterNoSnowDataValue) ?
                            sampleParameterNoSnowDataValue : 0.0;
            final double resultParameters = sampleParameterSnow * proportionNsamplesSnow +
                    sampleParameterNoSnow * proportionNsamplesNoSnow;

            if (sampleParameterNoSnow == 0.0 && sampleParameterSnow == 0.0) {
                targetSamples[trgParameters[index]].set(AlbedoInversionConstants.NO_DATA_VALUE);
            } else {
                targetSamples[trgParameters[index]].set(resultParameters);
            }
            index++;
        }

        // uncertainties
        index = 0;
        for (int i = 0; i < 3 * numSdrBands; i++) {
            for (int j = i; j < 3 * numSdrBands; j++) {
                final double sampleUncertaintySnowDataValue = sourceSamples[srcSnowParams.length + index].getDouble();
                final double sampleUncertaintySnow =
                        AlbedoInversionUtils.isValid(sampleUncertaintySnowDataValue) ? sampleUncertaintySnowDataValue : 0.0;
                final double sampleUncertaintyNoSnowDataValue = sourceSamples[sourceSampleOffset + srcSnowParams.length + index].getDouble();
                final double sampleUncertaintyNoSnow =
                        AlbedoInversionUtils.isValid(sampleUncertaintyNoSnowDataValue) ? sampleUncertaintyNoSnowDataValue : 0.0;
                final double resultUncertainties = sampleUncertaintySnow * proportionNsamplesSnow +
                        sampleUncertaintyNoSnow * proportionNsamplesNoSnow;

                if (sampleUncertaintySnow == 0.0 && sampleUncertaintyNoSnow == 0.0) {
                    targetSamples[trgParameters.length + trgUncertainties[index]].set(AlbedoInversionConstants.NO_DATA_VALUE);
                } else {
                    targetSamples[trgParameters.length + trgUncertainties[index]].set(resultUncertainties);
                }
                index++;
            }
        }

        final double sampleEntropySnowDataValue =
                sourceSamples[srcSnowParams.length + srcSnowUncertainties.length + SRC_SNOW_ENTROPY].getDouble();
        final double sampleEntropySnow = AlbedoInversionUtils.isValid
                (sampleEntropySnowDataValue) ? sampleEntropySnowDataValue : 0.0;
        final double sampleEntropyNoSnowDataValue =
                sourceSamples[sourceSampleOffset + srcNoSnowParams.length +
                        srcNoSnowUncertainties.length + SRC_NOSNOW_ENTROPY].getDouble();
        final double sampleEntropyNoSnow = AlbedoInversionUtils.isValid
                (sampleEntropyNoSnowDataValue) ? sampleEntropyNoSnowDataValue : 0.0;
        final double resultEntropy = sampleEntropySnow * proportionNsamplesSnow +
                sampleEntropyNoSnow * proportionNsamplesNoSnow;
        if (sampleEntropyNoSnow == 0.0 && sampleEntropySnow == 0.0) {
            targetSamples[trgParameters.length + trgUncertainties.length + TRG_ENTROPY].
                    set(AlbedoInversionConstants.NO_DATA_VALUE);
        } else {
            targetSamples[trgParameters.length + trgUncertainties.length + TRG_ENTROPY].set(resultEntropy);
        }

        final double sampleRelEntropySnowDataValue =
                sourceSamples[srcSnowParams.length + srcSnowUncertainties.length + SRC_SNOW_REL_ENTROPY].getDouble();
        final double sampleRelEntropySnow = AlbedoInversionUtils.isValid(
                sampleRelEntropySnowDataValue) ? sampleRelEntropySnowDataValue : 0.0;
        final double sampleRelEntropyNoSnowDataValue =
                sourceSamples[sourceSampleOffset + srcNoSnowParams.length +
                        srcNoSnowUncertainties.length + SRC_NOSNOW_REL_ENTROPY].getDouble();
        final double sampleRelEntropyNoSnow = AlbedoInversionUtils.isValid(
                sampleRelEntropyNoSnowDataValue) ? sampleRelEntropyNoSnowDataValue : 0.0;
        final double resultRelEntropy = sampleRelEntropySnow * proportionNsamplesSnow +
                sampleRelEntropyNoSnow * proportionNsamplesNoSnow;
        if (sampleRelEntropyNoSnow == 0.0 && sampleRelEntropySnow == 0.0) {
            targetSamples[trgParameters.length + trgUncertainties.length + TRG_REL_ENTROPY].
                    set(AlbedoInversionConstants.NO_DATA_VALUE);
        } else {
            targetSamples[trgParameters.length + trgUncertainties.length + TRG_REL_ENTROPY].set(resultRelEntropy);
        }

        final double sampleWeightedNumSamplesSnowDataValue = sourceSamples[srcSnowParams.length +
                srcSnowUncertainties.length + SRC_SNOW_WEIGHTED_NUM_SAMPLES].getDouble();
        final double sampleWeightedNumSamplesSnow = AlbedoInversionUtils.isValid(
                sampleWeightedNumSamplesSnowDataValue) ? sampleWeightedNumSamplesSnowDataValue : 0.0;
        final double sampleWeightedNumSamplesNoSnowDataValue = sourceSamples[sourceSampleOffset +
                srcNoSnowParams.length + srcNoSnowUncertainties.length + SRC_NOSNOW_WEIGHTED_NUM_SAMPLES].getDouble();
        final double sampleWeightedNumSamplesNoSnow = AlbedoInversionUtils.isValid(
                sampleWeightedNumSamplesNoSnowDataValue) ? sampleWeightedNumSamplesNoSnowDataValue : 0.0;
        final double resultWeightedNumSamples = sampleWeightedNumSamplesSnow * proportionNsamplesSnow +
                sampleWeightedNumSamplesNoSnow * proportionNsamplesNoSnow;
        if (sampleWeightedNumSamplesNoSnow == 0.0 && sampleWeightedNumSamplesSnow == 0.0) {
            targetSamples[trgParameters.length + trgUncertainties.length + TRG_WEIGHTED_NUM_SAMPLES].set(
                    AlbedoInversionConstants.NO_DATA_VALUE);
        } else {
            targetSamples[trgParameters.length + trgUncertainties.length + TRG_WEIGHTED_NUM_SAMPLES].set(
                    resultWeightedNumSamples);
        }

        final double sampleDoyClosestSampleSnowDataValue = sourceSamples[srcSnowParams.length +
                srcSnowUncertainties.length + SRC_SNOW_DAYS_CLOSEST_SAMPLE].getDouble();
        final double sampleDoyClosestSampleSnow = AlbedoInversionUtils.isValid(
                sampleDoyClosestSampleSnowDataValue) ? sampleDoyClosestSampleSnowDataValue : 0.0;
        final double sampleDoyClosestSampleNoSnowDataValue = sourceSamples[sourceSampleOffset +
                srcNoSnowParams.length + srcNoSnowUncertainties.length + SRC_NOSNOW_DAYS_CLOSEST_SAMPLE].getDouble();
        final double sampleDoyClosestSampleNoSnow = AlbedoInversionUtils.isValid(
                sampleDoyClosestSampleNoSnowDataValue) ? sampleDoyClosestSampleNoSnowDataValue : 0.0;
        final double resultDoyClosestSample = sampleDoyClosestSampleSnow * proportionNsamplesSnow +
                sampleDoyClosestSampleNoSnow * proportionNsamplesNoSnow;
        if (sampleDoyClosestSampleNoSnow == 0.0 && sampleDoyClosestSampleSnow == 0.0) {
            targetSamples[trgParameters.length + trgUncertainties.length + TRG_DAYS_CLOSEST_SAMPLE].set(
                    AlbedoInversionConstants.NO_DATA_VALUE);
        } else {
            targetSamples[trgParameters.length + trgUncertainties.length + TRG_DAYS_CLOSEST_SAMPLE].set(
                    resultDoyClosestSample);
        }

        final double sampleGoodnessOfFitSnowDataValue = sourceSamples[srcSnowParams.length +
                srcSnowUncertainties.length + SRC_SNOW_GOODNESS_OF_FIT].getDouble();
        final double sampleGoodnessOfFitSnow = AlbedoInversionUtils.isValid(
                sampleGoodnessOfFitSnowDataValue) ? sampleGoodnessOfFitSnowDataValue : 0.0;
        final double sampleGoodnessOfFitNoSnowDataValue = sourceSamples[sourceSampleOffset + srcNoSnowParams.length +
                srcNoSnowUncertainties.length + SRC_NOSNOW_GOODNESS_OF_FIT].getDouble();
        final double sampleGoodnessOfFitNoSnow = AlbedoInversionUtils.isValid(
                sampleGoodnessOfFitNoSnowDataValue) ? sampleGoodnessOfFitNoSnowDataValue : 0.0;
        final double resultGoodnessOfFit = sampleGoodnessOfFitSnow * proportionNsamplesSnow +
                sampleGoodnessOfFitNoSnow * proportionNsamplesNoSnow;
        if (sampleGoodnessOfFitNoSnow == 0.0 && sampleGoodnessOfFitSnow == 0.0) {
            targetSamples[trgParameters.length + trgUncertainties.length + TRG_GOODNESS_OF_FIT].
                    set(AlbedoInversionConstants.NO_DATA_VALUE);
        } else {
            targetSamples[trgParameters.length + trgUncertainties.length + TRG_GOODNESS_OF_FIT].set(resultGoodnessOfFit);
        }

    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) throws OperatorException {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        parameterBandNames = SpectralIOUtils.getSpectralInversionParameterBandNames(numSdrBands);
        for (String parameterBandName : parameterBandNames) {
            targetProduct.addBand(parameterBandName, ProductData.TYPE_FLOAT32);
        }

        uncertaintyBandNames = SpectralIOUtils.getSpectralInversionUncertaintyBandNames(numSdrBands, spectralWaveBandsMap);
        for (int i = 0; i < 3 * numSdrBands; i++) {
            // add bands only for UR triangular matrix
            for (int j = i; j < 3 * numSdrBands; j++) {
                targetProduct.addBand(uncertaintyBandNames[i][j], ProductData.TYPE_FLOAT32);
            }
        }

        entropyBandName = AlbedoInversionConstants.INV_ENTROPY_BAND_NAME;
        targetProduct.addBand(entropyBandName, ProductData.TYPE_FLOAT32);

        relEntropyBandName = AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME;
        targetProduct.addBand(relEntropyBandName, ProductData.TYPE_FLOAT32);

        weightedNumberOfSamplesBandName = AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME;
        targetProduct.addBand(weightedNumberOfSamplesBandName, ProductData.TYPE_FLOAT32);

        daysToTheClosestSampleBandName = AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME;
        targetProduct.addBand(daysToTheClosestSampleBandName, ProductData.TYPE_FLOAT32);

        goodnessOfFitBandName = AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME;
        targetProduct.addBand(goodnessOfFitBandName, ProductData.TYPE_FLOAT32);

        proportionNsamplesBandName = AlbedoInversionConstants.MERGE_PROPORTION_NSAMPLES_BAND_NAME;
        targetProduct.addBand(proportionNsamplesBandName, ProductData.TYPE_FLOAT32);

        for (Band b : targetProduct.getBands()) {
            b.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
            b.setNoDataValueUsed(true);
        }

//        targetProduct.setPreferredTileSize(100, 100);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {
        // BRDF parameters Snow product:
        for (int i = 0; i < 3 * numSdrBands; i++) {
            srcSnowParams[i] = i;
            configurator.defineSample(srcSnowParams[i], parameterBandNames[i], snowProduct);
        }

        int index = 0;
        for (int i = 0; i < 3 * numSdrBands; i++) {
            for (int j = i; j < 3 * numSdrBands; j++) {
                srcSnowUncertainties[index] = index;
                configurator.defineSample(srcSnowParams.length + srcSnowUncertainties[index],
                                          uncertaintyBandNames[i][j], snowProduct);
                index++;
            }
        }

        configurator.defineSample(srcSnowParams.length + srcSnowUncertainties.length + SRC_SNOW_ENTROPY,
                                  entropyBandName, snowProduct);
        configurator.defineSample(srcSnowParams.length + srcSnowUncertainties.length + SRC_SNOW_REL_ENTROPY,
                                  relEntropyBandName, snowProduct);
        configurator.defineSample(
                srcSnowParams.length + srcSnowUncertainties.length + SRC_SNOW_WEIGHTED_NUM_SAMPLES,
                weightedNumberOfSamplesBandName, snowProduct);
        configurator.defineSample(
                srcSnowParams.length + srcSnowUncertainties.length + SRC_SNOW_DAYS_CLOSEST_SAMPLE,
                AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME, snowProduct);
        configurator.defineSample(srcSnowParams.length + srcSnowUncertainties.length + SRC_SNOW_GOODNESS_OF_FIT,
                                  goodnessOfFitBandName, snowProduct);

        // BRDF parameters NoSnow product:
        for (int i = 0; i < 3 * numSdrBands; i++) {
            srcNoSnowParams[i] = sourceSampleOffset + i;
            configurator.defineSample(srcNoSnowParams[i], parameterBandNames[i], noSnowProduct);
        }

        index = 0;
        for (int i = 0; i < 3 * numSdrBands; i++) {
            for (int j = i; j < 3 * numSdrBands; j++) {
                srcNoSnowUncertainties[index] = sourceSampleOffset + index;
                configurator.defineSample(srcNoSnowParams.length + srcNoSnowUncertainties[index],
                                          uncertaintyBandNames[i][j], noSnowProduct);
                index++;
            }
        }

        configurator.defineSample(
                sourceSampleOffset + srcNoSnowParams.length + srcNoSnowUncertainties.length + SRC_NOSNOW_ENTROPY,
                entropyBandName, noSnowProduct);
        configurator.defineSample(
                sourceSampleOffset + srcNoSnowParams.length + srcNoSnowUncertainties.length + SRC_NOSNOW_REL_ENTROPY,
                relEntropyBandName, noSnowProduct);
        configurator.defineSample(
                sourceSampleOffset + srcNoSnowParams.length + srcNoSnowUncertainties.length + SRC_NOSNOW_WEIGHTED_NUM_SAMPLES,
                weightedNumberOfSamplesBandName, noSnowProduct);
        configurator.defineSample(
                sourceSampleOffset + srcNoSnowParams.length + srcNoSnowUncertainties.length + SRC_NOSNOW_DAYS_CLOSEST_SAMPLE,
                AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME, noSnowProduct);
        configurator.defineSample(
                sourceSampleOffset + srcNoSnowParams.length + srcNoSnowUncertainties.length + SRC_NOSNOW_GOODNESS_OF_FIT,
                goodnessOfFitBandName, noSnowProduct);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        for (int i = 0; i < 3 * numSdrBands; i++) {
            trgParameters[i] = i;
            configurator.defineSample(trgParameters[i], parameterBandNames[i]);
        }

        int index = 0;
        for (int i = 0; i < 3 * numSdrBands; i++) {
            for (int j = i; j < 3 * numSdrBands; j++) {
                trgUncertainties[index] = index;
                configurator.defineSample(trgParameters.length + trgUncertainties[index], uncertaintyBandNames[i][j]);
                index++;
            }
        }

        configurator.defineSample(trgParameters.length + trgUncertainties.length + TRG_ENTROPY, entropyBandName);
        configurator.defineSample(trgParameters.length + trgUncertainties.length + TRG_REL_ENTROPY,
                                  relEntropyBandName);
        configurator.defineSample(trgParameters.length + trgUncertainties.length + TRG_WEIGHTED_NUM_SAMPLES,
                                  weightedNumberOfSamplesBandName);
        configurator.defineSample(trgParameters.length + trgUncertainties.length + TRG_DAYS_CLOSEST_SAMPLE,
                                  daysToTheClosestSampleBandName);
        configurator.defineSample(trgParameters.length + trgUncertainties.length + TRG_GOODNESS_OF_FIT,
                                  goodnessOfFitBandName);
        configurator.defineSample(trgParameters.length + trgUncertainties.length + TRG_PROPORTION_NSAMPLES,
                                  proportionNsamplesBandName);
    }

    private void setupSpectralWaveBandsMap(int numSdrBands) {
        for (int i = 0; i < numSdrBands; i++) {
            spectralWaveBandsMap.put(i, "b" + (i + 1));
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MergeSpectralBrdfOp.class);
        }
    }

}

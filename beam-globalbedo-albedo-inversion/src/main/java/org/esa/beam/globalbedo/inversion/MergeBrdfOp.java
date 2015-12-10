package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;

import static java.lang.Math.pow;
import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS;

/**
 * Operator for merging BRDF Snow/NoSnow products.
 * The breadboard file is 'MergeBRDF.py' provided by Gerardo Lopez Saldana.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.albedo.mergebrdf",
        description = "Provides daily accumulation of single BBDR observations",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2011 by Brockmann Consult")
public class MergeBrdfOp extends PixelOperator {

    // source samples:
    private static final int[] SRC_SNOW_PARAMETERS = new int[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int[] SRC_NOSNOW_PARAMETERS = new int[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    // this offset is the number of UR matrix elements + diagonale. Should be 45 for 9x9 matrix...
    private static final int urMatrixOffset = ((int) pow(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 2.0)
            + 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS) / 2;

    private static final int[] SRC_SNOW_UNCERTAINTIES = new int[urMatrixOffset];
    private static final int[] SRC_NOSNOW_UNCERTAINTIES = new int[urMatrixOffset];

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

    private static final int sourceSampleOffset = 100;

    public static final int priorOffset = (int) pow(AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS, 2.0);
    public static final int SRC_PRIOR_NSAMPLES = 2 * sourceSampleOffset + 2 * priorOffset;

    public static final int SRC_PRIOR_MASK = 2 * sourceSampleOffset + 2 * priorOffset + 1;


    // target samples
    private static final int[] TRG_PARAMETERS = new int[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];


    private static final int[] TRG_UNCERTAINTIES = new int[urMatrixOffset];

    private static final int TRG_ENTROPY = 0;
    private static final int TRG_REL_ENTROPY = 1;
    private static final int TRG_WEIGHTED_NUM_SAMPLES = 2;
    private static final int TRG_DAYS_CLOSEST_SAMPLE = 3;
    private static final int TRG_GOODNESS_OF_FIT = 4;
    private static final int TRG_PROPORTION_NSAMPLES = 5;

    private String[] parameterBandNames = new String[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private String[][] uncertaintyBandNames = new String[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS]
            [3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private String entropyBandName;
    private String relEntropyBandName;
    private String weightedNumberOfSamplesBandName;
    private String daysToTheClosestSampleBandName;
    private String goodnessOfFitBandName;
    private String proportionNsamplesBandName;

    @SourceProduct(description = "BRDF Snow product")
    private Product snowProduct;

    @SourceProduct(description = "BRDF NoSnow product")
    private Product noSnowProduct;

    @SourceProduct(description = "Prior product", optional = true)
    private Product priorProduct;

    @Parameter(defaultValue = "MEAN:_BAND_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    // Oct. 2015:
//    @Parameter(defaultValue = "Mean_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    private String priorMeanBandNamePrefix;

    @Parameter(defaultValue = "SD:_BAND_", description = "Prefix of prior SD band (default fits to the latest prior version)")
//    @Parameter(defaultValue = "Cov_", description = "Prefix of prior SD band (default fits to the latest prior version)")
    private String priorSdBandNamePrefix;

    @Parameter(defaultValue = "7", description = "Prior broad bands start index (default fits to the latest prior version)")
    private int priorBandStartIndex;

    @Parameter(defaultValue = "Weighted_number_of_samples", description = "Prior NSamples band name (default fits to the latest prior version)")
    private String priorNSamplesBandName;

    @Parameter(defaultValue = "land_mask", description = "Prior NSamples band name (default fits to the latest prior version)")
    private String priorLandMaskBandName;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final double nSamplesSnowDataValue = sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length +
                SRC_SNOW_WEIGHTED_NUM_SAMPLES].getDouble();
        final double nSamplesSnow = AlbedoInversionUtils.isValid(nSamplesSnowDataValue) ? nSamplesSnowDataValue : 0.0;

        final double nSamplesNoSnowDataValue = sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length +
                SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_WEIGHTED_NUM_SAMPLES].getDouble();
        final double nSamplesNoSnow = AlbedoInversionUtils.isValid(nSamplesNoSnowDataValue) ? nSamplesNoSnowDataValue : 0.0;

        final double totalNSamples = nSamplesSnow + nSamplesNoSnow;


        final double entropySnowDataValue = sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length +
                SRC_SNOW_ENTROPY].getDouble();
        final double entropySnow = AlbedoInversionUtils.isValid(entropySnowDataValue) ? entropySnowDataValue : 0.0;
        final double entropyNoSnowDataValue = sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length +
                SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_ENTROPY].getDouble();
        final double entropyNoSnow = AlbedoInversionUtils.isValid(entropyNoSnowDataValue) ? entropyNoSnowDataValue : 0.0;

        double proportionNsamplesSnow;
        double proportionNsamplesNoSnow;

        double priorMask = 0.0;
        if (priorProduct != null) {
            final double priorMaskDataValue = sourceSamples[SRC_PRIOR_MASK].getDouble();
            priorMask = AlbedoInversionUtils.isValid(priorMaskDataValue) ? priorMaskDataValue : 0.0;
        }

        if (totalNSamples > 0.0 && (!areSnowSamplesZero() || !areNoSnowSamplesZero())) {
            if ((nSamplesNoSnow == 0.0 && !areNoSnowSamplesZero()) && priorMask == 3.0) {
                // Inland water bodies
                setMergedBandsToZero(sourceSamples, targetSamples);
                targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length +
                        TRG_PROPORTION_NSAMPLES].set(AlbedoInversionConstants.NO_DATA_VALUE);
            } else if (priorMask == 0.0 || priorMask == 2.0) {
                // Shoreline
                setMergedBandsToNoSnowBands(sourceSamples, targetSamples);
                targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_PROPORTION_NSAMPLES].set(0.0);
            } else {
                proportionNsamplesNoSnow = nSamplesNoSnow / totalNSamples;
                proportionNsamplesSnow = nSamplesSnow / totalNSamples;
                setMergedBands(sourceSamples, targetSamples, proportionNsamplesSnow, proportionNsamplesNoSnow);
                targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_PROPORTION_NSAMPLES].set(
                        proportionNsamplesSnow);
            }
        } else if (priorMaskOk(priorMask) && entropySnow != 0.0 && entropyNoSnow != 0.0) {
            if (entropySnow <= entropyNoSnow) {
                setMergedBandsToSnowBands(sourceSamples, targetSamples);
                targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_PROPORTION_NSAMPLES].set(1.0);
            } else {
                setMergedBandsToNoSnowBands(sourceSamples, targetSamples);
                targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_PROPORTION_NSAMPLES].set(0.0);
            }
        } else if (priorMaskOk(priorMask) && entropySnow != 0.0) {
            setMergedBandsToSnowBands(sourceSamples, targetSamples);
            targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_PROPORTION_NSAMPLES].set(1.0);
        } else if (priorMaskOk(priorMask) && entropyNoSnow != 0.0) {
            setMergedBandsToNoSnowBands(sourceSamples, targetSamples);
            targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_PROPORTION_NSAMPLES].set(0.0);
        } else {
            setMergedBandsToZero(sourceSamples, targetSamples);
            targetSamples[TRG_PROPORTION_NSAMPLES].set(AlbedoInversionConstants.NO_DATA_VALUE);
        }
    }

    private boolean areSnowSamplesZero() {
        for (int SRC_SNOW_PARAMETER : SRC_SNOW_PARAMETERS) {
            if (SRC_SNOW_PARAMETER > 0.0) {
                return false;
            }
        }
        return true;
    }

    private boolean areNoSnowSamplesZero() {
        for (int SRC_NOSNOW_PARAMETER : SRC_NOSNOW_PARAMETERS) {
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
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
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
                    targetSamples[TRG_PARAMETERS[index]].set(AlbedoInversionConstants.NO_DATA_VALUE);
                } else {
                    targetSamples[TRG_PARAMETERS[index]].set(resultParameters);
                }
                index++;
            }
        }

        // uncertainties
        index = 0;
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = i; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                final double sampleUncertaintySnowDataValue = sourceSamples[SRC_SNOW_PARAMETERS.length + index].getDouble();
                final double sampleUncertaintySnow =
                        AlbedoInversionUtils.isValid(sampleUncertaintySnowDataValue) ? sampleUncertaintySnowDataValue : 0.0;
                final double sampleUncertaintyNoSnowDataValue = sourceSamples[sourceSampleOffset + SRC_SNOW_PARAMETERS.length + index].getDouble();
                final double sampleUncertaintyNoSnow =
                        AlbedoInversionUtils.isValid(sampleUncertaintyNoSnowDataValue) ? sampleUncertaintyNoSnowDataValue : 0.0;
                final double resultUncertainties = sampleUncertaintySnow * proportionNsamplesSnow +
                        sampleUncertaintyNoSnow * proportionNsamplesNoSnow;

                if (sampleUncertaintySnow == 0.0 && sampleUncertaintyNoSnow == 0.0) {
                    targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES[index]].set(AlbedoInversionConstants.NO_DATA_VALUE);
                } else {
                    targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES[index]].set(resultUncertainties);
                }
                index++;
            }
        }

        final double sampleEntropySnowDataValue = sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_ENTROPY].getDouble();
        final double sampleEntropySnow = AlbedoInversionUtils.isValid
                (sampleEntropySnowDataValue) ? sampleEntropySnowDataValue : 0.0;
        final double sampleEntropyNoSnowDataValue = sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_ENTROPY].getDouble();
        final double sampleEntropyNoSnow = AlbedoInversionUtils.isValid
                (sampleEntropyNoSnowDataValue) ? sampleEntropyNoSnowDataValue : 0.0;
        final double resultEntropy = sampleEntropySnow * proportionNsamplesSnow +
                sampleEntropyNoSnow * proportionNsamplesNoSnow;
        if (sampleEntropyNoSnow == 0.0 && sampleEntropySnow == 0.0) {
            targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_ENTROPY].set(AlbedoInversionConstants.NO_DATA_VALUE);
        } else {
            targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_ENTROPY].set(resultEntropy);
        }

        final double sampleRelEntropySnowDataValue = sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_REL_ENTROPY].getDouble();
        final double sampleRelEntropySnow = AlbedoInversionUtils.isValid(
                sampleRelEntropySnowDataValue) ? sampleRelEntropySnowDataValue : 0.0;
        final double sampleRelEntropyNoSnowDataValue = sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_REL_ENTROPY].getDouble();
        final double sampleRelEntropyNoSnow = AlbedoInversionUtils.isValid(
                sampleRelEntropyNoSnowDataValue) ? sampleRelEntropyNoSnowDataValue : 0.0;
        final double resultRelEntropy = sampleRelEntropySnow * proportionNsamplesSnow +
                sampleRelEntropyNoSnow * proportionNsamplesNoSnow;
        if (sampleRelEntropyNoSnow == 0.0 && sampleRelEntropySnow == 0.0) {
            targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_REL_ENTROPY].set(AlbedoInversionConstants.NO_DATA_VALUE);
        } else {
            targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_REL_ENTROPY].set(resultRelEntropy);
        }

        final double sampleWeightedNumSamplesSnowDataValue = sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_WEIGHTED_NUM_SAMPLES].getDouble();
        final double sampleWeightedNumSamplesSnow = AlbedoInversionUtils.isValid(
                sampleWeightedNumSamplesSnowDataValue) ? sampleWeightedNumSamplesSnowDataValue : 0.0;
        final double sampleWeightedNumSamplesNoSnowDataValue = sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_WEIGHTED_NUM_SAMPLES].getDouble();
        final double sampleWeightedNumSamplesNoSnow = AlbedoInversionUtils.isValid(
                sampleWeightedNumSamplesNoSnowDataValue) ? sampleWeightedNumSamplesNoSnowDataValue : 0.0;
        final double resultWeightedNumSamples = sampleWeightedNumSamplesSnow * proportionNsamplesSnow +
                sampleWeightedNumSamplesNoSnow * proportionNsamplesNoSnow;
        if (sampleWeightedNumSamplesNoSnow == 0.0 && sampleWeightedNumSamplesSnow == 0.0) {
            targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_WEIGHTED_NUM_SAMPLES].set(
                    AlbedoInversionConstants.NO_DATA_VALUE);
        } else {
            targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_WEIGHTED_NUM_SAMPLES].set(
                    resultWeightedNumSamples);
        }

        final double sampleDoyClosestSampleSnowDataValue = sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_DAYS_CLOSEST_SAMPLE].getDouble();
        final double sampleDoyClosestSampleSnow = AlbedoInversionUtils.isValid(
                sampleDoyClosestSampleSnowDataValue) ? sampleDoyClosestSampleSnowDataValue : 0.0;
        final double sampleDoyClosestSampleNoSnowDataValue = sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_DAYS_CLOSEST_SAMPLE].getDouble();
        final double sampleDoyClosestSampleNoSnow = AlbedoInversionUtils.isValid(
                sampleDoyClosestSampleNoSnowDataValue) ? sampleDoyClosestSampleNoSnowDataValue : 0.0;
        final double resultDoyClosestSample = sampleDoyClosestSampleSnow * proportionNsamplesSnow +
                sampleDoyClosestSampleNoSnow * proportionNsamplesNoSnow;
        if (sampleDoyClosestSampleNoSnow == 0.0 && sampleDoyClosestSampleSnow == 0.0) {
            targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_DAYS_CLOSEST_SAMPLE].set(
                    AlbedoInversionConstants.NO_DATA_VALUE);
        } else {
            targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_DAYS_CLOSEST_SAMPLE].set(
                    resultDoyClosestSample);
        }

        final double sampleGoodnessOfFitSnowDataValue = sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_GOODNESS_OF_FIT].getDouble();
        final double sampleGoodnessOfFitSnow = AlbedoInversionUtils.isValid(
                sampleGoodnessOfFitSnowDataValue) ? sampleGoodnessOfFitSnowDataValue : 0.0;
        final double sampleGoodnessOfFitNoSnowDataValue = sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_GOODNESS_OF_FIT].getDouble();
        final double sampleGoodnessOfFitNoSnow = AlbedoInversionUtils.isValid(
                sampleGoodnessOfFitNoSnowDataValue) ? sampleGoodnessOfFitNoSnowDataValue : 0.0;
        final double resultGoodnessOfFit = sampleGoodnessOfFitSnow * proportionNsamplesSnow +
                sampleGoodnessOfFitNoSnow * proportionNsamplesNoSnow;
        if (sampleGoodnessOfFitNoSnow == 0.0 && sampleGoodnessOfFitSnow == 0.0) {
            targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_GOODNESS_OF_FIT].set(AlbedoInversionConstants.NO_DATA_VALUE);
        } else {
            targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_GOODNESS_OF_FIT].set(resultGoodnessOfFit);
        }

    }

    private boolean priorMaskOk(double priorMask) {
        final boolean maskValueOk =
                priorMask != 0.0 && priorMask != 2.0 && priorMask != 3.0 && priorMask != 5.0 && priorMask != 15.0;
        // just do not mask if we have no Prior product
        return priorProduct == null || maskValueOk;
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) throws OperatorException {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        parameterBandNames = IOUtils.getInversionParameterBandNames();
        for (String parameterBandName : parameterBandNames) {
            Band band = targetProduct.addBand(parameterBandName, ProductData.TYPE_FLOAT32);
            band.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
            band.setNoDataValueUsed(true);
        }

        uncertaintyBandNames = IOUtils.getInversionUncertaintyBandNames();
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            // add bands only for UR triangular matrix
            for (int j = i; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                Band band = targetProduct.addBand(uncertaintyBandNames[i][j], ProductData.TYPE_FLOAT32);
                band.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
                band.setNoDataValueUsed(true);
            }
        }

        entropyBandName = AlbedoInversionConstants.INV_ENTROPY_BAND_NAME;
        Band entropyBand = targetProduct.addBand(entropyBandName, ProductData.TYPE_FLOAT32);
        entropyBand.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
        entropyBand.setNoDataValueUsed(true);

        relEntropyBandName = AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME;
        Band relEntropyBand = targetProduct.addBand(relEntropyBandName, ProductData.TYPE_FLOAT32);
        relEntropyBand.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
        relEntropyBand.setNoDataValueUsed(true);

        weightedNumberOfSamplesBandName = AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME;
        Band weightedNumberOfSamplesBand = targetProduct.addBand(weightedNumberOfSamplesBandName,
                                                                 ProductData.TYPE_FLOAT32);
        weightedNumberOfSamplesBand.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
        weightedNumberOfSamplesBand.setNoDataValueUsed(true);

        daysToTheClosestSampleBandName = AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME;
        Band daysToTheClosestSampleBand = targetProduct.addBand(
                daysToTheClosestSampleBandName, ProductData.TYPE_FLOAT32);
        daysToTheClosestSampleBand.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
        daysToTheClosestSampleBand.setNoDataValueUsed(true);

        goodnessOfFitBandName = AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME;
        Band goodnessOfFitBand = targetProduct.addBand(goodnessOfFitBandName, ProductData.TYPE_FLOAT32);
        goodnessOfFitBand.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
        goodnessOfFitBand.setNoDataValueUsed(true);

        proportionNsamplesBandName = AlbedoInversionConstants.MERGE_PROPORTION_NSAMPLES_BAND_NAME;
        Band proportionNsamplesBand = targetProduct.addBand(proportionNsamplesBandName, ProductData.TYPE_FLOAT32);
        proportionNsamplesBand.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
        proportionNsamplesBand.setNoDataValueUsed(true);

//        targetProduct.setPreferredTileSize(100, 100);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {
        // BRDF parameters Snow product:
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_SNOW_PARAMETERS[i] = i;
            configurator.defineSample(SRC_SNOW_PARAMETERS[i], parameterBandNames[i], snowProduct);
        }

        int index = 0;
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = i; j < 3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS; j++) {
                SRC_SNOW_UNCERTAINTIES[index] = index;
                configurator.defineSample(SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES[index],
                                          uncertaintyBandNames[i][j], snowProduct);
                index++;
            }
        }

        configurator.defineSample(SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_ENTROPY,
                                  entropyBandName, snowProduct);
        configurator.defineSample(SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_REL_ENTROPY,
                                  relEntropyBandName, snowProduct);
        configurator.defineSample(
                SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_WEIGHTED_NUM_SAMPLES,
                weightedNumberOfSamplesBandName, snowProduct);
        configurator.defineSample(
                SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_DAYS_CLOSEST_SAMPLE,
                AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME, snowProduct);
        configurator.defineSample(SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_GOODNESS_OF_FIT,
                                  goodnessOfFitBandName, snowProduct);

        // BRDF parameters NoSnow product:
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_NOSNOW_PARAMETERS[i] = sourceSampleOffset + i;
            configurator.defineSample(SRC_NOSNOW_PARAMETERS[i], parameterBandNames[i], noSnowProduct);
        }

        index = 0;
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = i; j < 3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS; j++) {
                SRC_NOSNOW_UNCERTAINTIES[index] = sourceSampleOffset + index;
                configurator.defineSample(SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES[index],
                                          uncertaintyBandNames[i][j], noSnowProduct);
                index++;
            }
        }

        configurator.defineSample(
                sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_ENTROPY,
                entropyBandName, noSnowProduct);
        configurator.defineSample(
                sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_REL_ENTROPY,
                relEntropyBandName, noSnowProduct);
        configurator.defineSample(
                sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_WEIGHTED_NUM_SAMPLES,
                weightedNumberOfSamplesBandName, noSnowProduct);
        configurator.defineSample(
                sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_DAYS_CLOSEST_SAMPLE,
                AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME, noSnowProduct);
        configurator.defineSample(
                sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_GOODNESS_OF_FIT,
                goodnessOfFitBandName, noSnowProduct);

        // prior product:
        // we have:
        // 3x3 mean, 3x3 SD, Nsamples, mask
//        for (int i = 0; i < NUM_ALBEDO_PARAMETERS; i++) {
//            for (int j = 0; j < NUM_ALBEDO_PARAMETERS; j++) {
//                final String indexString = Integer.toString(priorBandStartIndex + i);
//                final String meanBandName = priorMeanBandNamePrefix + indexString + "_PARAMETER_F" + j;
//                final int srcPriorMeanIndex = 2 * sourceSampleOffset + NUM_ALBEDO_PARAMETERS * i + j;
//                configurator.defineSample(srcPriorMeanIndex, meanBandName, priorProduct);
//
//                final String sdMeanBandName = priorSdBandNamePrefix + indexString + "_PARAMETER_F" + j;
//                final int srcPriorSdIndex = 2 * sourceSampleOffset + priorOffset + NUM_ALBEDO_PARAMETERS * i + j;
//                configurator.defineSample(srcPriorSdIndex, sdMeanBandName, priorProduct);
//            }
//        }
//        configurator.defineSample(SRC_PRIOR_NSAMPLES, priorNSamplesBandName, priorProduct);
        if (priorProduct != null) {
            configurator.defineSample(SRC_PRIOR_MASK, priorLandMaskBandName, priorProduct);
        }
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            TRG_PARAMETERS[i] = i;
            configurator.defineSample(TRG_PARAMETERS[i], parameterBandNames[i]);
        }

        int index = 0;
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = i; j < 3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS; j++) {
                TRG_UNCERTAINTIES[index] = index;
                configurator.defineSample(TRG_PARAMETERS.length + TRG_UNCERTAINTIES[index], uncertaintyBandNames[i][j]);
                index++;
            }
        }

        configurator.defineSample(TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_ENTROPY, entropyBandName);
        configurator.defineSample(TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_REL_ENTROPY,
                                  relEntropyBandName);
        configurator.defineSample(TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_WEIGHTED_NUM_SAMPLES,
                                  weightedNumberOfSamplesBandName);
        configurator.defineSample(TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_DAYS_CLOSEST_SAMPLE,
                                  daysToTheClosestSampleBandName);
        configurator.defineSample(TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_GOODNESS_OF_FIT,
                                  goodnessOfFitBandName);
        configurator.defineSample(TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_PROPORTION_NSAMPLES,
                                  proportionNsamplesBandName);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MergeBrdfOp.class);
        }
    }

}

package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.PixelOperator;
import org.esa.beam.framework.gpf.pointop.ProductConfigurer;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.globalbedo.inversion.util.IOUtils;

import static java.lang.Math.*;

/**
 * Operator for merging BRDF Snow/NoSnow products.
 * The breadboard file is 'MergseBRDF.py' provided by Gerardo López Saldaña.
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
    private static final int[] SRC_SNOW_PARAMETERS = new int[3 * AlbedoInversionConstants.numBBDRWaveBands];
    private static final int[] SRC_NOSNOW_PARAMETERS = new int[3 * AlbedoInversionConstants.numBBDRWaveBands];

    // this offset is the number of UR matrix elements + diagonale. Should be 45 for 9x9 matrix...
    private static final int urMatrixOffset = ((int) pow(3 * AlbedoInversionConstants.numBBDRWaveBands, 2.0)
                                               + 3 * AlbedoInversionConstants.numBBDRWaveBands) / 2;

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

    public static final int[][] SRC_PRIOR_MEAN =
            new int[AlbedoInversionConstants.numAlbedoParameters]
                    [AlbedoInversionConstants.numAlbedoParameters];

    public static final int[][] SRC_PRIOR_SD =
            new int[AlbedoInversionConstants.numAlbedoParameters]
                    [AlbedoInversionConstants.numAlbedoParameters];

    public static final int priorOffset = (int) pow(AlbedoInversionConstants.numAlbedoParameters, 2.0);
    public static final int SRC_PRIOR_NSAMPLES = 2 * sourceSampleOffset + 2 * priorOffset;

    public static final int SRC_PRIOR_MASK = 2 * sourceSampleOffset + 2 * priorOffset + 1;


    // target samples
    private static final int[] TRG_PARAMETERS = new int[3 * AlbedoInversionConstants.numBBDRWaveBands];


    private static final int[] TRG_UNCERTAINTIES = new int[urMatrixOffset];

    private static final int TRG_ENTROPY = 0;
    private static final int TRG_REL_ENTROPY = 1;
    private static final int TRG_WEIGHTED_NUM_SAMPLES = 2;
    private static final int TRG_DAYS_CLOSEST_SAMPLE = 3;
    private static final int TRG_GOODNESS_OF_FIT = 4;
    private static final int TRG_PROPORTION_NSAMPLES = 5;

    private String[] parameterBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands];

    private String[][] uncertaintyBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands]
            [3 * AlbedoInversionConstants.numBBDRWaveBands];

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

    @SourceProduct(description = "Prior product")
    private Product priorProduct;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final double nSamplesSnow = sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_WEIGHTED_NUM_SAMPLES].getDouble();
        final double nSamplesNoSnow = sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_WEIGHTED_NUM_SAMPLES].getDouble();
        final double totalNSamples = nSamplesSnow + nSamplesNoSnow;
        final double priorMask = sourceSamples[SRC_PRIOR_MASK].getDouble();

        final double entropySnow = sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_ENTROPY].getDouble();
        final double entropyNoSnow = sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_ENTROPY].getDouble();

        double proportionNsamplesSnow;
        double proportionNsamplesNoSnow;

        if ((x == 346 && y == 422) || (x == 591 && y == 591) || (x == 832 && y == 376) || (x == 842 && y == 369)) {
            System.out.println("x,y = " + x + "," + y);
        }

        if (totalNSamples > 0.0) {
            if (nSamplesNoSnow == 0.0 && priorMask == 3.0) {
                // Inland water bodies
                setMergedBandsToZero(sourceSamples, targetSamples);
            } else if (priorMask == 0.0 || priorMask == 2.0) {
                // Shoreline
                setMergedBandsToSnowBands(sourceSamples, targetSamples);
            } else {
                proportionNsamplesNoSnow = nSamplesNoSnow / totalNSamples;
                proportionNsamplesSnow = nSamplesSnow / totalNSamples;
                setMergedBands(sourceSamples, targetSamples, proportionNsamplesSnow, proportionNsamplesNoSnow);
                targetSamples[TRG_PROPORTION_NSAMPLES].set(proportionNsamplesSnow);
            }
        } else if (priorMaskOk(priorMask) && entropySnow != 0.0 && entropyNoSnow != 0.0) {
            if (entropySnow < entropyNoSnow) {
                setMergedBandsToSnowBands(sourceSamples, targetSamples);
                targetSamples[TRG_PROPORTION_NSAMPLES].set(1.0);
            } else {
                setMergedBandsToNoSnowBands(sourceSamples, targetSamples);
            }
        } else if (priorMaskOk(priorMask) && entropySnow != 0.0) {
            setMergedBandsToSnowBands(sourceSamples, targetSamples);
            targetSamples[TRG_PROPORTION_NSAMPLES].set(1.0);
        } else if (priorMaskOk(priorMask) && entropyNoSnow != 0.0) {
            setMergedBandsToNoSnowBands(sourceSamples, targetSamples);
        } else {
            setMergedBandsToZero(sourceSamples, targetSamples);
        }
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
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = 0; j < AlbedoInversionConstants.numBBDRWaveBands; j++) {
                final double sampleParameterSnow = sourceSamples[index].getDouble();
                final double sampleParameterNoSnow = sourceSamples[sourceSampleOffset + index].getDouble();
                final double resultParameters = sampleParameterSnow * proportionNsamplesSnow +
                                                sampleParameterNoSnow * proportionNsamplesNoSnow;
                targetSamples[TRG_PARAMETERS[index]].set(resultParameters);
                index++;
            }
        }

        // uncertainties
        index = 0;
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = i; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                final double sampleUncertaintySnow = sourceSamples[SRC_SNOW_PARAMETERS.length + index].getDouble();
                final double sampleUncertaintyNoSnow = sourceSamples[sourceSampleOffset + SRC_SNOW_PARAMETERS.length + index].getDouble();
                final double resultUncertainties = sampleUncertaintySnow * proportionNsamplesSnow +
                                                   sampleUncertaintyNoSnow * proportionNsamplesNoSnow;
                targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES[index]].set(resultUncertainties);
                index++;
            }
        }

        final double sampleEntropySnow =
                sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_ENTROPY].getDouble();
        final double sampleEntropyNoSnow =
                sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_ENTROPY].getDouble();
        final double resultEntropy = sampleEntropySnow * proportionNsamplesSnow +
                                     sampleEntropyNoSnow * proportionNsamplesNoSnow;
        targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_ENTROPY].set(resultEntropy);

        final double sampleRelEntropySnow =
                sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_REL_ENTROPY].getDouble();
        final double sampleRelEntropyNoSnow =
                sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_REL_ENTROPY].getDouble();
        final double resultRelEntropy = sampleRelEntropySnow * proportionNsamplesSnow +
                                        sampleRelEntropyNoSnow * proportionNsamplesNoSnow;
        targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_REL_ENTROPY].set(resultRelEntropy);

        final double sampleWeightedNumSamplesSnow =
                sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_WEIGHTED_NUM_SAMPLES].getDouble();
        final double sampleWeightedNumSamplesNoSnow =
                sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_WEIGHTED_NUM_SAMPLES].getDouble();
        final double resultWeightedNumSamples = sampleWeightedNumSamplesSnow * proportionNsamplesSnow +
                                                sampleWeightedNumSamplesNoSnow * proportionNsamplesNoSnow;
        targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_WEIGHTED_NUM_SAMPLES].set(
                resultWeightedNumSamples);

        final double sampleDoyClosestSampleSnow =
                sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_DAYS_CLOSEST_SAMPLE].getDouble();
        final double sampleDoyClosestSampleNoSnow =
                sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_DAYS_CLOSEST_SAMPLE].getDouble();
        final double resultDoyClosestSample = sampleDoyClosestSampleSnow * proportionNsamplesSnow +
                                              sampleDoyClosestSampleNoSnow * proportionNsamplesNoSnow;
        targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_DAYS_CLOSEST_SAMPLE].set(
                resultDoyClosestSample);


        final double sampleGoodnessOfFitSnow =
                sourceSamples[SRC_SNOW_PARAMETERS.length + SRC_SNOW_UNCERTAINTIES.length + SRC_SNOW_GOODNESS_OF_FIT].getDouble();
        final double sampleGoodnessOfFitNoSnow =
                sourceSamples[sourceSampleOffset + SRC_NOSNOW_PARAMETERS.length + SRC_NOSNOW_UNCERTAINTIES.length + SRC_NOSNOW_GOODNESS_OF_FIT].getDouble();
        final double resultGoodnessOfFit = sampleGoodnessOfFitSnow * proportionNsamplesSnow +
                                           sampleGoodnessOfFitNoSnow * proportionNsamplesNoSnow;
        targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_GOODNESS_OF_FIT].set(resultGoodnessOfFit);

    }

    private boolean priorMaskOk(double priorMask) {
        return priorMask != 0.0 && priorMask != 2.0 && priorMask != 3.0 && priorMask != 5.0 && priorMask != 15.0;
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) throws OperatorException {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        parameterBandNames = IOUtils.getInversionParameterBandNames();
        for (String parameterBandName : parameterBandNames) {
            Band band = targetProduct.addBand(parameterBandName, ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }

        uncertaintyBandNames = IOUtils.getInversionUncertaintyBandNames();
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            // add bands only for UR triangular matrix
            for (int j = i; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                Band band = targetProduct.addBand(uncertaintyBandNames[i][j], ProductData.TYPE_FLOAT32);
                band.setNoDataValue(Float.NaN);
                band.setNoDataValueUsed(true);
            }
        }

        entropyBandName = AlbedoInversionConstants.INV_ENTROPY_BAND_NAME;
        Band entropyBand = targetProduct.addBand(entropyBandName, ProductData.TYPE_FLOAT32);
        entropyBand.setNoDataValue(Float.NaN);
        entropyBand.setNoDataValueUsed(true);

        relEntropyBandName = AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME;
        Band relEntropyBand = targetProduct.addBand(relEntropyBandName, ProductData.TYPE_FLOAT32);
        relEntropyBand.setNoDataValue(Float.NaN);
        relEntropyBand.setNoDataValueUsed(true);

        weightedNumberOfSamplesBandName = AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME;
        Band weightedNumberOfSamplesBand = targetProduct.addBand(weightedNumberOfSamplesBandName,
                                                                 ProductData.TYPE_FLOAT32);
        weightedNumberOfSamplesBand.setNoDataValue(Float.NaN);
        weightedNumberOfSamplesBand.setNoDataValueUsed(true);

        daysToTheClosestSampleBandName = AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME;
        Band daysToTheClosestSampleBand = targetProduct.addBand(
                daysToTheClosestSampleBandName, ProductData.TYPE_FLOAT32);
        daysToTheClosestSampleBand.setNoDataValue(Float.NaN);
        daysToTheClosestSampleBand.setNoDataValueUsed(true);

        goodnessOfFitBandName = AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME;
        Band goodnessOfFitBand = targetProduct.addBand(goodnessOfFitBandName, ProductData.TYPE_FLOAT32);
        goodnessOfFitBand.setNoDataValue(Float.NaN);
        goodnessOfFitBand.setNoDataValueUsed(true);

        proportionNsamplesBandName = AlbedoInversionConstants.MERGE_PROPORTION_NSAMPLES_BAND_NAME;
        Band proportionNsamplesBand = targetProduct.addBand(proportionNsamplesBandName, ProductData.TYPE_FLOAT32);
        proportionNsamplesBand.setNoDataValue(Float.NaN);
        proportionNsamplesBand.setNoDataValueUsed(true);

        targetProduct.setPreferredTileSize(100, 100);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {
        // BRDF parameters Snow product:
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            SRC_SNOW_PARAMETERS[i] = i;
            configurator.defineSample(SRC_SNOW_PARAMETERS[i], parameterBandNames[i], snowProduct);
        }

        int index = 0;
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = i; j < 3 * AlbedoInversionConstants.numAlbedoParameters; j++) {
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
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            SRC_NOSNOW_PARAMETERS[i] = sourceSampleOffset + i;
            configurator.defineSample(SRC_NOSNOW_PARAMETERS[i], parameterBandNames[i], noSnowProduct);
        }

        index = 0;
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = i; j < 3 * AlbedoInversionConstants.numAlbedoParameters; j++) {
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
        for (int i = 0; i < AlbedoInversionConstants.numAlbedoParameters; i++) {
            for (int j = 0; j < AlbedoInversionConstants.numAlbedoParameters; j++) {
                final String meanBandName = "MEAN__BAND________" + i + "_PARAMETER_F" + j;
                SRC_PRIOR_MEAN[i][j] = 2 * sourceSampleOffset + AlbedoInversionConstants.numAlbedoParameters * i + j;
                configurator.defineSample(SRC_PRIOR_MEAN[i][j], meanBandName, priorProduct);
            }
        }

        for (int i = 0; i < AlbedoInversionConstants.numAlbedoParameters; i++) {
            for (int j = 0; j < AlbedoInversionConstants.numAlbedoParameters; j++) {
                final String sdMeanBandName = "SD_MEAN__BAND________" + i + "_PARAMETER_F" + j;
                SRC_PRIOR_SD[i][j] = 2 * sourceSampleOffset + priorOffset + AlbedoInversionConstants.numAlbedoParameters * i + j;
                configurator.defineSample(SRC_PRIOR_SD[i][j], sdMeanBandName, priorProduct);
            }
        }
        configurator.defineSample(SRC_PRIOR_NSAMPLES, AlbedoInversionConstants.PRIOR_NSAMPLES_NAME, priorProduct);
        configurator.defineSample(SRC_PRIOR_MASK, AlbedoInversionConstants.PRIOR_MASK_NAME, priorProduct);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            TRG_PARAMETERS[i] = i;
            configurator.defineSample(TRG_PARAMETERS[i], parameterBandNames[i]);
        }

        int index = 0;
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = i; j < 3 * AlbedoInversionConstants.numAlbedoParameters; j++) {
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

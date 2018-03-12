package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;

/**
 * Pixel operator for getting monthly from daily albedo mosaics.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.albedo.monthlyfromdailymosaics",
        description = "Provides monthly from daily albedo mosaics.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2018 by Brockmann Consult")
public class MonthlyFromDailyAlbedoMosaicsOp extends PixelOperator {

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

    private static int[] SRC_DHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] SRC_DHR_ALPHA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] SRC_DHR_SIGMA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] SRC_BHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] SRC_BHR_ALPHA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static int[] SRC_BHR_SIGMA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int SRC_WEIGHTED_NUM_SAMPLES = 18;
    private static final int SRC_SNOW_FRACTION = 19;
    private static final int SRC_REL_ENTROPY = 20;
    private static final int SRC_GOODNESS_OF_FIT = 21;
    private static final int SRC_DATA_MASK = 22;
    private static final int SRC_SZA = 23;

    private static final int SOURCE_SAMPLE_OFFSET = 24;  // this value must be >= number of bands in a source product

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


    @SourceProducts(description = "Albedo daily products")
    private Product[] albedoDailyProducts;

    @Parameter(defaultValue = "1", interval = "[1,12]", description = "Month index")
    private int monthIndex;

    @Parameter(defaultValue = "Merge", valueSet = {"Merge", "Snow", "NoSnow"},
            description = "Input BRDF type, either Merge, Snow or NoSnow.")
    private String snowMode;


    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        double[] monthlyDHR = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[] monthlyBHR = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[] monthlyDHRAlpha = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[] monthlyBHRAlpha = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[] monthlyDHRSigma = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[] monthlyBHRSigma = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double monthlyNsamples = 0.0;
        double monthlyRelativeEntropy = 0.0;
        double monthlyGoodnessOfFit = 0.0;
        double monthlySnowFraction = 0.0;
        double monthlyDataMask = 0.0;
        double monthlySza = 0.0;

        double sumWeights = 0.0;

        for (int j = 0; j < albedoDailyProducts.length; j++) {
//        for (int j = 0; j < 1; j++) {
            final double relEntropy = sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_REL_ENTROPY].getDouble();
            if (AlbedoInversionUtils.isValid(relEntropy) && relEntropy > 0.0) { // the mask is 0.0/1.0, derived from entropy!!!
                final float thisWeight = 1.0f;  // no weighted averaging
                for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                    final double dhrSample = sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_DHR[i]].getDouble();
                    if (AlbedoInversionUtils.isValid(dhrSample)) {
                        monthlyDHR[i] += thisWeight * dhrSample;
                    }
                    final double bhrSample = sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_BHR[i]].getDouble();
                    if (AlbedoInversionUtils.isValid(bhrSample)) {
                        monthlyBHR[i] += thisWeight * bhrSample;
                    }
                    final double dhrAlphaSample = sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_DHR_ALPHA[i]].getDouble();
                    if (AlbedoInversionUtils.isValid(dhrAlphaSample)) {
                        monthlyDHRAlpha[i] += thisWeight * dhrAlphaSample;
                    }
                    final double bhrAlphaSample = sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_BHR_ALPHA[i]].getDouble();
                    if (AlbedoInversionUtils.isValid(bhrAlphaSample)) {
                        monthlyBHRAlpha[i] += thisWeight * bhrAlphaSample;
                    }
                    final double dhrSigmaSample = sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_DHR_SIGMA[i]].getDouble();
                    if (AlbedoInversionUtils.isValid(dhrSigmaSample)) {
                        monthlyDHRSigma[i] += thisWeight * dhrSigmaSample;
                    }
                    final double bhrSigmaSample = sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_BHR_SIGMA[i]].getDouble();
                    if (AlbedoInversionUtils.isValid(bhrSigmaSample)) {
                        monthlyBHRSigma[i] += thisWeight * bhrSigmaSample;
                    }
                }
                if (AlbedoInversionUtils.isValid(sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_WEIGHTED_NUM_SAMPLES].getDouble())) {
                    monthlyNsamples += thisWeight * sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_WEIGHTED_NUM_SAMPLES].getDouble();
                }
                if (AlbedoInversionUtils.isValid(sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_REL_ENTROPY].getDouble())) {
                    monthlyRelativeEntropy += thisWeight * sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_REL_ENTROPY].getDouble();
                }
                if (AlbedoInversionUtils.isValid(sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_GOODNESS_OF_FIT].getDouble())) {
                    monthlyGoodnessOfFit += thisWeight * sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_GOODNESS_OF_FIT].getDouble();
                }
                if (snowMode.equals("Merge")) {
                    if (AlbedoInversionUtils.isValid(sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_SNOW_FRACTION].getDouble())) {
                        monthlySnowFraction += thisWeight * sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_SNOW_FRACTION].getDouble();
                    }
                }
                monthlyDataMask = 1.0;
                if (AlbedoInversionUtils.isValid(sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_SZA].getDouble())) {
                    monthlySza += thisWeight * sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_SZA].getDouble();
                }

                sumWeights += thisWeight;
            }
        }

        if (sumWeights > 0.0) {
            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                monthlyDHR[i] /= sumWeights;
                monthlyBHR[i] /= sumWeights;
                monthlyDHRSigma[i] /= sumWeights;
                monthlyBHRSigma[i] /= sumWeights;
                monthlyDHRAlpha[i] /= sumWeights;
                monthlyBHRAlpha[i] /= sumWeights;
            }
            monthlyNsamples /= sumWeights;
            monthlyRelativeEntropy /= sumWeights;
            monthlyGoodnessOfFit /= sumWeights;
            monthlySnowFraction /= sumWeights;
            monthlySza /= sumWeights;
        }

        AlbedoResult result = new AlbedoResult(monthlyDHR, monthlyDHRAlpha, monthlyDHRSigma,
                monthlyBHR, monthlyBHRAlpha, monthlyBHRSigma,
                monthlyNsamples, monthlyRelativeEntropy, monthlyGoodnessOfFit,
                AlbedoInversionConstants.NO_DATA_VALUE,
                monthlySnowFraction,
                monthlyDataMask, monthlySza);

        fillTargetSamples(targetSamples, result);
    }


    private void fillTargetSamples(WritableSample[] targetSamples, AlbedoResult result) {
        // DHR (black sky albedo)
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetSamples[TRG_DHR[i]].set(result.getBsa()[i]);
        }

        // DHR_ALPHA (black sky albedo)
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetSamples[TRG_DHR_ALPHA[i]].set(result.getBsaAlpha()[i]);
        }

        // DHR_sigma (black sky albedo uncertainty)
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetSamples[TRG_DHR_SIGMA[i]].set(result.getBsaSigmaArray()[i]);
        }

        // BHR (white sky albedo)
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetSamples[TRG_BHR[i]].set(result.getWsa()[i]);
        }

        // BHR_ALPHA (white sky albedo)
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetSamples[TRG_BHR_ALPHA[i]].set(result.getWsaAlpha()[i]);
        }

        // BHR_sigma (white sky albedo uncertainty)
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetSamples[TRG_BHR_SIGMA[i]].set(result.getWsaSigmaArray()[i]);
        }

        targetSamples[TRG_WEIGHTED_NUM_SAMPLES].set(result.getWeightedNumberOfSamples());
        if (snowMode.equals("Merge")) {
            targetSamples[TRG_SNOW_FRACTION].set(result.getSnowFraction());
        }
        targetSamples[TRG_REL_ENTROPY].set(result.getRelEntropy());
        targetSamples[TRG_GOODNESS_OF_FIT].set(result.getGoodnessOfFit());
        targetSamples[TRG_DATA_MASK].set(result.getDataMask());
        targetSamples[TRG_SZA].set(result.getSza());
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        dhrBandNames = IOUtils.getAlbedoDhrBandNames();
        int index = 0;
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_DHR[i] = index;
            index++;
            targetProduct.addBand(dhrBandNames[i], ProductData.TYPE_FLOAT32);
        }

        dhrAlphaBandNames = IOUtils.getAlbedoDhrAlphaBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_DHR_ALPHA[i] = index;
            index++;
            targetProduct.addBand(dhrAlphaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        dhrSigmaBandNames = IOUtils.getAlbedoDhrSigmaBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_DHR_SIGMA[i] = index;
            index++;
            targetProduct.addBand(dhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        bhrBandNames = IOUtils.getAlbedoBhrBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_BHR[i] = index;
            index++;
            targetProduct.addBand(bhrBandNames[i], ProductData.TYPE_FLOAT32);
        }

        bhrAlphaBandNames = IOUtils.getAlbedoBhrAlphaBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_BHR_ALPHA[i] = index;
            index++;
            targetProduct.addBand(bhrAlphaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        bhrSigmaBandNames = IOUtils.getAlbedoBhrSigmaBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_BHR_SIGMA[i] = index;
            index++;
            targetProduct.addBand(bhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        weightedNumberOfSamplesBandName = AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME;
        targetProduct.addBand(weightedNumberOfSamplesBandName, ProductData.TYPE_FLOAT32);

        relEntropyBandName = AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME;
        targetProduct.addBand(relEntropyBandName, ProductData.TYPE_FLOAT32);

        goodnessOfFitBandName = AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME;
        targetProduct.addBand(goodnessOfFitBandName, ProductData.TYPE_FLOAT32);

        if (snowMode.equals("Merge")) {
            snowFractionBandName = AlbedoInversionConstants.ALB_SNOW_FRACTION_BAND_NAME;
            targetProduct.addBand(snowFractionBandName, ProductData.TYPE_FLOAT32);
        }

        dataMaskBandName = AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME;
        targetProduct.addBand(dataMaskBandName, ProductData.TYPE_FLOAT32);

        szaBandName = AlbedoInversionConstants.ALB_SZA_BAND_NAME;
        targetProduct.addBand(szaBandName, ProductData.TYPE_FLOAT32);

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

        for (int j = 0; j < albedoDailyProducts.length; j++) {
//        for (int j = 0; j < 1; j++) {
            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_DHR[i], dhrBandNames[i], albedoDailyProducts[j]);
            }

            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_DHR_ALPHA[i], dhrAlphaBandNames[i], albedoDailyProducts[j]);
            }

            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_BHR[i], bhrBandNames[i], albedoDailyProducts[j]);
            }

            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_BHR_ALPHA[i], bhrAlphaBandNames[i], albedoDailyProducts[j]);
            }

            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_DHR_SIGMA[i], dhrSigmaBandNames[i], albedoDailyProducts[j]);
            }

            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_BHR_SIGMA[i], bhrSigmaBandNames[i], albedoDailyProducts[j]);
            }

            configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_WEIGHTED_NUM_SAMPLES, weightedNumberOfSamplesBandName, albedoDailyProducts[j]);
            configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_REL_ENTROPY, relEntropyBandName, albedoDailyProducts[j]);
            configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_GOODNESS_OF_FIT, goodnessOfFitBandName, albedoDailyProducts[j]);
            if (snowMode.equals("Merge")) {
                configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_SNOW_FRACTION, snowFractionBandName, albedoDailyProducts[j]);
            }
            configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_DATA_MASK, dataMaskBandName, albedoDailyProducts[j]);
            configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_SZA, szaBandName, albedoDailyProducts[j]);
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

        configurator.defineSample(TRG_WEIGHTED_NUM_SAMPLES, weightedNumberOfSamplesBandName);
        if (snowMode.equals("Merge")) {
            configurator.defineSample(TRG_SNOW_FRACTION, snowFractionBandName);
        }
        configurator.defineSample(TRG_REL_ENTROPY, relEntropyBandName);
        configurator.defineSample(TRG_GOODNESS_OF_FIT, goodnessOfFitBandName);
        configurator.defineSample(TRG_DATA_MASK, dataMaskBandName);
        configurator.defineSample(TRG_SZA, szaBandName);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MonthlyFromDailyAlbedoMosaicsOp.class);
        }
    }
}

package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.globalbedo.inversion.util.IOUtils;

/**
 * Pixel operator for getting monthly albedos from 8-day periods.
 * The breadboard file is 'MonthlyAlbedoFrom8day.py' provided by Gerardo Lopez Saldana.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@SuppressWarnings("MismatchedReadAndWriteOfArray")
@OperatorMetadata(alias = "ga.albedo.monthly",
        description = "Provides monthly albedos from 8-day periods",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2011 by Brockmann Consult")
public class MonthlyFrom8DayAlbedoOp extends PixelOperator {

    private String[] dhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] dhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private String relEntropyBandName;
    private String weightedNumberOfSamplesBandName;
    private String goodnessOfFitBandName;
    private String snowFractionBandName;
    private String dataMaskBandName;
    private String szaBandName;

    private static final int[] SRC_DHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int[] SRC_BHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int[] SRC_SIGMA_DHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int[] SRC_SIGMA_BHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int SRC_WEIGHTED_NUM_SAMPLES = 4 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS;
    private static final int SRC_REL_ENTROPY = 4 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS + 1;
    private static final int SRC_GOODNESS_OF_FIT = 4 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS + 2;
    private static final int SRC_SNOW_FRACTION = 4 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS + 3;
    private static final int SRC_DATA_MASK = 4 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS + 4;
    private static final int SRC_SZA = 4 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS + 5;

    public static final int SOURCE_SAMPLE_OFFSET = 20;  // this value must be >= number of bands in a source product

    private static final int[] TRG_DHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int[] TRG_BHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int[] TRG_SIGMA_DHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int[] TRG_SIGMA_BHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int TRG_REL_ENTROPY = 1;
    private static final int TRG_GOODNESS_OF_FIT = 2;
    private static final int TRG_SNOW_FRACTION = 3;
    private static final int TRG_DATA_MASK = 4;
    private static final int TRG_SZA = 5;


    @SourceProducts(description = "Albedo 8-day products")
    private Product[] albedo8DayProduct;

    @Parameter(description = "Monthly weighting coeffs")
    private float[][] monthlyWeighting;

    @Parameter(defaultValue = "1", interval = "[1,12]", description = "Month index")
    private int monthIndex;


    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        double[] monthlyDHR = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[] monthlyBHR = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[] monthlyDHRSigma = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[] monthlyBHRSigma = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double monthlyNsamples = 0.0;
        double monthlyRelativeEntropy = 0.0;
        double monthlyGoodnessOfFit = 0.0;
        double monthlySnowFraction = 0.0;
        double monthlyDataMask = 0.0;
        double monthlySza = 0.0;

        double sumWeights = 0.0;

        for (int j = 0; j < albedo8DayProduct.length; j++) {
            final double dataMask = sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_DATA_MASK].getDouble();
            if (dataMask > 0.0) { // the mask is 0.0/1.0, derived from entropy!!!
                int doy = IOUtils.getDoyFromAlbedoProductName(albedo8DayProduct[j].getName());
                final float thisWeight = monthlyWeighting[monthIndex - 1][doy - 1];
                for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                    monthlyDHR[i] += thisWeight * sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_DHR[i]].getDouble();
                    monthlyBHR[i] += thisWeight * sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_BHR[i]].getDouble();
                    monthlyDHRSigma[i] += thisWeight * sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_SIGMA_DHR[i]].getDouble();
                    monthlyBHRSigma[i] += thisWeight * sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_SIGMA_BHR[i]].getDouble();
                }
                monthlyNsamples += thisWeight * sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_WEIGHTED_NUM_SAMPLES].getDouble();
                monthlyRelativeEntropy += thisWeight * sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_REL_ENTROPY].getDouble();
                monthlyGoodnessOfFit += thisWeight * sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_GOODNESS_OF_FIT].getDouble();
                monthlySnowFraction += thisWeight * sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_SNOW_FRACTION].getDouble();
                monthlyDataMask = 1.0;
                monthlySza += sourceSamples[j * SOURCE_SAMPLE_OFFSET + SRC_SZA].getDouble();

                sumWeights += thisWeight;
            }
        }

        if (sumWeights > 0.0) {
            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                monthlyDHR[i] /= sumWeights;
                monthlyBHR[i] /= sumWeights;
                monthlyDHRSigma[i] /= sumWeights;
                monthlyBHRSigma[i] /= sumWeights;
            }
            monthlyNsamples /= sumWeights;
            monthlyRelativeEntropy /= sumWeights;
            monthlyGoodnessOfFit /= sumWeights;
            monthlySnowFraction /= sumWeights;
            monthlySza /= sumWeights;
        }

        AlbedoResult result = new AlbedoResult(monthlyDHR, monthlyBHR, monthlyDHRSigma, monthlyBHRSigma,
                monthlyNsamples, monthlyRelativeEntropy, monthlyGoodnessOfFit, monthlySnowFraction,
                monthlyDataMask, monthlySza);

        fillTargetSamples(targetSamples, result);
    }


    private void fillTargetSamples(WritableSample[] targetSamples, AlbedoResult result) {
        // DHR (black sky albedo)
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetSamples[TRG_DHR[i]].set(result.getBsa()[i]);
        }

        // BHR (white sky albedo)
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetSamples[TRG_BHR[i]].set(result.getWsa()[i]);
        }

        // DHR_sigma (black sky albedo uncertainty)
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetSamples[TRG_SIGMA_DHR[i]].set(result.getBsaSigmaArray()[i]);
        }

        // BHR_sigma (white sky albedo uncertainty)
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetSamples[TRG_SIGMA_BHR[i]].set(result.getWsaSigmaArray()[i]);
        }

        int index = 4 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS;
        targetSamples[index].set(result.getWeightedNumberOfSamples());
        targetSamples[index + TRG_REL_ENTROPY].set(result.getRelEntropy());
        targetSamples[index + TRG_GOODNESS_OF_FIT].set(result.getGoodnessOfFit());
        targetSamples[index + TRG_SNOW_FRACTION].set(result.getSnowFraction());
        targetSamples[index + TRG_DATA_MASK].set(result.getDataMask());
        targetSamples[index + TRG_SZA].set(result.getSza());
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
            Band band = targetProduct.addBand(dhrBandNames[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }

        bhrBandNames = IOUtils.getAlbedoBhrBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_BHR[i] = index;
            index++;
            Band band = targetProduct.addBand(bhrBandNames[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }

        dhrSigmaBandNames = IOUtils.getAlbedoDhrSigmaBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_SIGMA_DHR[i] = index;
            index++;
            Band band = targetProduct.addBand(dhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }


        bhrSigmaBandNames = IOUtils.getAlbedoBhrSigmaBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_SIGMA_BHR[i] = index;
            index++;
            Band band = targetProduct.addBand(bhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }

        weightedNumberOfSamplesBandName = AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME;
        Band weightedNumberOfSamplesBand = targetProduct.addBand(weightedNumberOfSamplesBandName,
                ProductData.TYPE_FLOAT32);
        weightedNumberOfSamplesBand.setNoDataValue(Float.NaN);
        weightedNumberOfSamplesBand.setNoDataValueUsed(true);

        relEntropyBandName = AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME;
        Band relEntropyBand = targetProduct.addBand(relEntropyBandName, ProductData.TYPE_FLOAT32);
        relEntropyBand.setNoDataValue(Float.NaN);
        relEntropyBand.setNoDataValueUsed(true);

        goodnessOfFitBandName = AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME;
        Band goodnessOfFitBand = targetProduct.addBand(goodnessOfFitBandName, ProductData.TYPE_FLOAT32);
        goodnessOfFitBand.setNoDataValue(Float.NaN);
        goodnessOfFitBand.setNoDataValueUsed(true);

        snowFractionBandName = AlbedoInversionConstants.ALB_SNOW_FRACTION_BAND_NAME;
        Band snowFractionBand = targetProduct.addBand(snowFractionBandName, ProductData.TYPE_FLOAT32);
        snowFractionBand.setNoDataValue(Float.NaN);
        snowFractionBand.setNoDataValueUsed(true);

        dataMaskBandName = AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME;
        Band dataMaskBand = targetProduct.addBand(dataMaskBandName, ProductData.TYPE_FLOAT32);
        dataMaskBand.setNoDataValue(Float.NaN);
        dataMaskBand.setNoDataValueUsed(true);

        szaBandName = AlbedoInversionConstants.ALB_SZA_BAND_NAME;
        // is not in breadboard monthly product
        Band szaBand = targetProduct.addBand(szaBandName, ProductData.TYPE_FLOAT32);
        szaBand.setNoDataValue(Float.NaN);
        szaBand.setNoDataValueUsed(true);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {
        // source products:
        // for a number of source products, we have:
        // 3x DHR, 3x BHR, 3x DHR_sigma, 3x BHR_sigma, weightedNumSamples, relEntropy, goodnessOfFit,#
        // snowFraction, datamask, SZA

        for (int j = 0; j < albedo8DayProduct.length; j++) {
            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_DHR[i], dhrBandNames[i], albedo8DayProduct[j]);
            }

            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_BHR[i], bhrBandNames[i], albedo8DayProduct[j]);
            }

            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_SIGMA_DHR[i], dhrSigmaBandNames[i], albedo8DayProduct[j]);
            }

            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_SIGMA_BHR[i], bhrSigmaBandNames[i], albedo8DayProduct[j]);
            }

            configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_WEIGHTED_NUM_SAMPLES, weightedNumberOfSamplesBandName, albedo8DayProduct[j]);
            configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_REL_ENTROPY, relEntropyBandName, albedo8DayProduct[j]);
            configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_GOODNESS_OF_FIT, goodnessOfFitBandName, albedo8DayProduct[j]);
            configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_SNOW_FRACTION, snowFractionBandName, albedo8DayProduct[j]);
            configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_DATA_MASK, dataMaskBandName, albedo8DayProduct[j]);
            configurator.defineSample(j * SOURCE_SAMPLE_OFFSET + SRC_SZA, szaBandName, albedo8DayProduct[j]);
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
            TRG_BHR[i] = index;
            configurator.defineSample(TRG_BHR[i], bhrBandNames[i]);
            index++;
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            TRG_SIGMA_DHR[i] = index;
            configurator.defineSample(TRG_SIGMA_DHR[i], dhrSigmaBandNames[i]);
            index++;
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            TRG_SIGMA_BHR[i] = index;
            configurator.defineSample(TRG_SIGMA_BHR[i], bhrSigmaBandNames[i]);
            index++;
        }

        configurator.defineSample(index++, weightedNumberOfSamplesBandName);
        configurator.defineSample(index++, relEntropyBandName);
        configurator.defineSample(index++, goodnessOfFitBandName);
        configurator.defineSample(index++, snowFractionBandName);
        configurator.defineSample(index++, dataMaskBandName);
        configurator.defineSample(index++, szaBandName);      // is not in breadboard monthly product
    }

}

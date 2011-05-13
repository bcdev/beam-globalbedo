package org.esa.beam.globalbedo.inversion;

import Jama.LUDecomposition;
import Jama.Matrix;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.PixelOperator;
import org.esa.beam.framework.gpf.pointop.ProductConfigurer;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;

import static java.lang.Math.*;

/**
 * Operator for retrieval of albedo from BRDF model parameters.
 * The breadboard file is 'Albedo.py' provided by Gerardo López Saldaña.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.albedo.albedo",
                  description = "Provides final albedo retrieval from merged BRDF files",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2011 by Brockmann Consult")
public class AlbedoOp extends PixelOperator {

    // source samples:
    private static final int[] SRC_PARAMETERS = new int[3 * AlbedoInversionConstants.numBBDRWaveBands];

    private String[] parameterBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands];

    private String[][] uncertaintyBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands]
            [3 * AlbedoInversionConstants.numBBDRWaveBands];


    // this offset is the number of UR matrix elements + diagonale. Should be 45 for 9x9 matrix...
    private static final int urMatrixOffset = ((int) pow(3 * AlbedoInversionConstants.numBBDRWaveBands, 2.0)
                                               + 3 * AlbedoInversionConstants.numBBDRWaveBands) / 2;

    private static final int[] SRC_UNCERTAINTIES = new int[urMatrixOffset];

    private static final int SRC_ENTROPY = 0;
    private static final int SRC_REL_ENTROPY = 1;
    private static final int SRC_WEIGHTED_NUM_SAMPLES = 2;
    private static final int SRC_DAYS_CLOSEST_SAMPLE = 3;
    private static final int SRC_GOODNESS_OF_FIT = 4;
    private static final int SRC_PROPORTION_NSAMPLE = 5;

    private static final int sourceSampleOffset = 100;

    private String[] dhrBandNames = new String[AlbedoInversionConstants.numBBDRWaveBands];
    private String[] bhrBandNames = new String[AlbedoInversionConstants.numBBDRWaveBands];
    private String[] dhrSigmaBandNames = new String[AlbedoInversionConstants.numBBDRWaveBands];
    private String[] bhrSigmaBandNames = new String[AlbedoInversionConstants.numBBDRWaveBands];

    private String entropyBandName;
    private String relEntropyBandName;
    private String weightedNumberOfSamplesBandName;
    private String daysToTheClosestSampleBandName;
    private String goodnessOfFitBandName;
    private String proportionNsamplesBandName;
    private String snowFractionBandName;
    private String dataMaskBandName;
    private String szaBandName;

    private static final int[] TRG_DHR = new int[AlbedoInversionConstants.numBBDRWaveBands];
    private static final int[] TRG_BHR = new int[AlbedoInversionConstants.numBBDRWaveBands];
    private static final int[] TRG_SIGMA_DHR = new int[AlbedoInversionConstants.numBBDRWaveBands];
    private static final int[] TRG_SIGMA_BHR = new int[AlbedoInversionConstants.numBBDRWaveBands];
    private static final int TRG_WEIGHTED_NUM_SAMPLES = 0;
    private static final int TRG_REL_ENTROPY = 1;
    private static final int TRG_GOODNESS_OF_FIT = 2;
    private static final int TRG_SNOW_FRACTION = 3;
    private static final int TRG_DATA_MASK = 4;
    private static final int TRG_SZA = 5;


    @SourceProduct(description = "BRDF merged product")
    private Product brdfMergedProduct;

    @Parameter(description = "doy")
    private int doy;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        /*
        Get Albedo based on model:
        F = BRDF model parameters (1x9 vector)
        C = Variance/Covariance matrix (9x9 matrix)
        U = Polynomial coefficients

        Black-sky Albedo = f0 + f1 * (-0.007574 + (-0.070887 * SZA^2) + (0.307588 * SZA^3)) + f2 * (-1.284909 + (-0.166314 * SZA^2) + (0.041840 * SZA^3))

        White-sky Albedo = f0 + f1 * (0.189184) + f2 * (-1.377622)

        Uncertainties = U^T C^-1 U , U is stored as a 1X9 vector (transpose), so, actually U^T is the regulat 9x1 vector
        */

        final PixelPos pixelPos = new PixelPos(x, y);
        final GeoPos geoPos = brdfMergedProduct.getGeoCoding().getGeoPos(pixelPos, null);
        final double SZA = AlbedoInversionUtils.computeSza(geoPos, doy);

        Matrix F = new Matrix(AlbedoInversionConstants.numBBDRWaveBands * AlbedoInversionConstants.numAlbedoParameters,
                              1);
        Matrix U = new Matrix(AlbedoInversionConstants.numBBDRWaveBands * AlbedoInversionConstants.numAlbedoParameters,
                              1);

        final Matrix C = getCMatrixFromInversionProduct(sourceSamples);

        Matrix[] wsaSigma = new Matrix[AlbedoInversionConstants.numBBDRWaveBands];
        Matrix[] bsaSigma = new Matrix[AlbedoInversionConstants.numBBDRWaveBands];

        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            wsaSigma[i] = new Matrix(1, 1, Double.NaN);
            bsaSigma[i] = new Matrix(1, 1, Double.NaN);
        }

        Matrix uWsaVis = new Matrix(1, 3 * AlbedoInversionConstants.numBBDRWaveBands);
        Matrix uWsaNir = new Matrix(1, 3 * AlbedoInversionConstants.numBBDRWaveBands);
        Matrix uWsaSw = new Matrix(1, 3 * AlbedoInversionConstants.numBBDRWaveBands);

        uWsaVis.set(0, 0, 1.0);
        uWsaVis.set(0, 1, 0.189184);
        uWsaVis.set(0, 2, -1.377622);
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            uWsaNir.set(0, i + 3, uWsaVis.get(0, i));
            uWsaSw.set(0, i + 6, uWsaVis.get(0, i));
        }

        // # Calculate uncertainties...
        // Breadboard uses relative entropy as maskRelEntropy here
        // but write entropy as Mask in output product!! see BB, GetInversion
        final double maskRelEntropy = sourceSamples[SRC_REL_ENTROPY].getDouble();
        if (maskRelEntropy > 0.0) {
            final LUDecomposition cLUD = new LUDecomposition(C);
            if (cLUD.isNonsingular()) {
                // # Calculate White-Sky sigma
                wsaSigma[0] = uWsaVis.times(C).times(uWsaVis.transpose());
                wsaSigma[1] = uWsaNir.times(C).times(uWsaNir.transpose());
                wsaSigma[2] = uWsaSw.times(C).times(uWsaSw.transpose());

                // # Calculate Black-Sky sigma
                Matrix uBsaVis = new Matrix(1, 3 * AlbedoInversionConstants.numBBDRWaveBands);
                Matrix uBsaNir = new Matrix(1, 3 * AlbedoInversionConstants.numBBDRWaveBands);
                Matrix uBsaSw = new Matrix(1, 3 * AlbedoInversionConstants.numBBDRWaveBands);

                final double[] uBsaArray = new double[]{
                        1.0,
                        -0.007574 + (-0.070887 * Math.pow(SZA, 2.0)) + (0.307588 * Math.pow(SZA, 3.0)),
                        -1.284909 + (-0.166314 * Math.pow(SZA, 2.0)) + (0.041840 * Math.pow(SZA, 3.0))
                };

                for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
                    uBsaVis.set(0, i, uBsaArray[i]);
                    uBsaNir.set(0, i + 3, uBsaArray[i]);
                    uBsaSw.set(0, i + 6, uBsaArray[i]);
                }

                bsaSigma[0] = uBsaVis.times(C).times(uBsaVis.transpose());
                bsaSigma[1] = uBsaNir.times(C).times(uBsaNir.transpose());
                bsaSigma[2] = uBsaSw.times(C).times(uBsaSw.transpose());
            } else {
                for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
                    wsaSigma[i].set(0, 0, Double.NaN);
                    bsaSigma[i].set(0, 0, Double.NaN);
                }
            }
        }

        // Calculate Black-Sky Albedo...
        final int numParams = AlbedoInversionConstants.numBBDRWaveBands * AlbedoInversionConstants.numAlbedoParameters;
        double[] fParams = new double[numParams];
        for (int i = 0; i < numParams; i++) {
            fParams[i] = sourceSamples[i].getDouble();
        }

        double[] blackSkyAlbedo = new double[AlbedoInversionConstants.numBBDRWaveBands];
        for (int i = 0; i < blackSkyAlbedo.length; i++) {
            blackSkyAlbedo[i] = fParams[0 + 3 * i] +
                                fParams[1 + 3 * i] * (-0.007574 + (-0.070887 * Math.pow(SZA,
                                                                                        2.0)) + (0.307588 * Math.pow(
                                        SZA,
                                        3.0))) +
                                fParams[2 + 3 * i] * (-1.284909 + (-0.166314 * Math.pow(SZA,
                                                                                        2.0)) + (0.041840 * Math.pow(
                                        SZA,
                                        3.0)));
        }

        // # Calculate White-Sky Albedo...
        double[] whiteSkyAlbedo = new double[AlbedoInversionConstants.numBBDRWaveBands];
        for (int i = 0; i < whiteSkyAlbedo.length; i++) {
            whiteSkyAlbedo[i] = fParams[0 + 3 * i] + (fParams[1 + 3 * i] * uWsaVis.get(0,
                                                                                       1 + 3 * i)) + (fParams[2 + 3 * i] * uWsaVis.get(
                    0, 2 + 3 * i));
        }

        // # Cap uncertainties and calculate sqrt
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            final double wsa = wsaSigma[i].get(0, 0);
            if (!Double.isNaN(wsa)) {
                wsaSigma[i].set(0, 0, Math.min(1.0, Math.sqrt(wsa)));
            }
            final double bsa = bsaSigma[i].get(0, 0);
            if (!Double.isNaN(bsa)) {
                bsaSigma[i].set(0, 0, Math.min(1.0, Math.sqrt(bsa)));
            }
        }

        // todo: simplify rel. entr.
        double relEntropy = sourceSamples[SRC_REL_ENTROPY].getDouble();
        relEntropy = Math.exp(relEntropy / 9.0);

        // write results to target product...
        double weightedNumberOfSamples = sourceSamples[SRC_WEIGHTED_NUM_SAMPLES].getDouble();
        double goodnessOfFit = sourceSamples[SRC_GOODNESS_OF_FIT].getDouble();
        double snowFraction = sourceSamples[SRC_PROPORTION_NSAMPLE].getDouble();
        double mask = sourceSamples[SRC_ENTROPY].getDouble();
        AlbedoResult result = new AlbedoResult(blackSkyAlbedo, whiteSkyAlbedo, bsaSigma, wsaSigma,
                                               weightedNumberOfSamples, relEntropy, goodnessOfFit, snowFraction,
                                               mask, SZA);

        fillTargetSamples(targetSamples, result);
    }

    private void fillTargetSamples(WritableSample[] targetSamples, AlbedoResult result) {
        // DHR (black sky albedo)
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            targetSamples[TRG_DHR[i]].set(result.getBsa()[i]);
        }

        // BHR (white sky albedo)
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            targetSamples[TRG_BHR[i]].set(result.getWsa()[i]);
        }

        // DHR_sigma (black sky albedo uncertainty)
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            targetSamples[TRG_SIGMA_DHR[i]].set(result.getBsaSigma()[i].get(0, 0));
        }

        // BHR_sigma (white sky albedo uncertainty)
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            targetSamples[TRG_SIGMA_BHR[i]].set(result.getWsaSigma()[i].get(0, 0));
        }

        int index = 4 * AlbedoInversionConstants.numBBDRWaveBands;
        targetSamples[index + TRG_WEIGHTED_NUM_SAMPLES].set(result.getWeightedNumberOfSamples());
        targetSamples[index + TRG_REL_ENTROPY].set(result.getRelEntropy());
        targetSamples[index + TRG_GOODNESS_OF_FIT].set(result.getGoodnessOfFit());
        targetSamples[index + TRG_SNOW_FRACTION].set(result.getSnowFraction());
        targetSamples[index + TRG_DATA_MASK].set(result.getDataMask());
        targetSamples[index + TRG_SZA].set(result.getSza());
    }

    private Matrix getCMatrixFromInversionProduct(Sample[] sourceSamples) {
        Matrix C = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                              3 * AlbedoInversionConstants.numAlbedoParameters);
        double[] cTmp = new double[SRC_UNCERTAINTIES.length];

        int index = 0;
        final int n = AlbedoInversionConstants.numBBDRWaveBands * AlbedoInversionConstants.numBBDRWaveBands;
        for (int i = 0; i < SRC_UNCERTAINTIES.length; i++) {
            final double sampleUncertainty = sourceSamples[SRC_PARAMETERS.length + index].getDouble();
            cTmp[i] = sampleUncertainty;
            index++;
        }

        int index1 = 0;
        int index2 = 0;
        for (int k = n; k > 0; k--) {
            if (k == n) {
                index1 = n;
                index2 = 2 * index1 - 1;
            } else {
                index1 = index2 + 1;
                index2 = index2 + k;
            }

            for (int i = 0; i < k; i++) {
//                System.out.println("i, k, n, index1 = " + i + "," + k + "," + n + "," + index1);
                C.set(n - k, n - k + i, cTmp[index1 - n + i]);
                C.set(n - k + i, n - k, cTmp[index1 - n + i]);
            }
        }

        return C;
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        dhrBandNames = IOUtils.getAlbedoDhrBandNames();
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            Band band = targetProduct.addBand(dhrBandNames[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }

        bhrBandNames = IOUtils.getAlbedoBhrBandNames();
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            Band band = targetProduct.addBand(bhrBandNames[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }

        dhrSigmaBandNames = IOUtils.getAlbedoDhrSigmaBandNames();
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            Band band = targetProduct.addBand(dhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }


        bhrSigmaBandNames = IOUtils.getAlbedoBhrSigmaBandNames();
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
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
        Band szaBand = targetProduct.addBand(szaBandName, ProductData.TYPE_FLOAT32);
        szaBand.setNoDataValue(Float.NaN);
        szaBand.setNoDataValueUsed(true);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {
        // merged BRDF product...
        parameterBandNames = IOUtils.getInversionParameterBandNames();
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            SRC_PARAMETERS[i] = i;
            configurator.defineSample(SRC_PARAMETERS[i], parameterBandNames[i], brdfMergedProduct);
        }

        int index = 0;
        uncertaintyBandNames = IOUtils.getInversionUncertaintyBandNames();
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = i; j < 3 * AlbedoInversionConstants.numAlbedoParameters; j++) {
                SRC_UNCERTAINTIES[index] = index;
                configurator.defineSample(SRC_PARAMETERS.length + SRC_UNCERTAINTIES[index],
                                          uncertaintyBandNames[i][j], brdfMergedProduct);
                index++;
            }
        }

        entropyBandName = AlbedoInversionConstants.INV_ENTROPY_BAND_NAME;
        configurator.defineSample(SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_ENTROPY,
                                  entropyBandName, brdfMergedProduct);
        relEntropyBandName = AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME;
        configurator.defineSample(SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_REL_ENTROPY,
                                  relEntropyBandName, brdfMergedProduct);
        weightedNumberOfSamplesBandName = AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME;
        configurator.defineSample(
                SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_WEIGHTED_NUM_SAMPLES,
                weightedNumberOfSamplesBandName, brdfMergedProduct);
        daysToTheClosestSampleBandName = AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME;
        configurator.defineSample(
                SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_DAYS_CLOSEST_SAMPLE,
                AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME, brdfMergedProduct);
        goodnessOfFitBandName = AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME;
        configurator.defineSample(SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_GOODNESS_OF_FIT,
                                  goodnessOfFitBandName, brdfMergedProduct);
        proportionNsamplesBandName = AlbedoInversionConstants.MERGE_PROPORTION_NSAMPLES_BAND_NAME;
        configurator.defineSample(SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_PROPORTION_NSAMPLE,
                                  proportionNsamplesBandName, brdfMergedProduct);

        // angles product...
        // todo: define
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        int index = 0;
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            TRG_DHR[i] = index;
            configurator.defineSample(TRG_DHR[i], dhrBandNames[i]);
            index++;
        }

        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            TRG_BHR[i] = index;
            configurator.defineSample(TRG_BHR[i], bhrBandNames[i]);
            index++;
        }

        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            TRG_SIGMA_DHR[i] = index;
            configurator.defineSample(TRG_SIGMA_DHR[i], dhrSigmaBandNames[i]);
            index++;
        }

        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            TRG_SIGMA_BHR[i] = index;
            configurator.defineSample(TRG_SIGMA_BHR[i], bhrSigmaBandNames[i]);
            index++;
        }

        configurator.defineSample(index++, weightedNumberOfSamplesBandName);
        configurator.defineSample(index++, relEntropyBandName);
        configurator.defineSample(index++, goodnessOfFitBandName);
        configurator.defineSample(index++, snowFractionBandName);
        configurator.defineSample(index++, dataMaskBandName);
        configurator.defineSample(index++, szaBandName);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AlbedoOp.class);
        }
    }

}

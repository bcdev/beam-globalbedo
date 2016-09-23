package org.esa.beam.globalbedo.inversion.spectral;

import Jama.LUDecomposition;
import Jama.Matrix;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.AlbedoResult;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.util.math.MathUtils;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.pow;

/**
 * Operator for retrieval of spectral albedo from spectral BRDF model parameters.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.albedo.albedo.spectral",
        description = "Provides final spectral albedo from spectral BRDF files",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")
public class SpectralBrdfToAlbedoOp extends PixelOperator {

    @SourceProduct(description = "Spectral BRDF product")
    private Product spectralBrdfProduct;

    @Parameter(description = "doy")
    private int doy;

    @Parameter(defaultValue = "7", description = "Number of spectral bands (7 for standard MODIS spectral mapping")
    private int numSdrBands;

    private static final int SRC_ENTROPY = 0;

    private static final int SRC_REL_ENTROPY = 1;
    private static final int SRC_WEIGHTED_NUM_SAMPLES = 2;
    private static final int SRC_DAYS_CLOSEST_SAMPLE = 3;
    private static final int SRC_GOODNESS_OF_FIT = 4;
    private static final int SRC_PROPORTION_NSAMPLE = 5;

    private int[] srcParameters;
    private int[] srcUncertainties;

    private String[] dhrBandNames;
    private String[] bhrBandNames;
    private String[][] dhrAlphaBandNames;
    private String[][] bhrAlphaBandNames;
    private String[] dhrSigmaBandNames;
    private String[] bhrSigmaBandNames;

    private String relEntropyBandName;
    private String weightedNumberOfSamplesBandName;
    private String goodnessOfFitBandName;
    private String snowFractionBandName;
    private String dataMaskBandName;
    private String szaBandName;

    private String[] parameterBandNames;
    private String[][] uncertaintyBandNames;

    private Map<Integer, String> spectralWaveBandsMap = new HashMap<>();
    private int numAlphaTerms;

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        setupSpectralWaveBandsMap(numSdrBands);

        srcParameters = new int[3 * numSdrBands];
        final int urMatrixOffset = ((int) pow(3 * numSdrBands, 2.0) + 3 * numSdrBands) / 2;
        srcUncertainties = new int[urMatrixOffset];

        numAlphaTerms = ((numSdrBands - 1) * (numSdrBands - 1) - (numSdrBands - 1)) / 2 + (numSdrBands - 1);

        dhrBandNames = new String[numSdrBands];
        bhrBandNames = new String[numSdrBands];
        dhrAlphaBandNames = new String[numSdrBands - 1][numSdrBands - 1];
        bhrAlphaBandNames = new String[numSdrBands - 1][numSdrBands - 1];
        dhrSigmaBandNames = new String[numSdrBands];
        bhrSigmaBandNames = new String[numSdrBands];

        parameterBandNames = SpectralIOUtils.getSpectralInversionParameterBandNames(numSdrBands);
        uncertaintyBandNames = SpectralIOUtils.getSpectralInversionUncertaintyBandNames(numSdrBands, spectralWaveBandsMap);
    }

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
        final GeoPos latLon = spectralBrdfProduct.getGeoCoding().getGeoPos(pixelPos, null);
        final double SZAdeg = AlbedoInversionUtils.computeSza(latLon, doy);
        final double SZA = SZAdeg * MathUtils.DTOR;

        final Matrix C = getCMatrixFromSpectralInversionProduct(sourceSamples);

        Matrix[] sigmaBHR = new Matrix[numSdrBands];
        Matrix[] sigmaDHR = new Matrix[numSdrBands];
        Matrix[] uWsa = new Matrix[numSdrBands];
        Matrix[] uBsa = new Matrix[numSdrBands];

        for (int i = 0; i < numSdrBands; i++) {
            uWsa[i] = new Matrix(1, 3 * numSdrBands);
            uBsa[i] = new Matrix(1, 3 * numSdrBands);

            sigmaBHR[i] = new Matrix(1, 1, AlbedoInversionConstants.NO_DATA_VALUE);
            sigmaDHR[i] = new Matrix(1, 1, AlbedoInversionConstants.NO_DATA_VALUE);
        }

        // todo: Lewis to provide uWsaArray numbers for spectral approach, likely different to broadband case
//        uWsaVis.set(0, 0, 1.0);
//        uWsaVis.set(0, 1, 0.189184);
//        uWsaVis.set(0, 2, -1.377622);
//        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
//            uWsaNir.set(0, i + 3, uWsaVis.get(0, i));
//            uWsaSw.set(0, i + 6, uWsaVis.get(0, i));
//        }
        // becomes for spectral approach:
        // numbers from table 3-3 ATBD
        final double[] uWsaArray = new double[]{
                1.0,
                0.189184,
                -1.377622
        };
        for (int i = 0; i < numSdrBands; i++) {
            for (int j = 0; j < AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS; j++) {
                uWsa[i].set(0, 3 * i + j, uWsaArray[j]);
            }
        }

        // # Calculate uncertainties...
        // Breadboard uses relative entropy as maskRelEntropy here
        // but write entropy as Mask in output product!! see BB, GetInversion
//        double maskRelEntropyDataValue = sourceSamples[srcParameters.length + srcUncertainties.length + SRC_REL_ENTROPY].getDouble();
//        double maskRelEntropy = AlbedoInversionUtils.isValid(maskRelEntropyDataValue) ?
//                Math.exp(maskRelEntropyDataValue / 9.0) : 0.0;
        double maskEntropyDataValue = sourceSamples[srcParameters.length + srcUncertainties.length + SRC_ENTROPY].getDouble();
//        if (maskRelEntropy > 0.0) {
        if (AlbedoInversionUtils.isValid(maskEntropyDataValue)) {
            final LUDecomposition cLUD = new LUDecomposition(C.transpose());
            if (cLUD.isNonsingular()) {
                // # Calculate White-Sky sigma
                for (int i = 0; i < numSdrBands; i++) {
                    sigmaBHR[i] = uWsa[i].times(C.transpose()).times(uWsa[i].transpose());
                }

                // # Calculate Black-Sky sigma
                for (int i = 0; i < numSdrBands; i++) {
                    uBsa[i] = new Matrix(1, 3 * numSdrBands);
                }

                // todo: Lewis to provide uBsaArray numbers for spectral approach
                final double[] uBsaArray = new double[]{
                        1.0,
                        -0.007574 + (-0.070887 * Math.pow(SZA, 2.0)) + (0.307588 * Math.pow(SZA, 3.0)),
                        -1.284909 + (-0.166314 * Math.pow(SZA, 2.0)) + (0.041840 * Math.pow(SZA, 3.0))
                };

//                for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
//                    uBsaVis.set(0, i, uBsaArray[i]);
//                    uBsaNir.set(0, i + 3, uBsaArray[i]);
//                    uBsaSw.set(0, i + 6, uBsaArray[i]);
//                }
                // becomes for spectral approach:
                for (int i = 0; i < numSdrBands; i++) {
                    for (int j = 0; j < AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS; j++) {
                        uBsa[i].set(0, 3 * i + j, uBsaArray[j]);
                    }
                }

                for (int i = 0; i < numSdrBands; i++) {
                    sigmaDHR[i] = uBsa[i].times(C.transpose()).times(uBsa[i].transpose());
                }
            } else {
                for (int i = 0; i < numSdrBands; i++) {
                    sigmaBHR[i].set(0, 0, AlbedoInversionConstants.NO_DATA_VALUE);
                    sigmaDHR[i].set(0, 0, AlbedoInversionConstants.NO_DATA_VALUE);
                }
            }
        }

        // Calculate Black-Sky Albedo...
        final int numParams = numSdrBands * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS;
        double[] fParams = new double[numParams];
        for (int i = 0; i < numParams; i++) {
            fParams[i] = sourceSamples[i].getDouble();
        }

        double[] DHR = new double[numSdrBands];
        for (int i = 0; i < DHR.length; i++) {
            if (AlbedoInversionUtils.isValid(fParams[3 * i]) && AlbedoInversionUtils.isValid(fParams[1 + 3 * i]) &&
                    AlbedoInversionUtils.isValid(fParams[2 + 3 * i])) {
                DHR[i] = fParams[3 * i] +
                        fParams[1 + 3 * i] * uBsa[i].get(0, 3 * i + 1) +
                        fParams[2 + 3 * i] * uBsa[i].get(0, 3 * i + 2);
            } else {
                DHR[i] = AlbedoInversionConstants.NO_DATA_VALUE;
            }
        }

        // # Calculate White-Sky Albedo...
        double[] BHR = new double[numSdrBands];
        for (int i = 0; i < BHR.length; i++) {
            if (AlbedoInversionUtils.isValid(fParams[3 * i]) && AlbedoInversionUtils.isValid(fParams[1 + 3 * i]) &&
                    AlbedoInversionUtils.isValid(fParams[2 + 3 * i])) {
                BHR[i] = fParams[3 * i] +
                        fParams[1 + 3 * i] * uWsa[i].get(0, 3 * i + 1) +
                        fParams[2 + 3 * i] * uWsa[i].get(0, 3 * i + 2);
            } else {
                BHR[i] = AlbedoInversionConstants.NO_DATA_VALUE;
            }
        }


        double relEntropy = sourceSamples[srcParameters.length + srcUncertainties.length + SRC_REL_ENTROPY].getDouble();
        if (AlbedoInversionUtils.isValid(relEntropy)) {
            relEntropy = Math.exp(relEntropy / 9.0);
        }

        double[] alphaDHR = new double[numAlphaTerms];
        double[] alphaBHR = new double[numAlphaTerms];

        // calculate alpha terms
//        if (AlbedoInversionUtils.isValid(relEntropy)) {
        if (AlbedoInversionUtils.isValid(maskEntropyDataValue)) {
            alphaDHR = computeSpectralAlphaDHR(SZA, C); // bsa = DHR
            alphaBHR = computeSpectralAlphaBHR(C);      // wsa = BHR
        } else {
            for (int i = 0; i < numAlphaTerms; i++) {
                alphaDHR[i] = AlbedoInversionConstants.NO_DATA_VALUE;
                alphaBHR[i] = AlbedoInversionConstants.NO_DATA_VALUE;
            }
        }

        // # Cap uncertainties and calculate sqrt
        for (int i = 0; i < numSdrBands; i++) {
            final double wsa = sigmaBHR[i].get(0, 0);
            if (AlbedoInversionUtils.isValid(wsa)) {
                sigmaBHR[i].set(0, 0, Math.min(1.0, Math.sqrt(wsa)));
            }
            final double bsa = sigmaDHR[i].get(0, 0);
            if (AlbedoInversionUtils.isValid(bsa)) {
                sigmaDHR[i].set(0, 0, Math.min(1.0, Math.sqrt(bsa)));
            }
        }

        // write results to target product...
        final double weightedNumberOfSamples = sourceSamples[srcParameters.length + srcUncertainties.length + SRC_WEIGHTED_NUM_SAMPLES].getDouble();
        final double goodnessOfFit = sourceSamples[srcParameters.length + srcUncertainties.length + SRC_GOODNESS_OF_FIT].getDouble();
        final double snowFraction = sourceSamples[srcParameters.length + srcUncertainties.length + SRC_PROPORTION_NSAMPLE].getDouble();
        final double entropy = sourceSamples[srcParameters.length + srcUncertainties.length + SRC_ENTROPY].getDouble();
        final double maskEntropy = (AlbedoInversionUtils.isValid(entropy)) ? 1.0 : 0.0;
        AlbedoResult result = new AlbedoResult(DHR, alphaDHR, sigmaDHR,
                                               BHR, alphaBHR, sigmaBHR,
                                               weightedNumberOfSamples, relEntropy, goodnessOfFit, snowFraction,
                                               maskEntropy, SZAdeg);

        fillTargetSamples(targetSamples, result);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        final Product targetProduct = productConfigurer.getTargetProduct();

        dhrBandNames = SpectralIOUtils.getSpectralAlbedoDhrBandNames(numSdrBands, spectralWaveBandsMap);
        for (int i = 0; i < numSdrBands; i++) {
            targetProduct.addBand(dhrBandNames[i], ProductData.TYPE_FLOAT32);
        }

        dhrAlphaBandNames = SpectralIOUtils.getSpectralAlbedoAlphaBandNames("DHR", numSdrBands, spectralWaveBandsMap);
        for (int i = 0; i < numSdrBands - 1; i++) {
            for (int j = i; j < numSdrBands - 1; j++) {
                targetProduct.addBand(dhrAlphaBandNames[i][j], ProductData.TYPE_FLOAT32);
            }
        }

        dhrSigmaBandNames = SpectralIOUtils.getSpectralAlbedoDhrSigmaBandNames(numSdrBands, spectralWaveBandsMap);
        for (int i = 0; i < numSdrBands; i++) {
            targetProduct.addBand(dhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        bhrBandNames = SpectralIOUtils.getSpectralAlbedoBhrBandNames(numSdrBands, spectralWaveBandsMap);
        for (int i = 0; i < numSdrBands; i++) {
            targetProduct.addBand(bhrBandNames[i], ProductData.TYPE_FLOAT32);
        }

        bhrAlphaBandNames = SpectralIOUtils.getSpectralAlbedoAlphaBandNames("BHR", numSdrBands, spectralWaveBandsMap);
        for (int i = 0; i < numSdrBands - 1; i++) {
            for (int j = i; j < numSdrBands - 1; j++) {
                targetProduct.addBand(bhrAlphaBandNames[i][j], ProductData.TYPE_FLOAT32);
            }
        }

        bhrSigmaBandNames = SpectralIOUtils.getSpectralAlbedoBhrSigmaBandNames(numSdrBands, spectralWaveBandsMap);
        for (int i = 0; i < numSdrBands; i++) {
            targetProduct.addBand(bhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        weightedNumberOfSamplesBandName = AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME;
        targetProduct.addBand(weightedNumberOfSamplesBandName, ProductData.TYPE_FLOAT32);

        relEntropyBandName = AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME;
        targetProduct.addBand(relEntropyBandName, ProductData.TYPE_FLOAT32);

        goodnessOfFitBandName = AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME;
        targetProduct.addBand(goodnessOfFitBandName, ProductData.TYPE_FLOAT32);

        snowFractionBandName = AlbedoInversionConstants.ALB_SNOW_FRACTION_BAND_NAME;
        targetProduct.addBand(snowFractionBandName, ProductData.TYPE_FLOAT32);

        dataMaskBandName = AlbedoInversionConstants.ALB_DATA_MASK_BAND_NAME;
        targetProduct.addBand(dataMaskBandName, ProductData.TYPE_FLOAT32);

        szaBandName = AlbedoInversionConstants.ALB_SZA_BAND_NAME;
        targetProduct.addBand(szaBandName, ProductData.TYPE_FLOAT32);

        for (Band b : targetProduct.getBands()) {
            b.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
            b.setNoDataValueUsed(true);
        }

        for (Band b : targetProduct.getBands()) {
            b.setValidPixelExpression(AlbedoInversionConstants.SEAICE_ALBEDO_VALID_PIXEL_EXPRESSION);
        }

    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {
        // (merged?) BRDF product...
        parameterBandNames = SpectralIOUtils.getSpectralInversionParameterBandNames(numSdrBands);
        for (int i = 0; i < 3 * numSdrBands; i++) {
            srcParameters[i] = i;
            configurator.defineSample(srcParameters[i], parameterBandNames[i], spectralBrdfProduct);
        }

        int index = 0;
        uncertaintyBandNames = SpectralIOUtils.getSpectralInversionUncertaintyBandNames(numSdrBands, spectralWaveBandsMap);
        for (int i = 0; i < 3 * numSdrBands; i++) {
            for (int j = i; j < 3 * numSdrBands; j++) {
                srcUncertainties[index] = index;
                configurator.defineSample(srcParameters.length + srcUncertainties[index],
                                          uncertaintyBandNames[i][j], spectralBrdfProduct);
                index++;
            }
        }

        String entropyBandName = AlbedoInversionConstants.INV_ENTROPY_BAND_NAME;
        configurator.defineSample(srcParameters.length + srcUncertainties.length + SRC_ENTROPY,
                                  entropyBandName, spectralBrdfProduct);
        relEntropyBandName = AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME;
        configurator.defineSample(srcParameters.length + srcUncertainties.length + SRC_REL_ENTROPY,
                                  relEntropyBandName, spectralBrdfProduct);
        weightedNumberOfSamplesBandName = AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME;
        configurator.defineSample(
                srcParameters.length + srcUncertainties.length + SRC_WEIGHTED_NUM_SAMPLES,
                weightedNumberOfSamplesBandName, spectralBrdfProduct);
        configurator.defineSample(
                srcParameters.length + srcUncertainties.length + SRC_DAYS_CLOSEST_SAMPLE,
                AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME, spectralBrdfProduct);
        goodnessOfFitBandName = AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME;
        configurator.defineSample(srcParameters.length + srcUncertainties.length + SRC_GOODNESS_OF_FIT,
                                  goodnessOfFitBandName, spectralBrdfProduct);
        String proportionNsamplesBandName = AlbedoInversionConstants.MERGE_PROPORTION_NSAMPLES_BAND_NAME;
        configurator.defineSample(srcParameters.length + srcUncertainties.length + SRC_PROPORTION_NSAMPLE,
                                  proportionNsamplesBandName, spectralBrdfProduct);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        int index = 0;
        for (int i = 0; i < numSdrBands; i++) {
            configurator.defineSample(index++, dhrBandNames[i]);
        }

        for (int i = 0; i < numSdrBands - 1; i++) {
            for (int j = i; j < numSdrBands - 1; j++) {
                configurator.defineSample(index++, dhrAlphaBandNames[i][j]);
            }
        }

        for (int i = 0; i < numSdrBands; i++) {
            configurator.defineSample(index++, bhrBandNames[i]);
        }

        for (int i = 0; i < numSdrBands - 1; i++) {
            for (int j = i; j < numSdrBands - 1; j++) {
                configurator.defineSample(index++, bhrAlphaBandNames[i][j]);
            }
        }

        for (int i = 0; i < numSdrBands; i++) {
            configurator.defineSample(index++, dhrSigmaBandNames[i]);
        }

        for (int i = 0; i < numSdrBands; i++) {
            configurator.defineSample(index++, bhrSigmaBandNames[i]);
        }

        configurator.defineSample(index++, weightedNumberOfSamplesBandName);
        configurator.defineSample(index++, relEntropyBandName);
        configurator.defineSample(index++, goodnessOfFitBandName);
        configurator.defineSample(index++, snowFractionBandName);
        configurator.defineSample(index++, dataMaskBandName);
        configurator.defineSample(index, szaBandName);

    }

    private void setupSpectralWaveBandsMap(int numSdrBands) {
        for (int i = 0; i < numSdrBands; i++) {
            spectralWaveBandsMap.put(i, "b" + (i + 1));
        }
    }

    private double[] computeSpectralAlphaDHR(double SZA, Matrix c) {
        double[] alphaDHR = new double[numAlphaTerms];

        for (int i = 0; i < numAlphaTerms; i++) {
            // todo: SK to provide algorithm adapted to numSdrBands  (see email OD --> SB 3.9.2015)
            alphaDHR[i] = AlbedoInversionConstants.NO_DATA_VALUE;
        }

        return alphaDHR;
    }

    private double[] computeSpectralAlphaBHR(Matrix c) {
        double[] alphaBHR = new double[numAlphaTerms];    // should have 28 terms now!?

        for (int i = 0; i < numAlphaTerms; i++) {
            // todo: SK to provide algorithm adapted to numSdrBands  (see email OD --> SB 3.9.2015)
            alphaBHR[i] = AlbedoInversionConstants.NO_DATA_VALUE;
        }

        return alphaBHR;
    }

    private void fillTargetSamples(WritableSample[] targetSamples, AlbedoResult result) {
        // DHR (black sky albedo)
        int index = 0;
        for (int i = 0; i < numSdrBands; i++) {
            targetSamples[index++].set(result.getBsa()[i]);
        }

        // DHR_ALPHA (black sky albedo)
        int dhrAlphaIndex = 0;
        for (int i = 0; i < numSdrBands - 1; i++) {
            for (int j = i; j < numSdrBands - 1; j++) {
                targetSamples[index++].set(result.getBsaAlpha()[dhrAlphaIndex++]);
            }
        }

        // BHR (white sky albedo)
        for (int i = 0; i < numSdrBands; i++) {
            targetSamples[index++].set(result.getWsa()[i]);
        }

        // BHR_ALPHA (white sky albedo)
        int bhrAlphaIndex = 0;
        for (int i = 0; i < numSdrBands - 1; i++) {
            for (int j = i; j < numSdrBands - 1; j++) {
                targetSamples[index++].set(result.getWsaAlpha()[bhrAlphaIndex++]);
            }
        }

        // DHR_sigma (black sky albedo uncertainty)
        for (int i = 0; i < numSdrBands; i++) {
            targetSamples[index++].set(result.getBsaSigma()[i].get(0, 0));
        }

        // BHR_sigma (white sky albedo uncertainty)
        for (int i = 0; i < numSdrBands; i++) {
            targetSamples[index++].set(result.getWsaSigma()[i].get(0, 0));
        }

        targetSamples[index++].set(result.getWeightedNumberOfSamples());
        targetSamples[index++].set(result.getRelEntropy());
        targetSamples[index++].set(result.getGoodnessOfFit());
        targetSamples[index++].set(result.getSnowFraction());
        targetSamples[index++].set(result.getDataMask());
        targetSamples[index].set(result.getSza());
    }

    private Matrix getCMatrixFromSpectralInversionProduct(Sample[] sourceSamples) {
        final int n = numSdrBands * numSdrBands;
        Matrix C = new Matrix(n, n);
        double[] cTmp = new double[srcUncertainties.length];

        int index = 0;
        for (int i = 0; i < srcUncertainties.length; i++) {
            final double sampleUncertainty = sourceSamples[srcParameters.length + index].getDouble();
            cTmp[i] = sampleUncertainty;
            index++;
        }

        int index1;
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
                // todo: this does not work for 7 bands, index 'finalIndex' becomes larger than #uncertainties
                // maybe this worked by accident for numBands=3
                // C matrix affects alpha and sigma terms (not albedos)
                // see code in Albedo.py and clarify with Gerardo!
//                final int finalIndex = index1 - n + i;    // original
                final int finalIndex = (index1 - n) / 7 + i;  // keep index reasonably small fort the moment to get code running
//                System.out.println("n,k,i,index1,index2,n-k,n-k+i,finalIndex = " + n + "," + k + "," + i + "," + index1 + "," + index2 + "," +
//                                           (n-k) + "," + (n-k+i) + "," + finalIndex);
                C.set(n - k, n - k + i, cTmp[finalIndex]);
                C.set(n - k + i, n - k, cTmp[finalIndex]);
            }
        }

        return C;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SpectralBrdfToAlbedoOp.class);
        }
    }
}

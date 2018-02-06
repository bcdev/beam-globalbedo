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
@OperatorMetadata(alias = "ga.albedo.albedo.spectral.uncertainties",
        description = "Provides final spectral albedo uncertainties from spectral BRDF uncertainties",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")
public class SpectralBrdfUncertaintiesToAlbedoUncertaintiesOp extends PixelOperator {

    @SourceProduct(description = "Spectral BRDF product")
    private Product spectralBrdfProduct;

    @SourceProduct(description = "Spectral BRDF uncertainties product")
    private Product spectralBrdfUncertaintiesProduct;

    @Parameter(description = "doy", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "7", description = "Number of spectral bands (7 for standard MODIS spectral mapping")
    private int numSdrBands;

    @Parameter(defaultValue = "4,3,2", description = "Band indices (3 spectral bands)")
    private int[] bandIndices;


    private static final int SRC_ENTROPY = 0;

    private int[] srcParameters;
    private int[] srcUncertainties;

    private String[] dhrSigmaBandNames;
    private String[] bhrSigmaBandNames;

    private String dataMaskBandName;

    private String[] parameterBandNames;
    private String[] uncertaintyBandNames;

    private Map<Integer, String> spectralWaveBandsMap = new HashMap<>();

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        if (bandIndices.length != 3) {
            throw new OperatorException("3 band indices must be specified!");
        }
        for (int i = 0; i < bandIndices.length; i++) {
            if (bandIndices[i] > 7 || bandIndices[i] < 1) {
                throw new OperatorException("band index '" + bandIndices[i] + "' out of range!");
            }
            spectralWaveBandsMap.put(0, "b" + (bandIndices[i]));
        }

        // todo: setup correct numbers
        srcParameters = new int[3 * numSdrBands];
        final int urMatrixOffset = ((int) pow(3 * numSdrBands, 2.0) + 3 * numSdrBands) / 2;
        srcUncertainties = new int[urMatrixOffset];

        // we only have diagonal terms in QA4ECV spectral approach
        srcUncertainties = new int[3 * numSdrBands];

        dhrSigmaBandNames = new String[numSdrBands];
        bhrSigmaBandNames = new String[numSdrBands];

        parameterBandNames = SpectralIOUtils.getSpectralInversionParameter3BandNames(bandIndices);
        uncertaintyBandNames = SpectralIOUtils.getSpectralInversionUncertainty3BandNames(bandIndices, spectralWaveBandsMap);
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
        final GeoPos latLon = spectralBrdfUncertaintiesProduct.getGeoCoding().getGeoPos(pixelPos, null);
        final double SZAdeg = AlbedoInversionUtils.computeSza(latLon, doy);
        final double SZA = SZAdeg * MathUtils.DTOR;

        // todo: clarify issues:
        // implementation follows breadboard, assuming 3 broadbands.
        // Does not work for 7 bands at all.
        // Maybe does not work at all, as we have only diagonal elements of full uncertainty matrix.
        // Scientists to provide new scheme for uncertainties, and following sigma/alpha computation.
        // For the moment, set alpha/sigma terms to zero or leave out (20171116)
        // In Jan 2017, we only processed until BRDF and did not notice this issue.
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

//        if (x == 600 && y == 550) {
//            System.out.println("x = " + x);
//        }

        double maskEntropyDataValue = sourceSamples[srcParameters.length + srcUncertainties.length + SRC_ENTROPY].getDouble();

        // skip for the moment (20171116):
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
        fillTargetSamples(targetSamples, sigmaDHR, sigmaBHR);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        final Product targetProduct = productConfigurer.getTargetProduct();

        dhrSigmaBandNames = SpectralIOUtils.getSpectralAlbedoDhrSigmaBandNames(numSdrBands, spectralWaveBandsMap);
        for (int i = 0; i < numSdrBands; i++) {
            targetProduct.addBand(dhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        bhrSigmaBandNames = SpectralIOUtils.getSpectralAlbedoBhrSigmaBandNames(numSdrBands, spectralWaveBandsMap);
        for (int i = 0; i < numSdrBands; i++) {
            targetProduct.addBand(bhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        for (Band b : targetProduct.getBands()) {
            b.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
            b.setNoDataValueUsed(true);
        }

        targetProduct.setAutoGrouping("DHR_sigma:BHR_sigma");
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {
        // (merged?) BRDF product...
        parameterBandNames = SpectralIOUtils.getSpectralInversionParameterBandNames(numSdrBands);
        for (int i = 0; i < 3 * numSdrBands; i++) {
            srcParameters[i] = i;
            configurator.defineSample(srcParameters[i], parameterBandNames[i], spectralBrdfUncertaintiesProduct);
        }

        int index = 0;
        uncertaintyBandNames = SpectralIOUtils.getSpectralInversionUncertainty3BandNames(bandIndices, spectralWaveBandsMap);

        for (int i = 0; i < numSdrBands; i++) {
            for (int j = 0; j < AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS; j++) {
                srcUncertainties[index] = index;
                configurator.defineSample(srcParameters.length + srcUncertainties[index],
                        uncertaintyBandNames[index], spectralBrdfUncertaintiesProduct);
                index++;
            }
        }

        String entropyBandName = AlbedoInversionConstants.INV_ENTROPY_BAND_NAME;
        configurator.defineSample(srcParameters.length + srcUncertainties.length + SRC_ENTROPY,
                entropyBandName, spectralBrdfUncertaintiesProduct);

        // todo: add BRDF uncertainty product
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        int index = 0;
        for (int i = 0; i < numSdrBands; i++) {
            configurator.defineSample(index++, dhrSigmaBandNames[i]);
        }

        for (int i = 0; i < numSdrBands; i++) {
            configurator.defineSample(index++, bhrSigmaBandNames[i]);
        }
    }

    private void setupSpectralWaveBandsMap(int numSdrBands) {
        for (int i = 0; i < numSdrBands; i++) {
            spectralWaveBandsMap.put(i, "b" + (i + 1));
        }
    }

    private void fillTargetSamples(WritableSample[] targetSamples, Matrix[] sigma_dhr, Matrix[] sigma_bhr) {
        int index = 0;
        // DHR_sigma (black sky albedo uncertainty)
        for (int i = 0; i < numSdrBands; i++) {
            targetSamples[index++].set(sigma_dhr[i].get(0, 0));
        }

        // BHR_sigma (white sky albedo uncertainty)
        for (int i = 0; i < numSdrBands; i++) {
            targetSamples[index++].set(sigma_bhr[i].get(0, 0));
        }
    }

    private Matrix getCMatrixFromSpectralInversionProduct(Sample[] sourceSamples) {
        final int n = bandIndices.length * bandIndices.length;
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
                final int finalIndex = index1 - n + i;    // original
//                final int finalIndex = (index1 - n) / 7 + i;  // keep index reasonably small fort the moment to get code running
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
            super(SpectralBrdfUncertaintiesToAlbedoUncertaintiesOp.class);
        }
    }
}

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

    @SourceProduct(description = "Spectral BRDF uncertainties product")
    private Product spectralBrdfUncertaintiesProduct;

    @Parameter(description = "doy", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "3", description = "Number of spectral bands")
    private int numSdrBands;

//    VIS-NIR: 3,1,2 (3 sigma & 3 alpha) and
//    BGR=3,4,1; and NIR: b2,b5 & b6 (sigma only)
//    (email JPM, 20180122
    @Parameter(defaultValue = "3,1,2", description = "Band indices (3 spectral bands)")
    private int[] bandIndices;

    private String[] dhrSigmaBandNames;
    private String[] bhrSigmaBandNames;

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
            spectralWaveBandsMap.put(i, "b" + (bandIndices[i]));
        }

        dhrSigmaBandNames = new String[numSdrBands];
        bhrSigmaBandNames = new String[numSdrBands];

        uncertaintyBandNames = SpectralIOUtils.getSpectralInversionUncertainty3BandNames(bandIndices, spectralWaveBandsMap);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
       /*
        Get Albedo based on model:
        F = BRDF model parameters (1x9 vector)
        C = Variance/Covariance matrix (9x9 matrix)
        U = Polynomial coefficients

        Black-sky Albedo = f0 + f1 * (-0.007574 + (-0.070887 * SZA^2) + (0.307588 * SZA^3)) +
        f2 * (-1.284909 + (-0.166314 * SZA^2) + (0.041840 * SZA^3))

        White-sky Albedo = f0 + f1 * (0.189184) + f2 * (-1.377622)

        Uncertainties = U^T C^-1 U , U is stored as a 1X9 vector (transpose), so, actually U^T is the regular 9x1 vector
        */

        final PixelPos pixelPos = new PixelPos(x, y);
        final GeoPos latLon = spectralBrdfUncertaintiesProduct.getGeoCoding().getGeoPos(pixelPos, null);
        final double SZAdeg = AlbedoInversionUtils.computeSza(latLon, doy);
        final double SZA = SZAdeg * MathUtils.DTOR;

//        if (x == 400 && y == 500) {
//            System.out.println("x = " + x);
//        }

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

//        if (x == 400 && y == 500) {
//            System.out.println("x = " + x);
//        }

        if (AlbedoInversionUtils.isValid(sourceSamples[0].getDouble())) { // todo: is this ok everywhwere?
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

        dhrSigmaBandNames = SpectralIOUtils.getSpectralAlbedoDhrSigmaBandNames(bandIndices);
        for (int i = 0; i < numSdrBands; i++) {
            targetProduct.addBand(dhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
        }

        bhrSigmaBandNames = SpectralIOUtils.getSpectralAlbedoBhrSigmaBandNames(bandIndices);
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
        for (int i = 0; i < uncertaintyBandNames.length; i++) {
            configurator.defineSample(i, uncertaintyBandNames[i], spectralBrdfUncertaintiesProduct);
        }
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
        double[] cTmp = new double[uncertaintyBandNames.length];

        for (int i = 0; i < uncertaintyBandNames.length; i++) {
            final double sampleUncertainty = sourceSamples[i].getDouble();
            cTmp[i] = sampleUncertainty;
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
                int finalIndex = index1 - n + i;
                int ii = n - k;
                int jj = n - k + i;
                C.set(ii, jj, cTmp[finalIndex]);
                ii = n - k + i;
                jj = n - k;
                C.set(ii, jj, cTmp[finalIndex]);
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

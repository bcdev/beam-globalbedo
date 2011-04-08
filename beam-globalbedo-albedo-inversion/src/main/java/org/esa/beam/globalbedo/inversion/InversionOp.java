package org.esa.beam.globalbedo.inversion;


import Jama.LUDecomposition;
import Jama.Matrix;
import Jama.SingularValueDecomposition;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.PixelOperator;
import org.esa.beam.framework.gpf.experimental.PointOperator;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;

/**
 * Pixel operator implementing the inversion part of python breadboard.
 * The breadboard file is 'AlbedoInversion_multisensor_FullAccum_MultiProcessing.py' provided by Gerardo López Saldaña.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.inversion",
                  description = "Performs final inversion from fully accumulated optimal estimation matrices",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2011 by Brockmann Consult")

public class InversionOp extends PixelOperator {

    // BB line 597:
    private static final int xmin = 1;
    private static final int xmax = 1;
    private static final int ymin = 1;
    private static final int ymax = 1;

    public static final int[][] SRC_ACCUM_M =
            new int[3 * AlbedoInversionConstants.numBBDRWaveBands]
                    [3 * AlbedoInversionConstants.numBBDRWaveBands];

    public static final int[] SRC_ACCUM_V =
            new int[3 * AlbedoInversionConstants.numBBDRWaveBands];

    public static final int SRC_ACCUM_E = 90;
    public static final int SRC_ACCUM_MASK = 91;
    public static final int SRC_ACCUM_DOY_CLOSEST_SAMPLE = 92;

    public static final int[][] SRC_PRIOR_MEAN =
            new int[AlbedoInversionConstants.numAlbedoParameters]
                    [AlbedoInversionConstants.numAlbedoParameters];

    public static final int[][] SRC_PRIOR_SD =
            new int[AlbedoInversionConstants.numAlbedoParameters]
                    [AlbedoInversionConstants.numAlbedoParameters];

    public static final int priorOffset = (int) pow(AlbedoInversionConstants.numAlbedoParameters, 2.0);
    public static final int SRC_PRIOR_NSAMPLES = 2 * priorOffset + 1;
    public static final int SRC_PRIOR_MASK = 2 * priorOffset + 2;

    private static final int sourceSampleOffset = 100;  // this value must be >= number of bands in a source product

    private String[][] mBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands]
            [3 * AlbedoInversionConstants.numBBDRWaveBands];

    private String[] vBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands];

    private static final int[] TRG_PARAMETERS = new int[3 * AlbedoInversionConstants.numBBDRWaveBands];

    // this offset is the number of UR matrix elements + diagonale. Should be 45 for 9x9 matrix...
    private static final int targetOffset = ((int) pow(3 * AlbedoInversionConstants.numBBDRWaveBands, 2.0)
                                             + 3 * AlbedoInversionConstants.numBBDRWaveBands) / 2;

    private static final int[] TRG_UNCERTAINTIES = new int[targetOffset];

    private static final int TRG_ENTROPY = targetOffset + 1;
    private static final int TRG_REL_ENTROPY = targetOffset + 2;
    private static final int TRG_WEIGHTED_NUM_SAMPLES = targetOffset + 3;
    private static final int TRG_DAYS_CLOSEST_SAMPLE = targetOffset + 4;
    private static final int TRG_GOODNESS_OF_FIT = targetOffset + 5;

    private String[] parameterBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands];

    private String[][] uncertaintyBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands]
            [3 * AlbedoInversionConstants.numBBDRWaveBands];

    private static final Map<Integer, String> waveBandsOffsetMap = new HashMap<Integer, String>();
    private String entropyBandName;
    private String relEntropyBandName;
    private String weightedNumberOfSamplesBandName;
    private String daysToTheClosestSampleBandName;
    private String goodnessOfFitBandName;

    static {
        waveBandsOffsetMap.put(0, "VIS");
        waveBandsOffsetMap.put(1, "NIR");
        waveBandsOffsetMap.put(2, "SW");
    }

    @SourceProduct(description = "Full accumulation product")
    private Product accumulationProduct;

    @SourceProduct(description = "Prior product")
    private Product priorProduct;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "Tile")
    private String tile;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "true", description = "Use prior information")
    private boolean usePrior;


    @Override
    protected void configureTargetProduct(Product targetProduct) {

        int index = 0;
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = 0; j < AlbedoInversionConstants.numAlbedoParameters; j++) {
                parameterBandNames[index++] = "MEAN_" + waveBandsOffsetMap.get(j) + "_f" + j;
                Band band = targetProduct.addBand(parameterBandNames[i], ProductData.TYPE_FLOAT32);
                band.setNoDataValue(Float.NaN);
                band.setNoDataValueUsed(true);
            }
        }

        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            // only UR matrix + diagonale
            for (int j = i; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                uncertaintyBandNames[i][j] = "VAR_" + waveBandsOffsetMap.get(i) + "_f" + i +
                                             waveBandsOffsetMap.get(j) + "_f" + j;
                Band band = targetProduct.addBand(uncertaintyBandNames[i][j], ProductData.TYPE_FLOAT32);
                band.setNoDataValue(Float.NaN);
                band.setNoDataValueUsed(true);
            }
        }

        entropyBandName = "Entropy";
        Band entropyBand = targetProduct.addBand(entropyBandName, ProductData.TYPE_FLOAT32);
        entropyBand.setNoDataValue(Float.NaN);
        entropyBand.setNoDataValueUsed(true);

        relEntropyBandName = "Relative_Entropy";
        Band relEntropyBand = targetProduct.addBand(relEntropyBandName, ProductData.TYPE_FLOAT32);
        relEntropyBand.setNoDataValue(Float.NaN);
        relEntropyBand.setNoDataValueUsed(true);

        weightedNumberOfSamplesBandName = "Weighted_Number_of_Samples";
        Band weightedNumberOfSamplesBand = targetProduct.addBand(weightedNumberOfSamplesBandName,
                                                                 ProductData.TYPE_FLOAT32);
        weightedNumberOfSamplesBand.setNoDataValue(Float.NaN);
        weightedNumberOfSamplesBand.setNoDataValueUsed(true);

        daysToTheClosestSampleBandName = "Days_to_the_Closest_Sample";
        Band daysToTheClosestSampleBand = targetProduct.addBand(daysToTheClosestSampleBandName, ProductData.TYPE_INT16);
        daysToTheClosestSampleBand.setNoDataValue(-1);
        daysToTheClosestSampleBand.setNoDataValueUsed(true);

        goodnessOfFitBandName = "Goodness_of_Fit";
        Band goodnessOfFitBand = targetProduct.addBand(goodnessOfFitBandName, ProductData.TYPE_FLOAT32);
        goodnessOfFitBand.setNoDataValue(Float.NaN);
        goodnessOfFitBand.setNoDataValueUsed(true);
    }

    @Override
    protected void configureSourceSamples(Configurator configurator) {
        // accumulation product:
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = 0; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                SRC_ACCUM_M[i][j] = 3 * AlbedoInversionConstants.numBBDRWaveBands * i + j;
                configurator.defineSample(SRC_ACCUM_M[i][j], mBandNames[i][j], accumulationProduct);
            }
        }
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            SRC_ACCUM_V[i] = 3 * 3 * AlbedoInversionConstants.numBBDRWaveBands * AlbedoInversionConstants.numBBDRWaveBands + i;
            configurator.defineSample(SRC_ACCUM_V[i], vBandNames[i], accumulationProduct);
        }
        // todo: define constants for names
        configurator.defineSample(SRC_ACCUM_E, "E", accumulationProduct);
        configurator.defineSample(SRC_ACCUM_MASK, "mask", accumulationProduct);
        configurator.defineSample(SRC_ACCUM_DOY_CLOSEST_SAMPLE, "doy_closest_sample", accumulationProduct);

        // prior product:
        // we have:
        // 3x3 mean, 3x3 SD, Nsamples, mask
        for (int i = 0; i < AlbedoInversionConstants.numAlbedoParameters; i++) {
            for (int j = 0; j < AlbedoInversionConstants.numAlbedoParameters; j++) {
                final String meanBandName = "MEAN__BAND________" + i + "_PARAMETER_F" + j;
                SRC_PRIOR_MEAN[i][j] = AlbedoInversionConstants.numAlbedoParameters * i + j;
                configurator.defineSample(SRC_PRIOR_MEAN[i][j], meanBandName, priorProduct);
            }
        }

        for (int i = 0; i < AlbedoInversionConstants.numAlbedoParameters; i++) {
            for (int j = 0; j < AlbedoInversionConstants.numAlbedoParameters; j++) {
                final String sdMeanBandName = "SD_MEAN__BAND________" + i + "_PARAMETER_F" + j;
                SRC_PRIOR_SD[i][j] = priorOffset + AlbedoInversionConstants.numAlbedoParameters * i + j + 1;
                configurator.defineSample(SRC_PRIOR_SD[i][j], sdMeanBandName, priorProduct);
            }
        }
        // todo: define constants for names
        configurator.defineSample(SRC_PRIOR_NSAMPLES, "N_samples", priorProduct);
        configurator.defineSample(SRC_PRIOR_MASK, "Mask", priorProduct);
    }

    @Override
    protected void configureTargetSamples(Configurator configurator) {

        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            TRG_PARAMETERS[i] = i;
            configurator.defineSample(TRG_PARAMETERS[i], parameterBandNames[i]);
        }

        int index = 0;
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = i; j < 3 * AlbedoInversionConstants.numAlbedoParameters; j++) {
                TRG_UNCERTAINTIES[index] = index;
                configurator.defineSample(TRG_UNCERTAINTIES[index], uncertaintyBandNames[i][j]);
                index++;
            }
        }

        configurator.defineSample(TRG_ENTROPY, entropyBandName);
        configurator.defineSample(TRG_REL_ENTROPY, relEntropyBandName);
        configurator.defineSample(TRG_WEIGHTED_NUM_SAMPLES, weightedNumberOfSamplesBandName);
        configurator.defineSample(TRG_DAYS_CLOSEST_SAMPLE, daysToTheClosestSampleBandName);
        configurator.defineSample(TRG_GOODNESS_OF_FIT, goodnessOfFitBandName);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples,
                                WritableSample[] targetSamples) {

        Matrix parameters = new Matrix(AlbedoInversionConstants.numBBDRWaveBands,
                                       AlbedoInversionConstants.numAlbedoParameters);
        Matrix parametersNoPrior = new Matrix(AlbedoInversionConstants.numBBDRWaveBands,
                                              AlbedoInversionConstants.numAlbedoParameters);

        Matrix uncertainties = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                                          3 * AlbedoInversionConstants.numAlbedoParameters);
        Matrix uncertaintiesNoPrior = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                                                 3 * AlbedoInversionConstants.numAlbedoParameters);

        double entropy = 0.0; // == det in BB
        double relEntropy = 0.0;

        final Accumulator accumulator = Accumulator.createForInversion(sourceSamples);
        final Prior prior = Prior.createForInversion(sourceSamples);

        final Matrix mAcc = accumulator.getM();
        Matrix vAcc = accumulator.getV();
        final Matrix eAcc = accumulator.getE();
        int maskAcc = accumulator.getMask();
        final int doyClosestSample = accumulator.getDoyClosestSample();

        int maskPrior = prior.getMask();

        if (maskAcc > 0 && maskPrior > 0) {

            if (usePrior) {
                for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
                    double m_ii_accum = mAcc.get(i, i);
                    mAcc.set(i, i, m_ii_accum + prior.getM().get(i, i));
                }
                vAcc = vAcc.plus(prior.getV());
            }

            final LUDecomposition lud = new LUDecomposition(mAcc);
            if (lud.isNonsingular()) {
                Matrix tmpM = mAcc.inverse();
                if (AlbedoInversionUtils.matrixHasNanElements(tmpM) || AlbedoInversionUtils.matrixHasZerosInDiagonale(
                        tmpM)) {
                    tmpM = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                                                                  3 * AlbedoInversionConstants.numAlbedoParameters,
                                                                  AlbedoInversionConstants.INVALID);
                }
                uncertainties = tmpM;
            } else {
                parameters = new Matrix(AlbedoInversionConstants.numBBDRWaveBands,
                                                                    AlbedoInversionConstants.numAlbedoParameters,
                                                                    AlbedoInversionConstants.INVALID);
                uncertainties = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                                                                       3 * AlbedoInversionConstants.numAlbedoParameters,
                                                                       AlbedoInversionConstants.INVALID);
                maskAcc = 0;
            }

            if (maskAcc != 0) {
                // do parameters estimation:

//                # Compute least-squares solution to equation Ax = b
//                # http://docs.scipy.org/doc/scipy/reference/generated/scipy.linalg.lstsq.html
//
//                (P, rho_residuals, rank, svals) = lstsq(mAcc, vAcc)
//                parameters[:,column,row] = P
                parameters = mAcc.solve(vAcc);
//
//                # Compute singluar value decomposition
//                # http://docs.scipy.org/doc/scipy/reference/generated/scipy.linalg.svd.html
//                U, S, Vh = svd(mAcc)
//                det[column,row] = 0.5*numpy.log(numpy.product(1/S)) + S.shape[0] * numpy.sqrt(numpy.log(2*numpy.pi*numpy.e))
                entropy = getEntropy(mAcc);

//                # This can be calculated earlier for the prior
//                U, S, Vh = svd(numpy.matrix(M_p))
                double entropyPrior = getEntropy(prior.getM());
//                if UsePrior == 1:
//                    RelativeEntropy[column,row] = PriorDet - det[column,row]
//                else:
//                    RelativeEntropy[column,row] = Invalid # As this has no meaning
                if (usePrior) {
                    relEntropy = entropyPrior - entropy;
                } else {
                    relEntropy = AlbedoInversionConstants.INVALID;
                }
            }
        } else {
            if (maskPrior > 0) {
                parameters = prior.getParameters();
                if (usePrior) {
                    uncertainties = prior.getM().inverse();
                    entropy = getEntropy(prior.getM());
                    relEntropy = 0.0;
                } else {
                    uncertainties = new Matrix(
                            3 * AlbedoInversionConstants.numBBDRWaveBands,
                            3 * AlbedoInversionConstants.numAlbedoParameters, AlbedoInversionConstants.INVALID);
                    entropy = AlbedoInversionConstants.INVALID;
                    relEntropy = AlbedoInversionConstants.INVALID;
                }
            }
        }

        // finally we need the 'Goodness of Fit'...
        double goodnessOfFit = getGoodnessOfFit(mAcc, vAcc, eAcc, parameters, maskAcc);

        // we have the final result - fill target samples...
        InversionResult result = new InversionResult(parameters, uncertainties, entropy, relEntropy,
                                                     maskAcc, doyClosestSample, goodnessOfFit);
        fillTargetSamples(targetSamples, result);
    }

    private double getGoodnessOfFit(Matrix mAcc, Matrix vAcc, Matrix eAcc, Matrix fPars, int maskAcc) {
        Matrix goodnessOfFitMatrix = new Matrix(1, 1, 0.0);
        if (maskAcc > 0) {
            final Matrix gofTerm1 = fPars.times(mAcc).times(fPars.transpose());
            final Matrix gofTerm2 = fPars.times(vAcc.transpose());
            final Matrix m2 = new Matrix(1, 1, 2.0);
            final Matrix gofTerm3 = m2.times(eAcc);
            goodnessOfFitMatrix = gofTerm1.plus(gofTerm2).minus(gofTerm3);
        }
        return goodnessOfFitMatrix.get(0, 0);
    }

    private void fillTargetSamples(PointOperator.WritableSample[] targetSamples, InversionResult result) {

        // parameters
        int index = 0;
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = 0; j < AlbedoInversionConstants.numBBDRWaveBands; j++) {
                targetSamples[TRG_PARAMETERS[index]].set(result.getParameters().get(i, j));
                index++;
            }
        }

        index = 0;
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = i; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                targetSamples[TRG_UNCERTAINTIES[index]].set(result.getUncertainties().get(i, j));
            }
        }

        targetSamples[TRG_ENTROPY].set(result.getEntropy());
        targetSamples[TRG_REL_ENTROPY].set(result.getRelEntropy());
        targetSamples[TRG_WEIGHTED_NUM_SAMPLES].set(result.getWeightedNumberOfSamples());
        targetSamples[TRG_DAYS_CLOSEST_SAMPLE].set(result.getDoyClosestSample());
        targetSamples[TRG_GOODNESS_OF_FIT].set(result.getGoodnessOfFit());

    }

    private double getEntropy(Matrix m) {
        final SingularValueDecomposition svdM = m.svd();
        final Matrix svdMS = svdM.getS();
        final Matrix svdMSRecip = AlbedoInversionUtils.getReciprocalMatrix(svdMS);
        final double productSvdMSRecip = AlbedoInversionUtils.getMatrixAllElementsProduct(svdMSRecip);
        double entropy = 0.5 * log(productSvdMSRecip) + svdMS.getRowDimension() * sqrt(log(2.0 * PI * E));

        return entropy;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(InversionOp.class);
        }
    }
}

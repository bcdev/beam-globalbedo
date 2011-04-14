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
import org.esa.beam.globalbedo.inversion.util.IOUtils;

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

    public static final int sourceSampleOffset = 100;  // this value must be >= number of bands in a source product
    public static final int priorOffset = (int) pow(AlbedoInversionConstants.numAlbedoParameters, 2.0);
    public static final int SRC_PRIOR_NSAMPLES = sourceSampleOffset + 2 * priorOffset;

    public static final int SRC_PRIOR_MASK = sourceSampleOffset + 2 * priorOffset + 1;

    private String[][] mBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands]
            [3 * AlbedoInversionConstants.numBBDRWaveBands];

    private String[] vBandNames = new String[3 * AlbedoInversionConstants.numBBDRWaveBands];

    private static final int[] TRG_PARAMETERS = new int[3 * AlbedoInversionConstants.numBBDRWaveBands];

    // this offset is the number of UR matrix elements + diagonale. Should be 45 for 9x9 matrix...
    private static final int targetOffset = ((int) pow(3 * AlbedoInversionConstants.numBBDRWaveBands, 2.0)
            + 3 * AlbedoInversionConstants.numBBDRWaveBands) / 2;

    private static final int[] TRG_UNCERTAINTIES = new int[targetOffset];

    private static final int TRG_ENTROPY = 0;
    private static final int TRG_REL_ENTROPY = 1;
    private static final int TRG_WEIGHTED_NUM_SAMPLES = 2;
    private static final int TRG_DAYS_CLOSEST_SAMPLE = 3;
    private static final int TRG_GOODNESS_OF_FIT = 4;

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
    private Product fullAccumulationProduct;

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

    @Parameter(defaultValue = "30.0", description = "Prior scale factor")
    private double priorScaleFactor;


    @Override
    protected void configureTargetProduct(Product targetProduct) {

        parameterBandNames = IOUtils.getInversionParameterBandNames();
        for (int i = 0; i < parameterBandNames.length; i++) {
            Band band = targetProduct.addBand(parameterBandNames[i], ProductData.TYPE_FLOAT32);
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
                mBandNames[i][j] = "M_" + i + "" + j;
                SRC_ACCUM_M[i][j] = 3 * AlbedoInversionConstants.numBBDRWaveBands * i + j;
                configurator.defineSample(SRC_ACCUM_M[i][j], mBandNames[i][j], fullAccumulationProduct);
            }
        }
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            vBandNames[i] = "V_" + i;
            SRC_ACCUM_V[i] = 3 * 3 * AlbedoInversionConstants.numBBDRWaveBands * AlbedoInversionConstants.numBBDRWaveBands + i;
            configurator.defineSample(SRC_ACCUM_V[i], vBandNames[i], fullAccumulationProduct);
        }
        // todo: define constants for names
        configurator.defineSample(SRC_ACCUM_E, "E", fullAccumulationProduct);
        configurator.defineSample(SRC_ACCUM_MASK, "mask", fullAccumulationProduct);
        configurator.defineSample(SRC_ACCUM_DOY_CLOSEST_SAMPLE, "doy_closest_sample", fullAccumulationProduct);

        // prior product:
        // we have:
        // 3x3 mean, 3x3 SD, Nsamples, mask
        for (int i = 0; i < AlbedoInversionConstants.numAlbedoParameters; i++) {
            for (int j = 0; j < AlbedoInversionConstants.numAlbedoParameters; j++) {
                final String meanBandName = "MEAN__BAND________" + i + "_PARAMETER_F" + j;
                SRC_PRIOR_MEAN[i][j] = sourceSampleOffset + AlbedoInversionConstants.numAlbedoParameters * i + j;
                configurator.defineSample(SRC_PRIOR_MEAN[i][j], meanBandName, priorProduct);
            }
        }

        for (int i = 0; i < AlbedoInversionConstants.numAlbedoParameters; i++) {
            for (int j = 0; j < AlbedoInversionConstants.numAlbedoParameters; j++) {
                final String sdMeanBandName = "SD_MEAN__BAND________" + i + "_PARAMETER_F" + j;
                SRC_PRIOR_SD[i][j] = sourceSampleOffset + priorOffset + AlbedoInversionConstants.numAlbedoParameters * i + j;
                configurator.defineSample(SRC_PRIOR_SD[i][j], sdMeanBandName, priorProduct);
            }
        }
        // todo: define constants for names
        configurator.defineSample(SRC_PRIOR_NSAMPLES, "N samples", priorProduct);
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
                configurator.defineSample(TRG_PARAMETERS.length + TRG_UNCERTAINTIES[index], uncertaintyBandNames[i][j]);
                index++;
            }
        }

        configurator.defineSample(TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_ENTROPY, entropyBandName);
        configurator.defineSample(TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_REL_ENTROPY, relEntropyBandName);
        configurator.defineSample(TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_WEIGHTED_NUM_SAMPLES, weightedNumberOfSamplesBandName);
        configurator.defineSample(TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_DAYS_CLOSEST_SAMPLE, daysToTheClosestSampleBandName);
        configurator.defineSample(TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_GOODNESS_OF_FIT, goodnessOfFitBandName);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples,
                                WritableSample[] targetSamples) {

        Matrix parameters = new Matrix(AlbedoInversionConstants.numBBDRWaveBands *
                AlbedoInversionConstants.numAlbedoParameters, 1);
        Matrix parametersNoPrior = new Matrix(AlbedoInversionConstants.numBBDRWaveBands *
                AlbedoInversionConstants.numAlbedoParameters, 1);

        Matrix uncertainties = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                3 * AlbedoInversionConstants.numAlbedoParameters);
        Matrix uncertaintiesNoPrior = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                3 * AlbedoInversionConstants.numAlbedoParameters);

        double entropy = 0.0; // == det in BB
        double relEntropy = 0.0;

        if ((x == 342 && y == 200) || (x == 427 && y == 383) || (x == 570 && y == 288) || (x == 727 && y == 291) || (x == 714 && y == 541)) {
            System.out.println();
        }

        final Accumulator accumulator = Accumulator.createForInversion(sourceSamples);
        final Prior prior = Prior.createForInversion(sourceSamples, priorScaleFactor);

        final Matrix mAcc = accumulator.getM();
        Matrix vAcc = accumulator.getV();
        final Matrix eAcc = accumulator.getE();
        double maskAcc = accumulator.getMask();
        final int doyClosestSample = accumulator.getDoyClosestSample();

        double maskPrior = prior.getMask();



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
                parameters = new Matrix(AlbedoInversionConstants.numBBDRWaveBands *
                        AlbedoInversionConstants.numAlbedoParameters, 1,
                        AlbedoInversionConstants.INVALID);
                uncertainties = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                        3 * AlbedoInversionConstants.numAlbedoParameters,
                        AlbedoInversionConstants.INVALID);
                maskAcc = 0.0;
            }

            if (maskAcc != 0.0) {
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
            if (maskPrior > 0.0) {
                parameters = prior.getParameters();
                if (usePrior) {
                    final LUDecomposition lud = new LUDecomposition(prior.getM());
                    if (lud.isNonsingular()) {
                        uncertainties = prior.getM().inverse();
                        entropy = getEntropy(prior.getM());
                    } else {
                        uncertainties = new Matrix(
                                3 * AlbedoInversionConstants.numBBDRWaveBands,
                                3 * AlbedoInversionConstants.numAlbedoParameters, AlbedoInversionConstants.INVALID);
                        entropy = AlbedoInversionConstants.INVALID;
                    }
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

    private double getGoodnessOfFit(Matrix mAcc, Matrix vAcc, Matrix eAcc, Matrix fPars, double maskAcc) {
        Matrix goodnessOfFitMatrix = new Matrix(1, 1, 0.0);
        if (maskAcc > 0) {
            final Matrix gofTerm1 = fPars.transpose().times(mAcc).times(fPars);
            final Matrix gofTerm2 = fPars.transpose().times(vAcc);
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
                targetSamples[TRG_PARAMETERS[index]].set(result.getParameters().get(index, 0));
                index++;
            }
        }

        index = 0;
        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = i; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES[index]].set(result.getUncertainties().get(i, j));
                index++;
            }
        }

        targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + TRG_ENTROPY].set(result.getEntropy());
        targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + +TRG_REL_ENTROPY].set(result.getRelEntropy());
        targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + +TRG_WEIGHTED_NUM_SAMPLES].set(result.getWeightedNumberOfSamples());
        targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + +TRG_DAYS_CLOSEST_SAMPLE].set(result.getDoyClosestSample());
        targetSamples[TRG_PARAMETERS.length + TRG_UNCERTAINTIES.length + +TRG_GOODNESS_OF_FIT].set(result.getGoodnessOfFit());

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

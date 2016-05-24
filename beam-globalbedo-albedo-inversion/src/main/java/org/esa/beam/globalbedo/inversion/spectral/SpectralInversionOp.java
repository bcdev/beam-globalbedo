package org.esa.beam.globalbedo.inversion.spectral;

import Jama.LUDecomposition;
import Jama.Matrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.globalbedo.inversion.Accumulator;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.FullAccumulation;
import org.esa.beam.globalbedo.inversion.FullAccumulator;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.*;

/**
 * Pixel operator implementing the inversion part extended from broadband to spectral appropach (MODIS bands).
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.inversion.spectral",
        description = "Implements the inversion part extended from broadband to spectral appropach (MODIS bands).",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")

public class SpectralInversionOp extends PixelOperator {

    @SourceProduct(description = "Prior product", optional = true)
    private Product priorProduct;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "Tile")
    private String tile;

    @Parameter(description = "Day of year")
    private int doy;

    @Parameter(defaultValue = "180", description = "Wings")  // means 3 months wings on each side of the year
    private int wings;

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "7", description = "Number of spectral bands (7 for standard MODIS spectral mapping")
    private int numSdrBands;


    private static final int TRG_REL_ENTROPY = 1;
    private static final int TRG_WEIGHTED_NUM_SAMPLES = 2;
    private static final int TRG_GOODNESS_OF_FIT = 3;
    private static final int TRG_DAYS_TO_THE_CLOSEST_SAMPLE = 4;
    private static final int TRG_GOODNESS_OF_FIT_TERM_1 = 5;
    private static final int TRG_GOODNESS_OF_FIT_TERM_2 = 6;
    private static final int TRG_GOODNESS_OF_FIT_TERM_3 = 7;

    private String[] parameterBandNames;
    private String[][] uncertaintyBandNames;

    private int numTargetParameters;
    private int numTargetUncertainties;

    static final Map<Integer, String> waveBandsOffsetMap = new HashMap<>();

    private FullAccumulator fullAccumulator;

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();

        setupWaveBandsOffsetMap(numSdrBands);

        parameterBandNames = getSpectralInversionParameterBandNames(numSdrBands);
        uncertaintyBandNames = getInversionUncertaintyBandNames(numSdrBands);

        numTargetParameters = 3 * numSdrBands;
        numTargetUncertainties = ((int) pow(3 * numSdrBands, 2.0) + 3 * numSdrBands) / 2;
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        Matrix parameters = new Matrix(numSdrBands * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS, 1,
                                       AlbedoInversionConstants.NO_DATA_VALUE);
        Matrix uncertainties = new Matrix(3 * numSdrBands, 3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS);  // todo: how to initialize??

        double entropy = 0.0; // == det in BB
        double relEntropy = 0.0;

        double maskAcc = 0.0;
        Accumulator accumulator = null;
        if (fullAccumulator != null) {
            accumulator = Accumulator.createForInversion(fullAccumulator.getSumMatrices(), x, y);
            maskAcc = accumulator.getMask();
        }

        double goodnessOfFit = 0.0;
        double[] goodnessOfFitTerms = new double[]{0.0, 0.0, 0.0};
        float daysToTheClosestSample = 0.0f;
        if (accumulator != null && maskAcc > 0) {
            final Matrix mAcc = accumulator.getM();
            Matrix vAcc = accumulator.getV();
            final Matrix eAcc = accumulator.getE();

            final LUDecomposition lud = new LUDecomposition(mAcc);
            if (lud.isNonsingular()) {
                Matrix tmpM = mAcc.inverse();
                if (AlbedoInversionUtils.matrixHasNanElements(tmpM) ||
                        AlbedoInversionUtils.matrixHasZerosInDiagonale(tmpM)) {
                    tmpM = new Matrix(3 * numSdrBands,
                                      3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS,
                                      AlbedoInversionConstants.NO_DATA_VALUE);
                }
                uncertainties = tmpM;
            } else {
                parameters = new Matrix(numSdrBands *
                                                AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS, 1,
                                        AlbedoInversionConstants.NO_DATA_VALUE
                );
                uncertainties = new Matrix(3 * numSdrBands,
                                           3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS,
                                           AlbedoInversionConstants.NO_DATA_VALUE);
                maskAcc = 0.0;
            }

            if (maskAcc != 0.0) {
                parameters = mAcc.solve(vAcc);
                entropy = getEntropy(mAcc);
            }
            // 'Goodness of Fit'...
            goodnessOfFit = getGoodnessOfFit(mAcc, vAcc, eAcc, parameters, maskAcc);
            goodnessOfFitTerms = getGoodnessOfFitTerms(mAcc, vAcc, eAcc, parameters, maskAcc);

            // finally we need the 'Days to the closest sample'...
            daysToTheClosestSample = fullAccumulator.getDaysToTheClosestSample()[x][y];
        } else {
            uncertainties = new Matrix(3 * numSdrBands, 3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS,
                                       AlbedoInversionConstants.NO_DATA_VALUE);
            entropy = AlbedoInversionConstants.NO_DATA_VALUE;
            relEntropy = AlbedoInversionConstants.NO_DATA_VALUE;
        }

        // we have the final result - fill target samples...
        fillTargetSamples(targetSamples,
                          parameters, uncertainties, entropy, relEntropy,
                          maskAcc, goodnessOfFit, goodnessOfFitTerms, daysToTheClosestSample);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        int rasterWidth = AlbedoInversionConstants.MODIS_TILE_WIDTH;
        int rasterHeight = AlbedoInversionConstants.MODIS_TILE_HEIGHT;

        FullAccumulation fullAccumulation = new FullAccumulation(rasterWidth, rasterHeight,
                                                                 gaRootDir, tile, year, doy,
                                                                 wings, computeSnow);
        fullAccumulator = fullAccumulation.getResult();
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        for (int i = 0; i < 3 * numSdrBands; i++) {
            configurator.defineSample(i, parameterBandNames[i]);
        }

        int index = 0;
        for (int i = 0; i < 3 * numSdrBands; i++) {
            for (int j = i; j < 3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS; j++) {
                configurator.defineSample(numTargetParameters + index, uncertaintyBandNames[i][j]);
                index++;
            }
        }

        int offset = numTargetParameters + numTargetUncertainties;
        configurator.defineSample(offset, AlbedoInversionConstants.INV_ENTROPY_BAND_NAME);
        configurator.defineSample(offset + TRG_REL_ENTROPY, AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME);
        configurator.defineSample(offset + TRG_WEIGHTED_NUM_SAMPLES, AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME);
        configurator.defineSample(offset + TRG_GOODNESS_OF_FIT, AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME);
        configurator.defineSample(offset + TRG_DAYS_TO_THE_CLOSEST_SAMPLE, AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME);
        configurator.defineSample(offset + TRG_GOODNESS_OF_FIT_TERM_1, "GoF_TERM_1");
        configurator.defineSample(offset + TRG_GOODNESS_OF_FIT_TERM_2, "GoF_TERM_2");
        configurator.defineSample(offset + TRG_GOODNESS_OF_FIT_TERM_3, "GoF_TERM_3");
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        for (String parameterBandName : parameterBandNames) {
            productConfigurer.addBand(parameterBandName, ProductData.TYPE_FLOAT32, AlbedoInversionConstants.NO_DATA_VALUE);
        }

        for (int i = 0; i < 3 * numSdrBands; i++) {
            // add bands only for UR triangular matrix
            for (int j = i; j < 3 * numSdrBands; j++) {
                productConfigurer.addBand(uncertaintyBandNames[i][j], ProductData.TYPE_FLOAT32, AlbedoInversionConstants.NO_DATA_VALUE);
            }
        }

        productConfigurer.addBand(AlbedoInversionConstants.INV_ENTROPY_BAND_NAME, ProductData.TYPE_FLOAT32,
                                  AlbedoInversionConstants.NO_DATA_VALUE);
        productConfigurer.addBand(AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME, ProductData.TYPE_FLOAT32,
                                  AlbedoInversionConstants.NO_DATA_VALUE);
        productConfigurer.addBand(AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME, ProductData.TYPE_FLOAT32,
                                  AlbedoInversionConstants.NO_DATA_VALUE);
        productConfigurer.addBand(AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME, ProductData.TYPE_FLOAT32,
                                  AlbedoInversionConstants.NO_DATA_VALUE);
        productConfigurer.addBand(AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME, ProductData.TYPE_FLOAT32,
                                  AlbedoInversionConstants.NO_DATA_VALUE);

        for (Band b : getTargetProduct().getBands()) {
            b.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
            b.setNoDataValueUsed(true);
        }
    }

    static String[] getSpectralInversionParameterBandNames(int numSdrBands) {
        String bandNames[] = new String[numSdrBands * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS];
        int index = 0;
        for (int i = 0; i < numSdrBands; i++) {
            for (int j = 0; j < AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS; j++) {
                bandNames[index] = "mean_lambda" + (i + 1) + "_f" + j;
                index++;
            }
        }
        return bandNames;
    }

    static String[][] getInversionUncertaintyBandNames(int numSdrBands) {
        String bandNames[][] = new String[3 * numSdrBands][3 * numSdrBands];

        for (int i = 0; i < 3 * numSdrBands; i++) {
            // only UR triangle matrix
            for (int j = i; j < 3 * numSdrBands; j++) {
                bandNames[i][j] = "VAR_" + waveBandsOffsetMap.get(i / 3) + "_f" + (i % 3) + "_" +
                        waveBandsOffsetMap.get(j / 3) + "_f" + (j % 3);
            }
        }
        return bandNames;

    }

    static void setupWaveBandsOffsetMap(int numSdrBands) {
        for (int i = 0; i < numSdrBands; i++) {
            waveBandsOffsetMap.put(i, "lambda" + (i + 1));
        }
    }

    private double getGoodnessOfFit(Matrix mAcc, Matrix vAcc, Matrix eAcc, Matrix fPars, double maskAcc) {
        Matrix goodnessOfFitMatrix = new Matrix(1, 1, 0.0);
        if (maskAcc > 0) {
            final Matrix gofTerm1 = fPars.transpose().times(mAcc).times(fPars);
            final Matrix gofTerm2 = fPars.transpose().times(vAcc);         // fpars = mAcc.solve(vAcc) ?!
//            final Matrix gofTerm2 = fPars.times(vAcc.transpose());         // like in breadboard but wrong?!
            final Matrix m2 = new Matrix(1, 1, 2.0);
            final Matrix gofTerm3 = m2.times(eAcc);
            goodnessOfFitMatrix = gofTerm1.plus(gofTerm2).minus(gofTerm3);
        }
        return goodnessOfFitMatrix.get(0, 0);
    }

    private double[] getGoodnessOfFitTerms(Matrix mAcc, Matrix vAcc, Matrix eAcc, Matrix fPars, double maskAcc) {
        if (maskAcc > 0) {
            final Matrix gofTerm1 = fPars.transpose().times(mAcc).times(fPars);
            final Matrix gofTerm2 = fPars.transpose().times(vAcc);
            final Matrix m2 = new Matrix(1, 1, 2.0);
            final Matrix gofTerm3 = m2.times(eAcc);
            return new double[]{gofTerm1.get(0, 0), gofTerm2.get(0, 0), gofTerm3.get(0, 0)};
        } else {
            return new double[]{0.0, 0.0, 0.0};
        }
    }


    private void fillTargetSamples(WritableSample[] targetSamples,
                                   Matrix parameters, Matrix uncertainties, double entropy, double relEntropy,
                                   double weightedNumberOfSamples,
                                   double goodnessOfFit,
                                   double[] goodnessOfFitTerms,
                                   float daysToTheClosestSample) {

        // parameters
        int index = 0;
        for (int i = 0; i < numSdrBands; i++) {
            for (int j = 0; j < numSdrBands; j++) {
                targetSamples[index].set(parameters.get(index, 0));
                index++;
            }
        }

        for (int i = 0; i < 3 * numSdrBands; i++) {
            for (int j = i; j < 3 * numSdrBands; j++) {
                targetSamples[index].set(uncertainties.get(i, j));
                index++;
            }
        }

        int offset = numTargetParameters + numTargetUncertainties;
        targetSamples[offset].set(entropy);
        targetSamples[offset + TRG_REL_ENTROPY].set(relEntropy);
        targetSamples[offset + TRG_WEIGHTED_NUM_SAMPLES].set(weightedNumberOfSamples);
        targetSamples[offset + TRG_GOODNESS_OF_FIT].set(goodnessOfFit);
        targetSamples[offset + TRG_DAYS_TO_THE_CLOSEST_SAMPLE].set(daysToTheClosestSample);
        targetSamples[offset + TRG_GOODNESS_OF_FIT_TERM_1].set(goodnessOfFitTerms[0]);
        targetSamples[offset + TRG_GOODNESS_OF_FIT_TERM_2].set(goodnessOfFitTerms[1]);
        targetSamples[offset + TRG_GOODNESS_OF_FIT_TERM_3].set(goodnessOfFitTerms[2]);

    }

    private double getEntropy(Matrix m) {
        // final SingularValueDecomposition svdM = m.svd();     // this sometimes gets stuck at CEMS!!
        //  --> single value decomposition from apache.commons.math3 seems to do better
        final RealMatrix rm = AlbedoInversionUtils.getRealMatrixFromJamaMatrix(m);
        final SingularValueDecomposition svdM = new SingularValueDecomposition(rm);
        final double[] svdMSingularValues = svdM.getSingularValues();
        // see python BB equivalent at http://nullege.com/codes/search/numpy.prod
        double productSvdMSRecip = 1.0;
        for (double svdMSingularValue : svdMSingularValues) {
            if (svdMSingularValue != 0.0) {
                productSvdMSRecip *= (1.0 / svdMSingularValue);
            }
        }
        return 0.5 * log(productSvdMSRecip) + svdMSingularValues.length * sqrt(log(2.0 * PI * E));
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SpectralInversionOp.class);
        }
    }
}

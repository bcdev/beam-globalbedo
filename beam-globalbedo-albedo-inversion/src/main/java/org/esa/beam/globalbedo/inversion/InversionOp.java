package org.esa.beam.globalbedo.inversion;


import Jama.LUDecomposition;
import Jama.Matrix;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.logging.Level;

import static java.lang.Math.*;
import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.*;

/**
 * Pixel operator implementing the inversion part of python breadboard.
 * The breadboard file is 'AlbedoInversion_multisensor_FullAccum_MultiProcessing.py' provided by Gerardo Lopez Saldana.
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

    public static final int[][] SRC_PRIOR_MEAN = new int[NUM_ALBEDO_PARAMETERS][NUM_ALBEDO_PARAMETERS];

    public static final int[][] SRC_PRIOR_SD = new int[NUM_ALBEDO_PARAMETERS][NUM_ALBEDO_PARAMETERS];

    public static final int SOURCE_SAMPLE_OFFSET = 0;  // this value must be >= number of bands in a source product
    public static final int PRIOR_OFFSET = (int) pow(NUM_ALBEDO_PARAMETERS, 2.0);
    public static final int SRC_PRIOR_NSAMPLES = 2 * PRIOR_OFFSET;

    public static final int SRC_PRIOR_MASK = SOURCE_SAMPLE_OFFSET + 2 * PRIOR_OFFSET + 1;

    private static final int NUM_TRG_PARAMETERS = 3 * NUM_BBDR_WAVE_BANDS;

    // this offset is the number of UR matrix elements + diagonale. Should be 45 for 9x9 matrix...
    private static final int NUM_TRG_UNCERTAINTIES = ((int) pow(3 * NUM_BBDR_WAVE_BANDS, 2.0) + 3 * NUM_BBDR_WAVE_BANDS) / 2;

    private static final int TRG_REL_ENTROPY = 1;
    private static final int TRG_WEIGHTED_NUM_SAMPLES = 2;
    private static final int TRG_GOODNESS_OF_FIT = 3;
    private static final int TRG_DAYS_TO_THE_CLOSEST_SAMPLE = 4;
    private static final int TRG_GOODNESS_OF_FIT_TERM_1 = 5;
    private static final int TRG_GOODNESS_OF_FIT_TERM_2 = 6;
    private static final int TRG_GOODNESS_OF_FIT_TERM_3 = 7;

    private static final String[] PARAMETER_BAND_NAMES = IOUtils.getInversionParameterBandNames();
    private static final String[][] UNCERTAINTY_BAND_NAMES = IOUtils.getInversionUncertaintyBandNames();

    static {
        for (int i = 0; i < NUM_ALBEDO_PARAMETERS; i++) {
            for (int j = 0; j < NUM_ALBEDO_PARAMETERS; j++) {
                SRC_PRIOR_MEAN[i][j] = SOURCE_SAMPLE_OFFSET + NUM_ALBEDO_PARAMETERS * i + j;
                SRC_PRIOR_SD[i][j] = SOURCE_SAMPLE_OFFSET + PRIOR_OFFSET + NUM_ALBEDO_PARAMETERS * i + j;
            }
        }
    }

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

    @Parameter(defaultValue = "", description = "Globalbedo BBDR root directory") // e.g., /data/Globalbedo
    private String bbdrRootDir;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "false", description = "Computation for seaice mode (polar tiles)")
    private boolean computeSeaice;

    @Parameter(defaultValue = "false", description = "Write only SW related outputs (usually in case of MVIRI/SEVIRI)")
    private boolean writeSwOnly;

    @Parameter(defaultValue = "true", description = "Use prior information")
    private boolean usePrior;

    @Parameter(defaultValue = "6", description = "Prior version (MODIS collection)")
    private int priorVersion;

    @Parameter(defaultValue = "30.0", description = "Prior scale factor")
    private double priorScaleFactor;

    //    @Parameter(defaultValue = "MEAN:_BAND_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    // Oct. 2015:
//    @Parameter(defaultValue = "Mean_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    // Oct. 2016:
    @Parameter(defaultValue = "BRDF_Albedo_Parameters_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    private String priorMeanBandNamePrefix;

    //    @Parameter(defaultValue = "SD:_BAND_", description = "Prefix of prior SD band (default fits to the latest prior version)")
//    @Parameter(defaultValue = "Cov_", description = "Prefix of prior SD band (default fits to the latest prior version)")
    // Oct. 2016:
    @Parameter(defaultValue = "BRDF_Albedo_Parameters_", description = "Prefix of prior SD band (default fits to the latest prior version)")
    private String priorSdBandNamePrefix;

    @Parameter(defaultValue = "7", description = "Prior broad bands start index (no longer needed for Collection 6 priors)")
    private int priorBandStartIndex;

//    @Parameter(defaultValue = "Weighted_number_of_samples", description = "Prior NSamples band name (default fits to the latest prior version)")
    @Parameter(defaultValue = "BRDF_Albedo_Parameters_bb_wns", description = "Prior NSamples band name (default fits to the latest prior version)")
    // Oct. 2016:
    private String priorNSamplesBandName;

    //    @Parameter(defaultValue = "land_mask", description = "Prior data mask band name (default fits to the latest prior version)")
//    @Parameter(defaultValue = "Data_Mask", description = "Prior data mask band name (default fits to the latest prior version)")
    // Oct. 2016:
    @Parameter(defaultValue = "snow", description = "Prior data mask band name (default fits to the latest prior version)")
    private String priorDataMaskBandName;

    @Parameter(defaultValue = "1.0",
            valueSet = {"0.5", "1.0", "2.0", "4.0", "6.0", "10.0", "12.0", "20.0", "60.0"},
            description = "Scale factor with regard to MODIS default 1200x1200. Values > 1.0 reduce product size." +
                    "Should usually be set to 6.0 for AVHRR/GEO (tiles of 200x200).")
    protected double modisTileScaleFactor;

    private FullAccumulator fullAccumulator;

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        for (String parameterBandName : PARAMETER_BAND_NAMES) {
            boolean addBand = !writeSwOnly || (writeSwOnly && parameterBandName.contains("SW"));
            if (addBand) {
                Band b = productConfigurer.addBand(parameterBandName, ProductData.TYPE_FLOAT32, AlbedoInversionConstants.NO_DATA_VALUE);
                if (computeSeaice) {
                    b.setValidPixelExpression(AlbedoInversionConstants.SEAICE_ALBEDO_VALID_PIXEL_EXPRESSION);
                }
            }
        }

        for (int i = 0; i < 3 * NUM_BBDR_WAVE_BANDS; i++) {
            // add bands only for UR triangular matrix
            for (int j = i; j < 3 * NUM_BBDR_WAVE_BANDS; j++) {
                boolean addBand = !writeSwOnly || (writeSwOnly && UNCERTAINTY_BAND_NAMES[i][j].contains("SW") &&
                        !UNCERTAINTY_BAND_NAMES[i][j].contains("VIS") && !UNCERTAINTY_BAND_NAMES[i][j].contains("NIR"));
                if (addBand) {
                    Band b = productConfigurer.addBand(UNCERTAINTY_BAND_NAMES[i][j], ProductData.TYPE_FLOAT32, AlbedoInversionConstants.NO_DATA_VALUE);
                    if (computeSeaice) {
                        b.setValidPixelExpression(AlbedoInversionConstants.SEAICE_ALBEDO_VALID_PIXEL_EXPRESSION);
                    }
                }
            }
        }

        Band bInvEntr = productConfigurer.addBand(INV_ENTROPY_BAND_NAME, ProductData.TYPE_FLOAT32, AlbedoInversionConstants.NO_DATA_VALUE);
        Band bInvRelEntr = productConfigurer.addBand(INV_REL_ENTROPY_BAND_NAME, ProductData.TYPE_FLOAT32, AlbedoInversionConstants.NO_DATA_VALUE);
        Band bInvWeighNumSampl = productConfigurer.addBand(INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME, ProductData.TYPE_FLOAT32, AlbedoInversionConstants.NO_DATA_VALUE);
        Band bAccDaysClSampl = productConfigurer.addBand(ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME, ProductData.TYPE_FLOAT32, AlbedoInversionConstants.NO_DATA_VALUE);
        Band bInvGoodnessFit = productConfigurer.addBand(INV_GOODNESS_OF_FIT_BAND_NAME, ProductData.TYPE_FLOAT32, AlbedoInversionConstants.NO_DATA_VALUE);
        if (computeSeaice) {
            bInvEntr.setValidPixelExpression(AlbedoInversionConstants.SEAICE_ALBEDO_VALID_PIXEL_EXPRESSION);
            bInvRelEntr.setValidPixelExpression(AlbedoInversionConstants.SEAICE_ALBEDO_VALID_PIXEL_EXPRESSION);
            bInvWeighNumSampl.setValidPixelExpression(AlbedoInversionConstants.SEAICE_ALBEDO_VALID_PIXEL_EXPRESSION);
            bAccDaysClSampl.setValidPixelExpression(AlbedoInversionConstants.SEAICE_ALBEDO_VALID_PIXEL_EXPRESSION);
            bInvGoodnessFit.setValidPixelExpression(AlbedoInversionConstants.SEAICE_ALBEDO_VALID_PIXEL_EXPRESSION);
        }

        for (Band b : getTargetProduct().getBands()) {
            b.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
            b.setNoDataValueUsed(true);
        }
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) {

        int rasterWidth;
        int rasterHeight;
        if (computeSeaice) {
            rasterWidth = AlbedoInversionConstants.SEAICE_TILE_WIDTH;
            rasterHeight = AlbedoInversionConstants.SEAICE_TILE_HEIGHT;
        } else {
            rasterWidth = (int) (AlbedoInversionConstants.MODIS_TILE_WIDTH / modisTileScaleFactor);
            rasterHeight = (int) (AlbedoInversionConstants.MODIS_TILE_HEIGHT / modisTileScaleFactor);
        }

        FullAccumulation fullAccumulation = new FullAccumulation(rasterWidth, rasterHeight,
                                                                 bbdrRootDir, tile, year, doy,
                                                                 wings, computeSnow);
        fullAccumulator = fullAccumulation.getResult();

        // prior product:
        // we have:
        // 3x3 mean, 3x3 SD, Nsamples, mask
        if (usePrior) {
            configurePriorSourceSamples(configurator);
        }
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) {

        int index = 0;
        for (int i = 0; i < 3 * NUM_BBDR_WAVE_BANDS; i++) {
            boolean addedBand = !writeSwOnly || (writeSwOnly && PARAMETER_BAND_NAMES[i].contains("SW"));
            if (addedBand) {
                configurator.defineSample(index++, PARAMETER_BAND_NAMES[i]);
            }
        }

        index = 0;
        for (int i = 0; i < 3 * NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = i; j < 3 * NUM_ALBEDO_PARAMETERS; j++) {
                boolean addedBand = !writeSwOnly || (writeSwOnly && UNCERTAINTY_BAND_NAMES[i][j].contains("SW") &&
                        !UNCERTAINTY_BAND_NAMES[i][j].contains("VIS") && !UNCERTAINTY_BAND_NAMES[i][j].contains("NIR"));
                if (addedBand) {
                    configurator.defineSample(NUM_TRG_PARAMETERS + index, UNCERTAINTY_BAND_NAMES[i][j]);
                    index++;
                }
            }
        }

        int offset = NUM_TRG_PARAMETERS + NUM_TRG_UNCERTAINTIES;
        configurator.defineSample(offset, INV_ENTROPY_BAND_NAME);
        configurator.defineSample(offset + TRG_REL_ENTROPY, INV_REL_ENTROPY_BAND_NAME);
        configurator.defineSample(offset + TRG_WEIGHTED_NUM_SAMPLES, INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME);
        configurator.defineSample(offset + TRG_GOODNESS_OF_FIT, INV_GOODNESS_OF_FIT_BAND_NAME);
        configurator.defineSample(offset + TRG_DAYS_TO_THE_CLOSEST_SAMPLE, ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        Matrix parameters = new Matrix(NUM_BBDR_WAVE_BANDS * NUM_ALBEDO_PARAMETERS, 1, AlbedoInversionConstants.NO_DATA_VALUE);
        Matrix uncertainties = new Matrix(3 * NUM_BBDR_WAVE_BANDS, 3 * NUM_ALBEDO_PARAMETERS);  // todo: how to initialize??

        if (x == 800 & y == 100) {
            System.out.println("x = " + x);
        }

        double entropy = 0.0; // == det in BB
        double relEntropy = 0.0;

        double maskAcc = 0.0;
        Accumulator accumulator = null;
        if (fullAccumulator != null) {
            accumulator = Accumulator.createForInversion(fullAccumulator.getSumMatrices(), x, y);
            maskAcc = accumulator.getMask();
        }

        double maskPrior = 1.0;
        Prior prior = null;
        if (usePrior) {
            prior = Prior.createForInversion(sourceSamples, priorScaleFactor);
            maskPrior = prior.getMask();
        }

//        if (x == 199 && y == 150) {
//            BeamLogManager.getSystemLogger().log(Level.INFO, "x,y = " + x + "," + y);
//            if (accumulator != null) {
//                AlbedoInversionUtils.printAccumulatorMatrices(accumulator);
//            }
//        }

        double goodnessOfFit = 0.0;
        double[] goodnessOfFitTerms = new double[]{0.0, 0.0, 0.0};
        float daysToTheClosestSample = 0.0f;
        if (accumulator != null && maskAcc > 0 && ((usePrior && maskPrior > 0) || !usePrior)) {
            final Matrix mAcc = accumulator.getM();
            Matrix vAcc = accumulator.getV();
            final Matrix eAcc = accumulator.getE();

            if (usePrior && prior != null) {
                for (int i = 0; i < 3 * NUM_BBDR_WAVE_BANDS; i++) {
                    double m_ii_accum = mAcc.get(i, i);
                    if (prior.getM() != null) {
                        m_ii_accum += prior.getM().get(i, i);
                    }
                    mAcc.set(i, i, m_ii_accum);
                }
                vAcc = vAcc.plus(prior.getV());
            }

            final LUDecomposition lud = new LUDecomposition(mAcc);
            if (lud.isNonsingular()) {
                Matrix tmpM = mAcc.inverse();
                if (AlbedoInversionUtils.matrixHasNanElements(tmpM) ||
                        AlbedoInversionUtils.matrixHasZerosInDiagonale(tmpM)) {
                    tmpM = new Matrix(3 * NUM_BBDR_WAVE_BANDS,
                                      3 * NUM_ALBEDO_PARAMETERS,
                                      AlbedoInversionConstants.NO_DATA_VALUE);
                }
                uncertainties = tmpM;
            } else {
                parameters = new Matrix(NUM_BBDR_WAVE_BANDS *
                                                NUM_ALBEDO_PARAMETERS, 1,
                                        AlbedoInversionConstants.NO_DATA_VALUE
                );
                uncertainties = new Matrix(3 * NUM_BBDR_WAVE_BANDS,
                                           3 * NUM_ALBEDO_PARAMETERS,
                                           AlbedoInversionConstants.NO_DATA_VALUE);
                maskAcc = 0.0;
            }

            if (maskAcc != 0.0) {
                parameters = mAcc.solve(vAcc);
                entropy = getEntropy(mAcc);
                relEntropy = entropy;
                if (usePrior && prior != null && prior.getM() != null) {
                    final double entropyPrior = getEntropy(prior.getM());
                    relEntropy = entropyPrior - entropy;
                }
            }
            // 'Goodness of Fit'...
            goodnessOfFit = getGoodnessOfFit(mAcc, vAcc, eAcc, parameters, maskAcc);
            if (x == 199 && y == 150) {
                BeamLogManager.getSystemLogger().log(Level.INFO, "goodnessOfFit = " + goodnessOfFit);
            }
            goodnessOfFitTerms = getGoodnessOfFitTerms(mAcc, vAcc, eAcc, parameters, maskAcc);

            // finally we need the 'Days to the closest sample'...
            daysToTheClosestSample = fullAccumulator.getDaysToTheClosestSample()[x][y];
        } else {
            if (maskPrior > 0.0) {
                if (usePrior) {
                    parameters = prior.getParameters();
                    final LUDecomposition lud = new LUDecomposition(prior.getM());
                    if (lud.isNonsingular()) {
                        uncertainties = prior.getM().inverse();
                        entropy = getEntropy(prior.getM());
                    } else {
                        uncertainties = new Matrix(
                                3 * NUM_BBDR_WAVE_BANDS,
                                3 * NUM_ALBEDO_PARAMETERS, AlbedoInversionConstants.NO_DATA_VALUE);
                        entropy = AlbedoInversionConstants.NO_DATA_VALUE;
                    }
                    relEntropy = 0.0;
                } else {
                    uncertainties = new Matrix(
                            3 * NUM_BBDR_WAVE_BANDS,
                            3 * NUM_ALBEDO_PARAMETERS, AlbedoInversionConstants.NO_DATA_VALUE);
                    entropy = AlbedoInversionConstants.NO_DATA_VALUE;
                    relEntropy = AlbedoInversionConstants.NO_DATA_VALUE;
                }
            } else {
                uncertainties = new Matrix(
                        3 * NUM_BBDR_WAVE_BANDS,
                        3 * NUM_ALBEDO_PARAMETERS, AlbedoInversionConstants.NO_DATA_VALUE);
                entropy = AlbedoInversionConstants.NO_DATA_VALUE;
                relEntropy = AlbedoInversionConstants.NO_DATA_VALUE;
            }
        }

        // we have the final result - fill target samples...
        fillTargetSamples(targetSamples,
                          parameters, uncertainties, entropy, relEntropy,
                          maskAcc, goodnessOfFit, goodnessOfFitTerms, daysToTheClosestSample);
    }

    // Python breadboard:
    //            return GoodnessOfFit
    //
    //                                                    ( 2 * numpy.matrix(E[column,row]) )
    //                                                    ( numpy.matrix(F[:,column,row]) * numpy.matrix(V[:,column,row]).T ) - \
    //                                                     numpy.matrix(F[:,column,row]).T ) + \
    //                        GoodnessOfFit[column,row] = ( numpy.matrix(F[:,column,row]) * numpy.matrix(M[:,:,column,row]) *
    //                    if Mask[column,row] > 0:
    //                for row in range(0,rows):
    //            for column in range(0,columns):
    //
    //    GoodnessOfFit = numpy.zeros((columns, rows), numpy.float32)
    //
    //    '''
    //
    //    see Eq. 19b in GlobAlbedo -  Albedo ATBD
    //
    //    F and V are stored as a 1X9 vector (transpose), so, actually F^T and V^T are the regular 9X1 vectors
    //
    //    x^2 = F^T M F + F^T V - 2E
    //
    //    Compute goodnes of fit of the model to the observation set x^2
    //            '''
    //    def GetGoodnessOfFit(M, V, E, F, Mask, columns, rows):
    static double getGoodnessOfFit(Matrix mAcc, Matrix vAcc, Matrix eAcc, Matrix fPars, double maskAcc) {
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

    static double[] getGoodnessOfFitTerms(Matrix mAcc, Matrix vAcc, Matrix eAcc, Matrix fPars, double maskAcc) {
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
        int bandIndex = 0;
        for (int i = 0; i < NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = 0; j < NUM_BBDR_WAVE_BANDS; j++) {
                boolean addedBand = !writeSwOnly || (writeSwOnly && PARAMETER_BAND_NAMES[bandIndex].contains("SW"));
                if (addedBand) {
                    targetSamples[index].set(parameters.get(bandIndex, 0));
                    index++;
                }
                bandIndex++;
            }
        }

        index = 0;
        for (int i = 0; i < 3 * NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = i; j < 3 * NUM_BBDR_WAVE_BANDS; j++) {
                boolean addBand = !writeSwOnly || (writeSwOnly && UNCERTAINTY_BAND_NAMES[i][j].contains("SW") &&
                        !UNCERTAINTY_BAND_NAMES[i][j].contains("VIS") && !UNCERTAINTY_BAND_NAMES[i][j].contains("NIR"));
                if (addBand) {
                    targetSamples[NUM_TRG_PARAMETERS + index].set(uncertainties.get(i, j));
                    index++;
                }
            }
        }

        int offset = NUM_TRG_PARAMETERS + NUM_TRG_UNCERTAINTIES;
        targetSamples[offset].set(entropy);
        targetSamples[offset + TRG_REL_ENTROPY].set(relEntropy);
        targetSamples[offset + TRG_WEIGHTED_NUM_SAMPLES].set(weightedNumberOfSamples);
        targetSamples[offset + TRG_GOODNESS_OF_FIT].set(goodnessOfFit);
        targetSamples[offset + TRG_DAYS_TO_THE_CLOSEST_SAMPLE].set(daysToTheClosestSample);
    }


    static double getEntropy(Matrix m) {
        // A bit unclear. The ATBD just calls the matrix determinant the 'entropy'. The breadboard uses a
        // SingularValue decomposition. Why?

        final int mDim = m.getRowDimension(); // we know we have a symmetric matrix here
        final double offset = mDim * sqrt(log(2.0 * PI * E));
        return 0.5 * log(m.det()) + offset;   // ATBD v4.12 p.272
    }

    private void configurePriorSourceSamples(SampleConfigurer configurator) {
        for (int i = 0; i < NUM_ALBEDO_PARAMETERS; i++) {
            if (priorVersion == 6) {
                // collection 6 , from Oct 2016
                for (int j = 0; j < NUM_ALBEDO_PARAMETERS; j++) {
                    // priorMeanBandNamePrefix = priorSdBandNamePrefix = 'BRDF_Albedo_Parameters_'
                    // BRDF_Albedo_Parameters_vis_f0_avr <--> MEAN_VIS_f0
                    // BRDF_Albedo_Parameters_nir_f0_avr <--> MEAN_NIR_f0
                    // BRDF_Albedo_Parameters_shortwave_f0_avr <--> MEAN_SW_f0
                    // same for f1, f2
                    final String meanBandName = priorMeanBandNamePrefix +
                            AlbedoInversionConstants.PRIOR_6_WAVE_BANDS[i] + "_f" + j + "_avr";
                    final int meanIndex = SRC_PRIOR_MEAN[i][j];
                    configurator.defineSample(meanIndex, meanBandName, priorProduct);

                    // BRDF_Albedo_Parameters_vis_f0_sd <--> sqrt (Cov_VIS_f0_VIS_f0)
                    // BRDF_Albedo_Parameters_nir_f0_sd <--> sqrt (Cov_NIR_f0_NIR_f0)
                    // BRDF_Albedo_Parameters_shortwave_f0_sd <--> sqrt (Cov_SW_f0_SW_f0)
                    // same for f1, f2
                     final String sdMeanBandName = priorMeanBandNamePrefix +
                            AlbedoInversionConstants.PRIOR_6_WAVE_BANDS[i] + "_f" + j + "_sd";
                    final int sdIndex = SRC_PRIOR_SD[i][j];
                    configurator.defineSample(sdIndex, sdMeanBandName, priorProduct);
                }
                configurator.defineSample(SRC_PRIOR_NSAMPLES, priorNSamplesBandName, priorProduct);
                configurator.defineSample(SRC_PRIOR_MASK, priorDataMaskBandName, priorProduct);
            } else {
                // collection 5 , before Oct 2016
                for (int j = 0; j < NUM_ALBEDO_PARAMETERS; j++) {
                    final String indexString = Integer.toString(priorBandStartIndex + i);
//                    final String meanBandName = "MEAN__BAND________" + i + "_PARAMETER_F" + j;
                    // 2014, e.g. MEAN:_BAND_7_PARAMETER_F1
//                    final String meanBandName = priorMeanBandNamePrefix + indexString + "_PARAMETER_F" + j;
                    // Oct. 2015 version, e.g. Mean_VIS_f0
                    final String meanBandName = priorMeanBandNamePrefix + AlbedoInversionConstants.BBDR_WAVE_BANDS[i] + "_f" + j;
                    final int meanIndex = SRC_PRIOR_MEAN[i][j];
                    configurator.defineSample(meanIndex, meanBandName, priorProduct);

//                    final String sdMeanBandName = "SD_MEAN__BAND________" + i + "_PARAMETER_F" + j;
                    // 2014, e.g. SD:_BAND_7_PARAMETER_F1
//                    final String sdMeanBandName = priorSdBandNamePrefix + indexString + "_PARAMETER_F" + j;
                    // Oct. 2015 version:
                    // SD:_BAND_7_PARAMETER_F0 --> now Cov_VIS_f0_VIS_f0
                    // SD:_BAND_7_PARAMETER_F1 --> now Cov_VIS_f1_VIS_f1
                    // SD:_BAND_7_PARAMETER_F2 --> now Cov_VIS_f2_VIS_f2
                    // SD:_BAND_8_PARAMETER_F0 --> now Cov_NIR_f0_NIR_f0
                    // SD:_BAND_8_PARAMETER_F1 --> now Cov_NIR_f1_NIR_f1
                    // SD:_BAND_8_PARAMETER_F2 --> now Cov_NIR_f2_NIR_f2
                    // SD:_BAND_9_PARAMETER_F0 --> now Cov_SW_f0_SW_f0
                    // SD:_BAND_9_PARAMETER_F1 --> now Cov_SW_f1_SW_f1
                    // SD:_BAND_9_PARAMETER_F2 --> now Cov_SW_f2_SW_f2
                    final String sdMeanBandName = priorSdBandNamePrefix +
                            AlbedoInversionConstants.BBDR_WAVE_BANDS[i] + "_f" + j + "_" +
                            AlbedoInversionConstants.BBDR_WAVE_BANDS[i] + "_f" + j;
                    final int sdIndex = SRC_PRIOR_SD[i][j];
                    configurator.defineSample(sdIndex, sdMeanBandName, priorProduct);
                }
//            configurator.defineSample(SRC_PRIOR_NSAMPLES, PRIOR_NSAMPLES_NAME, priorProduct);
                configurator.defineSample(SRC_PRIOR_NSAMPLES, priorNSamplesBandName, priorProduct);
//            configurator.defineSample(SRC_PRIOR_MASK, PRIOR_MASK_NAME, priorProduct);
                configurator.defineSample(SRC_PRIOR_MASK, priorDataMaskBandName, priorProduct);
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(InversionOp.class);
        }

    }
}

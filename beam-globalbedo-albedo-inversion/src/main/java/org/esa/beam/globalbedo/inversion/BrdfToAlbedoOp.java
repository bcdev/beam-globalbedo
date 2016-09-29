package org.esa.beam.globalbedo.inversion;

import Jama.LUDecomposition;
import Jama.Matrix;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.math.MathUtils;

import static java.lang.Math.pow;

/**
 * Operator for retrieval of albedo from BRDF model parameters.
 * The breadboard file is 'Albedo.py' provided by Gerardo Lopez Saldana.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.albedo.albedo",
        description = "Provides final albedo retrieval from merged BRDF files",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2011 by Brockmann Consult")
public class BrdfToAlbedoOp extends PixelOperator {

    // source samples:
    private static final int[] SRC_PARAMETERS = new int[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private String[] parameterBandNames = new String[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private String[][] uncertaintyBandNames = new String[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS]
            [3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];


    // this offset is the number of UR matrix elements + diagonale. Should be 45 for 9x9 matrix...
    private static final int urMatrixOffset = ((int) pow(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 2.0)
            + 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS) / 2;

    private static final int[] SRC_UNCERTAINTIES = new int[urMatrixOffset];

    private static final int SRC_ENTROPY = 0;
    private static final int SRC_REL_ENTROPY = 1;
    private static final int SRC_WEIGHTED_NUM_SAMPLES = 2;
    private static final int SRC_DAYS_CLOSEST_SAMPLE = 3;
    private static final int SRC_GOODNESS_OF_FIT = 4;
    private static final int SRC_PROPORTION_NSAMPLE = 5;

    private String[] dhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] dhrAlphaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrAlphaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] dhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private String[] bhrSigmaBandNames = new String[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private String relEntropyBandName;
    private String weightedNumberOfSamplesBandName;
    private String goodnessOfFitBandName;
    private String snowFractionBandName;
    private String dataMaskBandName;
    private String szaBandName;
    private String latBandName;
    private String lonBandName;

    private static final int[] TRG_DHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int[] TRG_DHR_ALPHA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int[] TRG_BHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int[] TRG_BHR_ALPHA = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int[] TRG_SIGMA_DHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int[] TRG_SIGMA_BHR = new int[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
    private static final int TRG_WEIGHTED_NUM_SAMPLES = 0;
    private static final int TRG_REL_ENTROPY = 1;
    private static final int TRG_GOODNESS_OF_FIT = 2;
    private static final int TRG_SNOW_FRACTION = 3;
    private static final int TRG_DATA_MASK = 4;


    private static final int TRG_SZA = 5;
    private static final int TRG_LAT = 6;
    private static final int TRG_LON = 7;

    @SourceProduct(description = "BRDF merged product")
    private Product brdfMergedProduct;

    @Parameter(description = "doy")
    private int doy;

    @Parameter(description = "Geoposition")
    private GeoPos latLon;

    @Parameter(defaultValue = "false", description = "If true, a single pixel is processed (CSV I/O)")
    private boolean singlePixelMode;

//    @Parameter(defaultValue = "false", description = "Computation for seaice mode (polar tiles)")
//    private boolean computeSeaice;
    private boolean computeSeaice = false;   // currently disabled

    @Parameter(defaultValue = "false",
            description = "If set, not all bands (i.e. no uncertainties) are written. Set to true to save computation time and disk space.")
    private boolean reducedOutput;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        /*
        Get Albedo based on model:
        F = BRDF model parameters (1x9 vector)
        cMatrix = Variance/Covariance matrix (9x9 matrix)
        U = Polynomial coefficients

        Black-sky Albedo = f0 + f1 * (-0.007574 + (-0.070887 * SZA^2) + (0.307588 * SZA^3)) + f2 * (-1.284909 + (-0.166314 * SZA^2) + (0.041840 * SZA^3))

        White-sky Albedo = f0 + f1 * (0.189184) + f2 * (-1.377622)

        Uncertainties = U^T cMatrix^-1 U , U is stored as a 1X9 vector (transpose), so, actually U^T is the regulat 9x1 vector
        */

        if (!singlePixelMode) {
            // the standard mode. In single pixel mode, lat/lon are provided
            final PixelPos pixelPos = new PixelPos(x, y);
            latLon = brdfMergedProduct.getGeoCoding().getGeoPos(pixelPos, null);
        }
        final double SZAdeg = AlbedoInversionUtils.computeSza(latLon, doy);
        final double SZA = SZAdeg * MathUtils.DTOR;

        Matrix uWsaVis = new Matrix(1, 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);
        Matrix uWsaNir = new Matrix(1, 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);
        Matrix uWsaSw = new Matrix(1, 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);

        uWsaVis.set(0, 0, 1.0);
        uWsaVis.set(0, 1, 0.189184);
        uWsaVis.set(0, 2, -1.377622);
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            uWsaNir.set(0, i + 3, uWsaVis.get(0, i));
            uWsaSw.set(0, i + 6, uWsaVis.get(0, i));
        }

        double[] alphaDHR = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        double[] alphaBHR = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

        Matrix[] sigmaBHR = new Matrix[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        Matrix[] sigmaDHR = new Matrix[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

        if (!reducedOutput) {
            final Matrix cMatrix = getCMatrixFromInversionProduct(sourceSamples);


            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                sigmaBHR[i] = new Matrix(1, 1, AlbedoInversionConstants.NO_DATA_VALUE);
                sigmaDHR[i] = new Matrix(1, 1, AlbedoInversionConstants.NO_DATA_VALUE);
            }

            // # Calculate uncertainties...
            // Breadboard uses relative entropy as maskRelEntropy here
            // but write entropy as Mask in output product!! see BB, GetInversion
            // todo: shouldn't we use entropy instead?? With the given implementation all sigmas are zero if we do not
            // use Priors, see computation of relEntropy in inversion code!
//        double maskRelEntropyDataValue = sourceSamples[SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_REL_ENTROPY].getDouble();
//        double maskRelEntropy = AlbedoInversionUtils.isValid(maskRelEntropyDataValue) ?
//                Math.exp(maskRelEntropyDataValue / 9.0) : 0.0;
            double maskEntropyDataValue = sourceSamples[SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_ENTROPY].getDouble();
//        if (maskRelEntropy > 0.0) {
            if (AlbedoInversionUtils.isValid(maskEntropyDataValue)) {
                final LUDecomposition cLUD = new LUDecomposition(cMatrix.transpose());
                if (cLUD.isNonsingular()) {
                    // # Calculate White-Sky sigma
                    sigmaBHR[0] = uWsaVis.times(cMatrix.transpose()).times(uWsaVis.transpose());
                    sigmaBHR[1] = uWsaNir.times(cMatrix.transpose()).times(uWsaNir.transpose());
                    sigmaBHR[2] = uWsaSw.times(cMatrix.transpose()).times(uWsaSw.transpose());

                    // # Calculate Black-Sky sigma
                    Matrix uBsaVis = new Matrix(1, 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);
                    Matrix uBsaNir = new Matrix(1, 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);
                    Matrix uBsaSw = new Matrix(1, 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);

                    final double[] uBsaArray = new double[]{
                            1.0,
                            -0.007574 + (-0.070887 * Math.pow(SZA, 2.0)) + (0.307588 * Math.pow(SZA, 3.0)),
                            -1.284909 + (-0.166314 * Math.pow(SZA, 2.0)) + (0.041840 * Math.pow(SZA, 3.0))
                    };

                    for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                        uBsaVis.set(0, i, uBsaArray[i]);
                        uBsaNir.set(0, i + 3, uBsaArray[i]);
                        uBsaSw.set(0, i + 6, uBsaArray[i]);
                    }

                    sigmaDHR[0] = uBsaVis.times(cMatrix.transpose()).times(uBsaVis.transpose());
                    sigmaDHR[1] = uBsaNir.times(cMatrix.transpose()).times(uBsaNir.transpose());
                    sigmaDHR[2] = uBsaSw.times(cMatrix.transpose()).times(uBsaSw.transpose());
                } else {
                    for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                        sigmaBHR[i].set(0, 0, AlbedoInversionConstants.NO_DATA_VALUE);
                        sigmaDHR[i].set(0, 0, AlbedoInversionConstants.NO_DATA_VALUE);
                    }
                }
            }

            if (!computeSeaice) {
                // calculate alpha terms
//            if (AlbedoInversionUtils.isValid(relEntropy)) {
                if (AlbedoInversionUtils.isValid(maskEntropyDataValue)) {
                    alphaDHR = computeAlphaDHR(SZA, cMatrix); // bsa = DHR
                    alphaBHR = computeAlphaBHR(cMatrix);      // wsa = BHR
                } else {
                    for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                        alphaDHR[i] = AlbedoInversionConstants.NO_DATA_VALUE;
                        alphaBHR[i] = AlbedoInversionConstants.NO_DATA_VALUE;
                    }
                }
            }

            // # Cap uncertainties and calculate sqrt
            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                final double wsa = sigmaBHR[i].get(0, 0);
                if (AlbedoInversionUtils.isValid(wsa)) {
                    sigmaBHR[i].set(0, 0, Math.min(1.0, Math.sqrt(wsa)));
                }
                final double bsa = sigmaDHR[i].get(0, 0);
                if (AlbedoInversionUtils.isValid(bsa)) {
                    sigmaDHR[i].set(0, 0, Math.min(1.0, Math.sqrt(bsa)));
                }
            }

        } // end if !reducedOutput

        // Calculate Black-Sky Albedo...
        final int numParams = AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS;
        double[] fParams = new double[numParams];
        for (int i = 0; i < numParams; i++) {
            fParams[i] = sourceSamples[i].getDouble();
        }

        double[] DHR = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        for (int i = 0; i < DHR.length; i++) {
            if (AlbedoInversionUtils.isValid(fParams[3 * i]) && AlbedoInversionUtils.isValid(fParams[1 + 3 * i]) &&
                    AlbedoInversionUtils.isValid(fParams[2 + 3 * i])) {
                DHR[i] = fParams[3 * i] +
                        fParams[1 + 3 * i] * (-0.007574 + (-0.070887 * Math.pow(SZA,
                                                                                2.0)) + (0.307588 * Math.pow(
                                SZA,
                                3.0))) +
                        fParams[2 + 3 * i] * (-1.284909 + (-0.166314 * Math.pow(SZA,
                                                                                2.0)) + (0.041840 * Math.pow(
                                SZA,
                                3.0)));
            } else {
                DHR[i] = AlbedoInversionConstants.NO_DATA_VALUE;
            }
        }

        // # Calculate White-Sky Albedo...
        double[] BHR = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        if (AlbedoInversionUtils.isValid(fParams[0]) && AlbedoInversionUtils.isValid(fParams[1]) &&
                AlbedoInversionUtils.isValid(fParams[2])) {
            BHR[0] = fParams[0] +
                    (fParams[1] * uWsaVis.get(0, 1)) +
                    (fParams[2] * uWsaVis.get(0, 2));
        } else {
            BHR[0] = AlbedoInversionConstants.NO_DATA_VALUE;
        }

        if (AlbedoInversionUtils.isValid(fParams[3]) && AlbedoInversionUtils.isValid(fParams[4]) &&
                AlbedoInversionUtils.isValid(fParams[5])) {
            BHR[1] = fParams[3] +
                    (fParams[4] * uWsaNir.get(0, 4)) +
                    (fParams[5] * uWsaNir.get(0, 5));
        } else {
            BHR[1] = AlbedoInversionConstants.NO_DATA_VALUE;
        }

        if (AlbedoInversionUtils.isValid(fParams[6]) && AlbedoInversionUtils.isValid(fParams[7]) &&
                AlbedoInversionUtils.isValid(fParams[8])) {
            BHR[2] = fParams[6] +
                    (fParams[7] * uWsaSw.get(0, 7)) +
                    (fParams[8] * uWsaSw.get(0, 8));
        } else {
            BHR[2] = AlbedoInversionConstants.NO_DATA_VALUE;
        }

        double relEntropy = sourceSamples[SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_REL_ENTROPY].getDouble();
        if (AlbedoInversionUtils.isValid(relEntropy)) {
            relEntropy = Math.exp(relEntropy / 9.0);
        }



        // write results to target product...
        final double weightedNumberOfSamples = sourceSamples[SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_WEIGHTED_NUM_SAMPLES].getDouble();
        final double goodnessOfFit = sourceSamples[SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_GOODNESS_OF_FIT].getDouble();
        double snowFraction = AlbedoInversionConstants.NO_DATA_VALUE;
        if (brdfMergedProduct.containsBand(AlbedoInversionConstants.MERGE_PROPORTION_NSAMPLES_BAND_NAME)) {
            snowFraction = sourceSamples[SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_PROPORTION_NSAMPLE].getDouble();
        }
        final double entropy = sourceSamples[SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_ENTROPY].getDouble();
        final double maskEntropy = (AlbedoInversionUtils.isValid(entropy)) ? 1.0 : 0.0;
        AlbedoResult result = new AlbedoResult(DHR, alphaDHR, sigmaDHR,
                                               BHR, alphaBHR, sigmaBHR,
                                               weightedNumberOfSamples, relEntropy, goodnessOfFit, snowFraction,
                                               maskEntropy, SZAdeg);

        fillTargetSamples(targetSamples, result);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);
        final Product targetProduct = productConfigurer.getTargetProduct();

        dhrBandNames = IOUtils.getAlbedoDhrBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetProduct.addBand(dhrBandNames[i], ProductData.TYPE_FLOAT32);
        }

        bhrBandNames = IOUtils.getAlbedoBhrBandNames();
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            targetProduct.addBand(bhrBandNames[i], ProductData.TYPE_FLOAT32);
        }

        if (!reducedOutput) {
            if (!computeSeaice) {
                dhrAlphaBandNames = IOUtils.getAlbedoDhrAlphaBandNames();
                for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                    targetProduct.addBand(dhrAlphaBandNames[i], ProductData.TYPE_FLOAT32);
                }
            }

            dhrSigmaBandNames = IOUtils.getAlbedoDhrSigmaBandNames();
            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                targetProduct.addBand(dhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
            }


            if (!computeSeaice) {
                bhrAlphaBandNames = IOUtils.getAlbedoBhrAlphaBandNames();
                for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                    targetProduct.addBand(bhrAlphaBandNames[i], ProductData.TYPE_FLOAT32);
                }
            }

            bhrSigmaBandNames = IOUtils.getAlbedoBhrSigmaBandNames();
            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                targetProduct.addBand(bhrSigmaBandNames[i], ProductData.TYPE_FLOAT32);
            }
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
//            b.setNoDataValue(Float.NaN);
            b.setNoDataValueUsed(true);
        }

        if (computeSeaice) {
            for (Band b : targetProduct.getBands()) {
                b.setValidPixelExpression(AlbedoInversionConstants.SEAICE_ALBEDO_VALID_PIXEL_EXPRESSION);
            }
        }

        if (singlePixelMode) {
            latBandName = AlbedoInversionConstants.LAT_BAND_NAME;
            targetProduct.addBand(latBandName, ProductData.TYPE_FLOAT32);

            lonBandName = AlbedoInversionConstants.LON_BAND_NAME;
            targetProduct.addBand(lonBandName, ProductData.TYPE_FLOAT32);
        }
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {
        // merged BRDF product...
        parameterBandNames = IOUtils.getInversionParameterBandNames();
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            SRC_PARAMETERS[i] = i;
            configurator.defineSample(SRC_PARAMETERS[i], parameterBandNames[i], brdfMergedProduct);
        }

        int index = 0;

        if (!reducedOutput) {
            uncertaintyBandNames = IOUtils.getInversionUncertaintyBandNames();
            for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                for (int j = i; j < 3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS; j++) {
                    SRC_UNCERTAINTIES[index] = index;
                    configurator.defineSample(SRC_PARAMETERS.length + SRC_UNCERTAINTIES[index],
                                              uncertaintyBandNames[i][j], brdfMergedProduct);
                    index++;
                }
            }
        }

        String entropyBandName = AlbedoInversionConstants.INV_ENTROPY_BAND_NAME;
        configurator.defineSample(SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_ENTROPY,
                                  entropyBandName, brdfMergedProduct);
        relEntropyBandName = AlbedoInversionConstants.INV_REL_ENTROPY_BAND_NAME;
        configurator.defineSample(SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_REL_ENTROPY,
                                  relEntropyBandName, brdfMergedProduct);
        weightedNumberOfSamplesBandName = AlbedoInversionConstants.INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME;
        configurator.defineSample(
                SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_WEIGHTED_NUM_SAMPLES,
                weightedNumberOfSamplesBandName, brdfMergedProduct);
        configurator.defineSample(
                SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_DAYS_CLOSEST_SAMPLE,
                AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME, brdfMergedProduct);
        goodnessOfFitBandName = AlbedoInversionConstants.INV_GOODNESS_OF_FIT_BAND_NAME;
        configurator.defineSample(SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_GOODNESS_OF_FIT,
                                  goodnessOfFitBandName, brdfMergedProduct);
        if (brdfMergedProduct.containsBand(AlbedoInversionConstants.MERGE_PROPORTION_NSAMPLES_BAND_NAME)) {
            String proportionNsamplesBandName = AlbedoInversionConstants.MERGE_PROPORTION_NSAMPLES_BAND_NAME;
            configurator.defineSample(SRC_PARAMETERS.length + SRC_UNCERTAINTIES.length + SRC_PROPORTION_NSAMPLE,
                                      proportionNsamplesBandName, brdfMergedProduct);
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

        if (!reducedOutput) {
            if (!computeSeaice) {
                for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                    TRG_DHR_ALPHA[i] = index;
                    configurator.defineSample(TRG_DHR_ALPHA[i], dhrAlphaBandNames[i]);
                    index++;
                }
            }

            if (!computeSeaice) {
                for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                    TRG_BHR_ALPHA[i] = index;
                    configurator.defineSample(TRG_BHR_ALPHA[i], bhrAlphaBandNames[i]);
                    index++;
                }
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
        }

        configurator.defineSample(index++, weightedNumberOfSamplesBandName);
        configurator.defineSample(index++, relEntropyBandName);
        configurator.defineSample(index++, goodnessOfFitBandName);
        configurator.defineSample(index++, snowFractionBandName);
        configurator.defineSample(index++, dataMaskBandName);
        configurator.defineSample(index, szaBandName);

        if (singlePixelMode) {
            index++;
            configurator.defineSample(index++, latBandName);
            configurator.defineSample(index, lonBandName);
        }
    }


    private double[] computeAlphaDHR(double SZA, Matrix c) {
        double[] alphaDHR = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

        double[] kDHR = new double[]{
                1.0,
                -0.007574 + (-0.070887 * Math.pow(SZA, 2.0)) + (0.307588 * Math.pow(SZA, 3.0)),
                -1.284909 + (-0.166314 * Math.pow(SZA, 2.0)) + (0.041840 * Math.pow(SZA, 3.0))};

        Matrix mDHR = new Matrix(3, 9, 0.0);

        for (int a = 0; a < 3; a++) {
            mDHR.set(a, (a * 3), kDHR[0]);
            mDHR.set(a, (a * 3) + 1, kDHR[1]);
            mDHR.set(a, (a * 3) + 2, kDHR[2]);
        }

        Matrix cDHR = mDHR.times(c.transpose()).times(mDHR.transpose());

        if (AlbedoInversionUtils.isValidCMatrix(c)) {
            alphaDHR[0] = (float) (cDHR.get(0, 1) / Math.sqrt(cDHR.get(0, 0) * cDHR.get(1, 1)));
            alphaDHR[1] = (float) (cDHR.get(0, 2) / Math.sqrt(cDHR.get(0, 0) * cDHR.get(2, 2)));
            alphaDHR[2] = (float) (cDHR.get(1, 2) / Math.sqrt(cDHR.get(1, 1) * cDHR.get(2, 2)));
        } else {
            for (int i = 0; i < 3; i++) {
                alphaDHR[i] = AlbedoInversionConstants.NO_DATA_VALUE;
            }
        }

        return alphaDHR;
    }

    private double[] computeAlphaBHR(Matrix c) {
        double[] alphaBHR = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

        final double kBHR[] = {1, 0.189184, -1.377622};
        Matrix mBHR = new Matrix(3, 9, 0.0);
        for (int a = 0; a < 3; a++) {
            mBHR.set(a, (a * 3), kBHR[0]);
            mBHR.set(a, (a * 3) + 1, kBHR[1]);
            mBHR.set(a, (a * 3) + 2, kBHR[2]);
        }

        Matrix cBHR = mBHR.times(c.transpose()).times(mBHR.transpose());

        if (AlbedoInversionUtils.isValidCMatrix(c)) {
            alphaBHR[0] = (float) (cBHR.get(0, 1) / Math.sqrt(cBHR.get(0, 0) * cBHR.get(1, 1)));
            alphaBHR[1] = (float) (cBHR.get(0, 2) / Math.sqrt(cBHR.get(0, 0) * cBHR.get(2, 2)));
            alphaBHR[2] = (float) (cBHR.get(1, 2) / Math.sqrt(cBHR.get(1, 1) * cBHR.get(2, 2)));
        } else {
            for (int i = 0; i < 3; i++) {
                alphaBHR[i] = AlbedoInversionConstants.NO_DATA_VALUE;
            }
        }

        return alphaBHR;
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

        if (!reducedOutput) {
            if (!computeSeaice) {
                // DHR_ALPHA (black sky albedo)
                for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                    targetSamples[TRG_DHR_ALPHA[i]].set(result.getBsaAlpha()[i]);
                }
            }

            // DHR_sigma (black sky albedo uncertainty)
            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                targetSamples[TRG_SIGMA_DHR[i]].set(result.getBsaSigma()[i].get(0, 0));
            }

            if (!computeSeaice) {
                // BHR_ALPHA (white sky albedo)
                for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                    targetSamples[TRG_BHR_ALPHA[i]].set(result.getWsaAlpha()[i]);
                }
            }

            // BHR_sigma (white sky albedo uncertainty)
            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                targetSamples[TRG_SIGMA_BHR[i]].set(result.getWsaSigma()[i].get(0, 0));
            }
        }

        final int offsetFactor = reducedOutput ? 2 : 6;
        int index = offsetFactor * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS;
        targetSamples[index + TRG_WEIGHTED_NUM_SAMPLES].set(result.getWeightedNumberOfSamples());
        targetSamples[index + TRG_REL_ENTROPY].set(result.getRelEntropy());
        targetSamples[index + TRG_GOODNESS_OF_FIT].set(result.getGoodnessOfFit());
        targetSamples[index + TRG_SNOW_FRACTION].set(result.getSnowFraction());
        targetSamples[index + TRG_DATA_MASK].set(result.getDataMask());
        targetSamples[index + TRG_SZA].set(result.getSza());
        if (singlePixelMode) {
            targetSamples[index + TRG_LAT].set(latLon.getLat());
            targetSamples[index + TRG_LON].set(latLon.getLon());
        }
    }

    private Matrix getCMatrixFromInversionProduct(Sample[] sourceSamples) {
        Matrix C = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                              3 * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS);
        double[] cTmp = new double[SRC_UNCERTAINTIES.length];

        int index = 0;
        final int n = AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS;
        for (int i = 0; i < SRC_UNCERTAINTIES.length; i++) {
            final double sampleUncertainty = sourceSamples[SRC_PARAMETERS.length + index].getDouble();
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
                C.set(n - k, n - k + i, cTmp[index1 - n + i]);
                C.set(n - k + i, n - k, cTmp[index1 - n + i]);
            }
        }

        return C;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BrdfToAlbedoOp.class);
        }
    }

}

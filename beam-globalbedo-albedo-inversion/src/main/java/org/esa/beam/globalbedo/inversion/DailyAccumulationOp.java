package org.esa.beam.globalbedo.inversion;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.util.logging.Level;

/**
 * Pixel operator implementing the daily accumulation part of python breadboard.
 * For performance reasons, the daily accumulations are written to simple binary files and NOT saved as Dimap products.
 * The breadboard file is 'AlbedoInversionDailyAccumulator.py' provided by Gerardo Lopez Saldana.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@SuppressWarnings({"MismatchedReadAndWriteOfArray", "StatementWithEmptyBody"})
@OperatorMetadata(alias = "ga.inversion.dailyaccbinary",
        description = "Provides daily accumulation of single BBDR observations",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2011 by Brockmann Consult")
public class DailyAccumulationOp extends PixelOperator {

    private static final int SRC_BB_NIR = 1;
    private static final int SRC_BB_SW = 2;
    private static final int SRC_SIG_BB_VIS_VIS = 3;
    private static final int SRC_SIG_BB_VIS_NIR = 4;
    private static final int SRC_SIG_BB_VIS_SW = 5;
    private static final int SRC_SIG_BB_NIR_NIR = 6;
    private static final int SRC_SIG_BB_NIR_SW = 7;
    private static final int SRC_SIG_BB_SW_SW = 8;
    private static final int SRC_KVOL_BRDF_VIS = 9;
    private static final int SRC_KVOL_BRDF_NIR = 10;
    private static final int SRC_KVOL_BRDF_SW = 11;
    private static final int SRC_KGEO_BRDF_VIS = 12;
    private static final int SRC_KGEO_BRDF_NIR = 13;
    private static final int SRC_KGEO_BRDF_SW = 14;
    private static final int SRC_AOD550 = 15;
    private static final int SRC_NDVI = 16;
    private static final int SRC_SIG_NDVI = 17;
    private static final int SRC_VZA = 18;
    private static final int SRC_SZA = 19;
    private static final int SRC_RAA = 20;
    private static final int SRC_DEM = 21;
    private static final int SRC_SNOW_MASK = 22;
    private static final int SRC_LAND_MASK = 23;
    private static final int SRC_SEAICE_MASK = 24;

    private static final int[][] TRG_M =
            new int[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS]
                    [3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private static final int[] TRG_V =
            new int[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private static final int TRG_E = 90;
    private static final int TRG_MASK = 91;

    private static final int sourceSampleOffset = 100;  // this value must be >= number of bands in a source product

    @SourceProducts(description = "BBDR source product")
    private Product[] sourceProducts;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "false", description = "Computation for seaice mode (polar tiles)")
    private boolean computeSeaice;

    @Parameter(defaultValue = "false", description = "Debug - run additional parts of code if needed.")
    private boolean debug;

    @Parameter(defaultValue = "true", description = "Write binary accumulator file.")
    private boolean writeBinaryFile;

    @Parameter(defaultValue = "1.0", description = "Weighting of uncertainties (test option, should be 1.0 usually!).")
    private double uncertaintyWeightingFactor;


    @Parameter(description = "Daily accumulator binary file")
    private File dailyAccumulatorBinaryFile;


    private float[][][] resultArray;

    private int numPixelsProcessed = 0;

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) {

        for (int i = 0; i < sourceProducts.length; i++) {
            Product sourceProduct = sourceProducts[i];

            if (sourceProduct.getPreferredTileSize() == null) {
                sourceProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 45);
            }

            configurator.defineSample(sourceSampleOffset * i, AlbedoInversionConstants.BBDR_BB_VIS_NAME,
                    sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_BB_NIR, AlbedoInversionConstants.BBDR_BB_NIR_NAME,
                    sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_BB_SW, AlbedoInversionConstants.BBDR_BB_SW_NAME,
                    sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_BB_VIS_VIS,
                    AlbedoInversionConstants.BBDR_SIG_BB_VIS_VIS_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_BB_VIS_NIR,
                    AlbedoInversionConstants.BBDR_SIG_BB_VIS_NIR_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_BB_VIS_SW,
                    AlbedoInversionConstants.BBDR_SIG_BB_VIS_SW_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_BB_NIR_NIR,
                    AlbedoInversionConstants.BBDR_SIG_BB_NIR_NIR_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_BB_NIR_SW,
                    AlbedoInversionConstants.BBDR_SIG_BB_NIR_SW_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_BB_SW_SW,
                    AlbedoInversionConstants.BBDR_SIG_BB_SW_SW_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_KVOL_BRDF_VIS,
                    AlbedoInversionConstants.BBDR_KVOL_BRDF_VIS_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_KVOL_BRDF_NIR,
                    AlbedoInversionConstants.BBDR_KVOL_BRDF_NIR_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_KVOL_BRDF_SW,
                    AlbedoInversionConstants.BBDR_KVOL_BRDF_SW_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_KGEO_BRDF_VIS,
                    AlbedoInversionConstants.BBDR_KGEO_BRDF_VIS_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_KGEO_BRDF_NIR,
                    AlbedoInversionConstants.BBDR_KGEO_BRDF_NIR_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_KGEO_BRDF_SW,
                    AlbedoInversionConstants.BBDR_KGEO_BRDF_SW_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_AOD550, AlbedoInversionConstants.BBDR_AOD550_NAME,
                    sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_NDVI, AlbedoInversionConstants.BBDR_NDVI_NAME,
                    sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SIG_NDVI,
                    AlbedoInversionConstants.BBDR_SIG_NDVI_NAME, sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_VZA, AlbedoInversionConstants.BBDR_VZA_NAME,
                    sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SZA, AlbedoInversionConstants.BBDR_SZA_NAME,
                    sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_RAA, AlbedoInversionConstants.BBDR_RAA_NAME,
                    sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_DEM, AlbedoInversionConstants.BBDR_DEM_NAME,
                    sourceProduct);
            configurator.defineSample((sourceSampleOffset * i) + SRC_SNOW_MASK,
                    AlbedoInversionConstants.BBDR_SNOW_MASK_NAME, sourceProduct);

            // get land mask (sensor dependent)
            AlbedoInversionUtils.setLandMaskSourceSample(configurator, (sourceSampleOffset * i) + SRC_LAND_MASK,
                    sourceProduct, computeSeaice);
            if (computeSeaice) {
                AlbedoInversionUtils.setSeaiceMaskSourceSample(configurator, (sourceSampleOffset * i) + SRC_SEAICE_MASK,
                        sourceProduct);
            }
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) throws OperatorException {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();
        ProductUtils.copyGeoCoding(sourceProducts[0], targetProduct);

        // the target product is not really needed later on, but
        // we need one target band to make sure that computePixel is processed
        // and the binary outout is written after all 1200x1200 pixels are done

        final String maskBandName = AlbedoInversionConstants.ACC_MASK_NAME;
        targetProduct.addBand(maskBandName, ProductData.TYPE_INT8);

        if (computeSeaice) {
            resultArray = new float[AlbedoInversionConstants.NUM_ACCUMULATOR_BANDS]
                    [AlbedoInversionConstants.SEAICE_TILE_WIDTH]
                    [AlbedoInversionConstants.SEAICE_TILE_HEIGHT];
        } else {
            resultArray = new float[AlbedoInversionConstants.NUM_ACCUMULATOR_BANDS]
                    [AlbedoInversionConstants.MODIS_TILE_WIDTH]
                    [AlbedoInversionConstants.MODIS_TILE_HEIGHT];
        }
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) {
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = 0; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                TRG_M[i][j] = 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * i + j;
            }
        }
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            TRG_V[i] = 3 * 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS + i;
        }
        configurator.defineSample(TRG_MASK, AlbedoInversionConstants.ACC_MASK_NAME);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples,
                                WritableSample[] targetSamples) {

        Matrix M = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);
        Matrix V = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);
        Matrix E = new Matrix(1, 1);
        double mask = 0.0;

//        if (x == 1030 && y == 520) {
//            System.out.println("x = " + x);
//        }
        // accumulate the matrices from the single products...
        for (int i = 0; i < sourceProducts.length; i++) {
            final Accumulator accumulator = getMatricesPerBBDRDataset(sourceSamples, i);
            M.plusEquals(accumulator.getM());
            V.plusEquals(accumulator.getV());
            E.plusEquals(accumulator.getE());
            mask += accumulator.getMask();
        }

        // fill target samples...
        Accumulator accumulator = new Accumulator(M, V, E, mask);
        fillTargetSamples(targetSamples, accumulator);

        // we want to write binary files for performance reasons...
        // the following code only works correctly if we have EXACTLY ONE target band
        // todo: try to find something better...
        fillBinaryResultArray(accumulator, x, y);
        numPixelsProcessed++;
        if (numPixelsProcessed == sourceProducts[0].getSceneRasterWidth() *
                sourceProducts[0].getSceneRasterHeight()) {
            BeamLogManager.getSystemLogger().log(Level.INFO, "all pixels processed [" + numPixelsProcessed + "]");
            if (writeBinaryFile) {
                BeamLogManager.getSystemLogger().log(Level.INFO, "...writing accumulator result array...");
                IOUtils.writeFloatArrayToFile(dailyAccumulatorBinaryFile, resultArray);
                BeamLogManager.getSystemLogger().log(Level.INFO, "accumulator result array written.");
            }
        }
    }

    private Accumulator getMatricesPerBBDRDataset(Sample[] sourceSamples, int sourceProductIndex) {

        if (computeSeaice) {
            // do not consider land pixels, non-snowfilter pixels, pixels with BBDR == 0.0 or -9999.0, SD == 0.0
            if (isSeaiceFilter(sourceSamples, sourceProductIndex) ||
                    isBBDRFilter(sourceSamples, sourceProductIndex) ||
                    isSDFilter(sourceSamples, sourceProductIndex)) {
                return getZeroAccumulator();
            }
        } else if (isLandFilter(sourceSamples, sourceProductIndex) || isSnowFilter(sourceSamples, sourceProductIndex) ||
                isBBDRFilter(sourceSamples, sourceProductIndex) || isSDFilter(sourceSamples, sourceProductIndex)) {
            // do not consider non-land pixels, non-snowfilter pixels, pixels with BBDR == 0.0 or -9999.0, SD == 0.0
            return getZeroAccumulator();
        }

        // get kernels...
        Matrix kernels = getKernels(sourceSamples, sourceProductIndex);

        // compute C matrix...
        final double[] correlation = getCorrelation(sourceSamples, sourceProductIndex);
        final double[] SD = getSD(sourceSamples, sourceProductIndex);
        Matrix C = new Matrix(
                AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);
        Matrix thisC = new Matrix(AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);

        int count = 0;
        int cCount = 0;
        for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
            for (int k = j + 1; k < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; k++) {
                if (k == j + 1) {
                    cCount++;
                }
                C.set(cCount, 0, correlation[count] * SD[j] * SD[k]);
                count++;
                cCount++;
            }
        }

        cCount = 0;
        for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
            C.set(cCount, 0, SD[j] * SD[j]);
            cCount = cCount + AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS - j;
        }

        count = 0;
        for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
            for (int k = j; k < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; k++) {
                thisC.set(j, k, C.get(count, 0));
                thisC.set(k, j, thisC.get(j, k));
                count++;
            }
        }

        // compute M, V, E matrices...
        final Matrix bbdr = getBBDR(sourceSamples, sourceProductIndex);
        final Matrix inverseC = thisC.inverse();
        final Matrix M = (kernels.transpose().times(inverseC)).times(kernels);
        final Matrix inverseCDiagFlat = AlbedoInversionUtils.getRectangularDiagonalMatrix(inverseC);
        final Matrix kernelTimesInvCDiag = kernels.transpose().times(inverseCDiagFlat);
        final Matrix V = kernelTimesInvCDiag.times(bbdr);
        final Matrix E = (bbdr.transpose().times(inverseC)).times(bbdr);

        // return result
        return new Accumulator(M, V, E, 1);
    }

    private Accumulator getZeroAccumulator() {
        final Matrix zeroM = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);
        final Matrix zeroV = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);
        final Matrix zeroE = new Matrix(1, 1);

        return new Accumulator(zeroM, zeroV, zeroE, 0);
    }

    private Matrix getBBDR(Sample[] sourceSamples, int sourceProductIndex) {
        Matrix bbdr = new Matrix(AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);
        final double bbVis = sourceSamples[sourceProductIndex * sourceSampleOffset].getDouble();
        bbdr.set(0, 0, bbVis);
        final double bbNir = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_NIR].getDouble();
        bbdr.set(1, 0, bbNir);
        final double bbSw = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_SW].getDouble();
        bbdr.set(2, 0, bbSw);
        return bbdr;
    }

    private double[] getSD(Sample[] sourceSamples, int sourceProductIndex) {
        double[] SD = new double[3];
        SD[0] = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_VIS_VIS].getDouble();
        SD[1] = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_NIR_NIR].getDouble();
        SD[2] = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_SW_SW].getDouble();
        return SD;
    }

    private double[] getCorrelation(Sample[] sourceSamples, int sourceProductIndex) {
        double[] correlation = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        correlation[0] = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_VIS_NIR].getDouble();
        correlation[1] = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_VIS_SW].getDouble();
        correlation[2] = sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_NIR_SW].getDouble();
        return correlation;
    }

    private void fillTargetSamples(WritableSample[] targetSamples, Accumulator accumulator) {
        // just ONE target band...  !!!
        targetSamples[TRG_MASK].set(accumulator.getMask());
    }

    private void fillBinaryResultArray(Accumulator accumulator, int x, int y) {
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = 0; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                resultArray[TRG_M[i][j]][x][y] = (float) accumulator.getM().get(i, j);
            }
        }
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            resultArray[TRG_V[i]][x][y] = (float) accumulator.getV().get(i, 0);
        }
        resultArray[TRG_E][x][y] = (float) accumulator.getE().get(0, 0);
        resultArray[TRG_MASK][x][y] = (float) accumulator.getMask();
    }


    private Matrix getKernels(Sample[] sourceSamples, int sourceProductIndex) {
        Matrix kernels = new Matrix(AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);

        kernels.set(0, 0, 1.0);
        kernels.set(1, 3, 1.0);
        kernels.set(2, 6, 1.0);
        kernels.set(0, 1, sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_KVOL_BRDF_VIS].getDouble());
        kernels.set(1, 4, sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_KVOL_BRDF_NIR].getDouble());
        kernels.set(2, 7, sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_KVOL_BRDF_SW].getDouble());
        kernels.set(0, 2, sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_KGEO_BRDF_VIS].getDouble());
        kernels.set(1, 5, sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_KGEO_BRDF_NIR].getDouble());
        kernels.set(2, 8, sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_KGEO_BRDF_SW].getDouble());

        return kernels;
    }


    private boolean isLandFilter(Sample[] sourceSamples, int sourceProductIndex) {

        if (sourceProducts[sourceProductIndex].getProductType().startsWith("MER") ||
                sourceProducts[sourceProductIndex].getName().startsWith("MER")) {
            if (!sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_LAND_MASK].getBoolean()) {
                return true;
            }
        } else if (sourceProducts[sourceProductIndex].getProductType().startsWith("VGT") ||
                sourceProducts[sourceProductIndex].getName().startsWith("VGT")) {
            if ((sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_LAND_MASK].getInt() & 8) == 0) {
                return true;
            }
        } else if (sourceProducts[sourceProductIndex].getProductType().startsWith("ATS") ||
                sourceProducts[sourceProductIndex].getName().startsWith("ATS")) {
            return false; // test
        }
        return false;
    }

    private boolean isSnowFilter(Sample[] sourceSamples, int sourceProductIndex) {
        return ((computeSnow && !(sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SNOW_MASK].getInt() == 1)) ||
                (!computeSnow && (sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SNOW_MASK].getInt() == 1)));
    }

    private boolean isSeaiceFilter(Sample[] sourceSamples, int sourceProductIndex) {
        return !sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SEAICE_MASK].getBoolean();
    }

    private boolean isBBDRFilter(Sample[] sourceSamples, int sourceProductIndex) {
        return (sourceSamples[sourceProductIndex * sourceSampleOffset].getDouble() == 0.0 ||
                !AlbedoInversionUtils.isValid(sourceSamples[sourceProductIndex * sourceSampleOffset].getDouble()) ||
                sourceSamples[sourceProductIndex * sourceSampleOffset].getDouble() == 9999.0 ||
                sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_NIR].getDouble() == 0.0 ||
                !AlbedoInversionUtils.isValid(sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_NIR].getDouble()) ||
                sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_NIR].getDouble() == 9999.0 ||
                sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_SW].getDouble() == 0.0 ||
                !AlbedoInversionUtils.isValid(sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_SW].getDouble()) ||
                sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_BB_SW].getDouble() == 9999.0);
    }

    private boolean isSDFilter(Sample[] sourceSamples, int sourceProductIndex) {
        return (sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_VIS_VIS].getDouble() == 0.0 &&
                sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_NIR_NIR].getDouble() == 0.0 &&
                sourceSamples[sourceProductIndex * sourceSampleOffset + SRC_SIG_BB_SW_SW].getDouble() == 0.0);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(DailyAccumulationOp.class);
        }
    }
}

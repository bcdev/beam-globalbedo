package org.esa.beam.globalbedo.inversion;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;

/**
 * New class for Daily accumulation replacing the operator version to avoid massive file I/O
 *
 * @author olafd
 */
public class DailyAccumulation {

    private static final int[][] TRG_M =
            new int[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS]
                    [3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private static final int[] TRG_V =
            new int[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private static final int TRG_E = 90;
    private static final int TRG_MASK = 91;
    private static final int TRG_DCS = 92;

    static {
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = 0; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                TRG_M[i][j] = 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * i + j;
            }
        }
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            TRG_V[i] = 3 * 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS + i;
        }
    }

    /**
     * provides a daily accumulator. To be called per pixel.
     *
     * @param x             - x pixel
     * @param y             - y pixel
     * @param bbdrTiles     - BBDR tiles: 2D array over BBDR products and their bands
     * @param computeSnow   - computation shall consider snow
     * @param computeSeaice - computation shall consider sea ice   todo: do we need this?
     * @return
     */
    public static Accumulator compute(int x, int y, Product[] bbdrProducts, Tile[][] bbdrTiles,
                                      boolean computeSnow, boolean computeSeaice) {
        Matrix M = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);
        Matrix V = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);
        Matrix E = new Matrix(1, 1);
        double mask = 0.0;

        // accumulate the matrices from the single products...
        // bbdrTiles = bbdrTiles[productIndex][bandIndex]
        for (int i = 0; i < bbdrTiles.length; i++) {
            final Accumulator accumulator = getMatricesPerBBDRDataset(x, y, bbdrProducts, bbdrTiles, i,
                    computeSnow, computeSeaice);
            M.plusEquals(accumulator.getM());
            V.plusEquals(accumulator.getV());
            E.plusEquals(accumulator.getE());
            mask += accumulator.getMask();
        }

        return new Accumulator(M, V, E, mask);
    }

    public static float[] computeResultArray(int x, int y, Product[] bbdrProducts, Tile[][] bbdrTiles,
                                      boolean computeSnow, boolean computeSeaice) {
        Matrix M = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                              3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);
        Matrix V = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);
        Matrix E = new Matrix(1, 1);
        double mask = 0.0;

        // accumulate the matrices from the single products...
        // bbdrTiles = bbdrTiles[productIndex][bandIndex]
        for (int i = 0; i < bbdrTiles.length; i++) {
            final Accumulator accumulator = getMatricesPerBBDRDataset(x, y, bbdrProducts, bbdrTiles, i,
                                                                      computeSnow, computeSeaice);
            M.plusEquals(accumulator.getM());
            V.plusEquals(accumulator.getV());
            E.plusEquals(accumulator.getE());
            mask += accumulator.getMask();
        }

        return fillBinaryResultArray(new Accumulator(M, V, E, mask), x, y);
    }


    public static Accumulator createZeroAccumulator() {
        final Matrix zeroM = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);
        final Matrix zeroV = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);
        final Matrix zeroE = new Matrix(1, 1);

        return new Accumulator(zeroM, zeroV, zeroE, 0);
    }

    private static float[] fillBinaryResultArray(Accumulator accumulator, int x, int y) {
        float[] resultArray = new float[AlbedoInversionConstants.NUM_ACCUMULATOR_BANDS+1];
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = 0; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                resultArray[TRG_M[i][j]] = (float) accumulator.getM().get(i, j);
            }
        }
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            resultArray[TRG_V[i]] = (float) accumulator.getV().get(i, 0);
        }
        resultArray[TRG_E] = (float) accumulator.getE().get(0, 0);
        resultArray[TRG_MASK] = (float) accumulator.getMask();
        resultArray[TRG_DCS] = Float.NaN; // todo

        return resultArray;
    }


    private static Accumulator getMatricesPerBBDRDataset(int x, int y,
                                                         Product[] bbdrProducts,
                                                         Tile[][] bbdrTiles,
                                                         int sourceProductIndex,
                                                         boolean computeSnow,
                                                         boolean computeSeaice) {

        if (computeSeaice) {
            // do not consider land pixels, non-snowfilter pixels, pixels with BBDR == 0.0 or -9999.0, SD == 0.0
            if (isSeaiceFilter(x, y, bbdrTiles, sourceProductIndex) ||
                    isBBDRFilter(x, y, bbdrTiles, sourceProductIndex) ||
                    isSDFilter(x, y, bbdrTiles, sourceProductIndex)) {
                return createZeroAccumulator();
            }
        } else if (isLandFilter(x, y, bbdrProducts, bbdrTiles, sourceProductIndex) ||
                isSnowFilter(x, y, bbdrTiles, sourceProductIndex, computeSnow) ||
                isBBDRFilter(x, y, bbdrTiles, sourceProductIndex) ||
                isSDFilter(x, y, bbdrTiles, sourceProductIndex)) {
            // do not consider non-land pixels, non-snowfilter pixels, pixels with BBDR == 0.0 or -9999.0, SD == 0.0
            return createZeroAccumulator();
        }

        // get kernels...
        Matrix kernels = getKernels(x, y, bbdrTiles, sourceProductIndex);

        // compute C matrix...
        final double[] correlation = getCorrelation(x, y, bbdrTiles, sourceProductIndex);
        final double[] SD = getSD(x, y, bbdrTiles, sourceProductIndex);
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
        final Matrix bbdr = getBBDR(x, y, bbdrTiles, sourceProductIndex);
        final Matrix inverseC = thisC.inverse();
        final Matrix M = (kernels.transpose().times(inverseC)).times(kernels);
        final Matrix inverseCDiagFlat = AlbedoInversionUtils.getRectangularDiagonalMatrix(inverseC);
        final Matrix kernelTimesInvCDiag = kernels.transpose().times(inverseCDiagFlat);
        final Matrix V = kernelTimesInvCDiag.times(bbdr);
        final Matrix E = (bbdr.transpose().times(inverseC)).times(bbdr);

        // return result
        return new Accumulator(M, V, E, 1);
    }

    private static Matrix getBBDR(int x, int y, Tile[][] bbdrTiles, int sourceProductIndex) {
        Matrix bbdr = new Matrix(AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);
        final double bbVis = bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_BB_VIS].getSampleDouble(x, y);
        bbdr.set(0, 0, bbVis);
        final double bbNir = bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_BB_NIR].getSampleDouble(x, y);
        bbdr.set(1, 0, bbNir);
        final double bbSw = bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_BB_SW].getSampleDouble(x, y);
        bbdr.set(2, 0, bbSw);
        return bbdr;
    }

    private static double[] getSD(int x, int y, Tile[][] bbdrTiles, int sourceProductIndex) {
        double[] SD = new double[3];
        SD[0] = bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_SIG_BB_VIS_VIS].getSampleDouble(x, y);
        SD[1] = bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_SIG_BB_NIR_NIR].getSampleDouble(x, y);
        SD[2] = bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_SIG_BB_SW_SW].getSampleDouble(x, y);
        return SD;
    }

    private static double[] getCorrelation(int x, int y, Tile[][] bbdrTiles, int sourceProductIndex) {
        double[] correlation = new double[AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];
        correlation[0] = bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_SIG_BB_VIS_NIR].getSampleDouble(x, y);
        correlation[1] = bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_SIG_BB_VIS_SW].getSampleDouble(x, y);
        correlation[2] = bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_SIG_BB_NIR_SW].getSampleDouble(x, y);
        return correlation;
    }

    private static Matrix getKernels(int x, int y, Tile[][] bbdrTiles, int sourceProductIndex) {
        Matrix kernels = new Matrix(AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);

        kernels.set(0, 0, 1.0);
        kernels.set(1, 3, 1.0);
        kernels.set(2, 6, 1.0);
        kernels.set(0, 1, bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_KVOL_BRDF_VIS].getSampleDouble(x, y));
        kernels.set(1, 4, bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_KVOL_BRDF_NIR].getSampleDouble(x, y));
        kernels.set(2, 7, bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_KVOL_BRDF_SW].getSampleDouble(x, y));
        kernels.set(0, 2, bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_KGEO_BRDF_VIS].getSampleDouble(x, y));
        kernels.set(1, 5, bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_KGEO_BRDF_NIR].getSampleDouble(x, y));
        kernels.set(2, 8, bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_KGEO_BRDF_SW].getSampleDouble(x, y));

        return kernels;
    }

    private static boolean isLandFilter(int x, int y, Product[] bbdrProducts, Tile[][] bbdrTiles, int sourceProductIndex) {
        if (bbdrProducts[sourceProductIndex].getProductType().startsWith("MER") ||
                bbdrProducts[sourceProductIndex].getName().startsWith("MER") ||
                bbdrProducts[sourceProductIndex].getProductType().startsWith("ATS") ||
                bbdrProducts[sourceProductIndex].getName().startsWith("ATS")) {
            if (!bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_LAND_MASK].getSampleBoolean(x, y)) {
                return true;
            }
        } else if (bbdrProducts[sourceProductIndex].getProductType().startsWith("VGT") ||
                bbdrProducts[sourceProductIndex].getName().startsWith("VGT")) {
            if ((bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_LAND_MASK].getSampleInt(x, y) & 8) == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSnowFilter(int x, int y, Tile[][] bbdrTiles, int sourceProductIndex, boolean computeSnow) {
        return ((computeSnow && !(bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_SNOW_MASK].getSampleInt(x, y) == 1)) ||
                (!computeSnow && (bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_SNOW_MASK].getSampleInt(x, y) == 1)));
    }

    private static boolean isSeaiceFilter(int x, int y, Tile[][] bbdrTiles, int sourceProductIndex) {
        return !bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_LAND_MASK].getSampleBoolean(x, y);
    }

    private static boolean isBBDRFilter(int x, int y, Tile[][] bbdrTiles, int sourceProductIndex) {
        return (bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_BB_VIS].getSampleDouble(x, y) == 0.0 ||
                Double.isNaN(bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_BB_VIS].getSampleDouble(x, y)) ||
                bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_BB_VIS].getSampleDouble(x, y) == 9999.0 ||
                bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_BB_NIR].getSampleDouble(x, y) == 0.0 ||
                Double.isNaN(bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_BB_NIR].getSampleDouble(x, y)) ||
                bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_BB_NIR].getSampleDouble(x, y) == 9999.0 ||
                bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_BB_SW].getSampleDouble(x, y) == 0.0 ||
                Double.isNaN(bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_BB_SW].getSampleDouble(x, y)) ||
                bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_BB_SW].getSampleDouble(x, y) == 9999.0);
    }

    private static boolean isSDFilter(int x, int y, Tile[][] bbdrTiles, int sourceProductIndex) {
        return (bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_SIG_BB_VIS_VIS].getSampleDouble(x, y) == 0.0 &&
                bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_SIG_BB_NIR_NIR].getSampleDouble(x, y) == 0.0 &&
                bbdrTiles[sourceProductIndex][AlbedoInversionConstants.SRC_SIG_BB_SW_SW].getSampleDouble(x, y) == 0.0);
    }

}

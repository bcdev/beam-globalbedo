package org.esa.beam.globalbedo.inversion.util;

import Jama.Matrix;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;

/**
 * Utility class for Daily accumulation part
 *
 * @author olafd
 */
public class DailyAccumulationUtils {

    public static void setDailyAccThisCMatrix(Matrix c, Matrix thisC, int count) {
        for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
            for (int k = j; k < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; k++) {
                thisC.set(j, k, c.get(count, 0));
                thisC.set(k, j, thisC.get(j, k));
                count++;
            }
        }
    }

    public static void setDailyAccCMatrix(double[] stdev, double v, Matrix c, int count, int cCount) {
        for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
            for (int k = j + 1; k < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; k++) {
                if (k == j + 1) {
                    cCount++;
                }
                c.set(cCount, 0, v * stdev[j] * stdev[k]);
                count++;
                cCount++;
            }
        }
    }

    /**
     * Returns a matrix which contains on the diagonal the original elements, and zeros elsewhere
     * Input must be a nxm matrix with n = m
     *
     * @param m - original matrix
     * @return Matrix - the filtered matrix
     */
    public static Matrix getRectangularDiagonalMatrix(Matrix m) {
        if (m.getRowDimension() != m.getColumnDimension()) {
            return null;
        }

        Matrix diag = new Matrix(m.getRowDimension(), m.getRowDimension());
        for (int i = 0; i < m.getRowDimension(); i++) {
            diag.set(i, i, m.get(i, i));
        }
        return diag;
    }

    public static  boolean isAccumulatorInputInvalid(Matrix bbdr, double[] stdev, double[] correlation, Matrix kernels) {
        return  isBBDRInvalid(bbdr) || isSDInvalid(stdev) || isCorrelationInvalid(correlation) || areKernelsInvalid(kernels);
    }

    public static  boolean isAccumulatorInputInvalid(Matrix bbdr, double[] stdev, Matrix kernels) {
        return  isBBDRInvalid(bbdr) || isSDInvalid(stdev) || areKernelsInvalid(kernels);
    }

    public static boolean isBBDRInvalid(Matrix bbdr) {
        for (int i = 0; i < bbdr.getRowDimension(); i++) {
            for (int j = 0; j < bbdr.getColumnDimension(); j++) {
                final double elem = bbdr.get(i, j);
                if (elem <= 0.0 || elem >= 1.0 || Double.isNaN(elem)) {   // BBDR must be in ]0, 1[
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean areKernelsInvalid(Matrix kernels) {
        for (int i = 0; i < kernels.getRowDimension(); i++) {
            for (int j = 0; j < kernels.getColumnDimension(); j++) {
                final double elem = kernels.get(i, j);
                if (elem == AlbedoInversionConstants.NO_DATA_VALUE || Double.isNaN(elem)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isSDInvalid(double[] stdevArr) {
        for (double stdev : stdevArr) {
            if (stdev <= 0.0 | Double.isNaN(stdev)) {    // stdev must not be zero (algorithm would fail)
                return true;
            }
        }
        return false;
    }

    private static boolean isCorrelationInvalid(double[] correlArr) {
        for (double correl : correlArr) {
            if (correl < 0.0 | Double.isNaN(correl)) {    // correlation might be zero
                return true;
            }
        }
        return false;
    }

}

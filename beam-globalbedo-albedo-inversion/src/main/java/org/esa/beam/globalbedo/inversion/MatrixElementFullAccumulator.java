package org.esa.beam.globalbedo.inversion;

/**
 * Class representing a 'full' 8-day accumulator per matrix element (M, V, or E) or mask
 *
 * Created by IntelliJ IDEA.
 * User: olafd
 * Date: 19.06.11
 * Time: 16:52
 * To change this template use File | Settings | File Templates.
 */
public class MatrixElementFullAccumulator {
    private int year;
    private int doy;
    private float[][] sumMatrix; // holds either M_ij, V_i, E or mask

    public MatrixElementFullAccumulator(int year, int doy, float[][] sumMatrix) {
        this.year = year;
        this.doy = doy;
        this.sumMatrix = sumMatrix;
    }

    public int getYear() {
        return year;
    }

    public int getDoy() {
        return doy;
    }

    public void setSumMatrix(float[][] sumMatrix) {
        this.sumMatrix = sumMatrix;
    }

    public float[][] getSumMatrix() {
        return sumMatrix;
    }

    public void accumulateSumMatrixElement(float[][] sumMatrixElement) {
        final int size1 = sumMatrixElement.length;
        final int size2 = sumMatrixElement[0].length;
        if (size1 != sumMatrix.length || size2 != sumMatrix[0].length) {
            return;
        }

        for (int i=0; i<size1; i++) {
            for (int j=0; j<size2; j++) {
               sumMatrix[i][j] += sumMatrixElement[i][j];
            }
        }
    }

    public void resetSumMatrix() {
        for (int i=0; i<sumMatrix.length; i++) {
            for (int j=0; j<sumMatrix[0].length; j++) {
               sumMatrix[i][j] = 0.0f;
            }
        }
    }

}

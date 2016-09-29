package org.esa.beam.globalbedo.inversion.spectral;

import Jama.Matrix;

/**
 * Container object holding the M, V, E estimation matrices and mask value
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class SpectralAccumulator {
    private Matrix M;
    private Matrix V;
    private Matrix E;
    private double mask;

    public SpectralAccumulator(Matrix m, Matrix v, Matrix e, double mask) {
        this.M = m;
        this.V = v;
        this.E = e;
        this.mask = mask;
    }

    /**
     * Returns an accumulator object built from matrix array of a full accumulator product to be used
     * for inversion in {@link SpectralInversionOp}}.
     *
     * @param sumMatrices - array holding M, V, E, mask
     * @param x - pixel_x
     * @param y - pixel_y
     *
     * @return Accumulator
     */
    public static SpectralAccumulator createForInversion(float[][][] sumMatrices, int x, int y, int numWaveBands) {

        // from daily accumulation:
        // we have:
        // (3*7) * (3*7) + 3*7 + 1 + 1= 464 elements to store in daily acc :-(
        // final int resultArrayElements =  (3*7) * (3*7) + 3*7 + 1 + 1;
        // resultArray = new float[resultArrayElements][subtileWidth][subtileHeight];

        Matrix M = new Matrix(3 * numWaveBands,
                              3 * numWaveBands);
        Matrix V = new Matrix(3 * numWaveBands, 1);
        Matrix E = new Matrix(1, 1);

        int index = 0;
        for (int i = 0; i < 3 * numWaveBands; i++) {
            for (int j = 0; j < 3 * numWaveBands; j++) {
                M.set(i, j, sumMatrices[index++][x][y]);
            }
        }
        for (int i = 0; i < 3 * numWaveBands; i++) {
            V.set(i, 0, sumMatrices[index++][x][y]);
        }
        E.set(0, 0, sumMatrices[index++][x][y]);

        final double mask = sumMatrices[index][x][y];

        return new SpectralAccumulator(M, V, E, mask);
    }

    private static SpectralAccumulator createZeroAccumulator(int numWaveBands) {
        final Matrix zeroM = new Matrix(3 * numWaveBands,
                                        3 * numWaveBands);
        final Matrix zeroV = new Matrix(3 * numWaveBands, 1);
        final Matrix zeroE = new Matrix(1, 1);

        return new SpectralAccumulator(zeroM, zeroV, zeroE, 0);
    }

    // getters and setters...
    public Matrix getM() {
        return M;
    }

    public Matrix getV() {
        return V;
    }

    public Matrix getE() {
        return E;
    }

    public double getMask() {
        return mask;
    }

    public void setM(Matrix m) {
        M = m;
    }

    public void setV(Matrix v) {
        V = v;
    }

    public void setE(Matrix e) {
        E = e;
    }

    public void setMask(double mask) {
        this.mask = mask;
    }
}

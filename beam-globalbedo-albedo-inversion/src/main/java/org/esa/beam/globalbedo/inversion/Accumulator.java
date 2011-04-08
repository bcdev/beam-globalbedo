package org.esa.beam.globalbedo.inversion;

import Jama.Matrix;
import org.esa.beam.framework.gpf.experimental.PointOperator;

/**
 * Container object holding the M, V, E estimation matrices and mask value
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class Accumulator {

    private Matrix M;
    private Matrix V;
    private Matrix E;
    private int mask;
    private int doyClosestSample;  // needed in full accumulation part

    public Accumulator(Matrix m, Matrix v, Matrix e, int mask) {
        this.M = m;
        this.V = v;
        this.E = e;
        this.mask = mask;
    }

    public Accumulator(Matrix m, Matrix v, Matrix e, int mask, int doyClosestSample) {
        this.M = m;
        this.V = v;
        this.E = e;
        this.mask = mask;
        this.doyClosestSample = doyClosestSample;
    }

    /**
     * Returns an accumulator object built from source samples of an accumulator product to be used for inversion in {@link InversionOp}}.
     *
     * @param sourceSamples - the source samples as defined in {@link InversionOp}}.
     *
     * @return Accumulator
     */
    public static Accumulator createForInversion(PointOperator.Sample[] sourceSamples) {

        Matrix M = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                              3 * AlbedoInversionConstants.numBBDRWaveBands);
        Matrix V = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands, 1);
        Matrix E = new Matrix(1, 1);

        for (int i = 0; i < 3 * AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = 0; j < 3 * AlbedoInversionConstants.numBBDRWaveBands; j++) {
                M.set(i, j, sourceSamples[InversionOp.SRC_ACCUM_M[i][j]].getDouble());
            }
            V.set(i, 0, sourceSamples[InversionOp.SRC_ACCUM_V[i]].getDouble());
        }
        E.set(0, 0, sourceSamples[InversionOp.SRC_ACCUM_E].getDouble());
        final int mask = sourceSamples[InversionOp.SRC_ACCUM_MASK].getInt();

        return new Accumulator(M, V, E, mask);
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

    public int getMask() {
        return mask;
    }

    public int getDoyClosestSample() {
        return doyClosestSample;
    }
}

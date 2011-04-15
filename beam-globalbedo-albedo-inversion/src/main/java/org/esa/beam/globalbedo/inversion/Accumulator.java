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
    private double mask;

    public Accumulator(Matrix m, Matrix v, Matrix e, double mask) {
        this.M = m;
        this.V = v;
        this.E = e;
        this.mask = mask;
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
                final int srcIndexMij = InversionOp.SRC_ACCUM_M[i][j];
                M.set(i, j, sourceSamples[srcIndexMij].getDouble());
            }
            final int srcIndexVi = InversionOp.SRC_ACCUM_V[i];
            V.set(i, 0, sourceSamples[srcIndexVi].getDouble());
        }
        final int srcIndexE = InversionOp.SRC_ACCUM_E;
        E.set(0, 0, sourceSamples[srcIndexE].getDouble());

        final int srcIndexMask = InversionOp.SRC_ACCUM_MASK;
        final double mask = sourceSamples[srcIndexMask].getDouble();

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

    public double getMask() {
        return mask;
    }
}

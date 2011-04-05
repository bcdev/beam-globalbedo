package org.esa.beam.globalbedo.inversion;

import Jama.Matrix;

/**
 * Container object holding the M, V, E estimation matrices and mask value
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AccumulationMatrixContainer {

    Matrix M;
    Matrix V;
    Matrix E;
    int mask;
    int doyClosestSample;  // needed in full accumulation part

    // getters and setters...
    public Matrix getM() {
        return M;
    }

    public void setM(Matrix m) {
        M = m;
    }

    public Matrix getV() {
        return V;
    }

    public void setV(Matrix v) {
        V = v;
    }

    public Matrix getE() {
        return E;
    }

    public void setE(Matrix e) {
        E = e;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    public int getDoyClosestSample() {
        return doyClosestSample;
    }

    public void setDoyClosestSample(int doyClosestSample) {
        this.doyClosestSample = doyClosestSample;
    }
}

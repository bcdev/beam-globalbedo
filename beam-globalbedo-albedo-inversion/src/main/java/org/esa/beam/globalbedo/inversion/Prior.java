package org.esa.beam.globalbedo.inversion;

import Jama.LUDecomposition;
import Jama.Matrix;
import org.esa.beam.framework.gpf.experimental.PointOperator;

/**
 * Object holding the prior data elements M, V, Mask and Parameters
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class Prior {

    private Matrix M;
    private Matrix V;
    private double mask;
    private Matrix parameters;

    public Prior(Matrix m, Matrix v, double mask, Matrix parameters) {
        this.M = m;
        this.V = v;
        this.mask = mask;
        this.parameters = parameters;
    }

    /**
     * Returns a prior object built from source samples of a prior product to be used for inversion in {@link InversionOp}}.
     * This method basically represents the BB implementation 'GetPrior'
     *
     * @param sourceSamples - the source samples as defined in {@link InversionOp}}.
     * @param priorScaleFactor - the prior scale factor
     *
     * @return Prior
     */
    public static Prior createForInversion(PointOperator.Sample[] sourceSamples, double priorScaleFactor) {

        Matrix C = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                              3 * AlbedoInversionConstants.numBBDRWaveBands);              // 9x9
        Matrix inverseC = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands,
                                     3 * AlbedoInversionConstants.numBBDRWaveBands);       // 9x9
        Matrix inverseC_F = new Matrix(3 * AlbedoInversionConstants.numBBDRWaveBands, 1);  // 9x1

        double mask = 0.0;
        final int priorIndexNsamples = InversionOp.SRC_PRIOR_NSAMPLES;
        double nSamples = sourceSamples[priorIndexNsamples].getDouble();

        Matrix priorMean = new Matrix(
                AlbedoInversionConstants.numBBDRWaveBands * AlbedoInversionConstants.numBBDRWaveBands, 1);
        Matrix priorSD = new Matrix(
                AlbedoInversionConstants.numBBDRWaveBands * AlbedoInversionConstants.numBBDRWaveBands, 1);

        int index = 0;
        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
            for (int j = 0; j < AlbedoInversionConstants.numBBDRWaveBands; j++) {
                final int priorIndexMij = InversionOp.SRC_PRIOR_MEAN[i][j];
                final double m_ij = sourceSamples[priorIndexMij].getDouble();
                priorMean.set(index, 0, m_ij);
                final int priorIndexSDij = InversionOp.SRC_PRIOR_SD[i][j];
                final double sd_ij = sourceSamples[priorIndexSDij].getDouble();
                priorSD.set(index, 0, sd_ij);
                if (priorMean.get(index, 0) > 0.0 && priorSD.get(index, 0) == 0.0) {
                    mask = 1.0;
                    nSamples = 1.E-20;
                    priorSD.set(index, 0, 1.0);
                }
                if (priorMean.get(index, 0) > 0.0 && sd_ij > 0.0 && mask > 0) {
                    mask = 1.0;
                    nSamples = 1.E-20;
                    priorSD.set(index, 0, 1.0);
                }
                index++;
            }
        }

        for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands * AlbedoInversionConstants.numAlbedoParameters; i++) {
            priorSD.set(i, 0, Math.min(1.0, priorSD.get(i, 0) * priorScaleFactor));
            C.set(i, i, priorSD.get(i, 0) * priorSD.get(i, 0));
        }

        // # Calculate C inverse
        // simplified condition (GL, 20110407)
        if (nSamples > 0.0) {
            mask = 1.0;
            LUDecomposition lud = new LUDecomposition(C);
            if (lud.isNonsingular()) {
                inverseC = C.inverse();
            } else {
                index = 0;
                boolean processPixel = true;
                for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
                    for (int j = 0; j < AlbedoInversionConstants.numBBDRWaveBands; j++) {
                        if (priorMean.get(index, 0) <= 0.0 || priorMean.get(index, 0) > 1.0 ||
                            priorSD.get(index, 0) <= 0.0 || priorSD.get(index, 0) > 1.0) {
                            processPixel = false;
                            break;
                        }
                        index++;
                    }
                }
                if (processPixel) {
                    final Matrix cIdentity = Matrix.identity(3 * AlbedoInversionConstants.numBBDRWaveBands,
                                                             3 * AlbedoInversionConstants.numBBDRWaveBands);    // 9x9
                    inverseC = cIdentity.inverse();
                    index = 0;
                    for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
                        for (int j = 0; j < AlbedoInversionConstants.numBBDRWaveBands; j++) {
                            inverseC_F.set(index, 0, inverseC.get(index, index) * priorMean.get(index, 0));
                            index++;
                        }
                    }
                } else {
                    mask = 0.0;
                }
            }
            index = 0;
            for (int i = 0; i < AlbedoInversionConstants.numBBDRWaveBands; i++) {
                for (int j = 0; j < AlbedoInversionConstants.numBBDRWaveBands; j++) {
                    inverseC_F.set(index, 0, inverseC.get(index, index) * priorMean.get(index, 0));
                    index++;
                }
            }
        }

        return new Prior(inverseC, inverseC_F, mask, priorMean);
    }

    public Matrix getM() {
        return M;
    }

    public Matrix getV() {
        return V;
    }

    public double getMask() {
        return mask;
    }

    public Matrix getParameters() {
        return parameters;
    }
}

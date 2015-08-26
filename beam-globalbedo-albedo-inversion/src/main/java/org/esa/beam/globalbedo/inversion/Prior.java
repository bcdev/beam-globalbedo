package org.esa.beam.globalbedo.inversion;

import Jama.LUDecomposition;
import Jama.Matrix;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;

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
     * @param sourceSamples    - the source samples as defined in {@link InversionOp}}.
     * @param priorScaleFactor - the prior scale factor
     * @return Prior
     */
    public static Prior createForInversion(Sample[] sourceSamples, double priorScaleFactor) {

        Matrix C = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);              // 9x9
        Matrix inverseC = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);       // 9x9
        Matrix inverseC_F = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);  // 9x1

        double mask = 0.0;
        final int priorIndexNsamples = InversionOp.SRC_PRIOR_NSAMPLES;
        double nSamples = AlbedoInversionUtils.checkSummandForNan(sourceSamples[priorIndexNsamples].getDouble());

        Matrix priorMean = new Matrix(
                AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);
        Matrix priorSD = new Matrix(
                AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);

        int index = 0;
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                final int priorIndexMij = InversionOp.SRC_PRIOR_MEAN[i][j];
                final double m_ij = AlbedoInversionUtils.checkSummandForNan(sourceSamples[priorIndexMij].getDouble());
                priorMean.set(index, 0, m_ij);
                final int priorIndexSDij = InversionOp.SRC_PRIOR_SD[i][j];
                final double sd_ij = AlbedoInversionUtils.checkSummandForNan(sourceSamples[priorIndexSDij].getDouble());
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

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS; i++) {
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
                for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                    for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                        if (priorMean.get(index, 0) <= 0.0 || priorMean.get(index, 0) > 1.0 ||
                                priorSD.get(index, 0) <= 0.0 || priorSD.get(index, 0) > 1.0) {
                            processPixel = false;
                            break;
                        }
                        index++;
                    }
                }
                if (processPixel) {
                    final Matrix cIdentity = Matrix.identity(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                            3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);    // 9x9
                    inverseC = cIdentity.inverse();
                    index = 0;
                    for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                        for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                            inverseC_F.set(index, 0, inverseC.get(index, index) * priorMean.get(index, 0));
                            index++;
                        }
                    }
                } else {
                    mask = 0.0;
                }
            }
            index = 0;
            for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
                for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
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

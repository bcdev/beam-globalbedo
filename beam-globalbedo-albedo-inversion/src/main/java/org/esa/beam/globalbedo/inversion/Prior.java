package org.esa.beam.globalbedo.inversion;

import Jama.LUDecomposition;
import Jama.Matrix;
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
     * @param computeSnow
     * @return Prior
     */
    public static Prior createForInversion(Sample[] sourceSamples, double priorScaleFactor, boolean computeSnow) {

        Matrix C = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                              3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);              // 9x9
        Matrix inverseC = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                                     3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);       // 9x9
        Matrix inverseC_F = new Matrix(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);  // 9x1

        final int priorIndexNsamples = InversionOp.SRC_PRIOR_NSAMPLES;
        double nSamplesValue = sourceSamples[priorIndexNsamples].getDouble();
        double nSamples = AlbedoInversionUtils.isValid(nSamplesValue) ? nSamplesValue : 0.0;
        final int priorIndexSnowFraction = InversionOp.SRC_PRIOR_MASK;
        double priorSnowFraction = sourceSamples[priorIndexSnowFraction].getDouble();

        Matrix priorMean = new Matrix(
                AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);
        Matrix priorSD = new Matrix(
                AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS, 1);

        int index = 0;
        double mask = 1.0;
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                final int priorIndexMij = InversionOp.SRC_PRIOR_MEAN[i][j];
                final double m_ij_value = sourceSamples[priorIndexMij].getDouble();
                final double m_ij = AlbedoInversionUtils.isValid(m_ij_value) ? m_ij_value : 0.0;
                priorMean.set(index, 0, m_ij);
                final int priorIndexSDij = InversionOp.SRC_PRIOR_SD[i][j];
//                final double sd_ij_value = sourceSamples[priorIndexSDij].getDouble();
//                final double sd_ij = AlbedoInversionUtils.isValid(sd_ij_value) ? sd_ij_value : 0.0;
                final double cov_ij_value = sourceSamples[priorIndexSDij].getDouble();
                // Oct. 2015 Prior version:
                final double sd_ij = AlbedoInversionUtils.isValid(cov_ij_value) ? Math.sqrt(cov_ij_value) : 0.0;
                priorSD.set(index, 0, sd_ij);
                if (priorMean.get(index, 0) > 0.0 && priorSD.get(index, 0) == 0.0) {
//                    mask = 1.0;
//                    nSamples = 1.E-20;
                    priorSD.set(index, 0, 1.0);
                }
//                if (priorMean.get(index, 0) > 0.0 && sd_ij > 0.0 && mask > 0) {
//                    mask = 1.0;
//                    nSamples = 1.E-20;
//                    priorSD.set(index, 0, 1.0);
//                }
                index++;
            }
        }

        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS; i++) {
            // priorSD.set(i, 0, Math.min(1.0, priorSD.get(i, 0) * priorScaleFactor * 1.0)); // original
            // this will lead to higher weighting of the Prior:
            priorSD.set(i, 0, Math.min(1.0, priorSD.get(i, 0) * priorScaleFactor * 0.01));  // todo: make configurable!
            C.set(i, i, priorSD.get(i, 0) * priorSD.get(i, 0));
        }

        mask = 1.0;
        LUDecomposition lud = new LUDecomposition(C);
        // first check if pixel is regarded as valid
        boolean isValidPixel = validatePixel(computeSnow, priorSnowFraction, priorMean);
        if (isValidPixel) {
            // # Calculate C inverse
            // simplified condition (GL, 20110407)
            if (lud.isNonsingular()) {
                inverseC = C.inverse();
            } else {
                final Matrix cIdentity = Matrix.identity(3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                                                         3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);    // 9x9
                inverseC = cIdentity.inverse();
            }
            setInverseC_F(inverseC, inverseC_F, priorMean);
        } else {
            mask = 0.0;
        }

        return new Prior(inverseC, inverseC_F, mask, priorMean);
    }

    private static void setInverseC_F(Matrix inverseC, Matrix inverseC_F, Matrix priorMean) {
        int index;
        index = 0;
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                inverseC_F.set(index, 0, inverseC.get(index, index) * priorMean.get(index, 0));
                index++;
            }
        }
    }

    private static boolean validatePixel(boolean computeSnow, double priorSnowFraction, Matrix priorMean) {
        boolean processPixel = true;
        int index = 0;
        for (int i = 0; i < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = 0; j < AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                final boolean priorMeanNotOk = priorMean.get(index, 0) <= 0.0 || priorMean.get(index, 0) > 1.0;
                final boolean priorSnowFractionNotOk = (computeSnow && priorSnowFraction <= 0.03) ||
                        (!computeSnow && priorSnowFraction >= 0.93);
                if (priorMeanNotOk || priorSnowFractionNotOk) {
                    processPixel = false;
                    break;
                }
                index++;
            }
        }
        return processPixel;
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

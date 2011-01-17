/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.sdr.lutUtils.AerosolLookupTable;
import org.esa.beam.globalbedo.sdr.lutUtils.MomoLut;
import org.esa.beam.globalbedo.sdr.lutUtils.SdrDiffuseFrac;
import org.esa.beam.globalbedo.sdr.util.math.Function;
import org.esa.beam.globalbedo.sdr.util.math.MvFunction;
import org.esa.beam.globalbedo.sdr.util.math.Powell;
import org.esa.beam.util.Guardian;

/**
 *
 * @author akheckel
 */
public class BrentFitFunction implements UnivRetrievalFunction {

    public static final int ANGULAR_MODEL = 1;
    public static final int SPECTRAL_MODEL = 2;
    public static final int SYNERGY_MODEL = 3;


    private final int model;
    private final InputPixelData[] inPixField;
    private final AerosolLookupTable lut;
    private final float llimit;
    private final float penalty;
    private final double[] specWeights;
    private final double[] specSoil;
    private final double[] specVeg;

    public BrentFitFunction(int modelType, InputPixelData[] inPixField, MomoLut lut, double[] specWeights) {
        Guardian.assertEquals("modelType", modelType, ANGULAR_MODEL);
        this.model = modelType;
        this.inPixField = inPixField;
        this.lut = lut;
        this.llimit = 5e-6f;
        this.penalty = 1000f;
        this.specWeights = specWeights;
        this.specSoil = null;
        this.specVeg = null;
    }

    public BrentFitFunction(int modelType, InputPixelData[] inPixField, MomoLut lut, double[] specWeights, double[] specSoil, double[] specVeg) {
        this.model = modelType;
        this.inPixField = inPixField;
        this.lut = lut;
        this.llimit = 5e-6f;
        this.penalty = 1000f;
        this.specWeights = specWeights;
        this.specSoil = specSoil;
        this.specVeg = specVeg;
    }

    @Override
    public synchronized double f(double tau) {
        double fmin = 0;
        for (int i=0; i<inPixField.length; i++){
            fmin += fPix(tau, inPixField[i]);
        }
        return fmin;
    }

    @Override
    public float[] getModelReflec(float aot) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[] getSurfReflec(float aot) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double[] getpAtMin() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public synchronized double getMaxAOT() {
        if (lut instanceof MomoLut){
            int min = 0;
            for (int i=0; i<inPixField.length; i++)
                if (inPixField[i].getToaReflec()[0] < inPixField[min].getToaReflec()[0]) min=i;
            return ((MomoLut) lut).getMaxAOT(inPixField[min]);
        }
        return 2.0;
    }


    //private methods

    private double fPix(double tau, InputPixelData inPixData){
        lut.getSdrAndDiffuseFrac(inPixData, tau);
        double fmin = isSdrNegativ(inPixData.getSurfReflec());

        if ( !(fmin > 0) ) {
            double[] p = initStartVector(model);
            double xi[][] = initParameterBasis(p.length);
            double ftol = 2e-3;   // limit for optimization
            MvFunction surfModel = null;
            switch (model){
                case 1:
                    surfModel = new emodAng(inPixData.getDiffuseFrac(), inPixData.getSurfReflec(), specWeights);
                    break;
                case 2:
                    surfModel = new emodSpec(specSoil, specVeg, inPixData.getSurfReflec()[0], specWeights);
                    break;
                case 3:
                default: throw new OperatorException("invalid surface reflectance model");
            }

            Powell powell = new Powell(p, xi, ftol, surfModel);
            fmin = powell.getFmin();
        }
        else {
            fmin += 1e-8;
        }
        return fmin;
    }

    /**
     * inversion can lead to overcorrection of atmosphere
     * and thus to too small surface reflectances
     * this function defins a steep but smooth function
     * to guide the optimization
     *
     * @param sdr
     * @return
     */
    private double isSdrNegativ(double[][] sdr) {
        double fmin = 0;
        for (int iView = 0; iView < sdr.length; iView++) {
            for (int iWvl = 0; iWvl < sdr[0].length; iWvl++) {
                if (sdr[0][iWvl] < llimit) {
                    fmin += (sdr[iView][iWvl] - llimit) * (sdr[iView][iWvl] - llimit) * penalty;
                }
            }
        }
        return fmin;
    }

    /**
     * define initial vector p to start Powell optimization
     * according to the selected surface spectral model
     * @param model
     * @return p - start vector for Powell
     */
    private double[] initStartVector(int model) {
        double[] p = null;
        switch (model){
            case ANGULAR_MODEL:
                p = new double[]{0.1f, 0.1f, 0.1f, 0.1f, 0.5f, 0.3f};
                break;
            case SPECTRAL_MODEL:
                p = new double[]{0.9, 0.1};
                //p = new double[]{0.5, 0.5, 0.0};
                break;
            case SYNERGY_MODEL:
            default:
                throw new OperatorException("Surface Model not implemented");
        }
        return p;
    }


    /**
     * defining unit matrix as base of the parameter space
     * needed for Powell
     * @param length of parameter vector <b>p</b>
     * @return a unit matrix <b>xi</b> as orthonormal basis
     */
    private double[][] initParameterBasis(int length) {
            double xi[][] = new double[length][length];
            for (int i = 0; i < length; i++) xi[i][i] = 1.0;
            return xi;
    }

}

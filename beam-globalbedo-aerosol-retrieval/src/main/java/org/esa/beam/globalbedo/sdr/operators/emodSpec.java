/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import org.esa.beam.globalbedo.sdr.util.math.MvFunction;

/**
 * This class provides the spectrum model function to be minimised by Powell.
 * (see ATBD (4), (6))
 *
 *
 */
public class emodSpec implements MvFunction {

    private final double[] specSoil;
    private final double[] specVeg;
    private final double[] surfReflec;
    private final double[] specWeights;
    private final int nSpecChannels;

    public emodSpec(float[] specSoil, float[] specVeg, float[] surfReflec, double[] specWeights) {
        this.specSoil = float2Double(specSoil);
        this.specVeg = float2Double(specVeg);
        this.surfReflec = float2Double(surfReflec);
        this.specWeights = specWeights;
        this.nSpecChannels = surfReflec.length;
    }

    public emodSpec(float[] specSoil, float[] specVeg, double[] surfReflec, double[] specWeights) {
        this.specSoil = float2Double(specSoil);
        this.specVeg = float2Double(specVeg);
        this.surfReflec = surfReflec;
        this.specWeights = specWeights;
        this.nSpecChannels = surfReflec.length;
    }

    public emodSpec(double[] specSoil, double[] specVeg, double[] surfReflec, double[] specWeights) {
        this.specSoil = specSoil;
        this.specVeg = specVeg;
        this.surfReflec = surfReflec;
        this.specWeights = specWeights;
        this.nSpecChannels = surfReflec.length;
    }

    @Override
    public double f(double[] p) {
        double resid = 0.0;
        double[] mval = new double[nSpecChannels];
        for (int iwvl = 0; iwvl < nSpecChannels; iwvl++) {
            // mval: rho_spec_mod in ATBD (p. 22) (model function)
            //mval[iwvl] = p[0] * specVeg[iwvl] + p[1] * specSoil[iwvl] + p[2];
            mval[iwvl] = p[0] * specVeg[iwvl] + p[1] * specSoil[iwvl];
            // difference to measurement:
            double k = surfReflec[iwvl] - mval[iwvl];
            // residual:
            resid += specWeights[iwvl] * k * k;
        }

        // constraints for fit parameter p
        // specSoil and specVeg should not be scaled negative
        double limit;
        if (p[0] < 0.0) resid = resid + p[0] * p[0] * 1000;
        if (p[1] < 0.0) resid = resid + p[1] * p[1] * 1000;
        limit = -0.03;
        /*
        if (p[2] < limit) resid = resid + (p[2]-limit) * (p[2]-limit) * 1000;
        limit = 0.0;
        if (p[2] > limit) resid = resid + (p[2]-limit) * (p[2]-limit) * 1000;
         */

        return(resid);
    }

    @Override
    public void g(double[] x, double[] g) {
    }

    private double[] float2Double(float[] f){
        double[] d = new double[f.length];
        for (int i=0; i<f.length; i++) d[i] = (double) f[i];
        return d;
    }

}


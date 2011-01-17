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
public class emodAng implements MvFunction {

    private final double[][] diffuseFraction;
    private final double[][] surfReflec;
    private final double[] specWeights;
    private final int nSpecChannels;

    public emodAng(double[][] diffFrac, double[][] surfReflec, double[] specWeights) {
        this.diffuseFraction = diffFrac;
        this.surfReflec = surfReflec;
        this.specWeights = specWeights;
        this.nSpecChannels = surfReflec[0].length;
    }

    public emodAng(float[][] diffFrac, float[][] surfReflec, double[] specWeights) {
        this.surfReflec = new double[surfReflec.length][surfReflec[0].length];
        this.diffuseFraction = new double[diffFrac.length][diffFrac[0].length];
        for (int i=0; i<surfReflec.length; i++){
            for (int j=0; j<surfReflec[0].length; j++){
                this.surfReflec[i][j] = surfReflec[i][j];
                this.diffuseFraction[i][j] = diffFrac[i][j];
            }
        }
        this.specWeights = specWeights;
        this.nSpecChannels = surfReflec[0].length;
    }

    @Override
    public double f(double[] p) {
        double[][] mval = new double[2][nSpecChannels];
        double resid = getModelSpec(mval, p);

        // constraints for fit parameter p
        // Will Greys constraints from aatsr_aardvarc_4d
        if (p[0] < 0.01) resid=resid+(0.01-p[0])*(0.01-p[0])*1000.0;
        if (p[1] < 0.01) resid=resid+(0.01-p[1])*(0.01-p[1])*1000.0;
        if (p[4] < 0.2 ) resid=resid+(0.2 -p[4])*(0.2 -p[4])*1000.0;
        if (p[4] > 0.6 ) resid=resid+(0.6 -p[4])*(0.6 -p[4])*1000.0;
        if (p[5] < 0.2 ) resid=resid+(0.2 -p[5])*(0.2 -p[5])*1000.0;

        return(resid);
    }

    @Override
    public void g(double[] x, double[] g) {
    }

    public double getModelSpec(double[][] mval, double[] p){
        double resid = 0.0;
        //double DF = 1.0f;
        double DF = 0.3f;
        double gamma = 0.35f;
        double dir, g, dif, k;

        //p[4]=0.5;
        for (int iwvl = 0; iwvl < nSpecChannels; iwvl++){
            for (int iview = 0; iview < 2; iview++) {
                dir = (1.0 - DF * diffuseFraction[iview][iwvl]) * p[nSpecChannels+iview] * p[iwvl];
                g   = (1.0 - gamma) * p[iwvl];
                dif = (DF * diffuseFraction[iview][iwvl]
                        + g * (1.0 - DF * diffuseFraction[iview][iwvl])) * gamma * p[iwvl] / (1.0 - g);
                // mval: rho_spec_ang in ATBD (p. 23) (model function)
                mval[iview][iwvl] = (dir + dif);
                // difference to measurement:
                k   = surfReflec[iview][iwvl] - mval[iview][iwvl];
                // residual:
                resid = resid + specWeights[iwvl] * k * k;
            }
        }
        return resid;
    }

}


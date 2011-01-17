/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.lutUtils;

/**
 *
 * @author akheckel
 */
public class SdrDiffuseFrac {
    public double[][] surfReflec;
    public double[][] diffuseFrac;

    public SdrDiffuseFrac(double[][] surfReflec, double[][] diffuseFrac) {
        this.surfReflec = surfReflec;
        this.diffuseFrac = diffuseFrac;
    }

    public double[][] getDiffuseFrac() {
        return diffuseFrac;
    }

    public double[][] getSurfReflec() {
        return surfReflec;
    }
}

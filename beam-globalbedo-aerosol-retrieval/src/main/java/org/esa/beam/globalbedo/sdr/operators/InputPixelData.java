/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

/**
 *
 * @author akheckel
 */
public class InputPixelData {
    public final PixelGeometry geom;
    public final PixelGeometry geomFward;
    public final double surfPressure;
    public final double o3du;
    public final double wvCol;
    public final int nSpecWvl;
    public final float[] specWvl;
    public final double[] toaReflec;
    public final double[] toaReflecFward;
    public final double[][] surfReflec;
    public final double[][] diffuseFrac;

    public InputPixelData(PixelGeometry geom, PixelGeometry geomFward, float surfPressure, float o3du,
                          float wvCol, float[] specWvl, float[] toaReflec, float[] toaReflecFward) {
        this.geom = geom;
        this.geomFward = geomFward;
        this.surfPressure = surfPressure;
        this.o3du = o3du;
        this.wvCol = wvCol;
        this.specWvl = specWvl;
        this.nSpecWvl = specWvl.length;
        this.toaReflec = float2Double(toaReflec);
        this.toaReflecFward = float2Double(toaReflecFward);
        this.surfReflec = new double[2][nSpecWvl];
        this.diffuseFrac = new double[2][nSpecWvl];
    }

    public InputPixelData(PixelGeometry geom, PixelGeometry geomFward, double surfPressure, double o3du,
                          double wvCol, float[] specWvl, double[] toaReflec, double[] toaReflecFward) {
        this.geom = geom;
        this.geomFward = geomFward;
        this.surfPressure = surfPressure;
        this.o3du = o3du;
        this.wvCol = wvCol;
        this.specWvl = specWvl;
        this.nSpecWvl = specWvl.length;
        this.toaReflec = toaReflec;
        this.toaReflecFward = toaReflecFward;
        this.surfReflec = new double[2][nSpecWvl];
        this.diffuseFrac = new double[2][nSpecWvl];
    }

    public synchronized double[][] getDiffuseFrac() {
        return diffuseFrac;
    }

    public synchronized PixelGeometry getGeom() {
        return geom;
    }

    public synchronized PixelGeometry getGeomFward() {
        return geomFward;
    }

    public synchronized int getnSpecWvl() {
        return nSpecWvl;
    }

    public synchronized double getO3du() {
        return o3du;
    }

    public synchronized float[] getSpecWvl() {
        return specWvl;
    }

    public synchronized double getSurfPressure() {
        return surfPressure;
    }

    public synchronized double[][] getSurfReflec() {
        return surfReflec;
    }

    public synchronized double[] getToaReflec() {
        return toaReflec;
    }

    public synchronized double[] getToaReflecFward() {
        return toaReflecFward;
    }

    public synchronized double getWvCol() {
        return wvCol;
    }

    private double[] float2Double(float[] f) {
        double[] d = new double[f.length];
        for (int i=0; i<f.length; i++) d[i] = (double) f[i];
        return d;
    }

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

/**
 *
 * @author akheckel
 */
public class PixelGeometry {
    public final float sza;
    public final float vza;
    public final float razi;

    public PixelGeometry(float sza, float saa, float vza, float vaa) {
        this.sza = sza;
        this.vza = vza;
        this.razi = getRelativeAzi(saa, vaa);
    }

    public PixelGeometry(double sza, double saa, double vza, double vaa) {
        this.sza = (float) sza;
        this.vza = (float) vza;
        this.razi = getRelativeAzi((float)saa, (float)vaa);
    }

    private float getRelativeAzi(float saa, float vaa) {
        float relAzi = Math.abs(saa - vaa);
        relAzi = (relAzi > 180.0f) ? 180 - (360 - relAzi) : 180 - relAzi;
        return relAzi;
    }

}

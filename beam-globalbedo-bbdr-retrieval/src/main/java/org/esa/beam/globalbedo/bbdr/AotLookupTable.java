package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.util.math.LookupTable;

/**
 * wrapper object to hold an AOT lookup table as in IDL breadboard
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AotLookupTable {
    private LookupTable lut;
    private float[] wvl;
    private float[] solarIrradiance;

    public LookupTable getLut() {
        return lut;
    }

    public void setLut(LookupTable lut) {
        this.lut = lut;
    }

    public float[] getWvl() {
        return wvl;
    }

    public void setWvl(float[] wvl) {
        this.wvl = wvl;
    }

    public float[] getSolarIrradiance() {
        return solarIrradiance;
    }

    public void setSolarIrradiance(float[] solarIrradiance) {
        this.solarIrradiance = solarIrradiance;
    }
}

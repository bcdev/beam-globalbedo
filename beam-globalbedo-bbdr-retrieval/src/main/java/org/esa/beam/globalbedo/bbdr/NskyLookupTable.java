package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.util.math.LookupTable;

/**
 * wrapper object to hold an Nsky lookup table as in IDL breadboard
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class NskyLookupTable {
    private LookupTable lut;
    private double kppVol;
    private double kppGeo;

    public LookupTable getLut() {
        return lut;
    }

    public void setLut(LookupTable lut) {
        this.lut = lut;
    }

    public double getKppVol() {
        return kppVol;
    }

    public void setKppVol(double kppVol) {
        this.kppVol = kppVol;
    }

    public double getKppGeo() {
        return kppGeo;
    }

    public void setKppGeo(double kppGeo) {
        this.kppGeo = kppGeo;
    }
}

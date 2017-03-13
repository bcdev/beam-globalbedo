package org.esa.beam.globalbedo.bbdr;

/**
 * Enumeration for the two Meteosat instruments
 *
 * @author olafd
 */
public enum MeteosatSensor {
    MVIRI("MVIRI"),
    SEVIRI("MVIRI"),
    GMS("GMS"),
    GOES("GOES");

    private final String name;

    MeteosatSensor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

package org.esa.beam.globalbedo.inversion;

/**
 * The processing mode: either LEO (MERIS+VGT as GlobAlbedo) or AVHRRGEO (new in QA4ECV)
 *
 * @author olafd
 */
public enum ProcessingMode {
    AVHRRGEO("AVHRRGEO"),
    LEO("LEO");

    private final String name;

    ProcessingMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}

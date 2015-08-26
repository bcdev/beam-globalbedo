/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.globalbedo.bbdr;

import static org.esa.beam.globalbedo.bbdr.BbdrConstants.*;

/**
 * Encapsulates the differences between the 3 sensors and the 2 different views for AATSR
 */
public enum Sensor {

    MERIS("MERIS", 15, 0.02, 6, 12, 1.0, 0.999, 0.04, 0.05, MERIS_CALIBRATION_COEFFS, MERIS_WAVELENGHTS, 1.0),
    AATSR("AATSR", 4, 0.05, 1, 2, 1.008, 0.997, 0.04, 0.15, AATSR_CALIBRATION_COEFFS, AATSR_WAVELENGHTS, 1.2),
    AATSR_FWARD("AATSR", 4, 0.05, 1, 2, 1.008, 0.997, 0.04, 0.15, AATSR_CALIBRATION_COEFFS, AATSR_WAVELENGHTS, 1.4),
    VGT("VGT", 4, 0.05, 1, 2, 1.096, 1.089, 0.04, 0.05, VGT_CALIBRATION_COEFFS, VGT_WAVELENGHTS, 1.1),
    AVHRR("AVHRR", 2, 0.0, 0, 1, 1.0, 1.0, 0.0, 0.0, AVHRR_CALIBRATION_COEFFS, AVHRR_WAVELENGHTS, 1.0);

    private final String instrument;
    private final int numBands;
    private final double radiometricError;
    private final int indexRed;
    private final int indexNIR;
    private final double aNDVI;
    private final double bNDVI;
    private final double cwvError;
    private final double ozoError;
    private final float[] cal2Meris;
    private final float[] wavelength;
    private final double errCoregScale;

    private Sensor(String instrument, int numBands, double radiometricError, int indexRed, int indexNIR, double aNDVI,
                   double bNDVI, double cwvError, double ozoError, float[] cal2Meris, float[] wavelength, double errCoregScale) {
        this.instrument = instrument;
        this.numBands = numBands;
        this.radiometricError = radiometricError;
        this.indexRed = indexRed;
        this.indexNIR = indexNIR;
        this.aNDVI = aNDVI;
        this.bNDVI = bNDVI;
        this.cwvError = cwvError;
        this.ozoError = ozoError;
        this.cal2Meris = cal2Meris;
        this.wavelength = wavelength;
        this.errCoregScale = errCoregScale;
    }

    public String getInstrument() {
        return instrument;
    }

    public int getNumBands() {
        return numBands;
    }

    /**
     * a priori radiometric error (%)
     */
    public double getRadiometricError() {
        return radiometricError;
    }

    public int getIndexRed() {
        return indexRed;
    }

    public int getIndexNIR() {
        return indexNIR;
    }

    public double getAndvi() {
        return aNDVI;
    }

    public double getBndvi() {
        return bNDVI;
    }

    public double getCwvError() {
        return cwvError;
    }

    public double getOzoError() {
        return ozoError;
    }

    public float[] getWavelength() {
        return wavelength;
    }

    public float[] getCal2Meris() {
        return cal2Meris;
    }

    public double getErrCoregScale() {
        return errCoregScale;
    }
}

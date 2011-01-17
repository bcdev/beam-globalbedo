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

import org.esa.beam.gpf.operators.meris.MerisBasisOp;

/**
 * Encapsulates the differences between the 3 sensors
 */
enum Sensor {

    MERIS(15, 0.02, 6, 12, 1.0, 0.999, 2, 0.04, 0.05),
    AATSR(4, 0.05, 1, 2, 1.008, 0.997, 2, 0.04, 0.15),
    SPOT(4, 0.05, 1, 2, 1.096, 1.089, 1, 0.04, 0.05);

    private final int numBands;
    private final double radiometricError;
    private final int indexRed;
    private final int indexNIR;
    private final double aNDVI;
    private final double bNDVI;
    private final int cwv_ozo_flag;
    private final double cwvError;
    private final double ozoError;

    private Sensor(int numBands, double radiometricError, int indexRed, int indexNIR, double aNDVI, double bNDVI, int cwv_ozo_flag, double cwvError, double ozoError) {
        this.numBands = numBands;
        this.radiometricError = radiometricError;
        this.indexRed = indexRed;
        this.indexNIR = indexNIR;
        this.aNDVI = aNDVI;
        this.bNDVI = bNDVI;
        this.cwv_ozo_flag = cwv_ozo_flag;
        this.cwvError = cwvError;
        this.ozoError = ozoError;
    }

    int getNumBands() {
        return numBands;
    }

    /**
     * a priori radiometric error (%)
     */
    public double getRadiometricError() {
        return radiometricError;
    }

    int getIndexRed() {
        return indexRed;
    }

    int getIndexNIR() {
        return indexNIR;
    }

    public double getAndvi() {
        return aNDVI;
    }

    public double getBndvi() {
        return bNDVI;
    }

    public int getCwv_ozo_flag() {
        return cwv_ozo_flag;
    }

    public double getCwvError() {
        return cwvError;
    }

    public double getOzoError() {
        return ozoError;
    }
}

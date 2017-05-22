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
 * Encapsulates the differences between the different sensors
 */
public enum Sensor {

    // todo: define all numbers as constants
    MERIS("MERIS", 15, 0.02, 6, 12, 1.0, 0.999, 0.04, 0.05, MERIS_CALIBRATION_COEFFS, MERIS_WAVELENGHTS, 1.0,
          MERIS_TOA_BAND_NAMES, MERIS_ANCILLARY_BAND_NAMES, MERIS_SDR_BAND_NAMES, MERIS_SDR_ERROR_BAND_NAMES,
          LAND_EXPR_MERIS, MERIS_SPECTRAL_MAPPING_COEFFS),
    VGT("VGT", 4, 0.05, 1, 2, 1.096, 1.089, 0.04, 0.05, VGT_CALIBRATION_COEFFS, VGT_WAVELENGHTS, 1.1,
        VGT_TOA_BAND_NAMES, VGT_ANCILLARY_BAND_NAMES, VGT_SDR_BAND_NAMES, VGT_SDR_ERROR_BAND_NAMES,
        LAND_EXPR_VGT, VGT_SPECTRAL_MAPPING_COEFFS),
    PROBAV("PROBAV", 4, 0.05, 1, 2, 1.096, 1.089, 0.04, 0.05, PROBAV_CALIBRATION_COEFFS, PROBAV_WAVELENGHTS, 1.1,
           PROBAV_TOA_BAND_NAMES, PROBAV_ANCILLARY_BAND_NAMES, PROBAV_SDR_BAND_NAMES, PROBAV_SDR_ERROR_BAND_NAMES,
           LAND_EXPR_PROBAV, null);

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
    private final String[] toaBandNames;
    private final String[] ancillaryBandNames;
    private final String[] sdrBandNames;
    private final String[] sdrErrorBandNames;
    private final String landExpr;
    private final double[][] spectralMappingCoeffs;


    Sensor(String instrument, int numBands, double radiometricError, int indexRed, int indexNIR, double aNDVI,
                   double bNDVI, double cwvError, double ozoError, float[] cal2Meris, float[] wavelength, double errCoregScale,
           String[] toaBandNames, String[] ancillaryBandNames, String[] sdrBandBandNames, String[] sdrErrorBandNames,
           String landExpr, double[][] spectralMappingCoeffs) {
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
        this.toaBandNames = toaBandNames;
        this.ancillaryBandNames = ancillaryBandNames;
        this.sdrBandNames = sdrBandBandNames;
        this.sdrErrorBandNames = sdrErrorBandNames;
        this.landExpr = landExpr;
        this.spectralMappingCoeffs = spectralMappingCoeffs;
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

    public String[] getToaBandNames() {
        return toaBandNames;
    }

    public String[] getAncillaryBandNames() {
        return ancillaryBandNames;
    }

    public String[] getSdrBandNames() {
        return sdrBandNames;
    }

    public String[] getSdrErrorBandNames() {
        return sdrErrorBandNames;
    }

    public String getLandExpr() {
        return landExpr;
    }

    public double[][] getSpectralMappingCoeffs() {
        return spectralMappingCoeffs;
    }
}

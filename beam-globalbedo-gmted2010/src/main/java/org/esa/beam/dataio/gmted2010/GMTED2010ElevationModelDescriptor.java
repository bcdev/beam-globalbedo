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
package org.esa.beam.dataio.gmted2010;

import org.esa.beam.framework.dataop.dem.AbstractElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.resamp.Resampling;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class GMTED2010ElevationModelDescriptor extends AbstractElevationModelDescriptor {

    public static final String NAME = "GMTED2010_30";
    public static final int NUM_X_TILES = 12;
    public static final int NUM_Y_TILES = 9;
    public static final int DEGREE_RES_X = 30;
    public static final int DEGREE_RES_Y = 20;
    public static final int TILE_WIDTH = 3600;
    public static final int TILE_HEIGHT = 2400;
    public static final int NO_DATA_VALUE = -32768;
    public static final int RASTER_WIDTH = NUM_X_TILES * TILE_WIDTH;
    public static final int RASTER_HEIGHT = NUM_Y_TILES * TILE_HEIGHT;
    public static final Datum DATUM = Datum.WGS_84;
    private static final String GMTED_MEA300_TIF = "20101117_gmted_mea300.tif";

    public GMTED2010ElevationModelDescriptor() {
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Datum getDatum() {
        return DATUM;
    }

    @Override
    public float getNoDataValue() {
        return NO_DATA_VALUE;
    }

    @Override
    public boolean isDemInstalled() {
        final File file = getTileFile(0, 0);   // todo (nf) - check all tiles
        return file.canRead();
    }

    @Override
    public URL getDemArchiveUrl() {
        return null; // no online installation
    }

//    @Override
//    @Deprecated
//    public ElevationModel createDem() {
//        return createDem(Resampling.BILINEAR_INTERPOLATION);
//    }

    @Override
    public ElevationModel createDem(Resampling resampling) {
        try {
            return new GMTED2010ElevationModel(this, resampling);
        } catch (IOException e) {
            return null;
        }
    }

    public File getTileFile(int lonIndex, int latIndex) {
        int minLat = getMinLat(latIndex);
        int minLon = getMinLon(lonIndex);
        String tileFilename = createTileFilename(minLat, minLon);
        return new File(getDemInstallDir(), tileFilename);
    }

    static int getMinLat(int latIndex) {
        return (NUM_Y_TILES - 1 - latIndex) * DEGREE_RES_Y - 90;
    }

    static int getMinLon(int lonIndex) {
        return lonIndex * DEGREE_RES_X - 180;
    }

    static String createTileFilename(int minLat, int minLon) {
        int lat = Math.abs(minLat);
        int lon = Math.abs(minLon);
        String latChar = minLat < 0 ?  "S" : "N";
        String lonChar = minLon < 0 ?  "W" : "E";
        return String.format("%s%03d/%d%s%03d%s_" + GMTED_MEA300_TIF, lonChar, lon, lat, latChar, lon, lonChar);
    }

}

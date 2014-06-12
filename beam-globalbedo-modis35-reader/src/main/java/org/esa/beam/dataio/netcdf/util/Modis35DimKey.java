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

package org.esa.beam.dataio.netcdf.util;

import com.bc.ceres.core.Assert;
import ucar.nc2.Dimension;

import java.util.List;

/**
 * Wraps a NetCDF dimension array so that it can be used as key.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 */
public class Modis35DimKey {

    private static final String[] TYPICAL_X_DIM_NAMES = new String[]{"lon", "long", "longitude",
            "ni", "NX", "SX", "x", "xc", "XDim",
            "Cell_Across_Swath_5km",       // currently, these are the only differences to standard DimKey
            "Cell_Across_Swath_1km",
            "across_track", "numRows"};
    private static final String[] TYPICAL_Y_DIM_NAMES = new String[]{"lat", "lat", "latitude",
            "nj", "NY", "SY", "y", "yc", "YDim",
            "Cell_Along_Swath_5km",
            "Cell_Along_Swath_1km",
            "along_track", "numCells"};

    private final Dimension[] dims;
    private final int xDimIndex;
    private final int yDimIndex;

    public Modis35DimKey(Dimension... dims) {
        Assert.argument(dims.length >= 1, "dims.length >= 1");
        for (Dimension dim : dims) {
            Assert.notNull(dim, "dim");
        }
        this.dims = dims;
        xDimIndex = findXDimensionIndex();
        yDimIndex = findYDimensionIndex();
    }

    public int findXDimensionIndex() {
        for (int i = 0; i < dims.length; i++) {
            final String dimName = dims[i].getShortName();
            if (dimName != null) {
                for (String typicalXDimName : TYPICAL_X_DIM_NAMES) {
                    if (dimName.equalsIgnoreCase(typicalXDimName)) {
                        return i;
                    }
                }
            }
        }
        // fallback rank-1
        return getRank() - 1;
    }

    public int findYDimensionIndex() {
        for (int i = 0; i < dims.length; i++) {
            final String dimName = dims[i].getShortName();
            if (dimName != null) {
                for (String typicalYDimName : TYPICAL_Y_DIM_NAMES) {
                    if (dims[i].getShortName().equalsIgnoreCase(typicalYDimName)) {
                        return i;
                    }
                }
            }
        }
        // fallback rank-2
        return getRank() - 2;
    }

    public static int findStartIndexOfBandVariables(List<Dimension> dimensions) {
        final Modis35DimKey rasterDim = new Modis35DimKey(dimensions.toArray(new Dimension[dimensions.size()]));
        final int xIndex = rasterDim.findXDimensionIndex();
        final int yIndex = rasterDim.findYDimensionIndex();
        if (xIndex == 0 || yIndex == 0) {
            // return 2 if lat/lon bands are first two variables
            return 2;
        } else if (xIndex == dimensions.size() - 1 || yIndex == dimensions.size() - 1) {
            // return 0 if lat/lon bands are last two variables or if no lat/lon bands are found
            return 0;
        } else {
            // todo: find something clever if lat/lon bands are any two variables (e.g. bands data1, data2, lat, data3, lon, data4)
            return -1;
        }
    }

    public int getRank() {
        return dims.length;
    }

    public Dimension getDimensionX() {
        return getDimension(xDimIndex);
    }

    public Dimension getDimensionY() {
        return getDimension(yDimIndex);
    }

    public Dimension getDimension(int index) {
        return dims[index];
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Modis35DimKey dimKey = (Modis35DimKey) o;
        return getDimensionY().getLength() == dimKey.getDimensionY().getLength()
                && getDimensionX().getLength() == dimKey.getDimensionX().getLength();
    }

    @Override
    public int hashCode() {
        return 31 * getDimensionY().getLength() + getDimensionX().getLength();
    }

}

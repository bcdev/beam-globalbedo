/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.dataio.netcdf.metadata.profiles.cf;

import org.esa.beam.dataio.netcdf.Modis35ProfileReadContext;
import org.esa.beam.dataio.netcdf.Modis35ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.Modis35ProfilePartIO;
import org.esa.beam.dataio.netcdf.metadata.profiles.hdfeos.HdfEosGeocodingPart;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import org.esa.beam.dataio.netcdf.util.Modis35Constants;
import org.esa.beam.dataio.netcdf.util.Modis35DimKey;
import org.esa.beam.dataio.netcdf.util.ReaderUtils;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoCodingFactory;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;

import java.awt.Dimension;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class Modis35CfGeocodingPart extends Modis35ProfilePartIO {

    private boolean geographicCRS;

    @Override
    public void decode(Modis35ProfileReadContext ctx, Product p) throws IOException {
        // nothing to do here - for MODIS35 we build the geocoding manually in the reader implementation
    }

    @Override
    public void preEncode(Modis35ProfileWriteContext ctx, Product product) throws IOException {
        // not needed here
    }

    private boolean isLatLonPresent(NFileWriteable ncFile) {
        return ncFile.findVariable("lat") != null && ncFile.findVariable("lon") != null;
    }

    @Override
    public void encode(Modis35ProfileWriteContext ctx, Product product) throws IOException {
        // not needed here
    }

    static boolean isGeographicCRS(final GeoCoding geoCoding) {
        return (geoCoding instanceof CrsGeoCoding || geoCoding instanceof MapGeoCoding) &&
               CRS.equalsIgnoreMetadata(geoCoding.getMapCRS(), DefaultGeographicCRS.WGS84);
    }



    static boolean isGlobalShifted180(Array lonData) {
        // Idea: lonData values shall closely match [0,360] interval:
        // - first value of lonData shall be larger or equal 0.0, and less or equal 0.0 + lonDataInterval
        // - last value of lonData shall be smaller or equal 360.0, and larger or equal 360.0 - lonDataInterval
        // e.g. lonDataInterval=0.75, lonData={0.0, 0.75, 1.5,..,359.25} --> true
        // e.g. lonDataInterval=0.75, lonData={0.75, 1.5, 2.25,..,360.0} --> true
        // e.g. lonDataInterval=0.75, lonData={1.0, 1.75, 2.5,..,360.25} --> false
        final Index i0 = lonData.getIndex().set(0);
        final Index i1 = lonData.getIndex().set(1);
        final Index iN = lonData.getIndex().set((int) lonData.getSize() - 1);
        double lonDelta = (lonData.getDouble(i1) - lonData.getDouble(i0));

        final double firstValue = lonData.getDouble(0);
        final double lastValue = lonData.getDouble(iN);
        return (firstValue >= 0.0 && firstValue <= lonDelta &&
                lastValue >= 360.0 - lonDelta && lastValue <= 360.0);
    }

}

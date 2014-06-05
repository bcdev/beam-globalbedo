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

package org.esa.beam.dataio.netcdf;

import org.esa.beam.dataio.netcdf.util.Modis35RasterDigest;
import ucar.nc2.NetcdfFile;

/**
 * A context for reading metadata from netCDF into the BEAM product model.
 * While reading a product this context can be used to store properties to
 * share them between multiple {@link org.esa.beam.dataio.netcdf.metadata.Modis35ProfilePartReader ProfilePartReader}.
 */
public interface Modis35ProfileReadContext extends Modis35PropertyStore {

    /**
     * Gets the {@link NetcdfFile} to be read.
     *
     * @return the {@link NetcdfFile}
     */
    public NetcdfFile getNetcdfFile();

    /**
     * Sets the {@link org.esa.beam.dataio.netcdf.util.Modis35RasterDigest}.
     *
     * @param rasterDigest the {@link org.esa.beam.dataio.netcdf.util.Modis35RasterDigest}
     *
     * @see AbstractNetCdfReaderPlugIn#initReadContext(Modis35ProfileReadContext)
     */
    public void setRasterDigest(Modis35RasterDigest rasterDigest);

    /**
     * Gets the {@link org.esa.beam.dataio.netcdf.util.Modis35RasterDigest}.
     *
     * @return the {@link org.esa.beam.dataio.netcdf.util.Modis35RasterDigest}
     */
    public Modis35RasterDigest getRasterDigest();


}

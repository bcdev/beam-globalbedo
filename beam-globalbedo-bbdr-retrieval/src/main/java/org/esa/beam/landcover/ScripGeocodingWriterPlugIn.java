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

package org.esa.beam.landcover;

import org.esa.beam.dataio.netcdf.util.Constants;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

public class ScripGeocodingWriterPlugIn implements ProductWriterPlugIn {

    @Override
    public String[] getFormatNames() {
        return new String[] { "SCRIP" };
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[] { Constants.FILE_EXTENSION_NC };
    }

    @Override
    public String getDescription(Locale locale) {
        return "NetCDF following SCRIP convention";
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    @Override
    public Class[] getOutputTypes() {
        return new Class[] { String.class, File.class };
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new ScripGeocodingWriter(this);
    }
}

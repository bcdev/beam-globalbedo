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

package org.esa.beam.globalbedo.mosaic;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * A product reader plugin for the internal mosaic products of GlobAlbedo.
 *
 * @author  MarcoZ
 */
public class GlobAlbedoQa4ecvMosaicReaderPlugIn implements ProductReaderPlugIn {


    private static final Class[] INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String[] FILE_EXTENSIONS = new String[]{".dim", ".bin", ".hdr"};
    private static final String FORMAT_NAME = "GLOBALBEDO-L3-MOSAIC-QA4ECV";
    private static final String DESCRIPTION = "GlobAlbedo Sinusoidal Mosaic";

    @Override
    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return FILE_EXTENSIONS;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    /**
     * This reader doe not qualify for any product automatically. It has to be chosen explicitly.
     */
    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        return DecodeQualification.UNABLE;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new GlobAlbedoQa4ecvMosaicProductReader(this);
    }

}
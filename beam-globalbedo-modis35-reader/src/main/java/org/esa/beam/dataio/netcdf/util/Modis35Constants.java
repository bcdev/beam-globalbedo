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

package org.esa.beam.dataio.netcdf.util;

/**
 * Provides most of the constants used in this package.
 */
public interface Modis35Constants {

    public static final int MOD35_SCALE_FACTOR = 5;

    public static final String LOW_RES_RASTER_DIM_NAMES = "Cell_Along_Swath_5km,Cell_Across_Swath_5km";
    public static final String HIGH_RES_CLOUDMASK_RASTER_DIM_NAMES = "Byte_Segment,Cell_Along_Swath_1km,Cell_Across_Swath_1km";
    public static final String HIGH_RES_QA_RASTER_DIM_NAMES = "Cell_Along_Swath_1km,Cell_Across_Swath_1km,QA_Dimension";

    public static final String[] CLOUD_MASK_BAND_NAMES = new String[]{
            "Cloud_Mask_Byte_Segment1",
            "Cloud_Mask_Byte_Segment2",
            "Cloud_Mask_Byte_Segment3",
            "Cloud_Mask_Byte_Segment4",
            "Cloud_Mask_Byte_Segment5",
            "Cloud_Mask_Byte_Segment6"
    };

    public static final String[] QUALITY_ASSURANCE_BAND_NAMES = new String[]{
            "Quality_Assurance_QA_Dimension1",
            "Quality_Assurance_QA_Dimension2",
            "Quality_Assurance_QA_Dimension3",
            "Quality_Assurance_QA_Dimension4",
            "Quality_Assurance_QA_Dimension5",
            "Quality_Assurance_QA_Dimension6",
            "Quality_Assurance_QA_Dimension7",
            "Quality_Assurance_QA_Dimension8",
            "Quality_Assurance_QA_Dimension9",
            "Quality_Assurance_QA_Dimension10"
    };

    String FORMAT_NAME = "NetCDF";
    String FORMAT_DESCRIPTION = "NetCDF/CF Data Product";

    String SCALE_FACTOR_ATT_NAME = "scale_factor";
    String SLOPE_ATT_NAME = "slope";
    String ADD_OFFSET_ATT_NAME = "add_offset";
    String INTERCEPT_ATT_NAME = "intercept";
    String FILL_VALUE_ATT_NAME = "_FillValue";
    String MISSING_VALUE_ATT_NAME = "missing_value";
    String START_DATE_ATT_NAME = "start_date";
    String START_TIME_ATT_NAME = "start_time";
    String STOP_DATE_ATT_NAME = "stop_date";
    String STOP_TIME_ATT_NAME = "stop_time";

    // context properties
    String Y_FLIPPED_PROPERTY_NAME = "yFlipped";
    String CONVERT_LOGSCALED_BANDS_PROPERTY = "convertLogScaledBands";
    String PRODUCT_FILENAME_PROPERTY = "productName";

}

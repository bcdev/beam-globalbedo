package org.esa.beam.globalbedo.bbdr;


/**
 * Constants defined for BBDR retrieval
 *
 * @author Olaf Danne
 */
public class BbdrConstants {

    public final static float[] MERIS_WAVELENGHTS = {
        412.0f, 442.0f, 490.0f, 510.0f, 560.0f,
        620.0f, 665.0f, 681.0f, 708.0f, 753.0f,
        760.0f, 778.0f, 865.0f, 885.0f, 900.0f
    };

    public final static float[] MERIS_CALIBRATION_COEFFS = {
        1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f
    };

    public final static float[] AATSR_WAVELENGHTS = {
         550.0f, 670.0f, 870.0f, 1600.0f
    };

    public final static float[] AATSR_CALIBRATION_COEFFS = {
        1.0253f, 1.0093f, 1.0265f, 1.0f
    };

    public final static float[] VGT_WAVELENGHTS = {
         450.0f, 645.0f, 835.0f, 1665.0f
    };

    public final static float[] VGT_CALIBRATION_COEFFS = {
        1.012f, 0.953f, 0.971f, 1.0f
    };

    public final static float[] AVHRR_WAVELENGHTS = {
            645.0f, 830.0f
    };

    public final static float[] AVHRR_CALIBRATION_COEFFS = {
            1.0f, 1.0f
    };


    // MERIS has ozone, but no water vapour image
    // AATSR has neither ozone nor water vapour image
    // VGT has ozone and water vapour image
    // --> use constants if images are not available
    public static float CWV_CONSTANT_VALUE = 1.5f; // used for MERIS and AATSR only
    public static float OZO_CONSTANT_VALUE = 0.32f; // used for AATSR only

    public final static String MERIS_VZA_TP_NAME = "view_zenith";
    public final static String MERIS_VAA_TP_NAME = "view_azimuth";
    public final static String MERIS_SZA_TP_NAME = "sun_zenith";
    public final static String MERIS_SAA_TP_NAME = "sun_azimuth";
    public final static String MERIS_OZO_TP_NAME = "ozone";
    public final static String MERIS_DEM_BAND_NAME = "elevation";
    public final static String MERIS_AOT_BAND_NAME = "aot";
    public final static String MERIS_AOTERR_BAND_NAME = "aot_err";

    public final static String[] MERIS_TOA_BAND_NAMES = new String[]{
            "reflectance_1", "reflectance_2", "reflectance_3", "reflectance_4", "reflectance_5",
            "reflectance_6", "reflectance_7", "reflectance_8", "reflectance_9", "reflectance_10",
            "reflectance_11", "reflectance_12", "reflectance_13", "reflectance_14", "reflectance_15"
    };

    public final static String[] MERIS_TIE_POINT_GRID_NAMES = new String[]{     // would be useful in EnvisatConstants...
            "latitude", "longitude", "dem_alt", "dem_rough", "lat_corr", "lon_corr",
            "sun_zenith", "sun_azimuth", "view_zenith", "view_azimuth", "zonal_wind", "merid_wind",
            "atm_press", "ozone", "rel_hum"
    };

//    public final static String[] AATSR_TIE_POINT_GRID_NAMES = new String[]{     // would be useful in EnvisatConstants...
//            "latitude", "longitude", "altitude",
//            "lat_corr_nadir", "lon_corr_nadir", "sun_elev_nadir", "view_elev_nadir", "sun_azimuth_nadir", "view_azimuth_nadir",
//            "lat_corr_fward", "lon_corr_fward", "sun_elev_fward", "view_elev_fward", "sun_azimuth_fward", "view_azimuth_fward"
//    };


    public final static String[] AATSR_TOA_BAND_NAMES_NADIR = new String[]{
                    "reflec_nadir_0550", "reflec_nadir_0670", "reflec_nadir_0870", "reflec_nadir_1600"};

    public final static String[] AATSR_TOA_BAND_NAMES_FWARD = new String[]{
                    "reflec_fward_0550", "reflec_fward_0670", "reflec_fward_0870", "reflec_fward_1600"};

    public final static String VGT_OZO_BAND_NAME = "OG";

    public final static String[] VGT_TOA_BAND_NAMES = new String[]{
                    "B0", "B2", "B3", "MIR"};

    public static final int MODIS_TILE_WIDTH = 1200;
    public static final int MODIS_TILE_HEIGHT = 1200;

    public static final int SEAICE_PST_QUADRANT_PRODUCT_WIDTH = 2250;
    public static final int SEAICE_PST_QUADRANT_PRODUCT_HEIGHT = 2250;
    public static final double SEAICE_PST_PIXEL_SIZE_X = 1000.0;
    public static final double SEAICE_PST_PIXEL_SIZE_Y = 1000.0;

    public static final float NO_DATA_VALUE = -999.0f;
}


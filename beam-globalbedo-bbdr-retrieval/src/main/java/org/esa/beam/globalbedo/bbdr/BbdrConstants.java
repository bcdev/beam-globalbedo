package org.esa.beam.globalbedo.bbdr;


/**
 * Constants defined for BBDR retrieval
 *
 * @author Olaf Danne
 */
public class BbdrConstants {

    public static final int N_SPC = 3; // VIS, NIR, SW ; Broadband albedos

    public final static float[] MERIS_WAVELENGHTS = {
            412.0f, 442.0f, 490.0f, 510.0f, 560.0f,
            620.0f, 665.0f, 681.0f, 708.0f, 753.0f,
            760.0f, 778.0f, 865.0f, 885.0f, 900.0f
    };

    public final static float[] MERIS_CALIBRATION_COEFFS = {
            1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f
    };

    public final static float[] VGT_WAVELENGHTS = {
            450.0f, 645.0f, 835.0f, 1665.0f
    };

    public final static float[] VGT_CALIBRATION_COEFFS = {
            1.012f, 0.953f, 0.971f, 1.0f
    };

    public final static float[] PROBAV_WAVELENGHTS = {
            462.0f, 655.5f, 843.0f, 1599.0f
    };

    public final static float[] PROBAV_CALIBRATION_COEFFS = {
            1.012f, 0.953f, 0.971f, 1.0f
    };

    public final static double[] PROBAV_TO_VGT_FACTORS = {
            0.9971, 0.9983, 1.0005, 1.0214
    };

    public final static double[] PROBAV_TO_VGT_OFFSETS = {
            0.0034, 0.0029, 0.0024, 0.0062
    };

    // later:
//    public final static float[] AVHRR_WAVELENGHTS = {
//            645.0f, 830.0f
//    };
//
//    public final static float[] AVHRR_CALIBRATION_COEFFS = {
//            1.0f, 1.0f
//    };


    // MERIS has ozone, but no water vapour image
    // AATSR has neither ozone nor water vapour image
    // VGT has ozone and water vapour image
    // --> use constants if images are not available
    public static float CWV_CONSTANT_VALUE = 1.5f; // used for MERIS
    public static float OZO_CONSTANT_VALUE = 0.32f; // used for AATSR only

    public final static String MERIS_VZA_TP_NAME = "view_zenith";
    public final static String MERIS_VAA_TP_NAME = "view_azimuth";
    public final static String MERIS_SZA_TP_NAME = "sun_zenith";
    public final static String MERIS_SAA_TP_NAME = "sun_azimuth";
    public final static String MERIS_OZO_TP_NAME = "ozone";

    public final static String DEM_BAND_NAME = "elevation";
    public final static String AOT_BAND_NAME = "aot";
    public final static String AOTERR_BAND_NAME = "aot_err";

    public final static String[] MERIS_TOA_BAND_NAMES = new String[]{
            "reflectance_1", "reflectance_2", "reflectance_3", "reflectance_4", "reflectance_5",
            "reflectance_6", "reflectance_7", "reflectance_8", "reflectance_9", "reflectance_10",
            "reflectance_11", "reflectance_12", "reflectance_13", "reflectance_14", "reflectance_15"
    };

    public final static String[] MERIS_ANCILLARY_BAND_NAMES = new String[]{
            MERIS_VZA_TP_NAME, MERIS_VAA_TP_NAME, MERIS_SZA_TP_NAME, MERIS_SAA_TP_NAME,
            DEM_BAND_NAME, AOT_BAND_NAME, AOTERR_BAND_NAME, MERIS_OZO_TP_NAME
    };

    public final static String[] MERIS_SDR_BAND_NAMES = new String[]{
            "sdr_1", "sdr_2", "sdr_3", "sdr_4", "sdr_5",
            "sdr_6", "sdr_7", "sdr_8", "sdr_9", "sdr_10",
            "sdr_11", "sdr_12", "sdr_13", "sdr_14", "sdr_15"
    };


    public final static String[] MERIS_SDR_ERROR_BAND_NAMES = new String[]{
            "sdr_error_1", "sdr_error_2", "sdr_error_3", "sdr_error_4", "sdr_error_5",
            "sdr_error_6", "sdr_error_7", "sdr_error_8", "sdr_error_9", "sdr_error_10",
            "sdr_error_11", "sdr_error_12", "sdr_error_13", "sdr_error_14", "sdr_error_15"
    };

//    public final static String[] MERIS_TIE_POINT_GRID_NAMES = new String[]{     // would be useful in EnvisatConstants...
//            "latitude", "longitude", "dem_alt", "dem_rough", "lat_corr", "lon_corr",
//            "sun_zenith", "sun_azimuth", "view_zenith", "view_azimuth", "zonal_wind", "merid_wind",
//            "atm_press", "ozone", "rel_hum"
//    };

    public final static String[] VGT_TOA_BAND_NAMES = new String[]{
            "B0", "B2", "B3", "MIR"};

    public final static String VGT_OZO_BAND_NAME = "OG";

    public final static String[] VGT_ANCILLARY_BAND_NAMES = new String[]{
            "VZA", "VAA", "SZA", "SAA",
            DEM_BAND_NAME, AOT_BAND_NAME, AOTERR_BAND_NAME, VGT_OZO_BAND_NAME, "WVG"
    };

    public final static String[] VGT_SDR_BAND_NAMES = new String[]{
            "sdr_B0", "sdr_B2", "sdr_B3", "sdr_MIR"};

    public final static String[] VGT_SDR_ERROR_BAND_NAMES = new String[]{
            "sdr_error_B0", "sdr_error_B2", "sdr_error_B3", "sdr_error_MIR"};

    public final static String[] PROBAV_TOA_BAND_NAMES = new String[]{
            "TOA_REFL_BLUE", "TOA_REFL_RED", "TOA_REFL_NIR", "TOA_REFL_SWIR"};

    public final static String[] PROBAV_ANCILLARY_BAND_NAMES = new String[]{
            "VZA_VNIR", "VAA_VNIR", "SZA", "SAA",
            DEM_BAND_NAME, AOT_BAND_NAME, AOTERR_BAND_NAME
    };

    public final static String[] PROBAV_SDR_BAND_NAMES = new String[]{
            "sdr_BLUE", "sdr_RED", "sdr_NIR", "sdr_SWIR"};

    public final static String[] PROBAV_SDR_ERROR_BAND_NAMES = new String[]{
            "sdr_error_BLUE", "sdr_error_RED", "sdr_error_NIR", "sdr_error_SWIR"};

    public final static String[] BB_BAND_NAMES = new String[]{
            "BB_VIS", "BB_NIR", "BB_SW"};

    public final static String[] BB_SIGMA_BAND_NAMES = new String[]{
            "sig_BB_VIS_VIS", "sig_BB_VIS_NIR", "sig_BB_VIS_SW", "sig_BB_NIR_NIR", "sig_BB_NIR_SW", "sig_BB_SW_SW"};

    public final static String[] BB_KERNEL_BAND_NAMES = new String[]{
            "Kvol_BRDF_VIS", "Kvol_BRDF_NIR", "Kvol_BRDF_SW",
            "Kgeo_BRDF_VIS", "Kgeo_BRDF_NIR", "Kgeo_BRDF_SW"};

    public static final int MODIS_TILE_WIDTH = 1200;
    public static final int MODIS_TILE_HEIGHT = 1200;

    public static final int MODIS_NUM_SPECTRAL_BANDS = 7;

    //    public static final String COMMON_LAND_EXPR = "cloud_classif_flags.F_CLEAR_LAND OR cloud_classif_flags.F_CLEAR_SNOW";
    public static final String COMMON_LAND_EXPR =
            "(!cloud_classif_flags.F_CLOUD AND !cloud_classif_flags.F_CLOUD_BUFFER) OR cloud_classif_flags.F_CLEAR_SNOW";
//    public static final String COMMON_LAND_EXPR_AATSR =
//            "cloud_classif_flags_fward.F_CLEAR_LAND OR cloud_classif_flags_fward.F_CLEAR_SNOW";

    public static final String LAND_EXPR_MERIS =
            "NOT l1_flags.INVALID AND NOT l1_flags.COSMETIC AND (" + COMMON_LAND_EXPR + ")";
    public static final String LAND_EXPR_PROBAV =
            "SM_FLAGS.GOOD_BLUE AND SM_FLAGS.GOOD_RED AND SM_FLAGS.GOOD_NIR AND (" + COMMON_LAND_EXPR + ")";
    public static final String LAND_EXPR_VGT = "SM.B0_GOOD AND SM.B2_GOOD AND SM.B3_GOOD AND (" + COMMON_LAND_EXPR + ")";

//    public static final String L1_INVALID_EXPR_MERIS = "l1_flags.INVALID OR l1_flags.COSMETIC OR cloud_classif_flags.F_INVALID";
//    public static final String L1_INVALID_EXPR_VGT = "!SM.B0_GOOD OR !SM.B2_GOOD OR !SM.B3_GOOD OR (!SM.MIR_GOOD AND MIR > 0.65)";
//    public static final String L1_INVALID_EXPR_PROBAV =
//            "!SM_FLAGS.GOOD_BLUE OR !SM_FLAGS.GOOD_RED OR !SM_FLAGS.GOOD_NIR OR (!SM_FLAGS.GOOD_SWIR AND TOA_REFL_SWIR > 0.65)";

    public static final String AEROSOL_CLIMATOLOGY_MONTHLY_BAND_GROUP_NAME = "AOD550_aer_mo_time";
    public static final float AOT_CONST_VALUE = 0.15f;

    // Liang coeffs for AVHRR: bb := a*brf1^2 + b*brf2^2 + c*brf1*brf2 + d*brf1 + e*brf2 + f
    public static final double[][] AVHRR_LIANG_COEFFS = {{0.441, 0.0, 0.0, 0.591, 0.0, 0.0074},
            {-1.4759, -0.6536, 1.8591, 0.0, 1.063, 0.0},
            {-0.337, -0.2707, 0.7074, 0.2915, 0.5256, 0.0035}};

    public static final double[][] MERIS_SPECTRAL_MAPPING_COEFFS = new double[][] {
            {0.0, 0.0, 0.0, 0.0381683132278, 0.0, 0.0706581570918, 0.727938603627, 0.16365834236, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0} ,
            {0.0108626196304, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0269109404428, 0.0, 0.0, 0.0,
                    0.11219885911, 0.399278981479, 0.463244499785, 0.0},
            {0.0, 0.0, 0.52091545188, 0.298259211541, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0},
            {0.0, 0.0, 0.0, 0.138467743664, 0.867733708452, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0},
            {0.42274348004, 0.0, 0.0, -1.05933169333, 0.0, 0.0, 0.0, 0.0, 0.914892566036, -0.605256825945,
                    0.0, 0.0, 1.35086894047, -0.19698419591, -0.19698419591},
            {0.540254921442, 0.0, 0.0, -1.20929374247, 0.0, 0.0, 0.0, 0.164593497983, 1.95758407036, -1.48558749155,
                    0.0, 0.0, 1.48228762628, -0.491304016928, -0.491304016928},
            {0.866623679649, 0.0, -1.1550246423, -0.263974311816, 0.0, 0.0, 0.0, 1.56754833459, 0.12039074206,
                    -0.137673885865, 0.0, 0.0, 0.0, 0.0552984349493, 0.0552984349493}
    };

    public static final double[][] VGT_SPECTRAL_MAPPING_COEFFS = new double[][] {
            {0.0860828518334, 0.828921553716, 0.0330779825538, 0.0133288486504} ,
            {0.0417849338323, 0.0, 1.06278243332, 0.0},
            {0.984817497715, 0.0, 0.0137553829395, 0.0},
            {0.977250012143, 0.215288693766, 0.0774531877644, 0.0},
            {0.0, 0.0, 0.469467034072,	0.571825060878},
            {0.0557421498118, 0.0, 0.054100993273, 1.02414199042},
            {0.0, 0.359791016158,	0.0, 0.782006347696}
    };


}


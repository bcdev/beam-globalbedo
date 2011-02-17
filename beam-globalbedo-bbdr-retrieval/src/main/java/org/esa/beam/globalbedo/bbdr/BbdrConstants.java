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

    // taken from LUT 'MERIS_LUT_MOMO_ABSORB_SDR_noG_v2.bin' (make sure these values are up to date)
    public final static float[] MERIS_SOLAR_IRRADIANCES = {
        1714.49f,1879.69f, 1929.27f, 1928.54f, 1803.06f,
        1650.77f, 1531.52f, 1472.16f, 1407.89f, 1266.08f,
        1254.74f, 1177.28f, 958.059f, 930.005f, 895.347f
    };

    public final static float[] AATSR_WAVELENGHTS = {
         550.0f, 665.0f, 865.0f, 1610.0f
    };

    public final static float[] AATSR_CALIBRATION_COEFFS = {
        1.0253f, 1.0093f, 1.0265f, 1.0f
    };

    // taken from LUT 'AATSR_LUT_MOMO_ABSORB_SDR_noG_v2.bin' (make sure these values are up to date)
    public final static float[] AATSR_SOLAR_IRRADIANCES = {
         1819.54f, 1521.89f, 950.683f, 254.484f
    };

    public final static float[] VGT_WAVELENGHTS = {
         450.0f, 645.0f, 835.0f, 1665.0f
    };

    public final static float[] VGT_CALIBRATION_COEFFS = {
        1.012f, 0.953f, 0.971f, 1.0f
    };

    // taken from LUT 'VGT_LUT_MOMO_ABSORB_SDR_noG_v2.bin' (make sure these values are up to date)
    public final static float[] VGT_SOLAR_IRRADIANCES = {
         1938.27f, 1582.68f, 1036.09f, 224.581f
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

    public final static String AATSR_VZA_NADIR_TP_NAME = "view_elev_nadir";
    public final static String AATSR_VAA_NADIR_TP_NAME = "view_azimuth_nadir";
    public final static String AATSR_SZA_NADIR_TP_NAME = "sun_elev_nadir";
    public final static String AATSR_SAA_NADIR_TP_NAME = "sun_azimuth_nadir";
    public final static String AATSR_VZA_FWARD_TP_NAME = "view_elev_nadir";
    public final static String AATSR_VAA_FWARD_TP_NAME = "view_azimuth_nadir";
    public final static String AATSR_SZA_FWARD_TP_NAME = "sun_elev_nadir";
    public final static String AATSR_SAA_FWARD_TP_NAME = "sun_azimuth_nadir";
    public final static String AATSR_DEM_TP_NAME = "altitude";
    public final static String AATSR_AOT_BAND_NAME = "aot";
    public final static String AATSR_AOTERR_BAND_NAME = "aot_err";

    public final static String[] AATSR_TOA_BAND_NAMES_NADIR = new String[]{
                    "reflec_nadir_550", "reflec_nadir_670", "reflec_nadir_870", "reflec_nadir_1600"};

    public final static String[] AATSR_TOA_BAND_NAMES_FWARD = new String[]{
                    "reflec_fward_550", "reflec_fward_670", "reflec_fward_870", "reflec_fward_1600"};

    public final static String VGT_VZA_BAND_NAME = "VZA";
    public final static String VGT_VAA_BAND_NAME = "VAA";
    public final static String VGT_SZA_BAND_NAME = "SZA";
    public final static String VGT_SAA_BAND_NAME = "SAA";
    public final static String VGT_DEM_BAND_NAME = "elevation";
    public final static String VGT_OZO_BAND_NAME = "OG";
    public final static String VGT_AOT_BAND_NAME = "aot";
    public final static String VGT_AOTERR_BAND_NAME = "aot_err";

    public final static String[] VGT_TOA_BAND_NAMES = new String[]{
                    "B0", "B2", "B3", "MIR"};

}


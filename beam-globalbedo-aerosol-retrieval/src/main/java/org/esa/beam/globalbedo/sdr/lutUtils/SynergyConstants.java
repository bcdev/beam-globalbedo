package org.esa.beam.globalbedo.sdr.lutUtils;

import java.io.File;

/**
 * Constants defined for whole Synergy project
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class SynergyConstants {
    // Constants

    public static final String SYNERGY_AUXDATA_HOME_DEFAULT =
            System.getProperty("user.home")
                    + File.separator + ".beam"
                    + File.separator + "beam-meris-aatsr-synergy"
                    + File.separator + "auxdata";
    public static final String AUXDATA_PATH_PARAM_LABEL = "Path to auxiliary data for land and ocean aerosol algorithms";

    public static final String AUXDATA_INFO_MESSAGE =
//            "<html>This is how to an get html formated message:<ul><li><i>italics</i> and "
//                    + "<li><b>bold</b> and "
//                    + "<li><u>underlined</u>...</ul></html>";
            "For the Synergy aerosol retrieval and atmospheric correction, various \n"
                    + "auxiliary data (such as lookup tables and surface reflectance spectrum files), \n"
                    + "are required. Please follow the instructions in chapter 2 of the Software  \n"
                    + "User Manual for download and installation of auxiliary data. ";

//    public static final String AUXDATA_ERROR_MESSAGE =
//            "<html> "
//                    + "Synergy auxdata are missing or could not be read. <br><br>"
//                    + "For the Synergy aerosol retrieval and atmospheric correction, various <br>"
//                    + "auxiliary data (such as lookup tables and surface reflectance spectrum files), <br> "
//                    + "are required. Please follow the instructions in chapter x.x.x of the Software  <br>"
//                    + "User Manual or online help for download and installation of auxiliary data. "
//                    + "</html>";
    public static final String AUXDATA_ERROR_MESSAGE =
            "Synergy auxdata are missing or could not be read.\n\n"
                    + "For the Synergy aerosol retrieval and atmospheric correction, various \n"
                    + "auxiliary data (such as lookup tables and surface reflectance spectrum files), \n"
                    + "are required. Please follow the instructions in chapter 2 of the Software  \n"
                    + "User Manual or online help for download and installation of auxiliary data. ";

    public static final String AEROSOL_MODEL_INFO_MESSAGE =
                       "For the Synergy land aerosol retrieval, appropriate aerosol models are used. \n"
                       + "For most use cases, the default set of models will be sufficient. \n"
                       + "However, advanced users may want to specify an own subset from all available\n"
                       + "aerosol models.\n\n"
                       + "To download either the default or the full set of aerosol models, \n"
                       + "please follow the instructions in chapter 2 of the Software  \n"
                       + "User Manual for download and installation of auxiliary data. \n"
                       + "Note that the full set of models requires approximately 6 GB of disk space. \n\n"
                       + "For more details on these land aerosol models, see the Synergy documentation \n"
                       + "referenced in the Software User Manual \n";

    public static final String ATM_CORR_INFO_MESSAGE =
                       "Due to the nature of the algorithm, the retrieval of surface directional \n"
                       + "reflectances requires much more computation time than the pure aerosol retrieval. \n"
                       + "If you are interested in aerosol quantities only, it is recommended to keep \n"
                       + "this option unselected. For the computation of surface directional reflectances \n"
                       + "on larger datasets, it is further recommended to create in advance subsets of \n"
                       + "the input data which just cover the regions of interest. \n";

    public static final String preprocessingRadioButtonLabel = "Only create a colocated MERIS/AATSR product";
    public static final String cloudScreeningRadioButtonLabel = "Create a cloud screening product";
    public static final String aerosolRetrievalRadioButtonLabel = "Create an aerosol and atmospheric correction product";

    public static final String useForwardViewCheckboxLabel = "Use the AATSR forward view when classifying";
    public static final String computeCOTCheckboxLabel = "Compute cloud index";
    public static final String computeSFCheckboxLabel = "Compute snow risk flag";
    public static final String computeSHCheckboxLabel = "Compute cloud shadow risk flag";

    public static final String computeOceanCheckboxLabel = "Retrieve AODs over ocean";
    public static final String computeLandCheckboxLabel = "Retrieve AODs over land";
    public static final String computeSurfaceReflectancesCheckboxLabel =
            "Retrieve surface directional reflectances over land (time consuming!)";

    public static final String defaultLandAerosolButtonLabel = "Use default land aerosol models (recommended)";
    public static final String customLandAerosolButtonLabel = "Use specific land aerosol models (for advanced users)";

    public static final String GAUSS_PARS_LUT_FILE_NAME = "gauss_lut.nc";
    public static final String AEROSOL_MODEL_FILE_NAME = "all_mie.nc";

    public static final String SOIL_SPEC_PARAM_NAME    = "soilspec";
    public static final String SOIL_SPEC_PARAM_DEFAULT = "spec_soil.dat";
    public static final String SOIL_SPEC_PARAM_LABEL   = "Soil surface reflectance spectrum";
    public static final String SOIL_SPEC_PARAM_DESCRIPTION = "File containing soil surface reflectance spectrum";

    public static final String VEG_SPEC_PARAM_NAME    = "vegspec";
    public static final String VEG_SPEC_PARAM_DEFAULT = "spec_veg.dat";
    public static final String VEG_SPEC_PARAM_LABEL   = "Vegetation surface reflectance spectrum";
    public static final String VEG_SPEC_PARAM_DESCRIPTION = "File containing vegetation surface reflectance spectrum";

    public static final String LAND_AEROSOL_LABEL_TEXT = "Definition of land aerosol models";

    public static final String LUT_PATH_PARAM_NAME    = "lutpath";
    public static final String LUT_PATH_PARAM_LABEL = "Path to LUTs for land and ocean aerosol algorithms";
    public static final String LUT_PATH_PARAM_DESCRIPTION = "File path to LookUpTables root directory";
    public static final String LUT_PATH_PARAM_DEFAULT = "C:/synergy/aerosolLUTs";

//    public static final String LUT_LAND_PATH_PARAM_DEFAULT = "e:/model_data/momo/bin";
//    public static final String LUT_PATH_PARAM_DEFAULT = "e:/model_data/momo";
    public static final String[] LUT_LAND_AATSR_WAVELEN = {"00550.00","00665.00","00865.00","01610.00"};
    public static final String[] LUT_LAND_MERIS_WAVELEN = {"00412.00","00442.00","00490.00","00510.00","00560.00",
                                                      "00620.00","00665.00","00681.00","00708.00","00753.00",
                                                      "00778.00","00865.00","00885.00"};

    public static final String AEROSOL_MODEL_PARAM_NAME    = "aerosolModels";
//    public static final String AEROSOL_MODEL_PARAM_DEFAULT = "8,20,28";
    public static final String AEROSOL_MODEL_PARAM_DEFAULT = "8";
    public static final String AEROSOL_MODEL_PARAM_LABEL   = "List of land aerosol models";
    public static final String AEROSOL_MODEL_PARAM_DESCRIPTION = "Comma sep. list of aerosol model identifiers";

    public static final String OUTPUT_PRODUCT_NAME_NAME = "targetname";
    public static final String OUTPUT_PRODUCT_NAME_DEFAULT = "SYNERGY LAND AEROSOL";
    public static final String OUTPUT_PRODUCT_NAME_DESCRIPTION = "Product name of the target data set";
    public static final String OUTPUT_PRODUCT_NAME_LABEL = "Product name";

    public static final String OUTPUT_PRODUCT_TYPE_NAME = "targettype";
    public static final String OUTPUT_PRODUCT_TYPE_DEFAULT = "AEROSOL";
    public static final String OUTPUT_PRODUCT_TYPE_DESCRITPION = "Product type of the target data set";
    public static final String OUTPUT_PRODUCT_TYPE_LABEL = "Product type";

    // aot output
    public static final String OUTPUT_AOT_BAND_NAME = "aot";
    public static final String OUTPUT_AOT_BAND_DESCRIPTION = "MERIS AATSR Synergy AOT";
    public static final double OUTPUT_AOT_BAND_NODATAVALUE = -1.0;
    public static final boolean OUTPUT_AOT_BAND_NODATAVALUE_USED = Boolean.TRUE;

    // ang output
    public static final String OUTPUT_ANG_BAND_NAME = "ang";
    public static final String OUTPUT_ANG_BAND_DESCRIPTION = "MERIS AATSR Synergy Angstrom Parameter";
    public static final double OUTPUT_ANG_BAND_NODATAVALUE = -1.0;
    public static final boolean OUTPUT_ANG_BAND_NODATAVALUE_USED = Boolean.TRUE;

    // aot uncertainty output
    public static final String OUTPUT_AOTERR_BAND_NAME = "aot_uncertainty";
    public static final String OUTPUT_AOTERR_BAND_DESCRIPTION = "MERIS AATSR Synergy uncertainty of AOT";
    public static final double OUTPUT_AOTERR_BAND_NODATAVALUE = -1.0;
    public static final boolean OUTPUT_AOTERR_BAND_NODATAVALUE_USED = Boolean.TRUE;

    // ang uncertainty output
    public static final String OUTPUT_ANGERR_BAND_NAME = "ang_uncertainty";
    public static final String OUTPUT_ANGERR_BAND_DESCRIPTION = "MERIS AATSR Synergy uncertainty of Angstrom Parameter";
    public static final double OUTPUT_ANGERR_BAND_NODATAVALUE = -1.0;
    public static final boolean OUTPUT_ANGERR_BAND_NODATAVALUE_USED = Boolean.TRUE;

    // land aerosol model output
    public static final String OUTPUT_AOTMODEL_BAND_NAME = "land_aerosol_model";
    public static final String OUTPUT_AOTMODEL_BAND_DESCRIPTION = "MERIS AATSR Synergy LAnd Aerosol Model";
    public static final int OUTPUT_AOTMODEL_BAND_NODATAVALUE = -1;
    public static final boolean OUTPUT_AOTMODEL_BAND_NODATAVALUE_USED = Boolean.TRUE;

    // glint output
    public static final String OUTPUT_GLINT_BAND_NAME = "glint";
    public static final String OUTPUT_GLINT_BAND_DESCRIPTION = "Glint retrieval for first band used (debug output)";
    public static final double OUTPUT_GLINT_BAND_NODATAVALUE = -1.0;
    public static final boolean OUTPUT_GLINT_BAND_NODATAVALUE_USED = Boolean.TRUE;

    public static final String INPUT_PRESSURE_BAND_NAME = "atm_press";
    public static final String INPUT_OZONE_BAND_NAME = "ozone";
    public static final double[] o3CorrSlopeAatsr = {-0.183498, -0.109118, 0.0, 0.0};
    public static final double[] o3CorrSlopeMeris = {0.00, -0.00494535, -0.0378441, -0.0774214, -0.199693, -0.211885, -0.0986805, -0.0675948, -0.0383019, -0.0177211, 0.00, 0.00, 0.00};
    //public static final double wvCorrSlopeAatsr = -0.00552339;
    public static final double[] wvCorrSlopeAatsr = {0.00, -0.00493235, -0.000441272, -0.000941457};
    public static final double[] wvCorrSlopeMeris = {0.00, 0.00, 0.00, 0.00, 0.00, 0.00, -0.000416283, -0.000326704, -0.0110851, 0.00, -0.000993413, -0.000441272, -0.00286989};

    // windspeed output
    public static final String OUTPUT_WS_BAND_NAME = "windspeed";
    public static final String OUTPUT_WS_BAND_DESCRIPTION = "Windspeed retrieval from Glint algorithm (debug output)";
    public static final double OUTPUT_WS_BAND_NODATAVALUE = -1.0;
    public static final boolean OUTPUT_WS_BAND_NODATAVALUE_USED = Boolean.TRUE;

    // Surface Directional Reflectance Output
    public static final String OUTPUT_SDR_BAND_NAME = "SynergySDR";
    public static final String OUTPUT_SDR_BAND_DESCRIPTION = "Surface Directional Reflectance Band";
    public static final double OUTPUT_SDR_BAND_NODATAVALUE = -1.0;
    public static final boolean OUTPUT_SDR_BAND_NODATAVALUE_USED = Boolean.TRUE;


    public static final String INPUT_BANDS_PREFIX_MERIS = "reflectance_";
    public static final String INPUT_BANDS_PREFIX_AATSR_NAD = "reflec_nadir";
    public static final String INPUT_BANDS_PREFIX_AATSR_FWD = "reflec_fward";

    public static final String INPUT_BANDS_SUFFIX_AATSR = "AATSR";
    public static final String INPUT_BANDS_SUFFIX_MERIS = "MERIS";
    // todo: for FUB demo products only. change back later!
//    public static final String INPUT_BANDS_SUFFIX_AATSR = "S";
//    public static final String INPUT_BANDS_SUFFIX_MERIS = "M";

    public static final int[] EXCLUDE_INPUT_BANDS_MERIS = {11,15};
    public static final int[] EXCLUDE_INPUT_BANDS_AATSR = null;
    static String OUTPUT_ERR_BAND_NAME;

    public static final float[] refractiveIndex = new float[]{1.3295f,1.329f,1.31f,1.3295f,1.31f,1.3295f};
    public static final float[] rhoFoam = new float[]{0.2f, 0.2f, 0.1f, 0.2f, 0.1f, 0.2f};

    //aerosol flag constants
    public static final String aerosolFlagCodingName = "aerosol_land_flags";
    public static final String aerosolFlagCodingDesc = "Aerosol Retrieval Flags";
    public static final String flagCloudyName = "aerosol_cloudy";
    public static final String flagOceanName = "aerosol_ocean";
    public static final String flagSuccessName = "aerosol_successfull";
    public static final String flagBorderName = "aerosol_border";
    public static final String flagFilledName = "aerosol_filled";
    public static final String flagNegMetricName = "negative_metric";
    public static final String flagAotLowName = "aot_low";
    public static final String flagErrHighName = "aot_error_high";
    public static final String flagCoastName = "aerosol_coastline";
    public static final String flagCloudyDesc = "no land aerosol retrieval due to clouds";
    public static final String flagOceanDesc = "no land aerosol retrieval due to ocean";
    public static final String flagSuccessDesc = "land aerosol retrieval performed";
    public static final String flagBorderDesc = "land aerosol retrieval average for border pixel";
    public static final String flagFilledDesc = "no aerosol retrieval, pixel values interpolated";
    public static final String flagNegMetricDesc = "negative_metric";
    public static final String flagAotLowDesc = "aot_low";
    public static final String flagErrHighDesc = "aot_error_high";
    public static final String flagCoastDesc = "Coast Line Pixel";
    public static final int oceanMask = 1;
    public static final int cloudyMask = 2;
    public static final int successMask = 4;
    public static final int borderMask = 8;
    public static final int filledMask = 16;
    public static final int negMetricMask = 32;
    public static final int aotLowMask = 64;
    public static final int errHighMask = 128;
    public static final int coastMask = 256;


    // this is preliminary!
    public static float MERIS_12_SOLAR_FLUX = 930.0f;
    public static float MERIS_13_SOLAR_FLUX = 902.0f;
    public static float MERIS_14_SOLAR_FLUX = 870.0f;

    public static final String CONFID_NADIR_FLAGS_AATSR = "confid_flags_nadir_AATSR";
    public static final String CONFID_FWARD_FLAGS_AATSR = "confid_flags_fward_AATSR";
    public static final String CLOUD_NADIR_FLAGS_AATSR = "cloud_flags_nadir_AATSR";
    public static final String CLOUD_FWARD_FLAGS_AATSR = "cloud_flags_fward_AATSR";

    public static final String L1_FLAGS_MERIS = "l1_flags_MERIS";
    public static final String CLOUD_FLAG_MERIS = "cloud_flag_MERIS";

    // Constants
    public static double TAU_ATM = 0.222029504023959d;
    public static double NODATAVALUE = -10.0d;

    // Synergy product types
    public static final String SYNERGY_RR_PRODUCT_TYPE_NAME = "SYN_RR__1P";
    public static final String SYNERGY_FR_PRODUCT_TYPE_NAME = "SYN_FR__1P";
    public static final String SYNERGY_FS_PRODUCT_TYPE_NAME = "SYN_FS__1P";

    // Meris bands
    public static final int[] MERIS_VIS_BANDS = { 1, 2, 3, 4, 5, 6, 7, 8 };
    public static final int[] MERIS_NIR_BANDS = { 9, 10, 12, 13, 14 };

    // Bitmask flags
    public static final int FLAGMASK_SHADOW       = 1;
    public static final int FLAGMASK_SNOW_FILLED  = 2;
    public static final int FLAGMASK_SNOW         = 4;
    public static final int FLAGMASK_CLOUD_FILLED = 8;
    public static final int FLAGMASK_CLOUD        = 16;

    public static final String FLAGNAME_SHADOW       = "SHADOW";
    public static final String FLAGNAME_SNOW_FILLED  = "SNOW_FILLED";
    public static final String FLAGNAME_SNOW         = "SNOW";
    public static final String FLAGNAME_CLOUD_FILLED = "CLOUD_FILLED";
    public static final String FLAGNAME_CLOUD        = "CLOUD";

    // Strings to detect reduced or full resolution products
    public static final String RR_STR  = "_RR";
    public static final String FR_STR  = "_FR";
    public static final String FS_STR  = "_FS";
    public static final String FSG_STR = "_FSG";

    public static final String MERIS_RADIANCE     = "radiance";
    public static final String MERIS_REFLECTANCE  = "reflectance";
    public static final String AATSR_REFLEC_NADIR = "reflec_nadir";
    public static final String AATSR_REFLEC_FWARD = "reflec_fward";
    public static final String AATSR_BTEMP_NADIR  = "btemp_nadir";
    public static final String AATSR_BTEMP_FWARD  = "btemp_fward";
    public static final String DEM_ELEVATION      = "dem_elevation";
    // Our synergy product has a different name for CTP than the original
    public static final String ENVISAT_CTP        = "cloud_top_press";
    public static final String SYNERGY_CTP        = "p_ctp";

    public static final String FLAG_LAND_OCEAN     = "LAND_OCEAN";
    public static final String FLAG_COASTLINE      = "COASTLINE";
    public static final String FLAG_INVALID        = "INVALID";
    public static final String FLAG_SCAN_ABSENT    = "SCAN_ABSENT";
    public static final String FLAG_ABSENT         = "ABSENT";
    public static final String FLAG_NOT_DECOMPR    = "NOT_DECOMPR";
    public static final String FLAG_NO_SIGNAL      = "NO_SIGNAL";
    public static final String FLAG_OUT_OF_RANGE   = "OUT_OF_RANGE";
    public static final String FLAG_NO_CALIB_PARAM = "NO_CALIB_PARAM";
    public static final String FLAG_UNFILLED       = "UNFILLED";

    // Feature band names
    public static final String F_BRIGHTENESS_NIR   = "f_brightness_nir";
    public static final String F_WHITENESS_NIR     = "f_whiteness_nir";
    public static final String F_BRIGHTENESS_VIS   = "f_brightness_vis";
    public static final String F_WHITENESS_VIS     = "f_whiteness_vis";
    public static final String F_WATER_VAPOR_ABS   = "f_water_vapor_abs";
    public static final String F_761_754_865_RATIO = "f_761-754-865ratio";
    public static final String F_443_754_RATIO     = "f_443-754ratio";
    public static final String F_870_670_RATIO     = "f_870-670ratio";
    public static final String F_11_12_DIFF        = "f_11-12diff";
    public static final String F_865_890_NDSI      = "f_865-890NDSI";
    public static final String F_555_1600_NDSI     = "f_555-1600NDSI";
    public static final String F_COASTLINE         = "f_coastline";
    public static final String F_SURF_PRESS        = "f_surf_press";
    public static final String F_SURF_PRESS_DIFF   = "f_surf_press_diff";

    // Classification steps intermediate bands
    public static final String B_COAST_ERODED = "coast_eroded";

    // Final cloud mask band names
    public static final String B_CLOUDMASK        = "cloudmask";
    public static final String B_CLOUDMASK_FILLED = "cloudmask_filled";
    public static final String B_SNOWMASK         = "snowmask";
    public static final String B_SNOWMASK_FILLED  = "snowmask_filled";
    public static final String B_SHADOWMASK       = "shadowmask";
    public static final String B_CLOUD_COMB       = "nn_comb_cloud";
    public static final String B_SNOW_COMB        = "nn_comb_snow";
    public static final String B_CLOUDINDEX       = "cloud_index_synergy";
    public static final String B_CLOUDFLAGS       = "cloud_flags_synergy";

    // Synergy NNs
    public static final String nn_global_synergy_nadir = "nn_global_synergy_nadir";
    public static final String nn_global_synergy_dual  = "nn_global_synergy_dual";
    public static final String nn_land_synergy_nadir   = "nn_land_synergy_nadir";
    public static final String nn_land_synergy_dual    = "nn_land_synergy_dual";
    public static final String nn_ocean_synergy_nadir  = "nn_ocean_synergy_nadir";
    public static final String nn_ocean_synergy_dual   = "nn_ocean_synergy_dual";
    public static final String nn_snow_synergy_nadir   = "nn_snow_synergy_nadir";
    public static final String nn_snow_synergy_dual    = "nn_snow_synergy_dual";
    public static final String nn_index_synergy_nadir  = "nn_index_synergy_nadir";
    public static final String nn_index_synergy_dual   = "nn_index_synergy_dual";
    // For band names (land and ocean are combined into local)
    public static final String nn_local_synergy_nadir = "nn_local_synergy_nadir";
    public static final String nn_local_synergy_dual  = "nn_local_synergy_dual";

    public static final String[] nn_synergy = {
    	nn_global_synergy_nadir, nn_global_synergy_dual,
    	nn_land_synergy_nadir, nn_land_synergy_dual,
    	nn_ocean_synergy_nadir, nn_ocean_synergy_dual,
        nn_snow_synergy_nadir, nn_snow_synergy_dual,
        nn_index_synergy_nadir, nn_index_synergy_dual
    };

    // Single instrument NNs
    public static final String nn_global_meris_nadir = "nn_global_meris_nadir";
    public static final String nn_global_aatsr_nadir = "nn_global_aatsr_nadir";
    public static final String nn_global_aatsr_dual  = "nn_global_aatsr_dual";
    public static final String nn_land_meris_nadir   = "nn_land_meris_nadir";
    public static final String nn_land_aatsr_nadir   = "nn_land_aatsr_nadir";
    public static final String nn_land_aatsr_dual    = "nn_land_aatsr_dual";
    public static final String nn_ocean_meris_nadir  = "nn_ocean_meris_nadir";
    public static final String nn_ocean_aatsr_nadir  = "nn_ocean_aatsr_nadir";
    public static final String nn_ocean_aatsr_dual   = "nn_ocean_aatsr_dual";
    public static final String nn_snow_meris_nadir   = "nn_snow_meris_nadir";
    public static final String nn_snow_aatsr_nadir   = "nn_snow_aatsr_nadir";
    public static final String nn_snow_aatsr_dual    = "nn_snow_aatsr_dual";
    // For band names (land and ocean are combined into local)
    public static final String nn_local_meris_nadir = "nn_local_meris_nadir";
    public static final String nn_local_aatsr_nadir = "nn_local_aatsr_nadir";
    public static final String nn_local_aatsr_dual  = "nn_local_aatsr_dual";

    public static final String[] nn_single = {
        nn_global_meris_nadir, nn_global_aatsr_nadir, nn_global_aatsr_dual,
        nn_land_meris_nadir, nn_land_aatsr_nadir, nn_land_aatsr_dual,
        nn_ocean_meris_nadir, nn_ocean_aatsr_nadir, nn_ocean_aatsr_dual,
        nn_snow_meris_nadir, nn_snow_aatsr_nadir, nn_snow_aatsr_dual
    };
}

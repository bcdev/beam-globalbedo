package org.esa.beam.globalbedo.inversion;

/**
 * Constants for Albedo Inversion part
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AlbedoInversionConstants {

    public final static String BBDR_BB_VIS_NAME = "BB_VIS";
    public final static String BBDR_BB_NIR_NAME = "BB_NIR";
    public final static String BBDR_BB_SW_NAME = "BB_SW";
    public final static String BBDR_SIG_BB_VIS_VIS_NAME = "sig_BB_VIS_VIS";
    public final static String BBDR_SIG_BB_VIS_NIR_NAME = "sig_BB_VIS_NIR";
    public final static String BBDR_SIG_BB_VIS_SW_NAME = "sig_BB_VIS_SW";
    public final static String BBDR_SIG_BB_NIR_NIR_NAME = "sig_BB_NIR_NIR";
    public final static String BBDR_SIG_BB_NIR_SW_NAME = "sig_BB_NIR_SW";
    public final static String BBDR_SIG_BB_SW_SW_NAME = "sig_BB_SW_SW";
    public final static String BBDR_KVOL_BRDF_VIS_NAME = "Kvol_BRDF_VIS";
    public final static String BBDR_KVOL_BRDF_NIR_NAME = "Kvol_BRDF_NIR";
    public final static String BBDR_KVOL_BRDF_SW_NAME = "Kvol_BRDF_SW";
    public final static String BBDR_KGEO_BRDF_VIS_NAME = "Kgeo_BRDF_VIS";
    public final static String BBDR_KGEO_BRDF_NIR_NAME = "Kgeo_BRDF_NIR";
    public final static String BBDR_KGEO_BRDF_SW_NAME = "Kgeo_BRDF_SW";
    public final static String BBDR_AOD550_NAME = "AOD550";
    public final static String BBDR_NDVI_NAME = "NDVI";
    public final static String BBDR_SIG_NDVI_NAME = "sig_NDVI";
    public final static String BBDR_VZA_NAME = "VZA";
    public final static String BBDR_SZA_NAME = "SZA";
    public final static String BBDR_RAA_NAME = "RAA";
    public final static String BBDR_DEM_NAME = "DEM";
    public final static String BBDR_SNOW_MASK_NAME = "snow_mask";
    public final static String BBDR_VGT_SM_NAME = "SM"; // VGT only, should become a flag band!

    public static final String merisLandMaskExpression = "NOT l1_flags.INVALID AND l1_flags.LAND_OCEAN";

    public static final int numBBDRWaveBands = 3;

    // MODIS tile size increment in (x,y)-coordinates: 10 degrees in metres
    public static double modisSinusoidalProjectionTileSizeIncrement = 1111950.519667000044137;
}

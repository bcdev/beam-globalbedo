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

    public final static String ACC_E_NAME = "E";
    public final static String ACC_MASK_NAME = "mask";
    public static final String ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME_OLD = "Days_to_the_Closest_Sample";
    public static final String ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME= "Time_to_the_Closest_Sample";    // for CEMS todo: generalize!!

    public static final String PRIOR_NSAMPLES_NAME =  "N samples";
    public static final String PRIOR_MASK_NAME =  "Mask";

    public static final String INV_ENTROPY_BAND_NAME = "Entropy";
    public static final String INV_REL_ENTROPY_BAND_NAME = "Relative_Entropy";
    public static final String INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME = "Weighted_Number_of_Samples";
    public static final String INV_GOODNESS_OF_FIT_BAND_NAME = "Goodness_of_Fit";
    public static final String ALB_SNOW_FRACTION_BAND_NAME = "Snow_Fraction";
    public static final String ALB_DATA_MASK_BAND_NAME = "Data_Mask";
    public static final String ALB_SZA_BAND_NAME = "Solar_Zenith_Angle";

    public static final String MERGE_PROPORTION_NSAMPLES_BAND_NAME = "Proportion_NSamples";

    public static final String merisLandMaskExpression = "NOT l1_flags.INVALID AND l1_flags.LAND_OCEAN";
    public static final String aatsrLandMaskExpression = "cloud_flags_nadir.LAND";
    public static final String seaiceMaskExpression = "cloud_classif_flags.F_SEAICE";

    public static final String SEAICE_ALBEDO_VALID_PIXEL_EXPRESSION = "Weighted_Number_of_Samples > 0.0";

    public static final int NUM_BBDR_WAVE_BANDS = 3;
    public static final int NUM_ALBEDO_PARAMETERS = 3;  // f0, f1, f2

    public static final int NUM_ACCUMULATOR_BANDS = 92;

    public static final int MODIS_TILE_WIDTH = 1200;
    public static final int MODIS_TILE_HEIGHT = 1200;

    public static final int SEAICE_TILE_WIDTH = 2250;
    public static final int SEAICE_TILE_HEIGHT = 2250;

    public static final double MODIS_SIN_PROJECTION_PIXEL_SIZE_X = 926.6254330558;
    public static final double MODIS_SIN_PROJECTION_PIXEL_SIZE_Y = 926.6254330558;

    public static final double SEAICE_PST_PIXEL_SIZE_X = 1000.0;
    public static final double SEAICE_PST_PIXEL_SIZE_Y = 1000.0;

    public static final String MODIS_SIN_PROJECTION_CRS_STRING =
            "PROJCS[\"MODIS Sinusoidal\"," +
            "GEOGCS[\"WGS 84\"," +
            "  DATUM[\"WGS_1984\"," +
            "    SPHEROID[\"WGS 84\",6378137,298.257223563," +
            "      AUTHORITY[\"EPSG\",\"7030\"]]," +
            "    AUTHORITY[\"EPSG\",\"6326\"]]," +
            "  PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]]," +
            "  UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]]," +
            "   AUTHORITY[\"EPSG\",\"4326\"]]," +
            "PROJECTION[\"Sinusoidal\"]," +
            "PARAMETER[\"false_easting\",0.0]," +
            "PARAMETER[\"false_northing\",0.0]," +
            "PARAMETER[\"central_meridian\",0.0]," +
            "PARAMETER[\"semi_major\",6371007.181]," +
            "PARAMETER[\"semi_minor\",6371007.181]," +
            "UNIT[\"m\",1.0]," +
            "AUTHORITY[\"SR-ORG\",\"6974\"]]";

    // MODIS tile size increment in (x,y)-coordinates: 10 degrees in metres
    public static double modisSinusoidalProjectionTileSizeIncrement = 1111950.519667000044137;

    public static final double HALFLIFE = 11.54;

    public static final String POLAR_STEREOGRAPHIC_PROJECTION_CRS_STRING =
            "PROJCS[\"Polar_Stereographic / World Geodetic System 1984\"," +
            "GEOGCS[\"World Geodetic System 1984\"," +
            " DATUM[\"World Geodetic System 1984\"," +
            "  SPHEROID[\"WGS 84\",6378137.0, 298.257223563, AUTHORITY[\"EPSG\",\"7030\"]]," +
            "   AUTHORITY[\"EPSG\",\"6326\"]]," +
            "  PRIMEM[\"Greenwich\",0.0, AUTHORITY[\"EPSG\",\"8901\"]]," +
            "  UNIT[\"degree\",0.01745329251994328]," +
            "   AXIS[\"Geodetic longitude\", EAST]," +
            "   AXIS[\"Geodetic latitude\", NORTH]]," +
            "PROJECTION[\"Polar_Stereographic\"]," +
            "PARAMETER[\"semi_minor\",6378137.0]," +
            "PARAMETER[\"central_meridian\",0.0]," +
            "PARAMETER[\"latitude_of_origin\",90.0]," +
            "PARAMETER[\"scale_factor\",1.0]," +
            "PARAMETER[\"false_easting\",0.0]," +
            "PARAMETER[\"false_northing\",0.0]," +
            "UNIT[\"m\",1.0]," +
            "AXIS[\"Easting\", EAST]," +
            "AXIS[\"Northing\", NORTH]]";

    public static final String[][] doysOfMonth = {
                {"001","009","017","025"},    // Jan
                {"033","041","049","057"},    // Feb
                {"065","073","081","089"},    // Mar
                {"097","105","113"},          // Apr
                {"121","129","137","145"},    // May
                {"153","161","169","177"},    // Jun
                {"185","193","201","209"},    // Jul
                {"217","225","233","241"},    // Aug
                {"249","257","265","273"},    // Sep
                {"281","289","297"},          // Oct
                {"305","313","321","329"},    // Nov
                {"337","345","353","361"}     // Dec
        };

    public static final float NO_DATA_VALUE = -9999.0f;
}

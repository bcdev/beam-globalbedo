package org.esa.beam.globalbedo.bbdr;

import java.awt.*;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 23.02.2016
 * Time: 17:08
 *
 * @author olafd
 */
public class Mod09Constants {

    public static final String MOD09_STATE_BAND_NAME = "state_1km_1";
    public static final String MOD09_STATE_FLAG_BAND_NAME = "STATE_FLAGS";

    public static final int STATE_CLEAR_BIT_INDEX = 0;
    public static final int STATE_CLOUD_BIT_INDEX = 1;
    public static final int STATE_CLOUD_MIXED_BIT_INDEX = 2;
    public static final int STATE_CLOUD_UNKNOWN_BIT_INDEX = 3;
    public static final int STATE_CLOUD_SHADOW_BIT_INDEX = 4;

    public static final int STATE_SHALLOW_OCEAN_BIT_INDEX = 5;
    public static final int STATE_LAND_BIT_INDEX = 6;
    public static final int STATE_COASTLINE_BIT_INDEX = 7;
    public static final int STATE_SHALLOW_INLAND_WATER_BIT_INDEX = 8;
    public static final int STATE_EPHEMERAL_WATER_BIT_INDEX = 9;
    public static final int STATE_DEEP_INLAND_WATER_BIT_INDEX = 10;
    public static final int STATE_MODERATE_OCEAN_BIT_INDEX = 11;
    public static final int STATE_DEEP_OCEAN_BIT_INDEX = 12;

    public static final int STATE_AEROSOL_CLIMATOLOGY_BIT_INDEX = 13;
    public static final int STATE_AEROSOL_LOW_BIT_INDEX = 14;
    public static final int STATE_AEROSOL_AVERAGE_BIT_INDEX = 15;
    public static final int STATE_AEROSOL_HIGH_BIT_INDEX = 16;

    public static final int STATE_CIRRUS_NONE_BIT_INDEX = 17;
    public static final int STATE_CIRRUS_SMALL_BIT_INDEX = 18;
    public static final int STATE_CIRRUS_AVERAGE_BIT_INDEX = 19;
    public static final int STATE_CIRRUS_HIGH_BIT_INDEX = 20;

    public static final int STATE_INTERNAL_CLOUD_ALGORITHM_BIT_INDEX = 21;
    public static final int STATE_INTERNAL_FIRE_ALGORITHM_BIT_INDEX = 22;
    public static final int STATE_MOD35_SNOW_ICE_BIT_INDEX = 23;
    public static final int STATE_CLOUD_ADJACENCY_BIT_INDEX = 24;
    public static final int STATE_SALT_PAN_BIT_INDEX = 25;
    public static final int STATE_INTERNAL_SNOW_MASK_BIT_INDEX = 26;


    public static final String STATE_CLEAR_FLAG_NAME = "CLEAR";
    public static final String STATE_CLOUD_FLAG_NAME = "CLOUDY";
    public static final String STATE_CLOUD_MIXED_FLAG_NAME = "CLOUD_MIXED";
    public static final String STATE_CLOUD_UNKNOWN_FLAG_NAME = "CLOUD_UNKNOWN";
    public static final String STATE_CLOUD_SHADOW_FLAG_NAME = "CLOUD_SHADOW";

    public static final String STATE_SHALLOW_OCEAN_FLAG_NAME = "SHALLOW_OCEAN";
    public static final String STATE_LAND_FLAG_NAME = "LAND";
    public static final String STATE_COASTLINE_FLAG_NAME = "COASTLINE";
    public static final String STATE_SHALLOW_INLAND_WATER_FLAG_NAME = "SHALLOW_INLAND_WATER";
    public static final String STATE_EPHEMERAL_WATER_FLAG_NAME = "EPHEMERAL";
    public static final String STATE_DEEP_INLAND_WATER_FLAG_NAME = "DEEP_INLAND_WATER";
    public static final String STATE_MODERATE_OCEAN_FLAG_NAME = "MODERATE_OCEAN";
    public static final String STATE_DEEP_OCEAN_FLAG_NAME = "DEEP_OCEAN";

    public static final String STATE_AEROSOL_CLIMATOLOGY_FLAG_NAME = "AEROSOL_CLIMATOLOGY";
    public static final String STATE_AEROSOL_LOW_FLAG_NAME = "AEROSOL_LOW";
    public static final String STATE_AEROSOL_AVERAGE_FLAG_NAME = "AEROSOL_AVERAGE";
    public static final String STATE_AEROSOL_HIGH_FLAG_NAME = "AEROSOL_HIGH";

    public static final String STATE_CIRRUS_NONE_FLAG_NAME = "CIRRUS_NONE";
    public static final String STATE_CIRRUS_SMALL_FLAG_NAME = "CIRRUS_SMALL";
    public static final String STATE_CIRRUS_AVERAGE_FLAG_NAME = "CIRRUS_AVERAGE";
    public static final String STATE_CIRRUS_HIGH_FLAG_NAME = "CIRRUS_HIGH";

    public static final String STATE_INTERNAL_CLOUD_ALGORITHM_FLAG_NAME = "INTERNAL_CLOUD_ALGORITHM";
    public static final String STATE_INTERNAL_FIRE_ALGORITHM_FLAG_NAME = "INTERNAL_FIRE_ALGORITHM";
    public static final String STATE_MOD35_SNOW_ICE_FLAG_NAME = "MOD35_SNOW_ICE";
    public static final String STATE_CLOUD_ADJACENCY_FLAG_NAME = "CLOUD_ADJACENCY";
    public static final String STATE_SALT_PAN_FLAG_NAME = "SALT_PAN";
    public static final String STATE_INTERNAL_SNOW_MASK_FLAG_NAME = "SNOW";

    public static final String STATE_CLEAR_FLAG_DESCR = "CLEAR";
    public static final String STATE_CLOUD_FLAG_DESCR = "CLOUDY";
    public static final String STATE_CLOUD_MIXED_FLAG_DESCR = "CLOUD_MIXED";
    public static final String STATE_CLOUD_UNKNOWN_FLAG_DESCR = "CLOUD_UNKNOWN";
    public static final String STATE_CLOUD_SHADOW_FLAG_DESCR = "CLOUD_SHADOW";

    public static final String STATE_SHALLOW_OCEAN_FLAG_DESCR = "SHALLOW_OCEAN";
    public static final String STATE_LAND_FLAG_DESCR = "LAND";
    public static final String STATE_COASTLINE_FLAG_DESCR = "COASTLINE";
    public static final String STATE_SHALLOW_INLAND_WATER_FLAG_DESCR = "SHALLOW_INLAND_WATER";
    public static final String STATE_EPHEMERAL_WATER_FLAG_DESCR = "EPHEMERAL";
    public static final String STATE_DEEP_INLAND_WATER_FLAG_DESCR = "DEEP_INLAND_WATER";
    public static final String STATE_MODERATE_OCEAN_FLAG_DESCR = "MODERATE_OCEAN";
    public static final String STATE_DEEP_OCEAN_FLAG_DESCR = "DEEP_OCEAN";

    public static final String STATE_AEROSOL_CLIMATOLOGY_FLAG_DESCR = "AEROSOL_CLIMATOLOGY";
    public static final String STATE_AEROSOL_LOW_FLAG_DESCR = "AEROSOL_LOW";
    public static final String STATE_AEROSOL_AVERAGE_FLAG_DESCR = "AEROSOL_AVERAGE";
    public static final String STATE_AEROSOL_HIGH_FLAG_DESCR = "AEROSOL_HIGH";

    public static final String STATE_CIRRUS_NONE_FLAG_DESCR = "CIRRUS_NONE";
    public static final String STATE_CIRRUS_SMALL_FLAG_DESCR = "CIRRUS_SMALL";
    public static final String STATE_CIRRUS_AVERAGE_FLAG_DESCR = "CIRRUS_AVERAGE";
    public static final String STATE_CIRRUS_HIGH_FLAG_DESCR = "CIRRUS_HIGH";

    public static final String STATE_INTERNAL_CLOUD_ALGORITHM_FLAG_DESCR = "INTERNAL_CLOUD_ALGORITHM";
    public static final String STATE_INTERNAL_FIRE_ALGORITHM_FLAG_DESCR = "INTERNAL_FIRE_ALGORITHM";
    public static final String STATE_MOD35_SNOW_ICE_FLAG_DESCR = "MOD35_SNOW_ICE";
    public static final String STATE_CLOUD_ADJACENCY_FLAG_DESCR = "CLOUD_ADJACENCY";
    public static final String STATE_SALT_PAN_FLAG_DESCR = "SALT_PAN";
    public static final String STATE_INTERNAL_SNOW_MASK_FLAG_DESCR = "SNOW";


    public static final Color[] FLAG_COLORS = {
            new Color(120, 255, 180),
            new Color(255, 255, 0),
            new Color(0, 255, 255),
            new Color(255, 100, 0),
            new Color(255, 255, 180),
            new Color(255, 0, 255),
            new Color(0, 0, 255),
            new Color(180, 180, 255),
            new Color(255, 150, 100),
            new Color(0, 255, 0)
    };

}

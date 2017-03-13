package org.esa.beam.dataio.avhrr;

import java.awt.*;

/**
 * AVHRR LTDR reader constants
 *
 * @author olafd
 */
public class AvhrrLtdrConstants {

    public static final float[] WAVELENGTHS = {630.0f, 865.0f};
    public static final float[] BANDWIDTHS = {100.0f, 275.0f};
    public static final float[] BRIGHTNESS_TEMP_WAVELENGTHS = {3750.0f, 11000.0f, 12000.0f};

    public static final String AVHRR_LTDR_DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final String QA_FLAG_BAND_NAME = "QA_FLAGS";

    public static final int QA_CLOUD_BIT_INDEX = 1;
    public static final int QA_CLOUD_SHADOW_BIT_INDEX = 2;
    public static final int QA_WATER_BIT_INDEX = 3;
    public static final int QA_SUNGLINT_BIT_INDEX = 4;
    public static final int QA_DENSE_DARK_VEGETATION_BIT_INDEX = 5;
    public static final int QA_NIGHT_BIT_INDEX = 6;
    public static final int QA_VALID_CHANNEL_1to5_BIT_INDEX = 7;
    public static final int QA_INVALID_CHANNEL_1_BIT_INDEX = 8;
    public static final int QA_INVALID_CHANNEL_2_BIT_INDEX = 9;
    public static final int QA_INVALID_CHANNEL_3_BIT_INDEX = 10;
    public static final int QA_INVALID_CHANNEL_4_BIT_INDEX = 11;
    public static final int QA_INVALID_CHANNEL_5_BIT_INDEX = 12;
    public static final int QA_INVALID_RHO3_BIT_INDEX = 13;
    public static final int QA_BRDF_CORRECTION_BIT_INDEX = 14;
    public static final int QA_POLAR_BIT_INDEX = 15;

    public static final String QA_CLOUD_FLAG_NAME = "CLOUD";
    public static final String QA_CLOUD_SHADOW_FLAG_NAME = "CLOUD_SHADOW";
    public static final String QA_WATER_FLAG_NAME = "WATER";
    public static final String QA_SUNGLINT_FLAG_NAME = "SUNGLINT";
    public static final String QA_DENSE_DARK_VEGETATION_FLAG_NAME = "DENSE_DARK_VEGETATION";
    public static final String QA_NIGHT_FLAG_NAME = "NIGHT";
    public static final String QA_VALID_CHANNEL_1to5_FLAG_NAME = "VALID_CHANNELS_1to5";
    public static final String QA_INVALID_CHANNEL_1_FLAG_NAME = "INVALID_CHANNEL_1";
    public static final String QA_INVALID_CHANNEL_2_FLAG_NAME = "INVALID_CHANNEL_2";
    public static final String QA_INVALID_CHANNEL_3_FLAG_NAME = "INVALID_CHANNEL_3";
    public static final String QA_INVALID_CHANNEL_4_FLAG_NAME = "INVALID_CHANNEL_4";
    public static final String QA_INVALID_CHANNEL_5_FLAG_NAME = "INVALID_CHANNEL_5";
    public static final String QA_INVALID_RHO3_FLAG_NAME = "INVALID_RHO3";
    public static final String QA_BRDF_CORRECTION_FLAG_NAME = "BRDF_CORRECTION";
    public static final String QA_POLAR_FLAG_NAME = "POLAR";

    public static final String QA_CLOUD_FLAG_DESCR = " Pixel is cloudy";
    public static final String QA_CLOUD_SHADOW_FLAG_DESCR = "Pixel contains cloud shadow";
    public static final String QA_WATER_FLAG_DESCR = "Pixel is over water";
    public static final String QA_SUNGLINT_FLAG_DESCR = "Pixel is over sunglint";
    public static final String QA_DENSE_DARK_VEGETATION_FLAG_DESCR = "Pixel is over dense dark vegetation";
    public static final String QA_NIGHT_FLAG_DESCR = "Pixel is at night (high solar zenith)";
    public static final String QA_VALID_CHANNEL_1to5_FLAG_DESCR = "Channels 1 - 5 are valid";
    public static final String QA_INVALID_CHANNEL_1_FLAG_DESCR = "Channel 1 value is invalid";
    public static final String QA_INVALID_CHANNEL_2_FLAG_DESCR = "Channel 2 value is invalid";
    public static final String QA_INVALID_CHANNEL_3_FLAG_DESCR = "Channel 3 value is invalid";
    public static final String QA_INVALID_CHANNEL_4_FLAG_DESCR = "Channel 4 value is invalid";
    public static final String QA_INVALID_CHANNEL_5_FLAG_DESCR = "Channel 5 value is invalid";
    public static final String QA_INVALID_RHO3_FLAG_DESCR = "RHO3 value is invalid";
    public static final String QA_BRDF_CORRECTION_FLAG_DESCR = "BRDF-correction issues";
    public static final String QA_POLAR_FLAG_DESCR = "polar flag (latitude over 60 degrees (land) or 50 degrees (ocean))";

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
            new Color(170, 120, 0),
            new Color(10, 155, 180),
            new Color(80, 45, 200),
            new Color(30, 205, 19),
            new Color(180, 255, 240),
            new Color(150, 5, 0),
            new Color(5, 55, 120)
    };
}

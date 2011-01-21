package org.esa.beam.globalbedo.bbdr;


import java.util.HashMap;

/**
 * Constants defined for BBDR retrieval
 *
 * @author Olaf Danne
 */
public class BbdrConstants {


    public final static HashMap instrumentWavelengths = new HashMap<String, float[]>();

    public final static float[] MERIS_WAVELENGHTS = {
        412.0f, 442.0f, 490.0f, 510.0f, 560.0f,
        620.0f, 665.0f, 681.0f, 708.0f, 753.0f,
        760.0f, 778.0f, 865.0f, 885.0f, 900.0f
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

    // taken from LUT 'AATSR_LUT_MOMO_ABSORB_SDR_noG_v2.bin' (make sure these values are up to date)
    public final static float[] AATSR_SOLAR_IRRADIANCES = {
         1819.54f, 1521.89f, 950.683f, 254.484f
    };

    public final static float[] VGT_WAVELENGHTS = {
         450.0f, 645.0f, 835.0f, 1665.0f
    };

    // taken from LUT 'VGT_LUT_MOMO_ABSORB_SDR_noG_v2.bin' (make sure these values are up to date)
    public final static float[] VGT_SOLAR_IRRADIANCES = {
         1938.27f, 1582.68f, 1036.09f, 224.581f
    };

    static {
         instrumentWavelengths.put("MERIS", MERIS_WAVELENGHTS);
         instrumentWavelengths.put("AATSR", AATSR_WAVELENGHTS);
         instrumentWavelengths.put("VGT", VGT_WAVELENGHTS);
    };

}


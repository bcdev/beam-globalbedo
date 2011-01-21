package org.esa.beam.globalbedo.bbdr;


import java.util.HashMap;

/**
 * Constants defined for BBDR retrieval
 *
 * @author Olaf Danne
 */
public class BbdrConstants {


    public final static HashMap instrumentNumWavelengths = new HashMap<String, Integer>();

    public final static float[] MERIS_WAVELENGHTS = {
        412.0f, 442.0f, 490.0f, 510.0f, 560.0f,
        620.0f, 665.0f, 681.0f, 708.0f, 753.0f,
        760.0f, 778.0f, 865.0f, 885.0f, 900.0f
    };

    public final static float[] AATSR_WAVELENGHTS = {
        // TODO
    };

    public final static float[] VGT_WAVELENGHTS = {
        // TODO
    };

    static {
         instrumentNumWavelengths.put("MERIS", MERIS_WAVELENGHTS.length);
         instrumentNumWavelengths.put("AATSR", AATSR_WAVELENGHTS.length);
         instrumentNumWavelengths.put("VGT", VGT_WAVELENGHTS.length);
    };

}


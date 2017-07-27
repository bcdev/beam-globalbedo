package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.BitSetter;

import java.awt.*;

/**
 * Class representing BEAM/SNAP flag band for AVHRR MSSL mask.
 *
 * @author olafd
 */
public class AvhrrMsslFlag {

    public static final int CLEAR_LAND_BIT_INDEX = 1;
    public static final int CLOUD_BIT_INDEX = 2;
    public static final int SNOW_BIT_INDEX = 3;

    public static boolean isClearLand(int srcValue) {
        return srcValue == 1;
    }

    public static boolean isCloud(int srcValue) {
        return srcValue == 2;
    }

    public static boolean isSnow(int srcValue) {
        return srcValue == 3;
    }


    public static FlagCoding createAvhrrMsslMaskFlagCoding(String flagIdentifier) {
        FlagCoding flagCoding = new FlagCoding(flagIdentifier);
        flagCoding.addFlag("F_CLEAR_LAND", BitSetter.setFlag(0, CLEAR_LAND_BIT_INDEX), "CLEAR_LAND");
        flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, CLOUD_BIT_INDEX), "CLOUD");
        flagCoding.addFlag("F_SNOW", BitSetter.setFlag(0, SNOW_BIT_INDEX), "SNOW");

        return flagCoding;
    }

    public static int setupAvhrrMsslBitmasks(Product cloudProduct) {

        int index = 0;
        int w = cloudProduct.getSceneRasterWidth();
        int h = cloudProduct.getSceneRasterHeight();
        Mask mask;

        mask = Mask.BandMathsType.create("clear_land",
                                         "CLEAR_LAND", w, h,
                                         "AVHRR_MSSL_FLAG_snap.F_CLEAR_LAND",
                                         Color.green, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("cloud",
                                         "CLOUD", w, h,
                                         "AVHRR_MSSL_FLAG_snap.F_CLOUD",
                                         Color.yellow, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("snow",
                                         "SNOW", w, h,
                                         "AVHRR_MSSL_FLAG_snap.F_SNOW",
                                         Color.lightGray.brighter(), 0.5f);
        cloudProduct.getMaskGroup().add(index, mask);

        return index;
    }

}

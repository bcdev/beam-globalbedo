package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.BitSetter;

import java.awt.*;

/**
 * Class representing BEAM/SNAP flag band for AVHRR LTDR flag.
 *
 * @author olafd
 */
public class AvhrrLtdrFlag {

    public static final int SPARE_BIT_INDEX = 0;
    public static final int CLOUD_BIT_INDEX = 1;
    public static final int CLOUD_SHADOW_BIT_INDEX = 2;
    public static final int WATER_BIT_INDEX = 3;
    public static final int GLINT_BIT_INDEX = 4;
    public static final int DARK_VEGETATION_BIT_INDEX = 5;
    public static final int NIGHT_BIT_INDEX = 6;
    public static final int UNKNOWN_BIT_INDEX = 7;
    public static final int CHANNEL_1_INVALID_BIT_INDEX = 8;
    public static final int CHANNEL_2_INVALID_BIT_INDEX = 9;
    public static final int BRDF_CORR_ISSUES_BIT_INDEX = 10;
    public static final int POLAR_BIT_INDEX = 11;

    public static boolean isSpare(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 0));
    }

    public static boolean isCloud(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 1));
    }

    public static boolean isCloudShadow(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 2));
    }

    public static boolean isWater(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 3));
    }

    public static boolean isGlint(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 4));
    }

    public static boolean isDarkVegetation(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 5));
    }

    public static boolean isNight(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 6));
    }

    public static boolean isUnknown(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 7));
    }

    public static boolean isChannel1Invalid(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 8));
    }

    public static boolean isChannel2Invalid(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 9));
    }

    public static boolean isBrdfCorrIssues(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 14));
    }

    public static boolean isPolar(int srcValue) {
        return (BitSetter.isFlagSet(srcValue, 15));
    }


    public static FlagCoding createLtdrFlagCoding(String flagIdentifier) {
        FlagCoding flagCoding = new FlagCoding(flagIdentifier);
        flagCoding.addFlag("F_SPARE", BitSetter.setFlag(0, SPARE_BIT_INDEX), "SPARE_FLAG");
        flagCoding.addFlag("F_CLOUD", BitSetter.setFlag(0, CLOUD_BIT_INDEX), "CLOUD");
        flagCoding.addFlag("F_CLOUD_SHADOW", BitSetter.setFlag(0, CLOUD_SHADOW_BIT_INDEX), "CLOUD_SHADOW");
        flagCoding.addFlag("F_WATER", BitSetter.setFlag(0, WATER_BIT_INDEX), "WATER");
        flagCoding.addFlag("F_SUN_GLINT", BitSetter.setFlag(0, GLINT_BIT_INDEX), "SUN_GLINT");
        flagCoding.addFlag("F_DARK_VEGETATION", BitSetter.setFlag(0, DARK_VEGETATION_BIT_INDEX), "DARK_VEGETATION");
        flagCoding.addFlag("F_NIGHT", BitSetter.setFlag(0, NIGHT_BIT_INDEX), "NIGHT");
        flagCoding.addFlag("F_UNKNOWN", BitSetter.setFlag(0, UNKNOWN_BIT_INDEX), "unknown (not described)");
        flagCoding.addFlag("F_CHANNEL_1_INVALID", BitSetter.setFlag(0, CHANNEL_1_INVALID_BIT_INDEX), "CHANNEL_1_INVALID");
        flagCoding.addFlag("F_CHANNEL_2_INVALID", BitSetter.setFlag(0, CHANNEL_2_INVALID_BIT_INDEX), "CHANNEL_2_INVALID");
        flagCoding.addFlag("F_BRDF_CORR", BitSetter.setFlag(0, BRDF_CORR_ISSUES_BIT_INDEX), "BRDF_CORR_ISSUES");
        flagCoding.addFlag("F_POLAR", BitSetter.setFlag(0, POLAR_BIT_INDEX), "POLAR");

        return flagCoding;
    }

    public static int setupLtdrBitmasks(int index, Product cloudProduct) {

        int w = cloudProduct.getSceneRasterWidth();
        int h = cloudProduct.getSceneRasterHeight();
        Mask mask;

        mask = Mask.BandMathsType.create("spare_ltdr",
                                         "SPARE_FLAG_ltdr", w, h,
                                         "LTDR_FLAG_snap.F_SPARE",
                                         Color.darkGray, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("cloud_ltdr",
                                         "CLOUD_ltdr", w, h,
                                         "LTDR_FLAG_snap.F_CLOUD",
                                         Color.yellow, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("cloud_shadow_ltdr",
                                         "CLOUD_SHADOW_ltdr", w, h,
                                         "LTDR_FLAG_snap.F_CLOUD_SHADOW",
                                         Color.orange, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("water_ltdr",
                                         "WATER_ltdr", w, h,
                                         "LTDR_FLAG_snap.F_WATER",
                                         Color.blue, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("sun_glint_ltdr",
                                         "SUN_GLINT_ltdr", w, h,
                                         "LTDR_FLAG_snap.F_SUN_GLINT",
                                         Color.pink, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("dark_vegetation_ltdr",
                                         "DARK_VEGETATION_ltdr", w, h,
                                         "LTDR_FLAG_snap.F_DARK_VEGETATION",
                                         Color.green.darker(), 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("night_ltdr",
                                         "NIGHT_ltdr", w, h,
                                         "LTDR_FLAG_snap.F_NIGHT",
                                         Color.black, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("unknown_ltdr",
                                         "unknown_ltdr (not described)", w, h,
                                         "LTDR_FLAG_snap.F_UNKNOWN",
                                         Color.magenta, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("channel_1_invalid_ltdr",
                                         "CHANNEL_1_INVALID_ltdr", w, h,
                                         "LTDR_FLAG_snap.F_CHANNEL_1_INVALID",
                                         Color.red, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("channel_2_invalid_ltdr",
                                         "CHANNEL_2_INVALID_ltdr", w, h,
                                         "LTDR_FLAG_snap.F_CHANNEL_2_INVALID",
                                         Color.red, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("brdf_corr_issues_ltdr",
                                         "BRDF_CORR_ISSUES_ltdr", w, h,
                                         "LTDR_FLAG_snap.F_BRDF_CORR",
                                         Color.green.brighter(), 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);
        mask = Mask.BandMathsType.create("polar_ltdr",
                                         "POLAR_ltdr", w, h,
                                         "LTDR_FLAG_snap.F_POLAR",
                                         Color.cyan, 0.5f);
        cloudProduct.getMaskGroup().add(index++, mask);

        return index;
    }

}

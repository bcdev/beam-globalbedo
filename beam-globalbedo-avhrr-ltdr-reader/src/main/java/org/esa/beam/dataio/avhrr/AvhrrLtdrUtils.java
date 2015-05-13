package org.esa.beam.dataio.avhrr;

import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Datatype;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.util.BitSetter;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;

/**
 * AVHRR LTDR utility methods
 *
 * @author olafd
 */
public class AvhrrLtdrUtils {

    public static String getAttributeValue(Attribute attribute) {
        String result = "";
        switch (attribute.getType().getDatatypeClass()) {
            case Datatype.CLASS_INTEGER:
                int[] ivals = (int[]) attribute.getValue();
                for (int ival : ivals) {
                    result = result.concat(Integer.toString(ival) + " ");
                }
                break;
            case Datatype.CLASS_FLOAT:
                // same type for double and float, we must distinguish by their size
                if (attribute.getType().getDatatypeSize() == 4) {
                    float[] fvals = (float[]) attribute.getValue();
                    for (float fval : fvals) {
                        result = result.concat(Float.toString(fval) + " ");
                    }
                } else if (attribute.getType().getDatatypeSize() == 8) {
                    double[] dvals = (double[]) attribute.getValue();
                    for (double dval : dvals) {
                        result = result.concat(Double.toString(dval) + " ");
                    }
                }
                break;
            case Datatype.CLASS_CHAR:
            case Datatype.CLASS_STRING:
                String[] svals = (String[]) attribute.getValue();
                for (String sval : svals) {
                    result = result.concat(sval + " ");
                }
                break;
            default:
                break;
        }

        return result.trim();
    }

    public static float getFloatAttributeValue(List<Attribute> metadata, String attributeName) {
        float floatAttr = Float.NaN;

        for (Attribute attribute : metadata) {
            if (attribute.getName().equals(attributeName)) {
                try {
                    floatAttr = Float.parseFloat(getAttributeValue(attribute));
                } catch (NumberFormatException e) {
                    BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot parse float attribute: " + e.getMessage());
                }
            }
        }
        return floatAttr;
    }

    public static String[] getStartEndTimeFromAttributes(List<Attribute> metadata) {
        String[] startStopTimes = new String[2];
        String startDate = "";
        String startTime = "";
        String endDate = "";
        String endTime = "";
        for (Attribute attribute : metadata) {
            if (attribute.getName().equals("RangeBeginningDate")) {
                // format is yyyy-ddd
                final String startYearAndDoy = getAttributeValue(attribute);
                final int year = Integer.parseInt(startYearAndDoy.substring(0, 4));
                final int doy = Integer.parseInt(startYearAndDoy.substring(5,8));
                startDate = getDateFromDoy(year, doy);
            } else if (attribute.getName().equals("RangeBeginningTime")) {
                startTime = getAttributeValue(attribute);
            } else if (attribute.getName().equals("RangeEndingDate")) {
                // format is yyyy-ddd
                final String endYearAndDoy = getAttributeValue(attribute);
                final int year = Integer.parseInt(endYearAndDoy.substring(0, 4));
                final int doy = Integer.parseInt(endYearAndDoy.substring(5,8));
                endDate = getDateFromDoy(year, doy);
            } else if (attribute.getName().equals("RangeEndingTime")) {
                endTime = getAttributeValue(attribute);
            }
        }

        // format is 'yyyy-mm-dd hh:mm:ss'
        startStopTimes[0] = startDate + " " + startTime;
        startStopTimes[1] = endDate + " " + endTime;
        return startStopTimes;
    }

    /**
     * Converts a day of year to a datestring in yyyy-mm-dd  format
     *
     * @param year - the year
     * @param doy  - the day of year
     * @return String - the datestring
     */
    static String getDateFromDoy(int year, int doy) {
        final String DATE_FORMAT = "yyyy-MM-dd";
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, doy);
        calendar.set(Calendar.YEAR, year);
        return sdf.format(calendar.getTime());
    }

    public static void addQualityMasks(Product ltdrProduct) {
        ProductNodeGroup<Mask> maskGroup = ltdrProduct.getMaskGroup();
        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_CLOUD_FLAG_NAME,
                AvhrrLtdrConstants.QA_CLOUD_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[0], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_CLOUD_SHADOW_FLAG_NAME,
                AvhrrLtdrConstants.QA_CLOUD_SHADOW_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[1], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_WATER_FLAG_NAME,
                AvhrrLtdrConstants.QA_WATER_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[2], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_SUNGLINT_FLAG_NAME,
                AvhrrLtdrConstants.QA_SUNGLINT_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[3], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_DENSE_DARK_VEGETATION_FLAG_NAME,
                AvhrrLtdrConstants.QA_DENSE_DARK_VEGETATION_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[4], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_NIGHT_FLAG_NAME,
                AvhrrLtdrConstants.QA_NIGHT_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[5], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_VALID_CHANNEL_1to5_FLAG_NAME,
                AvhrrLtdrConstants.QA_VALID_CHANNEL_1to5_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[6], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_INVALID_CHANNEL_1_FLAG_NAME,
                AvhrrLtdrConstants.QA_INVALID_CHANNEL_1_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[7], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_INVALID_CHANNEL_2_FLAG_NAME,
                AvhrrLtdrConstants.QA_INVALID_CHANNEL_2_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[8], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_INVALID_CHANNEL_3_FLAG_NAME,
                AvhrrLtdrConstants.QA_INVALID_CHANNEL_3_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[9], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_INVALID_CHANNEL_4_FLAG_NAME,
                AvhrrLtdrConstants.QA_INVALID_CHANNEL_4_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[10], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_INVALID_CHANNEL_5_FLAG_NAME,
                AvhrrLtdrConstants.QA_INVALID_CHANNEL_5_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[11], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_INVALID_RHO3_FLAG_NAME,
                AvhrrLtdrConstants.QA_INVALID_RHO3_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[12], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_BRDF_CORRECTION_FLAG_NAME,
                AvhrrLtdrConstants.QA_BRDF_CORRECTION_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[13], 0.5f);

        addMask(ltdrProduct, maskGroup, AvhrrLtdrConstants.QA_FLAG_BAND_NAME, AvhrrLtdrConstants.QA_POLAR_FLAG_NAME,
                AvhrrLtdrConstants.QA_POLAR_FLAG_DESCR, AvhrrLtdrConstants.FLAG_COLORS[14], 0.5f);
    }

    public static void addQualityFlags(FlagCoding qaFlagCoding) {
        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_CLOUD_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_CLOUD_BIT_INDEX),
                             AvhrrLtdrConstants.QA_CLOUD_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_CLOUD_SHADOW_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_CLOUD_SHADOW_BIT_INDEX),
                             AvhrrLtdrConstants.QA_CLOUD_SHADOW_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_WATER_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_WATER_BIT_INDEX),
                             AvhrrLtdrConstants.QA_WATER_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_SUNGLINT_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_SUNGLINT_BIT_INDEX),
                             AvhrrLtdrConstants.QA_SUNGLINT_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_DENSE_DARK_VEGETATION_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_DENSE_DARK_VEGETATION_BIT_INDEX),
                             AvhrrLtdrConstants.QA_DENSE_DARK_VEGETATION_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_NIGHT_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_NIGHT_BIT_INDEX),
                             AvhrrLtdrConstants.QA_NIGHT_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_VALID_CHANNEL_1to5_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_VALID_CHANNEL_1to5_BIT_INDEX),
                             AvhrrLtdrConstants.QA_VALID_CHANNEL_1to5_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_INVALID_CHANNEL_1_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_INVALID_CHANNEL_1_BIT_INDEX),
                             AvhrrLtdrConstants.QA_INVALID_CHANNEL_1_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_INVALID_CHANNEL_2_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_INVALID_CHANNEL_2_BIT_INDEX),
                             AvhrrLtdrConstants.QA_INVALID_CHANNEL_2_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_INVALID_CHANNEL_3_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_INVALID_CHANNEL_3_BIT_INDEX),
                             AvhrrLtdrConstants.QA_INVALID_CHANNEL_3_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_INVALID_CHANNEL_4_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_INVALID_CHANNEL_4_BIT_INDEX),
                             AvhrrLtdrConstants.QA_INVALID_CHANNEL_4_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_INVALID_CHANNEL_5_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_INVALID_CHANNEL_5_BIT_INDEX),
                             AvhrrLtdrConstants.QA_INVALID_CHANNEL_5_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_INVALID_RHO3_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_INVALID_RHO3_BIT_INDEX),
                             AvhrrLtdrConstants.QA_INVALID_RHO3_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_BRDF_CORRECTION_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_BRDF_CORRECTION_BIT_INDEX),
                             AvhrrLtdrConstants.QA_BRDF_CORRECTION_FLAG_DESCR);

        qaFlagCoding.addFlag(AvhrrLtdrConstants.QA_POLAR_FLAG_NAME,
                             BitSetter.setFlag(0, AvhrrLtdrConstants.QA_POLAR_BIT_INDEX),
                             AvhrrLtdrConstants.QA_POLAR_FLAG_DESCR);
    }

    private static void addMask(Product mod35Product, ProductNodeGroup<Mask> maskGroup,
                                String bandName, String flagName, String description, Color color, float transparency) {
        int width = mod35Product.getSceneRasterWidth();
        int height = mod35Product.getSceneRasterHeight();
        String maskPrefix = "";
        Mask mask = Mask.BandMathsType.create(maskPrefix + flagName,
                                              description, width, height,
                                              bandName + "." + flagName,
                                              color, transparency);
        maskGroup.add(mask);
    }

}

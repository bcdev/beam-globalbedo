package org.esa.beam.dataio.avhrr;

import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.HObject;
import ncsa.hdf.object.h4.H4Datatype;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.List;
import java.util.logging.Level;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 12.05.2015
 * Time: 15:41
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

}

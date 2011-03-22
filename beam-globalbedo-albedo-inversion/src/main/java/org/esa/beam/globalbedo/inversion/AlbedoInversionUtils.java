package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.experimental.PointOperator;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AlbedoInversionUtils {

    public static List<String> getDailyBBDRFilenames(String[] bbdrFilenames, String daystring) {
        List<String> dailyBBDRFilenames = new ArrayList<String>();
        if (bbdrFilenames != null && bbdrFilenames.length > 0 && StringUtils.isNotNullAndNotEmpty(daystring)) {
            for (String s : bbdrFilenames) {
                if (s.endsWith(".dim") && s.contains(daystring)) {
                    dailyBBDRFilenames.add(s);
                }
            }
        }

        return dailyBBDRFilenames;
    }

    public static String getDateFromDoy(int year, int doy) {
        String DATE_FORMAT = "yyyyMMdd";
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, doy);
        calendar.set(Calendar.YEAR, year);
        return sdf.format(calendar.getTime());
    }

    public static int getDoyFromDate(String datestring) {
        // todo  later if needed
        return -1;
    }

    public static void setLandMaskSourceSample(PointOperator.Configurator configurator, int maskIndex,
                                               Product sourceProduct) {
        if (sourceProduct.getProductType().startsWith("MER")) {
           BandMathsOp landOp = BandMathsOp.createBooleanExpressionBand
                   (AlbedoInversionConstants.merisLandMaskExpression, sourceProduct);
            Product landMaskProduct = landOp.getTargetProduct();
            configurator.defineSample(maskIndex, landMaskProduct.getBandAt(0).getName(), landMaskProduct);
        } else if (sourceProduct.getProductType().startsWith("VGT")) {
            configurator.defineSample(maskIndex, AlbedoInversionConstants.BBDR_VGT_SM_NAME);
        } else {
            // todo: AATSR
        }
    }
}

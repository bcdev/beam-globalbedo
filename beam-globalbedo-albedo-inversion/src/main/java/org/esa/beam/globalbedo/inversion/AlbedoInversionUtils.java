package org.esa.beam.globalbedo.inversion;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.experimental.PointOperator;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Utility class for Albedo Inversion part
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AlbedoInversionUtils {

    /**
     * Filters from a list of BBDR file names the ones which contain a given daystring
     *
     * @param bbdrFilenames - the list of filenames
     * @param daystring  - the daystring
     * @return   List<String> - the filtered list
     */
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

    /**
     * Converts a day of year to a datestring in yyyyMMdd format
     *
     * @param year - the year
     * @param doy - the day of year
     * @return String - the datestring
     */
    public static String getDateFromDoy(int year, int doy) {
        final String DATE_FORMAT = "yyyyMMdd";
        final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, doy);
        calendar.set(Calendar.YEAR, year);
        return sdf.format(calendar.getTime());
    }

    /**
     * Adds a land mask sample definition to a configurator used in a point operator
     *
     * @param configurator - the configurator
     * @param index - the sample index
     * @param sourceProduct - the source product
     */
    public static void setLandMaskSourceSample(PointOperator.Configurator configurator, int index,
                                               Product sourceProduct) {
        if (sourceProduct.getProductType().startsWith("MER")) {
            BandMathsOp landOp = BandMathsOp.createBooleanExpressionBand
                    (AlbedoInversionConstants.merisLandMaskExpression, sourceProduct);
            Product landMaskProduct = landOp.getTargetProduct();
            configurator.defineSample(index, landMaskProduct.getBandAt(0).getName(), landMaskProduct);
        } else if (sourceProduct.getProductType().startsWith("VGT")) {
            configurator.defineSample(index, AlbedoInversionConstants.BBDR_VGT_SM_NAME, sourceProduct);
        } else {
            // todo: AATSR
        }
    }

    /**
     * Returns a matrix which contains on the diagonal the original elements, and zeros elsewhere
     * Input must be a nxm matrix with n = m
     *
     * @param m  - original matrix
     * @return Matrix - the filtered matrix
     */
    public static Matrix getRectangularDiagonalMatrix(Matrix m) {
        if (m.getRowDimension() != m.getColumnDimension()) {
            return null;
        }

        Matrix diag = new Matrix(m.getRowDimension(), m.getRowDimension());
        for (int i = 0; i < m.getRowDimension(); i++) {
            diag.set(i, i, m.get(i, i));
        }
        return diag;
    }

    /**
     * Returns a vector (as nx1 matrix) which contains the diagonal elements of the original matrix
     *
     * @param m - original matrix
     * @return  Matrix - the nx1 result matrix
     */
    public static Matrix getRectangularDiagonalFlatMatrix(Matrix m) {
        if (m.getRowDimension() != m.getColumnDimension()) {
            return null;
        }

        Matrix diagFlat = new Matrix(m.getRowDimension(), 1);
        for (int i = 0; i < m.getRowDimension(); i++) {
            diagFlat.set(i, 0, m.get(i, i));
        }
        return diagFlat;
    }
}

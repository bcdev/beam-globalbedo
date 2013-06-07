package org.esa.beam.globalbedo.inversion.util;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.util.math.MathUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Utility class for Albedo Inversion part
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@SuppressWarnings("StatementWithEmptyBody")
public class AlbedoInversionUtils {

    /**
     * Converts a day of year to a datestring in yyyyMMdd format
     *
     * @param year - the year
     * @param doy  - the day of year
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
     * @param configurator  - the configurator
     * @param index         - the sample index
     * @param sourceProduct - the source product
     */
    public static void setLandMaskSourceSample(SampleConfigurer configurator, int index,
                                               Product sourceProduct, boolean computeSeaice) {
        if (sourceProduct.getProductType().startsWith("MER") || computeSeaice) {
            BandMathsOp landOp = BandMathsOp.createBooleanExpressionBand
                    (AlbedoInversionConstants.merisLandMaskExpression, sourceProduct);
            Product landMaskProduct = landOp.getTargetProduct();
            configurator.defineSample(index, landMaskProduct.getBandAt(0).getName(), landMaskProduct);
        } else if (sourceProduct.getProductType().startsWith("VGT")) {
            configurator.defineSample(index, AlbedoInversionConstants.BBDR_VGT_SM_NAME, sourceProduct);
        } else {
            // AATSR excluded - no actions
        }
    }

    /**
     * Adds a land mask sample definition to a configurator used in a point operator
     *
     * @param configurator  - the configurator
     * @param index         - the sample index
     * @param sourceProduct - the source product
     */
    public static void setSeaiceMaskSourceSample(SampleConfigurer configurator, int index,
                                                 Product sourceProduct) {
        BandMathsOp seaiceOp = BandMathsOp.createBooleanExpressionBand
                (AlbedoInversionConstants.seaiceMaskExpression, sourceProduct);
        Product seaiceProduct = seaiceOp.getTargetProduct();
        configurator.defineSample(index, seaiceProduct.getBandAt(0).getName(), seaiceProduct);
    }

    /**
     * Returns a matrix which contains on the diagonal the original elements, and zeros elsewhere
     * Input must be a nxm matrix with n = m
     *
     * @param m - original matrix
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
     * @return Matrix - the nx1 result matrix
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

    /**
     * Returns a rows x columns matrix with reciprocal elements
     *
     * @param m - the input matrix
     * @return Matrix - the result matrix
     */
    public static Matrix getReciprocalMatrix(Matrix m) {
        final int rows = m.getRowDimension();
        final int cols = m.getColumnDimension();
        Matrix resultM = new Matrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (m.get(i, j) != 0.0) {
                    resultM.set(i, j, 1.0 / m.get(i, j));
                } else {
                    resultM.set(i, j, 0.0);
                }
            }
        }
        return resultM;
    }

    /**
     * Returns the product of all elements of a matrix
     * (equivalent to Python's numpy.product(m))
     *
     * @param m - the input matrix
     * @return double - the result
     */
    public static double getMatrixAllElementsProduct(Matrix m) {
        final int rows = m.getRowDimension();
        final int cols = m.getColumnDimension();
        double result = 1.0;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result *= m.get(i, j);
            }
        }
        return result;
    }

    /**
     * Checks if a matrix contains NaN elements
     *
     * @param m - original matrix
     * @return boolean
     */
    public static boolean matrixHasNanElements(Matrix m) {
        for (int i = 0; i < m.getRowDimension(); i++) {
            for (int j = 0; j < m.getColumnDimension(); j++) {
                if (Double.isNaN(m.get(i, j))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if a matrix contains zeros in diagonale
     * Input must be a nxm matrix with n = m
     *
     * @param m - original matrix
     * @return boolean
     */
    public static boolean matrixHasZerosInDiagonale(Matrix m) {
        for (int i = 0; i < m.getRowDimension(); i++) {
            if (m.get(i, i) == 0.0) {
                return true;
            }
        }
        return false;
    }


    /**
     * Returns the upper left corner of a MODIS tile (e.g. h18v04) in (x,y)-coordinates of sinusoidal projection used
     * in the Globalbedo project. These values represent the easting/northing parameters.
     *
     * @param modisTile - the MODIS tile name
     * @return double[] - the upper left corner coordinate
     * @throws NumberFormatException -
     */
    public static double[] getUpperLeftCornerOfModisTiles(String modisTile) throws NumberFormatException {
        double[] upperLeftPoint = new double[2];

        final int eastingIndex = Integer.parseInt(modisTile.substring(1, 3)) - 18;
        final int northingIndex = 9 - Integer.parseInt(modisTile.substring(4, 6));

        upperLeftPoint[0] = eastingIndex * AlbedoInversionConstants.modisSinusoidalProjectionTileSizeIncrement;
        upperLeftPoint[1] = northingIndex * AlbedoInversionConstants.modisSinusoidalProjectionTileSizeIncrement;

        return upperLeftPoint;
    }

    /**
     * Computes solar zenith angle at local noon as function of Geoposition and DoY
     *
     * @param geoPos - geoposition
     * @param doy    - day of year
     * @return sza - in degrees!!
     */
    public static double computeSza(GeoPos geoPos, int doy) {

        final double latitude = geoPos.getLat() * MathUtils.DTOR;

        // # To emulate MODIS products, set fixed LST = 12.00
        final double LST = 12.0;
        // # Now we can calculate the Sun Zenith Angle (SZArad):
        final double h = (12.0 - (LST)) / 12.0 * Math.PI;
        final double delta = -23.45 * (Math.PI / 180.0) * Math.cos(2 * Math.PI / 365.0 * (doy + 10));
        double SZArad = Math.acos(Math.sin(latitude) * Math.sin(delta) + Math.cos(latitude) * Math.cos(delta) * Math.cos(h));

        return SZArad * MathUtils.RTOD;
    }

    public static Product createDummySourceProduct(int width, int height) {
        Product product = new Product("dummy", "dummy", width, height);
        Band b = product.addBand("b1", ProductData.TYPE_FLOAT32);

        float[] bData = new float[width*height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                bData[i*width + j] = 1.0f;
            }
        }
        b.setDataElems(bData);

        return product;
    }

}

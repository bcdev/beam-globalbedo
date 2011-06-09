package org.esa.beam.globalbedo.inversion.util;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.beam.util.math.MathUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Utility class for Albedo Inversion part
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AlbedoInversionUtils {

    /**
     * Converts a day of year to a datestring in yyyyMMdd format
     *
     * @param year - the year
     * @param doy  - the day of year
     *
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
     * @param m - original matrix
     *
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
     *
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
     *
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
     *
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
     *
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
     *
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
     * // todo: apply this also in BBDR module, then easting/northing parameters will not be needed any more
     *
     * @param modisTile - the MODIS tile name
     *
     * @return double[] - the upper left corner coordinate
     *
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
     * This method applies sinusoidal projection on a source product.
     *
     * @param sourceProduct - the source product
     * @param easting - easting value
     * @param northing - northing value
     *
     * @return reprojected product
     */
    public static Product reprojectToSinusoidal(Product sourceProduct, double easting, double northing) {
        ReprojectionOp repro = new ReprojectionOp();
        repro.setParameter("easting", easting);
        repro.setParameter("northing", northing);

        repro.setParameter("crs", "PROJCS[\"MODIS Sinusoidal\"," +
                                  "GEOGCS[\"WGS 84\"," +
                                  "  DATUM[\"WGS_1984\"," +
                                  "    SPHEROID[\"WGS 84\",6378137,298.257223563," +
                                  "      AUTHORITY[\"EPSG\",\"7030\"]]," +
                                  "    AUTHORITY[\"EPSG\",\"6326\"]]," +
                                  "  PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]]," +
                                  "  UNIT[\"degree\",0.01745329251994328,AUTHORITY[\"EPSG\",\"9122\"]]," +
                                  "   AUTHORITY[\"EPSG\",\"4326\"]]," +
                                  "PROJECTION[\"Sinusoidal\"]," +
                                  "PARAMETER[\"false_easting\",0.0]," +
                                  "PARAMETER[\"false_northing\",0.0]," +
                                  "PARAMETER[\"central_meridian\",0.0]," +
                                  "PARAMETER[\"semi_major\",6371007.181]," +
                                  "PARAMETER[\"semi_minor\",6371007.181]," +
                                  "UNIT[\"m\",1.0]," +
                                  "AUTHORITY[\"SR-ORG\",\"6974\"]]");

        repro.setParameter("resampling", "Nearest");
        repro.setParameter("includeTiePointGrids", false);
        repro.setParameter("referencePixelX", 0.0);
        repro.setParameter("referencePixelY", 0.0);
        repro.setParameter("orientation", 0.0);
        repro.setParameter("pixelSizeX", 926.6254330558);
        repro.setParameter("pixelSizeY", 926.6254330558);
        repro.setParameter("width", AlbedoInversionConstants.MODIS_TILE_WIDTH);
        repro.setParameter("height", AlbedoInversionConstants.MODIS_TILE_HEIGHT);
        repro.setParameter("orthorectify", true);
        repro.setParameter("noDataValue", 0.0);
        repro.setSourceProduct(sourceProduct);
        return repro.getTargetProduct();
    }

    /**
     * Computes solar zenith angle at local noon as function of Geoposition and DoY
     *
     * @param geoPos - geoposition
     * @param doy - day of year
     * @return  sza - in degrees!!
     */
    public static double computeSza(GeoPos geoPos, int doy) {

        final double latitude = geoPos.getLat() * MathUtils.DTOR;
        double longitude = geoPos.getLon();

        // # To emulate MODIS products, set fixed LST = 12.00
        final double LST = 12.0;
        longitude *= MathUtils.DTOR;
        // # Now we can calculate the Sun Zenith Angle (SZArad):
        final double h = (12.0 - (LST)) / 12.0 * Math.PI;
        final double delta = -23.45 * (Math.PI/180.0) * Math.cos (2 * Math.PI/365.0 * (doy+10));
        double SZArad = Math.acos(Math.sin(latitude) * Math.sin(delta) + Math.cos(latitude) * Math.cos(delta) * Math.cos(h));

        final double SZAdeg = SZArad * MathUtils.RTOD;
        return SZAdeg;
    }
}

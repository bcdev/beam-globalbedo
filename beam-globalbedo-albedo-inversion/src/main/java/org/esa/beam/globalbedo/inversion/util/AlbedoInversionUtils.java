package org.esa.beam.globalbedo.inversion.util;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.experimental.PointOperator;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;

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
     * Returns a rows x columns matrix with constant elements
     *
     * @param rows                - the matrix row dimension
     * @param columns             - the matrix column dimension
     * @param constantValue - the constant value to set
     *
     * @return Matrix - the constant matrix
     */
    public static Matrix getConstantMatrix(int rows, int columns, double constantValue) {
        Matrix m = new Matrix(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                m.set(i, j, constantValue);
            }
        }
        return m;
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
     * @param modisTile
     *
     * @return
     *
     * @throws NumberFormatException
     */
    public static double[] getUpperLeftCornerOfModisTiles(String modisTile) throws NumberFormatException {
        double[] upperLeftPoint = new double[2];

        final int eastingIndex = Integer.parseInt(modisTile.substring(1, 3)) - 18;
        final int northingIndex = 9 - Integer.parseInt(modisTile.substring(4, 6));

        upperLeftPoint[0] = eastingIndex * AlbedoInversionConstants.modisSinusoidalProjectionTileSizeIncrement;
        upperLeftPoint[1] = northingIndex * AlbedoInversionConstants.modisSinusoidalProjectionTileSizeIncrement;

        return upperLeftPoint;
    }

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
        repro.setParameter("width", 1200);
        repro.setParameter("height", 1200);
        repro.setParameter("orthorectify", true);
        repro.setParameter("noDataValue", 0.0);
        repro.setSourceProduct(sourceProduct);
        return repro.getTargetProduct();
    }

    public static int getDoyFromPriorName(String priorName) {
        int doy;
        if (priorName.startsWith("Kernels_")) {
            doy = Integer.parseInt(priorName.substring(8, 11));
            if (Math.abs(doy) > 366) {
                throw new IllegalArgumentException("Invalid doy " + doy + " retrieved from prior name " + priorName);
            }
            return doy;
        } else {
            throw new IllegalArgumentException("Invalid prior name " + priorName);
        }
    }
}

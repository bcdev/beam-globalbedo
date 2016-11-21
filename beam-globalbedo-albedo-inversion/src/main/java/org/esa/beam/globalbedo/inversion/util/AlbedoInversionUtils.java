package org.esa.beam.globalbedo.inversion.util;

import Jama.Matrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.auxdata.ModisTileCoordinates;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.beam.util.math.MathUtils;

import java.awt.image.Raster;
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
        return getDateFromDoy(year, doy, "yyyyMMdd");
    }

    /**
     * Converts a day of year to a datestring
     *
     * @param year - the year
     * @param doy  - the day of year
     * @param dateFormat  - the date format (such as yyyyMMdd or yyyy-MM-dd)
     * @return String - the datestring
     */
    public static String getDateFromDoy(int year, int doy, String dateFormat) {
        final SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, doy);
        calendar.set(Calendar.YEAR, year);
        return sdf.format(calendar.getTime());
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

        float[] bData = new float[width * height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                bData[i * width + j] = 1.0f;
            }
        }
//        b.setDataElems(bData); // deprecated
        b.setRasterData(ProductData.createInstance(bData));
        product.setPreferredTileSize(product.getSceneRasterWidth(), Math.min(45, product.getSceneRasterHeight()));

        return product;
    }

    public static float[][] getMonthlyWeighting() {
        final int[] startingDoy = new int[]{1, 32, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335};
        final int[] nDays = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

        int[] eightDayTimePeriod = new int[46];
        int[] eightDayTimePeriodExtended = new int[47];
        for (int i = 0; i < eightDayTimePeriod.length; i++) {
            eightDayTimePeriod[i] = 1 + 8 * i;
            eightDayTimePeriodExtended[i] = 1 + 8 * i;
        }
        eightDayTimePeriodExtended[46] = 369;

        int[] daysInYear = new int[365];
        for (int i = 0; i < daysInYear.length; i++) {
            daysInYear[i] = 1 + i;
        }
        float[][] monthlyWeighting = new float[12][daysInYear.length];  // the result array

        float[][] weight = new float[47][365];
        int[] deltaTime = new int[365];
        for (int i = 0; i < eightDayTimePeriodExtended.length; i++) {
            for (int j = 0; j < deltaTime.length; j++) {
                deltaTime[j] = daysInYear[j] - eightDayTimePeriodExtended[i];
                weight[i][j] = getWeight(deltaTime[j]);
            }
        }

        int j = 0;
        for (final int startingDayInMonth : startingDoy) {
            final int numberOfDaysInMonth = nDays[j];
            for (final int day : daysInYear) {
                float nd = 0.0f;
                float sum = 0.0f;
                for (final int doy : eightDayTimePeriod) {
//                    if (doy >= startingDayInMonth - 8 && doy <= startingDayInMonth + numberOfDaysInMonth + 8) {
                    if (doy >= startingDayInMonth && doy <= startingDayInMonth + numberOfDaysInMonth) {
                        float monthlyWeight = 1.0f;
                        if (doy >= startingDayInMonth + numberOfDaysInMonth - 8) {
                            final float distance = (startingDayInMonth + numberOfDaysInMonth - doy) / 8.0f;
                            monthlyWeight = distance * 0.5f + 0.5f;
                        }
//                        if (doy <= startingDayInMonth + 8) {
                        if (doy <= startingDayInMonth) {
//                            final float distance = (startingDayInMonth + 8 - doy) / 8.0f;
                            final float distance = (startingDayInMonth - doy) / 8.0f;
                            monthlyWeight = distance * 0.5f + 0.5f;
                        }
                        nd += monthlyWeight;
//                        sum += weight[(doy + 8 - 1) / 8][day - 1] * monthlyWeight;
                        sum += weight[(doy - 1) / 8][day - 1] * monthlyWeight;
                    }
                }
                monthlyWeighting[j][day - 1] = sum / nd;
            }
            j++;
        }
        return monthlyWeighting;
    }

    public static RealMatrix getRealMatrixFromJamaMatrix(Matrix m) {
        final int rDim = m.getRowDimension();
        final int cDim = m.getColumnDimension();
        RealMatrix rm = new Array2DRowRealMatrix(rDim, cDim);
        for (int i = 0; i < rDim; i++) {
            for (int j = 0; j < cDim; j++) {
                rm.setEntry(i, j, m.get(i, j));
            }
        }

        return rm;
    }

    public static boolean isValid(float srcValue) {
        return (srcValue != AlbedoInversionConstants.NO_DATA_VALUE) && !Float.isNaN(srcValue);
    }

    public static boolean isValid(double srcValue) {
        return (srcValue != (double) AlbedoInversionConstants.NO_DATA_VALUE) && !Double.isNaN(srcValue);
    }

    public static float getWeight(int dayDifference) {
        return (float) Math.exp(-1.0 * Math.abs(dayDifference) / AlbedoInversionConstants.HALFLIFE);
    }

    public static boolean isValidCMatrix(Matrix c) {
        for (int i = 0; i <3; i++) {
            for (int j = 0; j < 3; j++) {
                if (c.get(i, j) != AlbedoInversionConstants.NO_DATA_VALUE) {
                    return true;
                }
            }
        }
        return false;
    }

    public static GeoPos getLatLonFromProduct(Product inputProduct) {
        final Band latBand = inputProduct.getBand(AlbedoInversionConstants.LAT_BAND_NAME);
        final Raster latData = latBand.getSourceImage().getData();
        final float latitude = latData.getSampleFloat(0, 0, 0);

        final Band lonBand = inputProduct.getBand(AlbedoInversionConstants.LON_BAND_NAME);
        final Raster lonData = lonBand.getSourceImage().getData();
        final float longitude = lonData.getSampleFloat(0, 0, 0);

        return new GeoPos(latitude, longitude);
    }

    public static Product reprojectToModisTile(Product bbdrProduct, String tileName, String resampling, double scaleFactor) {
        ModisTileCoordinates modisTileCoordinates = ModisTileCoordinates.getInstance();
        int tileIndex = modisTileCoordinates.findTileIndex(tileName);
        if (tileIndex == -1) {
            throw new OperatorException("Found no tileIndex for tileName=''" + tileName + "");
        }
        double easting = modisTileCoordinates.getUpperLeftX(tileIndex);
        double northing = modisTileCoordinates.getUpperLeftY(tileIndex);

        ReprojectionOp repro = new ReprojectionOp();
        repro.setParameterDefaultValues();
        repro.setParameter("easting", easting);
        repro.setParameter("northing", northing);
        repro.setParameter("crs", AlbedoInversionConstants.MODIS_SIN_PROJECTION_CRS_STRING);
        repro.setParameter("resampling", resampling);
        repro.setParameter("includeTiePointGrids", false);
        repro.setParameter("referencePixelX", 0.0);
        repro.setParameter("referencePixelY", 0.0);
        repro.setParameter("orientation", 0.0);

        // scale factor > 1 increases pixel size and decreases number of pixels;
        final double pixelSizeX = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_X * scaleFactor;
        final double pixelSizeY = AlbedoInversionConstants.MODIS_SIN_PROJECTION_PIXEL_SIZE_Y * scaleFactor;
        final int width = (int) (AlbedoInversionConstants.MODIS_TILE_WIDTH/scaleFactor);
        final int height = (int) (AlbedoInversionConstants.MODIS_TILE_HEIGHT/scaleFactor);

        repro.setParameter("pixelSizeX", pixelSizeX);
        repro.setParameter("pixelSizeY", pixelSizeY);
        repro.setParameter("width", width);
        repro.setParameter("height", height);

        repro.setParameter("orthorectify", true);
        repro.setParameter("noDataValue", 0.0);
        repro.setSourceProduct(bbdrProduct);
        return repro.getTargetProduct();
    }

    // activate for debug purposes
//    public static void printAccumulatorMatrices(Accumulator acc) {
//        BeamLogManager.getSystemLogger().log(Level.INFO, "printing M... ");
//        for (int i = 0; i < acc.getM().getRowDimension(); i++) {
//            for (int j = 0; j < acc.getM().getColumnDimension(); j++) {
//                BeamLogManager.getSystemLogger().log(Level.INFO,
//                                                     "i,j,M(i,j) = " + i + "," + j + "," + acc.getM().get(i, j));
//            }
//        }
//
//        BeamLogManager.getSystemLogger().log(Level.INFO, "printing V... ");
//        for (int i = 0; i < acc.getV().getRowDimension(); i++) {
//            for (int j = 0; j < acc.getV().getColumnDimension(); j++) {
//                BeamLogManager.getSystemLogger().log(Level.INFO,
//                                                     "i,j,V(i,j) = " + i + "," + j + "," + acc.getV().get(i, j));
//            }
//        }
//
//        BeamLogManager.getSystemLogger().log(Level.INFO, "printing E... ");
//        for (int i = 0; i < acc.getE().getRowDimension(); i++) {
//            for (int j = 0; j < acc.getE().getColumnDimension(); j++) {
//                BeamLogManager.getSystemLogger().log(Level.INFO,
//                                                     "i,j,E(i,j) = " + i + "," + j + "," + acc.getE().get(i, j));
//            }
//        }
//
//        BeamLogManager.getSystemLogger().log(Level.INFO, "printing mask... ");
//        BeamLogManager.getSystemLogger().log(Level.INFO, "acc.getMask() = " + acc.getMask());
//    }

}

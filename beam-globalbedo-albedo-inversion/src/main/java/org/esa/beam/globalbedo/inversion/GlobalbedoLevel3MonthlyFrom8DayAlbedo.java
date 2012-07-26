package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 'Master' operator for getting monthly albedos from 8-day periods
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.albedo.monthly")
public class GlobalbedoLevel3MonthlyFrom8DayAlbedo extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "1", interval = "[1,12]", description = "Month index")
    private int monthIndex;


    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();
//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        // STEP 1: get Albedo 8-day input files...
        final String albedoDir = gaRootDir + File.separator + "Albedo" + File.separator + tile + File.separator;

        Product[] albedo8DayProduct = IOUtils.getAlbedo8DayProducts(albedoDir, tile);
        if (albedo8DayProduct != null && albedo8DayProduct.length > 0) {

            // STEP 2: get monthly weighting...
            float[][] monthlyWeighting = getMonthlyWeighting();

            // STEP 3: get Albedo monthly product...

            MonthlyFrom8DayAlbedoOp monthlyAlbedoOp = new MonthlyFrom8DayAlbedoOp();
            monthlyAlbedoOp.setSourceProducts(albedo8DayProduct);
            monthlyAlbedoOp.setParameter("monthlyWeighting", monthlyWeighting);
            monthlyAlbedoOp.setParameter("monthIndex", monthIndex);
            setTargetProduct(monthlyAlbedoOp.getTargetProduct());

            logger.log(Level.ALL, "Finished monthly albedo computation process for tile: " + tile + ", year: " + year +
                    ", month: " + monthIndex);
        } else {
            logger.log(Level.WARNING, "No monthly albedos computated for tile: " + tile + ", year: " + year +
                    ", month: " + monthIndex);
        }
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
                weight[i][j] = (float) Math.exp(-1.0 * Math.abs(deltaTime[j]) / AlbedoInversionConstants.HALFLIFE);
            }
        }

        int j = 0;
        for (final int startingDayInMonth : startingDoy) {
            final int numberOfDaysInMonth = nDays[j];
            for (final int day : daysInYear) {
                float nd = 0.0f;
                float sum = 0.0f;
                for (final int doy : eightDayTimePeriod) {
                    if (doy >= startingDayInMonth - 8 && doy <= startingDayInMonth + numberOfDaysInMonth + 8) {
                        float monthlyWeight = 1.0f;
                        if (doy >= startingDayInMonth + numberOfDaysInMonth - 8) {
                            final float distance = (startingDayInMonth + numberOfDaysInMonth - doy) / 8.0f;
                            monthlyWeight = distance * 0.5f + 0.5f;
                        }
                        if (doy <= startingDayInMonth + 8) {
                            final float distance = (startingDayInMonth + 8 - doy) / 8.0f;
                            monthlyWeight = distance * 0.5f + 0.5f;
                        }
                        nd += monthlyWeight;
                        sum += weight[(doy + 8 - 1) / 8][day - 1] * monthlyWeight;
                    }
                }
                monthlyWeighting[j][day - 1] = sum / nd;
//                System.out.println("ii, jj, sum, ND, monthlyWeighting[ii, jj]: " +
//                        ii + ", " + jj + ", " + sum + ", " +
//                        ", " + nd + ", " + ", " + monthlyWeighting[ii][jj]);
            }
            j++;
        }
        return monthlyWeighting;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3MonthlyFrom8DayAlbedo.class);
        }
    }

}

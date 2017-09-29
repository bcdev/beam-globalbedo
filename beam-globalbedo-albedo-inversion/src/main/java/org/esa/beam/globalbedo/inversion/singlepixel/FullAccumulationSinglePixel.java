package org.esa.beam.globalbedo.inversion.singlepixel;

import Jama.Matrix;
import org.esa.beam.globalbedo.inversion.Accumulator;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.FullAccumulator;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.logging.Level;

/**
 * Full accumulation for single pixel mode.
 *
 * @author olafd
 */
public class FullAccumulationSinglePixel {

    private int year;
    private int doy;

    /**
     * FullAccumulationSinglePixel constructor
     *
     * @param year - reference year
     * @param doy - reference doy
     */
    public FullAccumulationSinglePixel(int year, int doy) {
        this.year = year;
        this.doy = doy;
    }

    /**
     * Performs the full accumulation
     *
     * @param dailyAccs - the daily accumulators
     * @return - the full accumulator
     */
    public FullAccumulator accumulate(Accumulator[] dailyAccs) {
        final int numDailyAccs = dailyAccs.length;

        float[][] daysToTheClosestSample = new float[1][1];
        Accumulator[][] tmpAcc = new Accumulator[1][1];

        tmpAcc[0][0] = Accumulator.createZeroAccumulator();
        daysToTheClosestSample[0][0] = Integer.MAX_VALUE;


        for (int iDay = -90; iDay < numDailyAccs-90; iDay++) {   // we have 90 + 365 + 90
            int currentYear = year;
            int currentDay = iDay;          // currentDay is in [0,365] !!
            if (iDay < 0) {
                currentYear--;
                currentDay = iDay + 365;
            } else if (iDay > 365) {
                currentYear++;
                currentDay = iDay - 365;
            }

            BeamLogManager.getSystemLogger().log(Level.FINEST, "Full accumulation for year/day:  " +
                    currentYear + "/" + IOUtils.getDoyString(currentDay) + " ...");

//            int dayDifference = getDayDifference(currentDay);  // this is wrong i.e. for wing years!!
            int dayDifference = IOUtils.getDayDifference(currentDay, currentYear, doy, year);
            final float weight = AlbedoInversionUtils.getWeight(dayDifference);

            final Matrix dailyAccM = dailyAccs[iDay+90].getM();
            final Matrix dailyAccV = dailyAccs[iDay+90].getV();
            final Matrix dailyAccE = dailyAccs[iDay+90].getE();
            final double dailyAccMask = dailyAccs[iDay+90].getMask();
//            final Matrix dailyAccM = AlbedoInversionUtils.getMatrix2DTruncated(dailyAccs[iDay+90].getM());
//            final Matrix dailyAccV = AlbedoInversionUtils.getMatrix2DTruncated(dailyAccs[iDay+90].getV());
//            final Matrix dailyAccE = AlbedoInversionUtils.getMatrix2DTruncated(dailyAccs[iDay+90].getE());
            tmpAcc[0][0].setM(tmpAcc[0][0].getM().plus(dailyAccM.times(weight)));
            tmpAcc[0][0].setV(tmpAcc[0][0].getV().plus(dailyAccV.times(weight)));
            tmpAcc[0][0].setE(tmpAcc[0][0].getE().plus(dailyAccE.times(weight)));
            tmpAcc[0][0].setMask(tmpAcc[0][0].getMask() + dailyAccMask*weight);

            // now update doy of closest sample...
            if (dailyAccMask > 0.0 ) {
                final float value = (float) (Math.abs(dayDifference) + 1);
                if (value < daysToTheClosestSample[0][0]) {
                    daysToTheClosestSample[0][0] = (float) (Math.abs(dayDifference) + 1);
                }
            }
        }

        return new FullAccumulator(year, doy, tmpAcc, daysToTheClosestSample);
    }

    private int getDayDifference(int dailAccDay) {
        int referenceYear = year;
        if (dailAccDay < 90) {
            referenceYear--;
        } else if (dailAccDay > 90 + 365) {
            referenceYear++;
        }
//        final int difference = 365 * (year - referenceYear) + (doy - dailAccDay);
        final int difference = 365 * (year - referenceYear) + ((doy+8) - dailAccDay); // this is as in old code
        return Math.abs(difference);
    }

    private int getDayDifference(int dailAccYear, int dailAccDay) {
        return 365 * (year - dailAccYear) + (doy - dailAccDay);
    }

}

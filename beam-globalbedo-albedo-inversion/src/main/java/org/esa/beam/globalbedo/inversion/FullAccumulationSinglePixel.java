package org.esa.beam.globalbedo.inversion;

import Jama.Matrix;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.logging.Level;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 08.02.2016
 * Time: 15:42
 *
 * @author olafd
 */
public class FullAccumulationSinglePixel {

    private int year;
    private int doy;

    public FullAccumulationSinglePixel(int year, int doy) {
        this.year = year;
        this.doy = doy;
    }


    public FullAccumulator accumulate(Accumulator[] allDailyAccs) {
        final int numDailyAccs = allDailyAccs.length;

        float[][] daysToTheClosestSample = new float[1][1];
        Accumulator[][] tmpAcc = new Accumulator[1][1];

        tmpAcc[0][0] = Accumulator.createZeroAccumulator();
        daysToTheClosestSample[0][0] = Integer.MAX_VALUE;


        for (int iDay = -90; iDay < numDailyAccs-90; iDay++) {   // we have 90 + 365 + 90
            if (iDay == 121) {
                System.out.println("iDay = " + iDay);
            }

            int currentYear = year;
            int currentDay;          // currentDay is in [0,365] !!
            if (iDay < 0) {
                currentYear--;
                currentDay = iDay + 365;
            } else if (iDay > 365) {
                currentYear++;
                currentDay = iDay - 365;
            } else {
                currentDay = iDay;
            }

            BeamLogManager.getSystemLogger().log(Level.INFO, "Full accumulation for year/day:  " +
                    currentYear + "/" + IOUtils.getDoyString(currentDay) + " ...");

            int dayDifference = getDayDifference(currentDay);
            final float weight = getWeight(dayDifference);

            final Matrix dailyAccM = allDailyAccs[iDay+90].getM();
            final Matrix dailyAccV = allDailyAccs[iDay+90].getV();
            final Matrix dailyAccE = allDailyAccs[iDay+90].getE();
            final double dailyAccMask = allDailyAccs[iDay+90].getMask();
//            final Matrix dailyAccM = AlbedoInversionUtils.getMatrix2DTruncated(allDailyAccs[iDay+90].getM());
//            final Matrix dailyAccV = AlbedoInversionUtils.getMatrix2DTruncated(allDailyAccs[iDay+90].getV());
//            final Matrix dailyAccE = AlbedoInversionUtils.getMatrix2DTruncated(allDailyAccs[iDay+90].getE());
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
        final int difference = 365 * (year - referenceYear) + ((doy+8) - dailAccDay); // this is like in old code
        return Math.abs(difference);
    }

    private float getWeight(int dayDifference) {
        return (float) Math.exp(-1.0 * Math.abs(dayDifference) / AlbedoInversionConstants.HALFLIFE);
    }

}

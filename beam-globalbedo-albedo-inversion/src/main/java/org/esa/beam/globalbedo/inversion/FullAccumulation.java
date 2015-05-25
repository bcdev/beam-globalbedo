package org.esa.beam.globalbedo.inversion;

/**
 * New class for Full accumulation replacing the old version to avoid massive file I/O.
 * Reduces to just a few utility methods.
 *
 * @author olafd
 */
public class FullAccumulation {

    public static final double HALFLIFE = 11.54;

    public static float getWeight(int dayOffset) {
        return (float) Math.exp(-1.0 * Math.abs(dayOffset) / HALFLIFE);
    }

    public static float updateDayOfClosestSample(float dayOfClosestSampleOld,
                                                 double mask,
                                                 int doyOffset,
                                                 int singleDay) {
        // this is done at the end of 'Accumulator' routine in breadboard...
        float dayOfClosestSample;
        float doy;
        final float bbdrDaysToDoY = (float) (Math.abs(doyOffset) + 1);
        if (singleDay == 0) {
            if (mask > 0.0) {
                doy = bbdrDaysToDoY;
            } else {
                doy = dayOfClosestSampleOld;
            }
        } else {
            if (mask > 0 && dayOfClosestSampleOld == 0.0f) {
                doy = bbdrDaysToDoY;
            } else {
                doy = dayOfClosestSampleOld;
            }
            if (mask > 0 && doy > 0) {
                doy = Math.min(bbdrDaysToDoY, doy);
            } else {
                doy = dayOfClosestSampleOld;
            }
        }
        dayOfClosestSample = doy;
        return dayOfClosestSample;
    }
}

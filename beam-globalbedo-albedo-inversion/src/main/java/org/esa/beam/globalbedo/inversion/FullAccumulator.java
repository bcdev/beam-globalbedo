package org.esa.beam.globalbedo.inversion;

/**
 * Class representing a 'full' 8-day accumulator, holding M, V, E (sumMatrices) and
 * daysToTheClosestSample.
 *
 * @author Olaf Danne
 */
public class FullAccumulator {
    private int year;
    private int doy;
    private Accumulator[][] accumulator;
    private float[][][] sumMatrices;
    private float[][] daysToTheClosestSample;

    /**
     * 'full' accumulator object
     *
     * @param year - reference year
     * @param doy  - reference DoY (001, 009,...,361 in year)
     * @param sumMatrices - accumulation matrix elements per pixel (x,y) and each of the 8 days per reference DoY period
     * @param daysToTheClosestSample - for given DoY, day difference to closest sample per pixel (x,y)
     */
    public FullAccumulator(int year,
                           int doy,
                           float[][][] sumMatrices,
                           float[][] daysToTheClosestSample) {
        this.year = year;
        this.doy = doy;
        this.sumMatrices = sumMatrices;
        this.daysToTheClosestSample = daysToTheClosestSample;
    }

    /**
     * 'full' accumulator object
     *
     * @param year - reference year
     * @param doy  - reference DoY (001, 009,...,361 in year)
     * @param accumulator - accumulation matrix per pixel (x,y) and each of the 8 days per reference DoY period
     * @param daysToTheClosestSample - for given DoY, day difference to closest sample per pixel (x,y)
     */
    public FullAccumulator(int year,
                           int doy,
                           Accumulator[][] accumulator,
                           float[][] daysToTheClosestSample) {
        this.year = year;
        this.doy = doy;
        this.accumulator = accumulator;
        this.daysToTheClosestSample = daysToTheClosestSample;
    }


    public int getYear() {
        return year;
    }

    public int getDoy() {
        return doy;
    }

    public Accumulator[][] getAccumulator() {
        return accumulator;
    }

    public float[][][] getSumMatrices() {
        return sumMatrices;
    }

    public float[][] getDaysToTheClosestSample() {
        return daysToTheClosestSample;
    }

}

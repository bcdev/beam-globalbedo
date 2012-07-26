package org.esa.beam.globalbedo.inversion;

/**
 * Class representing a 'full' 8-day accumulator, holding M, V, E (sumMatrices) and
 * daysToTheClosestSample.
 * This class is not yet used, but should be used later as output from 'GlobAlbedoFullAccumulation'
 * and as input for Inversion (instead of reading the daily accumulators multiple times and
 * doing the accumulation there)
 * <p/>
 * Created by IntelliJ IDEA.
 * User: olafd
 * Date: 19.06.11
 * Time: 16:52
 * To change this template use File | Settings | File Templates.
 */
public class FullAccumulator {
    private int year;
    private int doy;
    private float[][][] sumMatrices;
    private float[][] daysToTheClosestSample;

    public FullAccumulator(int year, int doy,
                           float[][][] sumMatrices, float[][] daysToTheClosestSample) {
        this.year = year;
        this.doy = doy;
        this.sumMatrices = sumMatrices;
        this.daysToTheClosestSample = daysToTheClosestSample;
    }

    public int getYear() {
        return year;
    }

    public int getDoy() {
        return doy;
    }

    public float[][][] getSumMatrices() {
        return sumMatrices;
    }

    public float[][] getDaysToTheClosestSample() {
        return daysToTheClosestSample;
    }

}

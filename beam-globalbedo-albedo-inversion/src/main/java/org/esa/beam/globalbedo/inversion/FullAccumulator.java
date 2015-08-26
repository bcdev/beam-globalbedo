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

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

    public void setDaysToTheClosestSample(float[][] daysToTheClosestSample) {
        this.daysToTheClosestSample = daysToTheClosestSample;
    }

    public void setSumMatrixElement(int elementIndex, float[][] sumMatrixElement) {
        sumMatrices[elementIndex] = sumMatrixElement;
    }

    public void accumulateSumMatrixElement(int elementIndex, float[][] sumMatrixElement) {
        final int size1 = sumMatrixElement.length;
        final int size2 = sumMatrixElement[0].length;
        if (size1 != sumMatrices[elementIndex].length || size2 != sumMatrices[elementIndex][0].length) {
            return;
        }

        for (int i=0; i<size1; i++) {
            for (int j=0; j<size2; j++) {
               sumMatrices[elementIndex][i][j] += sumMatrixElement[i][j];
            }
        }
    }


}

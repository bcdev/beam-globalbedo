package org.esa.beam.globalbedo.inversion;

/**
 * Class representing a 'full' 8-day accumulator, holding M, V, E (sumMatrices) and
 * daysToTheClosestSample.
 * This class is not yet used, but should be used later as output from 'GlobAlbedoFullAccumulation'
 * and as input for Inversion (instead of reading the daily accumulators multiple times and
 * doing the accumulation there)
 *
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
    private int[][] daysToTheClosestSample;
    private float[][][] resultArray;

    private static final int RASTER_WIDTH = AlbedoInversionConstants.MODIS_TILE_WIDTH;
    private static final int RASTER_HEIGHT = AlbedoInversionConstants.MODIS_TILE_HEIGHT;

    public FullAccumulator(int year, int doy,
                            float[][][] sumMatrices, int[][] daysToTheClosestSample) {
        this.year = year;
        this.doy = doy;
        this.sumMatrices = sumMatrices;
        this.daysToTheClosestSample = daysToTheClosestSample;
        fillBinaryResultArray();
    }

    private void fillBinaryResultArray() {
        resultArray = new float[sumMatrices.length + 1][RASTER_WIDTH][RASTER_HEIGHT];
        for (int k = 0; k < sumMatrices.length; k++) {
            for (int x = 0; x < RASTER_WIDTH; x++) {
                for (int y = 0; y < RASTER_HEIGHT; y++) {
                    resultArray[k][x][y] = sumMatrices[k][x][y];
                }
            }
        }

        for (int x = 0; x < RASTER_WIDTH; x++) {
            for (int y = 0; y < RASTER_HEIGHT; y++) {
                resultArray[sumMatrices.length + 1][x][y] = (float) daysToTheClosestSample[x][y];
            }
        }
    }

    public float[][][] getResultArray() {
        return resultArray;
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

    public int[][] getDaysToTheClosestSample() {
        return daysToTheClosestSample;
    }
}

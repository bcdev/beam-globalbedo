package org.esa.beam.globalbedo.inversion.util;

import Jama.Matrix;

import junit.framework.TestCase;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;

public class DailyAccumulationTest extends TestCase {

    public void testGetDiagonalMatrix() {
        Matrix m = new Matrix(3, 3);
        m.set(0, 0, 2.0);
        m.set(1, 0, 4.0);
        m.set(2, 0, 6.0);
        m.set(0, 1, 8.0);
        m.set(1, 1, 3.0);
        m.set(2, 1, 5.0);
        m.set(0, 2, 7.0);
        m.set(1, 2, 9.0);
        m.set(2, 2, 12.0);

        Matrix diag = DailyAccumulationUtils.getRectangularDiagonalMatrix(m);
        assertNotNull(diag);
        assertEquals(3, diag.getRowDimension());
        assertEquals(3, diag.getColumnDimension());
        assertEquals(2.0, diag.get(0, 0));
        assertEquals(0.0, diag.get(1, 0));
        assertEquals(0.0, diag.get(2, 0));
        assertEquals(0.0, diag.get(0, 1));
        assertEquals(3.0, diag.get(1, 1));
        assertEquals(0.0, diag.get(2, 1));
        assertEquals(0.0, diag.get(0, 2));
        assertEquals(0.0, diag.get(1, 2));
        assertEquals(12.0, diag.get(2, 2));
    }

    public void testIsAccumulatorInputInvalid() {
        Matrix bbdr = new Matrix(3, 1);
        bbdr.set(0, 0, 0.8);
        bbdr.set(1, 0, 0.6);
        bbdr.set(2, 0, 0.4);

        double[] stdev = new double[]{0.1, 0.3, 0.2};
        double[] correl = new double[]{0.4, 0.6, 0.8};

        Matrix kernels = new Matrix(AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS,
                                    3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS);

        kernels.set(0, 0, 1.0);
        kernels.set(1, 3, 1.0);
        kernels.set(2, 6, 1.0);
        kernels.set(0, 1, 1.0);
        kernels.set(1, 4, 1.0);
        kernels.set(2, 7, 1.0);
        kernels.set(0, 2, 1.0);
        kernels.set(1, 5, 1.0);
        kernels.set(2, 8, 1.0);

        assertFalse(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));

        bbdr.set(1, 0, Double.NaN);
        assertTrue(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));
        bbdr.set(1, 0, 2.0);
        assertTrue(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));
        bbdr.set(1, 0, -3.0);
        assertTrue(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));
        bbdr.set(1, 0, AlbedoInversionConstants.NO_DATA_VALUE);
        assertTrue(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));
        bbdr.set(1, 0, 0.6);
        assertFalse(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));

        stdev[2] = -1.0;
        assertTrue(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));
        stdev[2] = AlbedoInversionConstants.NO_DATA_VALUE;
        assertTrue(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));
        stdev[2] = Double.NaN;
        assertTrue(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));
        stdev[2] = 0.05;
        assertFalse(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));

        correl[2] = -1.0;
        assertTrue(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));
        correl[2] = AlbedoInversionConstants.NO_DATA_VALUE;
        assertTrue(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));
        correl[2] = Double.NaN;
        assertTrue(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));
        correl[2] = 0.05;
        assertFalse(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));

        kernels.set(2, 6, AlbedoInversionConstants.NO_DATA_VALUE);
        assertTrue(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));
        kernels.set(2, 6,Double.NaN);
        assertTrue(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));
        kernels.set(2, 6, 3.4);
        assertFalse(DailyAccumulationUtils.isAccumulatorInputInvalid(bbdr, stdev, correl, kernels));
    }
}

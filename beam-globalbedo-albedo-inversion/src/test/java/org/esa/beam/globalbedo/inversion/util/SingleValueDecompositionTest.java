package org.esa.beam.globalbedo.inversion.util;

import Jama.Matrix;
import Jama.SingularValueDecomposition;
import junit.framework.TestCase;
import org.apache.commons.math3.linear.RealMatrix;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class SingleValueDecompositionTest extends TestCase {

    private double[][] testSquare = {
            {24.0 / 25.0, 43.0 / 25.0},
            {57.0 / 25.0, 24.0 / 25.0}
    };

    private double[][] testNonSquare = {
            {-540.0 / 625.0, 963.0 / 625.0, -216.0 / 625.0},
            {-1730.0 / 625.0, -744.0 / 625.0, 1008.0 / 625.0},
            {-720.0 / 625.0, 1284.0 / 625.0, -288.0 / 625.0},
            {-360.0 / 625.0, 192.0 / 625.0, 1756.0 / 625.0},
    };

    public void testGetRealMatrixFromJamaMatrix() {
        Jama.Matrix jm = new Matrix(testSquare);
        RealMatrix rm = AlbedoInversionUtils.getRealMatrixFromJamaMatrix(jm);
        assertNotNull(rm);
        assertEquals(rm.getRowDimension(), jm.getRowDimension());
        assertEquals(rm.getColumnDimension(), jm.getColumnDimension());
        for (int i = 0; i < rm.getRowDimension(); i++) {
            for (int j = 0; j < rm.getColumnDimension(); j++) {
                assertEquals(rm.getEntry(i, j), jm.get(i, j));
            }
        }

        jm = new Matrix(testNonSquare);
        rm = AlbedoInversionUtils.getRealMatrixFromJamaMatrix(jm);
        assertNotNull(rm);
        assertEquals(rm.getRowDimension(), jm.getRowDimension());
        assertEquals(rm.getColumnDimension(), jm.getColumnDimension());
        for (int i = 0; i < rm.getRowDimension(); i++) {
            for (int j = 0; j < rm.getColumnDimension(); j++) {
                assertEquals(rm.getEntry(i, j), jm.get(i, j));
            }
        }
    }

    public void testSVD() {
        Jama.Matrix jm = new Matrix(testSquare);
        RealMatrix rm = AlbedoInversionUtils.getRealMatrixFromJamaMatrix(jm);

        SingularValueDecomposition jmSvd = jm.svd();
        org.apache.commons.math3.linear.SingularValueDecomposition rmSvd =
                new org.apache.commons.math3.linear.SingularValueDecomposition(rm);

        assertNotNull(jmSvd);
        assertNotNull(rmSvd);
        assertEquals(rmSvd.getSingularValues().length, jmSvd.getSingularValues().length);
        for (int i = 0; i < rmSvd.getSingularValues().length; i++) {
            assertEquals(rmSvd.getSingularValues()[i], jmSvd.getSingularValues()[i], 1.E-6);
        }

        jm = new Matrix(testNonSquare);
        rm = AlbedoInversionUtils.getRealMatrixFromJamaMatrix(jm);

        jmSvd = jm.svd();
        rmSvd = new org.apache.commons.math3.linear.SingularValueDecomposition(rm);

        assertNotNull(jmSvd);
        assertNotNull(rmSvd);
        assertEquals(rmSvd.getSingularValues().length, jmSvd.getSingularValues().length);
        for (int i = 0; i < rmSvd.getSingularValues().length; i++) {
            assertEquals(rmSvd.getSingularValues()[i], jmSvd.getSingularValues()[i], 1.E-6);
        }
    }


}

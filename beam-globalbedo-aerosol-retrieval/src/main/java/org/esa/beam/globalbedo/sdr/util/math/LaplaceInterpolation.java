/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.util.math;

/**
 *
 * @author akheckel
 */
public class LaplaceInterpolation extends Linbcg {

    private double[][] matrix;
    private int ii;   // rows of matrix
    private int jj;   // columns of matrix
    private int nn;   // total number of elements in matrix ( rows * cloumns)
    private int iter;
    private double[] b, y;
    private boolean[] mask;


    public LaplaceInterpolation(double[][] matrix) {

        this.matrix = matrix;
        ii = matrix.length;
        jj = matrix[0].length;
        nn = ii * jj;
        iter = 0;
        b = new double[nn];
        y = new double[nn];
        mask = new boolean[nn];

        //int i,j,k;
        double vl = 0;

        for (int k=0; k<nn; k++) {
            int i = k / jj;
            int j = k - i*jj;
            if (matrix[i][j] == Double.MAX_VALUE) {
                b[k] = 0;
                y[k] = vl;
                mask[k] = false;
            }
            else {
                vl = matrix[i][j];
                b[k] = matrix[i][j];
                y[k] = matrix[i][j];
                mask[k] = true;
            }
        }
    }

    public LaplaceInterpolation(double[][] matrix, double noDataValue) {

        this.matrix = matrix;
        ii = matrix.length;
        jj = matrix[0].length;
        nn = ii * jj;
        iter = 0;
        b = new double[nn];
        y = new double[nn];
        mask = new boolean[nn];

        //int i,j,k;
        double vl = 0;

        for (int k=0; k<nn; k++) {
            int i = k / jj;
            int j = k - i*jj;
            if (matrix[i][j] == noDataValue) {
                b[k] = 0;
                y[k] = vl;
                mask[k] = false;
            }
            else {
                vl = matrix[i][j];
                b[k] = matrix[i][j];
                y[k] = matrix[i][j];
                mask[k] = true;
            }
        }
    }

    @Override
    void asolve(double[] b, double[] x, boolean trnsp) {
        for (int i=0; i<b.length; i++) x[i] = b[i];
    }

    @Override
    void atimes(double[] x, double[] r, boolean trnsp) {

        double del;

        int n = r.length;
        for(int k=0; k<n; k++) r[k] = 0;
        for(int k=0; k<n; k++) {
            int i = k/jj;
            int j = k - i*jj;
            if (mask[k]) {
                r[k] += x[k];
            }
            else if (i>0 && i<ii-1 && j>0 && j<jj-1) {        // interior point
                if (trnsp) {
                    r[k] += x[k];
                    del = -0.25*x[k];
                    r[k-1] += del;
                    r[k+1] += del;
                    r[k-jj] += del;
                    r[k+jj] += del;
                }
                else {
                    r[k] = x[k] - 0.25*(x[k-1]+x[k+1]+x[k-jj]+x[k+jj]);
                }
            }
            else if (i>0 && i<ii-1) {                         // left and right edge
                if (trnsp) {
                    r[k] += x[k];
                    del = -0.5*x[k];
                    r[k-jj] += del;
                    r[k+jj] += del;
                }
                else {
                    r[k] = x[k] - 0.5*(x[k+jj] + x[k-jj]);
                }
            }
            else if (j>0 && j<jj-1) {                         // top and bottom edge
                if (trnsp) {
                    r[k] += x[k];
                    del = -0.5*x[k];
                    r[k-1] += del;
                    r[k+1] += del;
                }
                else {
                    r[k] = x[k] - 0.5*(x[k+1] + x[k-1]);
                }
            }
            else {                                            // corners
                int jjt = (i==0) ? jj : -jj;
                int it  = (j==0) ? 1 : -1;
                if (trnsp) {
                    r[k] += x[k];
                    del = -0.5*x[k];
                    r[k+jjt] += del;
                    r[k+it]  += del;
                }
                else {
                    r[k] = x[k] - 0.5*(x[k+jjt]+x[k+it]);
                }
            }
        }
    }

    public double solveInterp() throws Exception {
        return solveInterp(1.0e-6, -1);
    }

    public double solveInterp(double tol, int itmax) throws Exception {

        if (itmax <= 0) itmax = 2*Math.max(ii,jj);

        double err = 0;
        solve(b,y,1,tol,itmax);
        iter = getIter();
        err  = getErr();

        int k=0;
        for(int i=0; i<ii; i++) {
            for (int j=0; j<jj; j++) {
                matrix[i][j] = y[k];
                k++;
            }
        }

        return err;
    }

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import Jama.Matrix;
import org.esa.beam.globalbedo.sdr.util.math.Brent;
import org.esa.beam.globalbedo.sdr.util.math.Function;

/**
 * Provides aerosol retrieval class
 * currently not thread safe !!!
 *     subsequent calls to Brent and Powell.
 *     Somewhere along that line thread safety is broken
 * --> instantiate locally in computeTileStack()
 * 
 * @author akheckel
 */
public class PointRetrieval {

    private final UnivRetrievalFunction brentFitFct;

    public PointRetrieval(UnivRetrievalFunction brentFitFct) {
        this.brentFitFct = brentFitFct;
    }


// public methods

    public synchronized RetrievalResults runRetrieval(double maxAOT) {
        final Brent b = new Brent();
        b.brent(0.001, 0.5*maxAOT, maxAOT, brentFitFct, 5e-6);
        float optAOT = (float) b.getXmin();
        float optErr = (float) b.getFx();
        boolean failed = (optAOT <= 0.01);
        double curv = calcCurvature(optAOT, optErr, maxAOT);
        float retrievalErr = (float) calcErrFromCurv(optErr, curv);

        RetrievalResults results = new RetrievalResults(failed, optAOT, optErr, retrievalErr, (float) curv);
/*
        if (instrument.equals("VGT")){
            results.sdr = brentFitFct.getSurfReflec(optAOT);
            results.modelSpec = brentFitFct.getModelReflec(optAOT);
            results.pAtMin = brentFitFct.getpAtMin();
        }
 */
        return results;
    }

    public RetrievalResults retrieveSDR(double aot) {
        boolean failed = false;
        float optAOT = (float) aot;
        float optErr = (float) brentFitFct.f(aot);
        float retrievalErr = calcRetrievalErr(optAOT, optErr, 2.0);
        RetrievalResults results = new RetrievalResults(failed, optAOT, optErr, retrievalErr);
/*
        results.sdr = brentFitFct.getSurfReflec(optAOT);
        results.modelSpec = brentUnivFct.getModelReflec(optAOT);
        results.pAtMin = brentUnivFct.getpAtMin();
 * 
 */
        return results;
    }

// private methods

    private float calcRetrievalErr(double optAOT, double optErr, double maxAOT) {
        double a = calcCurvature(optAOT, optErr, maxAOT);
        return (float) calcErrFromCurv(optErr, a);
    }

    private double calcErrFromCurv(double optErr, double a) {
        double retrievalError;
        if (a < 0) {
            retrievalError = Math.sqrt(optErr / 0.8 * 2 / 1e-4) + 0.03;
        }
        else {
            retrievalError = Math.sqrt(optErr / 0.8 * 2 / a) + 0.03;
        }
        return retrievalError;
    }

    private double calcCurvature(double optAOT, double optErr, double maxAOT) {
        double p1 = 0.33*maxAOT;
        double p2 = 0.66*maxAOT;
        final double[] x0 = {1, 1, 1};
        final double[] x1 = {p1, optAOT, p2};
        final double[] x2 = {x1[0] * x1[0], x1[1] * x1[1], x1[2] * x1[2]};
        double optErrLow = brentFitFct.f(x1[0]);
        double optErrHigh = brentFitFct.f(x1[2]);
        final double[][] y = {{optErrLow}, {optErr}, {optErrHigh}};
        final double[][] xArr = {{x2[0], x1[0], x0[0]}, {x2[1], x1[1], x0[1]}, {x2[2], x1[2], x0[2]}};
        Matrix A = new Matrix(xArr);
        Matrix c = new Matrix(y);
        Matrix result = A.solve(c);
        final double[][] resultArr = result.getArray();
        final double a = resultArr[0][0]; // curvature term of parabola
/*
        if (optAOT<0.004){
            BrentFitFunction bb = (BrentFitFunction) brentFitFct;
            for (int i=0; i<bb.inPixField.length; i++){
                System.err.printf("TOA[%d]=%4.2f , ", i, bb.inPixField[i].toaReflec[0]);
            }
            System.err.println();
            System.err.println("curv " + a);
            System.err.printf("0.3  %f   %f\n", x1[0], y[1][0]);
            System.err.printf("opt  %f   %f\n", x1[1], y[1][0]);
            System.err.printf("0.6  %f   %f\n", x1[2], y[2][0]);
        }
*/
        return a;
    }

}

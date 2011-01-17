package org.esa.beam.globalbedo.sdr.util.math;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class Spline {
    private double y[];
    private double y2[];

    /**
     * The constructor calculates the second derivatives of the interpolating function
     * at the tabulated points xi, with xi = (i, y[i]).
     * Based on numerical recipes in C, http://www.library.cornell.edu/nr/bookcpdf/c3-3.pdf .
     *
     * @param y Array of y coordinates for cubic-spline interpolation.
     */
    public Spline(double y[]) {
        this.y = y;
        int n = y.length;
        y2 = new double[n];
        double u[] = new double[n];
        for (int i = 1; i < n - 1; i++) {
            y2[i] = -1.0 / (4.0 + y2[i - 1]);
            u[i] = (6.0 * (y[i + 1] - 2.0 * y[i] + y[i - 1]) - u[i - 1]) / (4.0 + y2[i - 1]);
        }
        for (int i = n - 2; i >= 0; i--) {
            y2[i] = y2[i] * y2[i + 1] + u[i];
        }
    }

    /**
     * Returns a cubic-spline interpolated value y for the point between
     * point (n, y[n]) and (n+1, y[n+1), with t ranging from 0 for (n, y[n])
     * to 1 for (n+1, y[n+1]).
     *
     * @param n The start point.
     * @param t The distance to the next point (0..1).
     * @return A cubic-spline interpolated value.
     */
    public double fn(int n, double t) {
        return t * y[n + 1] - ((t - 1.0) * t * ((t - 2.0) * y2[n] - (t + 1.0) * y2[n + 1])) / 6.0 + y[n] - t * y[n];
    }

//    public double xySplineFn(double xA[], double trueX) {
//        double X;
//        double Y;
//        double T = trueX / 3.0 + 0.3333333333;
//        do {
//            double aTinv = 1.0 - T;
//            double xC = (
//                            xA[0] * 1.0 * Math.pow(T, 3.0) +
//                            xA[1] * 3.0 * Math.pow(T, 2.0) * Math.pow(aTinv, 1.0) +
//                            xA[2] * 3.0 * Math.pow(T, 1.0) * Math.pow(aTinv, 2.0) +
//                            xA[3] * 1.0 * Math.pow(aTinv, 3.0)
//            );
//            T += (xC - T) / 2.0;
//            X = (T - 0.3333333333) * 3.0;
//        } while (Math.pow(X - trueX, 2.0) > 0.0001);
//
//        Y = (
//                        y[0] * 1.0 * Math.pow(1.0 - T, 3.0) +
//                        y[1] * 3.0 * Math.pow(1.0 - T, 2.0) * Math.pow(T, 1.0) +
//                        y[2] * 3.0 * Math.pow(1.0 - T, 1.0) * Math.pow(T, 2.0) +
//                        y[3] * 1.0 * Math.pow(T, 3.0)
//        );
//        return Y;
//    }
}

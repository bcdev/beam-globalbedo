package org.esa.beam.globalbedo.sdr.util.math;

/**
 * @author Andreas Heckel, Olaf Danne
 * @version $Revision: 5452 $ $Date: 2009-06-08 11:25:20 +0200 (Mo, 08 Jun 2009) $
 */
public interface MvFunction {

    /**
     *  multivariate function
     *
     * @param  x - point at which function should be calculated
     * @return     value of the function at x
     * @throws   UnsupportedOperationException -
     */
    double f(double[] x);


    /**
     *  vector of first derivitives of f(x)
     *
     * @param  x - point at which function should be calculated
     * @param  g - vector of first derivatives evaluated at x
     * @throws   UnsupportedOperationException -
     *
     */
    void g(double[] x, double[] g);
}

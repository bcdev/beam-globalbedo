package org.esa.beam.globalbedo.sdr.util.math;

/**
 * This class provides a multivariate mimimisation
 * (see Num. Recip., pp. 412)
 *
 * @author Andreas Heckel, Olaf Danne
 * @version $Revision: 6368 $ $Date: 2009-10-02 15:51:14 +0200 (Fr, 02 Okt 2009) $
 */
public class Powell {

    private double fret;
    private double[] pret;
    private final int ITMAX = 1000;

    /**
     *  Constructor for the powell object
     *
     */
    public Powell() {
    }

    /**
     *  Constructor for the powell object
     *
     *@param  p     Description of Parameter
     *@param  xi    Description of Parameter
     *@param  ftol  Description of Parameter
     *@param  func  Description of Parameter
     */
    public Powell(double[] p, double[][] xi, double ftol, MvFunction func) {
        powell(p, xi, ftol, func);
    }

    /**
     *  This method provides a minimisation of a function of n variables
     *
     *@param  p                                 array of variables (has length n)
     *@param  xi                                initial matrix
     *@param  ftol                              fractional tolerance in function value
     *@param  func                              function to be minimised
     *@exception  IllegalMonitorStateException  Description of Exception
     *@exception  IllegalArgumentException      Description of Exception
     */
    public synchronized void powell(double[] p, double[][] xi, double ftol, MvFunction func)
            throws IllegalMonitorStateException,
            IllegalArgumentException {

        Linmin linmin = new Linmin();

        int n = 0;
        if (p.length != xi.length || xi.length != xi[0].length) {
            throw new IllegalArgumentException("dimentions must agree");
        }
        if (n != p.length) {
            n = p.length;
        }
        double[] pt = new double[n];
        double[] ptt = new double[n];
        double[] xit = new double[n];

        fret = func.f(p);
        pret = p;

        System.arraycopy(p, 0, pt, 0, n);
        
        for (int iter = 1; true; ++iter) {
            double fp = fret;
            int ibig = 0;
            double del = 0.0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    xit[j] = xi[j][i];
                }
                double fptt = fret;
                linmin.linmin(p, xit, func);
                fret = linmin.getFret();
                pret = p;
                if (Math.abs(fptt - fret) > del) {
                    del = Math.abs(fptt - fret);
                    ibig = i;
                }
            }
            if (2.0 * Math.abs(fp - fret) <= ftol * (Math.abs(fp) + Math.abs(fret))) {
                return;
            }
            if (iter == ITMAX) {
                throw new IllegalMonitorStateException("powell exceeding maximum iterations.");
            }
            for (int j = 0; j < n; j++) {
                ptt[j] = 2.0 * p[j] - pt[j];
                xit[j] = p[j] - pt[j];
                pt[j] = p[j];
            }
            double fptt = func.f(ptt);
            if (fptt < fp) {
                double t = 2.0 * (fp - 2.0 * fret + fptt) * (fp - fret - del) * (fp - fret - del) -
                        del * (fp - fptt) * (fp - fptt);
                if (t < 0.0) {
                    linmin = new Linmin(p, xit, func);
                    fret = linmin.getFret();
                    pret = p;
                    for (int j = 0; j < n; j++) {
                        xi[j][ibig] = xi[j][n - 1];
                        xi[j][n - 1] = xit[j];
                    }
                }
            }
        }
    }

    public synchronized double getFmin() {
        return fret;
    }

    public synchronized  double[] getP() {
        return pret;
    }

}

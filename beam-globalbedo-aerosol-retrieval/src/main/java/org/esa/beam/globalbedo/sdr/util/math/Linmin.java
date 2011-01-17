package org.esa.beam.globalbedo.sdr.util.math;

/**
 * This class provides the 'linmin' implementation used within Powell minimisation.
 * (see Num. Recip., pp. 413)
 *
 * @author Andreas Heckel, Olaf Danne
 * @version $Revision: 6368 $ $Date: 2009-10-02 15:51:14 +0200 (Fr, 02 Okt 2009) $
 */
public class Linmin implements Function{

	private double fret;
	int ncom;
	double[] pcom, xicom;
	double[] xt;
	private MvFunction fun;
	private Mnbrak mnbrak = new Mnbrak();
	private Brent brent = new Brent();
    
	private final static double TOL = 2.0e-4;


	/**
	 *  put your documentation comment here
	 */
	public Linmin() {
	}

	/**
	 *  Constructor for the linmin object
	 *
	 *@param  p   Description of Parameter
	 *@param  xi  Description of Parameter
	 *@param  f   Description of Parameter
	 */
	public Linmin(double[] p, double xi[], MvFunction f) {
		linmin(p, xi, f);
	}


	/**
	 *  This method searches for a minimum in one distinct direction
	 *
	 *@param  p                             array of variables (has length n)
	 *@param  xi                            initial matrix
	 *@param  f                             the function
	 *@exception  IllegalArgumentException  Description of Exception
	 */
	public void linmin(double[] p, double xi[], MvFunction f) throws IllegalArgumentException {
		int n = 0;
        if (p.length != xi.length) {
			throw new IllegalArgumentException("dimentions must agree");
		}
		if (n != p.length) {
			n = p.length;
			pcom = new double[n];
			xicom = new double[n];
		}
		fun = f;
		ncom = n;
		for (int j = 0; j < n; j++) {
			pcom[j] = p[j];
			xicom[j] = xi[j];
		}
		double ax = 0.0;
		double xx = 1.0;
		mnbrak.mnbrak(ax, xx, this);
		ax = mnbrak.ax;
		xx = mnbrak.bx;
		double bx = mnbrak.cx;
		brent.brent(ax, xx, bx, this, TOL);
		fret = brent.getFx();
		double xmin = brent.getXmin();
		for (int j = 0; j < n; j++) {
			xi[j] *= xmin;
			p[j] += xi[j];
		}
	}

	public double f(double x) {
		return f1dim(x);
	}

	private double f1dim(double x) {
		if (xt==null||xt.length != ncom) {
			xt = new double[ncom];
		}
		for (int j = 0; j < ncom; j++) {
			xt[j] = pcom[j] + x * xicom[j];
		}
		return fun.f(xt);
	}

    public double getFret() {
        return fret;
    }
}

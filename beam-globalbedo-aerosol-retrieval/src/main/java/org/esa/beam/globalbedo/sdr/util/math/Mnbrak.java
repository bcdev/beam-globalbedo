package org.esa.beam.globalbedo.sdr.util.math;

/**
 * This class provides a method for initially bracketing a minimum.
 *
 * @author Andreas Heckel, Olaf Danne
 * @version $Revision: 6368 $ $Date: 2009-10-02 15:51:14 +0200 (Fr, 02 Okt 2009) $
 */
public class Mnbrak {
    
	public double ax, bx, cx, fa, fb, fc;

	private final double GOLD = 1.618034;
	private final int GLIMIT = 100;
	private final double TINY = 1.0e-20;

	/**
	 *  put your documentation comment here
	 */
	public Mnbrak() {
	}


	//#define SHFT(a,b,c,d) (a)=(b);(b)=(c);(c)=(d);
	/**
	 *  Constructor for the mnbrak object
	 *
	 *@param  Ax   Description of Parameter
	 *@param  Bx   Description of Parameter
	 *@param  fun  Description of Parameter
	 */
	public Mnbrak(double Ax, double Bx, Function fun) {
		mnbrak(Ax, Bx, fun);
	}


	/**
	 *  Given a function fun and distinct initial points Ax, Bx, this method searches in the downhill direction
     * and computes new points ax, bx, cx that bracket a minimum of the function.
	 *
	 *@param  Ax   left initial point
	 *@param  Bx   right initial point
	 *@param  fun  the function
	 */
	public void mnbrak(double Ax, double Bx, Function fun) {
		ax = Ax;
		bx = Bx;
		fa = fun.f(ax);
		fb = fun.f(bx);
		if (fb > fa) {
			double dum = ax;
			ax = bx;
			bx = dum;
			dum = fb;
			fb = fa;
			fa = dum;
		}
		cx = bx + GOLD * (bx - ax);
		fc = fun.f(cx);
		while (fb > fc) {
			double r = (bx - ax) * (fb - fc);
			double q = (bx - cx) * (fb - fa);
			double u = bx - ((bx - cx) * q - (bx - ax) * r) / (2.0 * (q - r < 0 ? -1 : 1) * Math.max(Math.abs(
					q - r), TINY));
			double ulim = bx + GLIMIT * (cx - bx);
            double fu;
			if ((bx - u) * (u - cx) > 0.0) {
				fu = fun.f(u);
				if (fu < fc) {
					ax = bx;
					bx = u;
					fa = fb;
					fb = fu;
					return;
				}
				else if (fu > fb) {
					cx = u;
					fc = fu;
					return;
				}
				u = cx + GOLD * (cx - bx);
				fu = fun.f(u);
			}
			else if ((cx - u) * (u - ulim) > 0.0) {
				fu = fun.f(u);
				if (fu < fc) {
					bx = cx;
					cx = u;
					u = cx + GOLD * (cx - bx);
					fb = fc;
					fc = fu;
					fu = fun.f(u);
				}
			}
			else if ((u - ulim) * (ulim - cx) >= 0.0) {
				u = ulim;
				fu = fun.f(u);
			}
			else {
				u = (cx) + GOLD * (cx - bx);
				fu = fun.f(u);
			}
			ax = bx;
			bx = cx;
			cx = u;
			fa = fb;
			fb = fc;
			fc = fu;
		}
	}
}

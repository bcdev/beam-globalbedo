/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.util.math;

/**
 *
 * @author akheckel
 */
public abstract class Linbcg {

    private int iter;   // to be returned by solve()
    private double err; // to be returned by solve()

    abstract void asolve(double[] b, double[] x, boolean trnsp);

    abstract void atimes (double[] x, double[] r, boolean trnsp);

    public void solve(double[] b, double[] x, int itol, double tol, int itmax) throws Exception {

        double EPS = 1.0e-14;

        int n = b.length;
        double[] p  = new double[n];
        double[] pp = new double[n];
        double[] r  = new double[n];
        double[] rr = new double[n];
        double[] z  = new double[n];
        double[] zz = new double[n];

        iter = 0;
        atimes(x, r, false);
        for(int j=0; j<n; j++) {
            r[j]  = b[j] - r[j];
            rr[j] = r[j];
        }

        double bnrm = 0;
        double znrm = 0;

        switch (itol) {
            case 1:
                bnrm = snrm(b, itol);
                asolve(r, z, false);
                break;
            case 2:
                asolve(b, z, false);
                bnrm = snrm(z, itol);
                asolve(r, z, false);
                break;
            case 3:
            case 4:
                asolve(b, z, false);
                bnrm = snrm(z, itol);
                asolve(r, z, false);
                znrm = snrm(z, itol);
                break;
            default:
                throw new Exception("illegal itol in Linbcg");
        }

        double bk;
        double bkden = 1.0;

        while (iter < itmax) {
            ++iter;
            asolve(rr, zz, true);
            double bknum = 0.0;
            for (int j=0; j<n; j++) bknum += z[j]*rr[j];
            if (iter == 1) {
                for (int j=0; j<n; j++) {
                    p[j]  = z[j];
                    pp[j] = zz[j];
                }
            }
            else {
                bk = bknum/bkden;
                for (int j=0; j<n; j++) {
                    p[j]  = bk*p[j]  + z[j];
                    pp[j] = bk*pp[j] + zz[j];
                }
            }
            bkden = bknum;
            atimes(p, z, false);
            double akden = 0.0;
            for (int j=0; j<n; j++) akden += z[j]*pp[j];
            double ak = bkden / akden;
            atimes(pp, zz, true);
            for (int j=0; j<n; j++) {
                x[j]  += ak*p[j];
                r[j]  -= ak*z[j];
                rr[j] -= ak*zz[j];
            }
            asolve(r, z, false);

            switch (itol) {
                case 1:
                    err = snrm(r, itol) / bnrm;
                    break;
                case 2:
                    err = snrm(z, itol) / bnrm;
                    break;
                case 3:
                case 4:
                    double zm1nrm = znrm;
                    znrm = snrm(z, itol);
                    if (Math.abs(zm1nrm-znrm) > EPS*znrm) {
                        double dxnrm = Math.abs(ak)*snrm(p, itol);
                        err = znrm / Math.abs(zm1nrm-znrm)*dxnrm;
                    }
                    else {
                        err = znrm / bnrm;
                        continue;
                    }
                    double xnrm = snrm(x, itol);
                    if (err <= 0.5*xnrm) {
                        err /= xnrm;
                    }
                    else {
                        err = znrm / bnrm;
                        continue;
                    }
            }
            if (err <= tol) break;
        } // end of main while loop
    }

    public double getErr() {
        return err;
    }

    public int getIter() {
        return iter;
    }

    private double snrm (double[] sx, int itol) {

        int n = sx.length;
        double ans;
        double result;

        if (itol <= 3) {
            ans = 0.0;
            for (int i=0; i<n; i++) ans += sx[i]*sx[i];   // vector magnitude norm
            result = Math.sqrt(ans);
        }
        else {
            int isamax = 0;
            for (int i=0; i<n; i++) {                     // largest component norm
                if (Math.abs(sx[i]) > Math.abs(sx[isamax])) isamax = i;
            }
            result = Math.abs(sx[isamax]);
        }

        return result;
    }
}

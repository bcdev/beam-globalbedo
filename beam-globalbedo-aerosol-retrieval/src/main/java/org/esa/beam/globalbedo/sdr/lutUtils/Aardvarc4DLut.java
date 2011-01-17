/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.lutUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.sdr.operators.InputPixelData;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.ColumnMajorMatrixFactory;
import org.esa.beam.util.math.MatrixLookupTable;

/**
 *
 * @author akheckel
 */
public class Aardvarc4DLut implements AerosolLookupTable {

    private final MatrixLookupTable sixSLutN;
    private final MatrixLookupTable sixSLutF;
    private final double[] aot;
    private final double[] sza;
    private final double[] vzan;
    private final double[] vzaf;
    private final double[] razi;
    private int nWvl;
    private final int nParam;
    private final int nAot;
    private final int nSza;
    private final int nVzan;
    private final int nVzaf;
    private final int nRazi;

    public Aardvarc4DLut(String lutName) {
        this.nWvl = 4;
        this.nParam = 5;

        final double aot_int = 0.05;
        final double aot_min = 0.001;
        final double aot_max = 2.01;
        nAot = (int)((aot_max-aot_min)/aot_int+1);
        aot = new double[nAot];
        for (int i=0; i<nAot; i++) aot[i] = aot_min + i*aot_int;

        final double sza_int = 5.0;
        final double sza_min = 0.0;
        final double sza_max = 80.0;
        nSza = (int)((sza_max-sza_min)/sza_int+1);
        sza = new double[nSza];
        for (int i=0; i<nSza; i++) sza[i] = sza_min + i*sza_int;

        final double vzan_int = 5.0;
        final double vzan_min = 0.0;
        final double vzan_max = 25.0;
        nVzan = (int)((vzan_max-vzan_min)/vzan_int+1);
        vzan = new double[nVzan];
        for (int i=0; i<nVzan; i++) vzan[i] = vzan_min + i*vzan_int;

        final double vzaf_int = 5.0;
        final double vzaf_min = 50.0;
        final double vzaf_max = 60.0;
        nVzaf = (int)((vzaf_max-vzaf_min)/vzaf_int+1);
        vzaf = new double[nVzaf];
        for (int i=0; i<nVzaf; i++) vzaf[i] = vzaf_min + i*vzaf_int;

        final double razi_int = 20.0;
        final double razi_min = 0.0;
        final double razi_max = 180.0;
        nRazi = (int)((razi_max-razi_min)/razi_int+1);
        razi = new double[nRazi];
        for (int i=0; i<nRazi; i++) razi[i] = razi_min + i*razi_int;

        final int lutsizeN = nSza * nVzan * nRazi * nAot;
        final double[] valuesN = new double[lutsizeN * nWvl * nParam];
        final int lutsizeF = nSza * nVzaf * nRazi * nAot;
        final double[] valuesF = new double[lutsizeF * nWvl * nParam];

        BufferedReader bufr = null;
        try {
            bufr = new BufferedReader(new InputStreamReader(new FileInputStream(lutName)));
            int nLines = readValuesGeom(bufr, nVzan, valuesN);
            nLines += readValuesGeom(bufr, nVzaf, valuesF);
            System.err.println("lines read: "+nLines);
        } catch (Exception ex) {
            throw new OperatorException(ex);
        } finally {
            try {
                bufr.close();
            } catch (IOException ex) {
                throw new OperatorException(ex);
            }
        }

        sixSLutN = new MatrixLookupTable(nWvl, nParam, new ColumnMajorMatrixFactory(), valuesN, sza, vzan, razi, aot);
        sixSLutF = new MatrixLookupTable(nWvl, nParam, new ColumnMajorMatrixFactory(), valuesF, sza, vzaf, razi, aot);
    }


    @Override
    public void getSdrAndDiffuseFrac(InputPixelData inPixel, double aot) {
        Guardian.assertEquals("InputPixelData.nSpecWvl", inPixel.nSpecWvl, nWvl);
        Guardian.assertNotNull("InputPixelData.diffuseFrac[][]", inPixel.diffuseFrac);
        Guardian.assertNotNull("InputPixelData.surfReflec[][]", inPixel.surfReflec);
        double[][] atmPars = sixSLutN.getValues(inPixel.geom.sza, inPixel.geom.vza, 180.0-inPixel.geom.razi, aot);
        for (int iWvl=0; iWvl<nWvl; iWvl++){
            double frac_dir = atmPars[iWvl][0];
            double cfac = atmPars[iWvl][1];
            double xa   = atmPars[iWvl][2];
            double xb   = atmPars[iWvl][3];
            double xc   = atmPars[iWvl][4];
            double yval = cfac * inPixel.toaReflec[iWvl] * xa - xb;
            inPixel.surfReflec[0][iWvl] = (yval / (1.0 + xc * yval));
            inPixel.diffuseFrac[0][iWvl] = (1.0 - frac_dir);
        }
        atmPars = sixSLutF.getValues(inPixel.geomFward.sza, inPixel.geomFward.vza, 180.0-inPixel.geomFward.razi, aot);
        for (int iWvl=0; iWvl<nWvl; iWvl++){
            double frac_dir = atmPars[iWvl][0];
            double cfac = atmPars[iWvl][1];
            double xa   = atmPars[iWvl][2];
            double xb   = atmPars[iWvl][3];
            double xc   = atmPars[iWvl][4];
            double yval = cfac * inPixel.toaReflecFward[iWvl] * xa - xb;
            inPixel.surfReflec[1][iWvl] = (yval / (1.0 + xc * yval));
            inPixel.diffuseFrac[1][iWvl] = (1.0 - frac_dir);
        }
    }
    public double[][][] getXaXbXc(InputPixelData inPixel, double aot) {
        double[][][] xaXbXc = new double[3][2][4];
        Guardian.assertEquals("InputPixelData.nSpecWvl", inPixel.nSpecWvl, nWvl);
        Guardian.assertNotNull("InputPixelData.diffuseFrac[][]", inPixel.diffuseFrac);
        Guardian.assertNotNull("InputPixelData.surfReflec[][]", inPixel.surfReflec);
        double[][] atmPars = sixSLutN.getValues(inPixel.geom.sza, inPixel.geom.vza, 180.0-inPixel.geom.razi, aot);
        for (int iWvl=0; iWvl<nWvl; iWvl++){
            double frac_dir = atmPars[iWvl][0];
            double cfac = atmPars[iWvl][1];
            double xa   = atmPars[iWvl][2];
            double xb   = atmPars[iWvl][3];
            double xc   = atmPars[iWvl][4];
            xaXbXc[0][0][iWvl] = xa*cfac;
            xaXbXc[1][0][iWvl] = xb;
            xaXbXc[2][0][iWvl] = xc;
        }
        atmPars = sixSLutF.getValues(inPixel.geomFward.sza, inPixel.geomFward.vza, 180.0-inPixel.geomFward.razi, aot);
        for (int iWvl=0; iWvl<nWvl; iWvl++){
            double frac_dir = atmPars[iWvl][0];
            double cfac = atmPars[iWvl][1];
            double xa   = atmPars[iWvl][2];
            double xb   = atmPars[iWvl][3];
            double xc   = atmPars[iWvl][4];
            xaXbXc[0][1][iWvl] = xa*cfac;
            xaXbXc[1][1][iWvl] = xb;
            xaXbXc[2][1][iWvl] = xc;
        }
        return xaXbXc;
    }

    /**
     * @param ipd - InputPixelData
     * @param aot - AOT
     * @return SDR Nadir, SDR FWRD, DiffuseFrac Nadir, DiffuseFrac Fward for nWvl
     */
    public float[][] getSdrDiff(InputPixelData ipd, float aot){
        Guardian.assertEquals("InputPixelData.nSpecWvl", ipd.nSpecWvl, nWvl);
        float[][] sdr = new float[4][nWvl];
        double[][] atmPars = sixSLutN.getValues(ipd.geom.sza, ipd.geom.vza, 180.0-ipd.geom.razi, aot);
        for (int iWvl=0; iWvl<nWvl; iWvl++){
            double frac_dir = atmPars[iWvl][0];
            double cfac = atmPars[iWvl][1];
            double xa   = atmPars[iWvl][2];
            double xb   = atmPars[iWvl][3];
            double xc   = atmPars[iWvl][4];
            double yval = cfac * ipd.toaReflec[iWvl] * xa - xb;
            sdr[0][iWvl] = (float) (yval / (1.0 + xc * yval));
            sdr[2][iWvl] = (float) (1.0 - frac_dir);
        }
        atmPars = sixSLutF.getValues(ipd.geomFward.sza, ipd.geomFward.vza, 180.0-ipd.geomFward.razi, aot);
        for (int iWvl=0; iWvl<nWvl; iWvl++){
            double frac_dir = atmPars[iWvl][0];
            double cfac = atmPars[iWvl][1];
            double xa   = atmPars[iWvl][2];
            double xb   = atmPars[iWvl][3];
            double xc   = atmPars[iWvl][4];
            double yval = cfac * ipd.toaReflecFward[iWvl] * xa - xb;
            sdr[1][iWvl] = (float) (yval / (1.0 + xc * yval));
            sdr[3][iWvl] = (float) (1.0 - frac_dir);
        }
        return sdr;
    }

    public double[][] getSdrDiff(InputPixelData ipd, double aot){
        Guardian.assertEquals("InputPixelData.nSpecWvl", ipd.nSpecWvl, nWvl);
        double[][] sdr = new double[9][nWvl];
        double[][] atmPars = sixSLutN.getValues(ipd.geom.sza, ipd.geom.vza, 180.0-ipd.geom.razi, aot);
        for (int iWvl=0; iWvl<nWvl; iWvl++){
            double frac_dir = atmPars[iWvl][0];
            double cfac = atmPars[iWvl][1];
            double xa   = atmPars[iWvl][2];
            double xb   = atmPars[iWvl][3];
            double xc   = atmPars[iWvl][4];
            double yval = cfac * ipd.toaReflec[iWvl] * xa - xb;
            sdr[0][iWvl] = (yval / (1.0 + xc * yval));
            sdr[2][iWvl] = (1.0 - frac_dir);
        }
        atmPars = sixSLutF.getValues(ipd.geomFward.sza, ipd.geomFward.vza, 180.0-ipd.geomFward.razi, aot);
        for (int iWvl=0; iWvl<nWvl; iWvl++){
            double frac_dir = atmPars[iWvl][0];
            double cfac = atmPars[iWvl][1];
            double xa   = atmPars[iWvl][2];
            double xb   = atmPars[iWvl][3];
            double xc   = atmPars[iWvl][4];
            double yval = cfac * ipd.toaReflecFward[iWvl] * xa - xb;
            sdr[1][iWvl] = (yval / (1.0 + xc * yval));
            sdr[3][iWvl] = (1.0 - frac_dir);

            sdr[4][iWvl] = frac_dir;
            sdr[5][iWvl] = cfac;
            sdr[6][iWvl] = xa;
            sdr[7][iWvl] = xb;
            sdr[8][iWvl] = xc;
        }
        return sdr;
    }

    public MatrixLookupTable getSixSLutF() {
        return sixSLutF;
    }

    public MatrixLookupTable getSixSLutN() {
        return sixSLutN;
    }

    private int readValuesGeom(BufferedReader bufread, int nVza, double[] values) throws NumberFormatException, IOException, OperatorException {
        final double[] ptmp = new double[nWvl];
        int index = 0;
        for (int iSza = 0; iSza < nSza; iSza++) {
            for (int iVza = 0; iVza < nVza; iVza++) {
                for (int iRazi = 0; iRazi < nRazi; iRazi++) {
                    for (int iAot = 0; iAot < nAot; iAot++) {
                        for (int iPar = 0; iPar < nParam; iPar++) {
                            readLutLine(bufread, ptmp);
                            System.arraycopy(ptmp, 0, values, index, nWvl);
                            index += nWvl;
                        }
                    }
                }
            }
        }
        //index -= nWvl;
        return index/nWvl;
    }

    private void readLutLine(BufferedReader bufread, final double[] arr) throws NumberFormatException, OperatorException, IOException {
        String line = bufread.readLine();
        String[] stmp = line.trim().split("\\s+");
        if (stmp.length != nWvl) {
            throw new OperatorException("wrong line format in LUT");
        }
        for (int i = 0; i < nWvl; i++) {
            arr[i] = Double.valueOf(stmp[i]);
        }
    }

}




/*
                             index = iSza * (nVzan*nRazi*nAot*nParam*nWvl)
                                    + iVza  * (nRazi*nAot*nParam*nWvl)
                                    + iRazi * (nAot*nParam*nWvl)
                                    + iAot  * (nParam*nWvl)
                                    + iPar  * nWvl;
*/
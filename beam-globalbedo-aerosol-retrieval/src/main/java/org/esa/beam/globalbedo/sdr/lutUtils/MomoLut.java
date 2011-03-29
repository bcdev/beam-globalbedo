/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.beam.globalbedo.sdr.lutUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.sdr.operators.InputPixelData;
import org.esa.beam.globalbedo.sdr.operators.PixelGeometry;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.ColumnMajorMatrixFactory;
import org.esa.beam.util.math.LookupTable;
import org.esa.beam.util.math.MatrixLookupTable;
import org.esa.beam.util.math.VectorLookupTable;

/**
 * class to read the lookuptables for the atmospheric correction
 * for GlobAlbedo provided by L.Guanter, FUB, Berlin
 * The LUTs contain values of the following parameters:
 *  1 - atmospheric path radiance
 *  2 - Tdown * Tup = product of downward and upward atmospheric transmission
 *  3 - spherical albedo of the atmosphere at ground level
 *  4 - ratio diff / total downward radiation
 *  5 - ratio diff / total upward radiation
 *
 *  Surface pressure values outside the LUT range are allowed
 *    the routines rely on the feature of LookupTable class that values
 *    outside the range are treated as equal to the highest or lowest values.
 *
 * @author akheckel
 */
public class MomoLut implements AerosolLookupTable{

    private final int nWvl;
    private final int nParameter;
    private final int nVza;
    private final int nSza;
    private final int nAzi;
    private final int nHsf;
    private final int nAot;
    private final float[] vza;
    private final float[] sza;
    private final float[] azi;
    private final float[] hsf;
    private final float[] aot;
    private final float[] wvl;
    private final float[] values;
    private final MatrixLookupTable sdrLut;
    private final VectorLookupTable gasTransLut;
    //private final LookupTable sdrLutKx;
    //private final LookupTable gasTransLutKx;
    private final float solIrrad;

    /**
     * standart constructor reading the binary LUT file from "lutName"
     * the number of channels or wavelength for which the LUTs is given
     * is not contained in the file
     * @param instrument - instrument name
     * @param lutName - file name of the binary LUTs (original format from FUB)
     * @param nWvl - number of spectral channels
     */
    public MomoLut(String lutName, int nWvl) {
        this.nParameter = 5;  // the 5 parameter i the LUT as described above
        this.nWvl = nWvl;

        ByteBuffer bb = readFileToByteBuffer(lutName);

        // read LUT dimensions and values
        this.vza = readDimension(bb);
        this.nVza = vza.length;
        this.sza = readDimension(bb);
        this.nSza = sza.length;
        this.azi = readDimension(bb);
        this.nAzi = azi.length;
        this.hsf = readDimension(bb);
        this.nHsf = hsf.length;
        // invert order to store stirctly increasing dimension in lut
        // swapping the actual values in the lut is taken care off in readValues()
        for (int i = 0; i < nHsf / 2; i++) {
            float swap = hsf[nHsf - 1 - i];
            hsf[nHsf - 1 - i] = hsf[i];
            hsf[i] = swap;
        }
        this.aot = readDimension(bb);
        this.nAot = aot.length;

        // attention readValues depends on the n??? fields !!!
        this.values = readValues(bb);

        this.wvl = readDimension(bb, nWvl);
        this.solIrrad = bb.getFloat();

        //this.sdrLut = new LookupTable(values, getDimensions());
        this.sdrLut = new MatrixLookupTable(nWvl, nParameter, new ColumnMajorMatrixFactory(), values, getDimensions());

        // generate filenames
        String lutPath = new File(lutName).getParentFile().getPath();
        lutPath += File.separator;
        String lutBaseName = new File(lutName).getName();
        int lutBaseEnd = lutBaseName.indexOf("_SDR_noG");
        int instrPrefixEnd = lutBaseName.indexOf("_");
        String instrumentPrefix = lutBaseName.substring(0, instrPrefixEnd);
        lutBaseName = lutBaseName.substring(0, lutBaseEnd);

        // getting LUT for gasous transmission correction
        String gasFileName = lutPath + instrumentPrefix + "_LUT_6S_Tg_CWV_OZO.bin";
        gasTransLut = readGasTransTable(gasFileName);

        // getting Luts of Jacobian matrices
        String sdrKxName = lutPath + lutBaseName + "_SU_noG_Kx-AOD.bin";
        //this.sdrLutKx = readSdrKxLut(sdrKxName);

        String gasTransKxName = lutPath + instrumentPrefix + "_LUT_6S_Kx-CWV_OZO.bin ";
        //this.gasTransLutKx = readGasTransKxLut(gasTransKxName);

    }

    // public methods
    @Override
    public synchronized void getSdrAndDiffuseFrac(InputPixelData inPix, double tau) {
        Guardian.assertEquals("InputPixelData.nSpecWvl", inPix.nSpecWvl, nWvl);
        Guardian.assertNotNull("InputPixelData.diffuseFrac[][]", inPix.diffuseFrac);
        Guardian.assertNotNull("InputPixelData.surfReflec[][]", inPix.surfReflec);
        PixelGeometry geom;
        for (int iView=0; iView<2; iView++){
            geom = (iView == 0)? inPix.geom : inPix.geomFward;
            final double[] toaR = (iView == 0) ? inPix.toaReflec : inPix.toaReflecFward;
            //final double rad2rfl = Math.PI / Math.cos(Math.toRadians(geom.sza));
            final float geomAMF = (float) ((1 / Math.cos(Math.toRadians(geom.sza))
                                            + 1 / Math.cos(Math.toRadians(geom.vza))));
            final double[] gasT = getGasTransmission(geomAMF, (float)inPix.wvCol, (float)(inPix.o3du/1000));
            double[][] lutValues = sdrLut.getValues(inPix.surfPressure, geom.vza, geom.sza, geom.razi, tau);

            for (int iWvl=0; iWvl<inPix.nSpecWvl; iWvl++){
                double rhoPath = lutValues[iWvl][0] * Math.PI / Math.cos(Math.toRadians(geom.sza));
                double tupTdown = lutValues[iWvl][1] / Math.cos(Math.toRadians(geom.sza));
                double spherAlb = lutValues[iWvl][2];
                //double tgO3 = Math.exp(inPix.o3du * o3corr[i] * geomAMF/2); //my o3 correction scheme uses AMF=SC/VC not AMF=SC
                double toaCorr = toaR[iWvl] / gasT[iWvl];
                double a = (toaCorr - rhoPath) / tupTdown;
                inPix.surfReflec[iView][iWvl] = a / (1 + spherAlb * a);
                inPix.diffuseFrac[iView][iWvl] = 1.0 - lutValues[iWvl][3];
            }
        }
    }

    public double[][][] getXaXbXc(InputPixelData inPix, double tau) {
        double[][][] xaXbXc = new double[3][2][4];
        Guardian.assertEquals("InputPixelData.nSpecWvl", inPix.nSpecWvl, nWvl);
        Guardian.assertNotNull("InputPixelData.diffuseFrac[][]", inPix.diffuseFrac);
        Guardian.assertNotNull("InputPixelData.surfReflec[][]", inPix.surfReflec);
        PixelGeometry geom;
        for (int iView=0; iView<2; iView++){
            geom = (iView == 0)? inPix.geom : inPix.geomFward;
            //final double rad2rfl = Math.PI / Math.cos(Math.toRadians(geom.sza));
            final float geomAMF = (float) ((1 / Math.cos(Math.toRadians(geom.sza))
                                            + 1 / Math.cos(Math.toRadians(geom.vza))));
            final double[] gasT = getGasTransmission(geomAMF, (float)inPix.wvCol, (float)(inPix.o3du/1000));
            double[][] lutValues = sdrLut.getValues(inPix.surfPressure, geom.vza, geom.sza, geom.razi, tau);
            for (int iWvl=0; iWvl<inPix.nSpecWvl; iWvl++){
                double rhoPath = lutValues[iWvl][0] * Math.PI / Math.cos(Math.toRadians(geom.sza));
                double tupTdown = lutValues[iWvl][1] / Math.cos(Math.toRadians(geom.sza));
                double spherAlb = lutValues[iWvl][2];

                double xa = 1/tupTdown/gasT[iWvl];
                double xb = rhoPath/tupTdown;
                double xc = spherAlb;

                xaXbXc[0][iView][iWvl] = xa;
                xaXbXc[1][iView][iWvl] = xb;
                xaXbXc[2][iView][iWvl] = xc;
            }
        }
        return xaXbXc;
    }

    public double[][] getAtmPar(InputPixelData inPix, double tau){
        PixelGeometry geom = inPix.geomFward;
        System.err.printf("reading values for:\n");
        System.err.printf("p: %f, VZA: %f, SZA: %f, PHI: %f, AOT: %f\n",inPix.surfPressure, geom.vza, geom.sza, geom.razi, tau);
        double[][] lutVals = this.sdrLut.getValues(inPix.surfPressure, geom.vza, geom.sza, geom.razi, tau);
        double[][] atmP = new double[4][4];
        for (int j=0; j<4; j++){
            atmP[0][j] = lutVals[j][0] * Math.PI / Math.cos(Math.toRadians(geom.sza));
            atmP[1][j] = lutVals[j][1] / Math.cos(Math.toRadians(geom.sza));
            atmP[2][j] = lutVals[j][2];
            atmP[3][j] = 1.0 - lutVals[j][3];
        }
        return atmP;
    }

    public boolean isInsideLut(InputPixelData ipd){
        Map<DimSelector, LutLimits> lutLimits = getLutLimits();
        boolean valid = (ipd.geom.vza >= lutLimits.get(DimSelector.VZA).min)
            && (ipd.geom.vza <= lutLimits.get(DimSelector.VZA).max)
            && (ipd.geom.sza >= lutLimits.get(DimSelector.SZA).min)
            && (ipd.geom.sza <= lutLimits.get(DimSelector.SZA).max)
            && (ipd.geom.razi >= lutLimits.get(DimSelector.AZI).min)
            && (ipd.geom.razi <= lutLimits.get(DimSelector.AZI).max);
            //&& (ipd.surfPressure >= lutLimits.get(DimSelector.HSF).min)
            //&& (ipd.surfPressure <= lutLimits.get(DimSelector.HSF).max);
        return valid;
    }

    public synchronized double getMaxAOT(InputPixelData ipd){
        final float geomAMF = (float) ((1 / Math.cos(Math.toRadians(ipd.geom.sza))
                                        + 1 / Math.cos(Math.toRadians(ipd.geom.vza))));
        final double[] gasT = getGasTransmission(geomAMF, (float)ipd.wvCol, (float)(ipd.o3du/1000));
        final double toa = ipd.toaReflec[0] / gasT[0];
        int iAot = 0;
        double[][] lutValues = sdrLut.getValues(ipd.surfPressure, ipd.geom.vza, ipd.geom.sza, ipd.geom.razi, aot[iAot]);
        double rhoPath1 = lutValues[0][0] * Math.PI / Math.cos(Math.toRadians(ipd.geom.sza));
        double rhoPath0 = rhoPath1;
        while (iAot < nAot-1 && rhoPath1 < toa) {
            rhoPath0 = rhoPath1;
            iAot++;
            lutValues = sdrLut.getValues(ipd.surfPressure, ipd.geom.vza, ipd.geom.sza, ipd.geom.razi, aot[iAot]);
            rhoPath1 = lutValues[0][0] * Math.PI / Math.cos(Math.toRadians(ipd.geom.sza));
        }
        if (iAot == 0) return 0.005;
        if (rhoPath1 < toa) return 2.0;
        return aot[iAot-1] + (aot[iAot] - aot[iAot-1]) * (toa - rhoPath0) / (rhoPath1 - rhoPath0);
    }

    // private methods
    private Map<DimSelector, LutLimits> getLutLimits(){
        Map<DimSelector, LutLimits> limits = new HashMap<DimSelector, LutLimits>(5);
        limits.put(DimSelector.VZA, new LutLimits(vza[0],vza[nVza-1]));
        limits.put(DimSelector.SZA, new LutLimits(sza[0],sza[nSza-1]));
        limits.put(DimSelector.AZI, new LutLimits(azi[0],azi[nAzi-1]));
        limits.put(DimSelector.HSF, new LutLimits(hsf[0],hsf[nHsf-1]));
        limits.put(DimSelector.AOT, new LutLimits(aot[0],aot[nAot-1]));
        return limits;
    }

    private int calcPosition(int[] indices, int[] sizes) {
        //old (((((iHsf*nVza + iVza)* nSza + iSza)* nAzi + iAzi)* nWvl + iWvl)* nAot + iAot)* nParameter + iPar
        // new version to store in MatrixLut
        //new (((((iHsf*nVza + iVza)* nSza + iSza)* nAzi + iAzi)* nAot + iAot)* nParameter + iPar)* nWvl + iWvl
        int pos = 0;
        for (int i = 0; i < sizes.length; i++) {
            pos = (pos * sizes[i] + indices[i]);
        }
        return pos;
    }

    private float[][] getDimensions() {
        return new float[][]{hsf, vza, sza, azi, aot};
    }

    private double[] getGasTransmission(float geomAmf, float wvCol, float o3AtmCm){
        return gasTransLut.getValues(geomAmf,wvCol,o3AtmCm);
    }

    private float[] readDimension(ByteBuffer bb) {
        int len = bb.getInt();
        return readDimension(bb, len);
    }

    private float[] readDimension(ByteBuffer bb, int len) {
        float[] dim = new float[len];
        for (int i = 0; i < len; i++) {
            dim[i] = bb.getFloat();
        }
        return dim;
    }

    private ByteBuffer readFileToByteBuffer(String lutName) {
        ByteBuffer bb = null;
        File momoFile = new File(lutName);
        Guardian.assertTrue("lookup table file exists", momoFile.exists());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(momoFile);
            byte[] buffer = new byte[(int) momoFile.length()];
            fis.read(buffer, 0, (int) momoFile.length());
            bb = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            throw new OperatorException(ex.getCause());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    System.err.println(ex.getMessage());
                    throw new OperatorException(ex.getCause());
                }
            }
        }
        return bb;
    }

    private float[] readValues(ByteBuffer bb) {
        int len = nWvl * nAot * nHsf * nAzi * nSza * nVza * nParameter;
        float[] val = new float[len];
        for (int iWvl = 0; iWvl < nWvl; iWvl++) {
            for (int iAot = 0; iAot < nAot; iAot++) {
                for (int iHsf = nHsf - 1; iHsf >= 0; iHsf--) {
                    for (int iAzi = 0; iAzi < nAzi; iAzi++) {
                        for (int iSza = 0; iSza < nSza; iSza++) {
                            for (int iVza = 0; iVza < nVza; iVza++) {
                                for (int iPar = 0; iPar < nParameter; iPar++) {
//                                    int pos = calcPosition(new int[]{iHsf, iVza, iSza, iAzi, iWvl, iAot, iPar},
//                                            new int[]{nHsf, nVza, nSza, nAzi, nWvl, nAot, nParameter});
                                    int pos = calcPosition(new int[]{iHsf, iVza, iSza, iAzi, iAot, iPar, iWvl},
                                            new int[]{nHsf, nVza, nSza, nAzi, nAot, nParameter, nWvl});
                                    val[pos] = bb.getFloat();
                                }
                            }
                        }
                    }
                }
            }
        }
        return val;
    }

    private float[] readO3corr() {
        final InputStream inputStream = MomoLut.class.getResourceAsStream("o3Correction.asc");
        BufferedReader reader = null;
        reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        float[] o3cwvl = new float[15 + 4 + 4];
        float[] o3c = new float[15 + 4 + 4];
        try {
            int i = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!(line.isEmpty() || line.startsWith("#") || line.startsWith("*"))) {
                    String[] stmp = line.split("[ \t]+");
                    o3cwvl[i] = Float.valueOf(stmp[1]);
                    o3c[i] = Float.valueOf(stmp[2]);
                    i++;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MomoLut.class.getName()).log(Level.SEVERE, null, ex);
            throw new OperatorException(ex.getMessage(), ex.getCause());
        }

        return getO3CorrOfBands(o3c, o3cwvl, wvl);
    }

    private float[] getO3CorrOfBands(float[] o3c, float[] o3cWvl, float[] lutWvl) {
        int jmin = 0;
        float[] lutO3c = new float[lutWvl.length];
        for (int i = 0; i < lutWvl.length; i++) {
            for (int j = 0; j < o3cWvl.length; j++) {
                if (Math.abs(o3cWvl[j] - lutWvl[i]) < Math.abs(o3cWvl[jmin] - lutWvl[i])) {
                    jmin = j;
                }
            }
            lutO3c[i] = o3c[jmin];
        }
        return lutO3c;
    }

    private VectorLookupTable readGasTransTable(String gasFileName) {
        ByteBuffer bb = readFileToByteBuffer(gasFileName);

        int nAng = bb.getInt();
        int nCwv = bb.getInt();
        int nOzo = bb.getInt();

        float[] ang = readDimension(bb, nAng);
        float[] cwv = readDimension(bb, nCwv);
        float[] ozo = readDimension(bb, nOzo);
        float[] tgLut = new float[nAng * nCwv * nOzo * nWvl];
        bb.asFloatBuffer().get(tgLut);

        float[] geomAmf = new float[nAng];
        for (int i=0; i<nAng; i++) geomAmf[i] = (float) (2.0 / Math.cos(Math.toRadians(ang[i])));
        //return new LookupTable(tgLut, new float[][]{geomAmf, cwv, ozo, wvl});
        return new VectorLookupTable(nWvl, tgLut, geomAmf, cwv, ozo);
    }

    private LookupTable readSdrKxLut(String sdrKxName) {
        ByteBuffer bb = readFileToByteBuffer(sdrKxName);
        int nDims = 7;     // number of dimensions
        int[] dimLen = new int[nDims];
        float[][] dims = new float[nDims][0];
        // dim arrays are stored with the fastest varying first
        // thus invert the order to store dims in LookupTable class
        dimLen[nDims-1] = 2;
        dims[nDims-1] = new float[]{0,1};
        int totalSize = dimLen[nDims-1]; // size of the LUT array
        for (int iDim=nDims-2; iDim>0; iDim--){
            dimLen[iDim] = bb.getInt();
            totalSize *= dimLen[iDim];
            dims[iDim] = new float[dimLen[iDim]];
            for(int j=0; j<dimLen[iDim]; j++) dims[iDim][j]=bb.getFloat();
        }
        dimLen[0] = nWvl;
        totalSize *= dimLen[0];
        dims[0] = wvl;

        float[] kxValues = new float[totalSize];
        bb.asFloatBuffer().get(kxValues);
        return new LookupTable(kxValues, dims);
    }

    private LookupTable readGasTransKxLut(String gasTransKxName) {
        ByteBuffer bb = readFileToByteBuffer(gasTransKxName);
        int nDims = 6;
        int[] dimLen = new int[nDims];
        float[][] dims = new float[nDims][0];
        // dim arrays are stored with the fastest varying first
        // thus invert the order to store dims in LookupTable class
        dimLen[nDims-1] = 2;
        dims[nDims-1] = new float[]{0,1};
        int totalSize = dimLen[nDims-1]; // size of the LUT array
        dimLen[nDims-2] = 2;
        dims[nDims-2] = new float[]{0,1};
        totalSize *= dimLen[nDims-2];
        for (int iDim=nDims-3; iDim>0; iDim--){
            dimLen[iDim] = bb.getInt();
            totalSize *= dimLen[iDim];
            dims[iDim] = new float[dimLen[iDim]];
            for(int j=0; j<dimLen[iDim]; j++) dims[iDim][j]=bb.getFloat();
        }
        dimLen[0] = nWvl;
        totalSize *= dimLen[0];
        dims[0] = wvl;
        
        float[] kxValues = new float[totalSize];
        bb.asFloatBuffer().get(kxValues);
        return new LookupTable(kxValues, dims);
    }

    public enum DimSelector {
        VZA, SZA, AZI, HSF, AOT;
    }

    public class LutLimits {
        public final float min;
        public final float max;

        public LutLimits(float min, float max) {
            this.min = min;
            this.max = max;
        }
    }
}

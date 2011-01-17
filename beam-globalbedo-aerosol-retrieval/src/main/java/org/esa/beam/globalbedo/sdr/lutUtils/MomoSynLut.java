/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.lutUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.math.LookupTable;

/**
 * Class representing a reflectance LUT for land aerosol / SDR retrieval
 *
 * @author Andreas Heckel
 * @version $Revision: 8034 $ $Date: 2010-01-20 14:47:34 +0000 (Mi, 20 Jan 2010) $
 */
public class MomoSynLut {
    private LookupTable[] lut;
    private float[] szaDim;
    private float[] raziDim;
    private float[] vzaDim;
    private float[] albDim;
    private float[] aotDim;
    private float[] presDim;
    private float[] logPresDim;
    private float[] specWvl;
    private final int aerosolModel;
    private final String[] LUT_LAND_AATSR_WAVELEN = {"00550.00","00665.00","00865.00","01610.00"};
    private final String[] LUT_LAND_MERIS_WAVELEN = {"00412.00","00442.00","00490.00","00510.00","00560.00",
                                                      "00620.00","00665.00","00681.00","00708.00","00753.00",
                                                      "00778.00","00865.00","00885.00"};

    public MomoSynLut(String lutPath, int aerosolModelID, String instrument, float[] specWvl) throws OperatorException {
        aerosolModel = aerosolModelID;

        if (!lutPath.endsWith(File.separator)) {
            lutPath += File.separator;
        }

        String lutFileName;
        String lutFileNameStub = lutPath + instrument.toUpperCase() + File.separator;
        lutFileNameStub += instrument.toUpperCase() + "_%WVLSTR%_" + String.format("%02d", aerosolModelID);

        // if Dimensions not already read, read Dimensions of LUT first
        if (szaDim == null) readLutDims(lutPath + "lutDimensions.asc");

        this.specWvl = specWvl;

        Map<String, String[]> lutWavelenStr = new HashMap<String, String[]>(2);
        lutWavelenStr.put("MERIS", LUT_LAND_MERIS_WAVELEN);
        lutWavelenStr.put("AATSR", LUT_LAND_AATSR_WAVELEN);

        // read the actual LUT (simulated Top Of Atmosphere Radiances)
        // for all wavelength

        this.lut = new LookupTable[specWvl.length];
        for (String ws : lutWavelenStr.get(instrument.toUpperCase())) {
            lutFileName = lutFileNameStub.replaceAll("%WVLSTR%", ws);
            float lutWvl = Float.valueOf(ws);
            int iWvl = findWvlIndex(lutWvl, specWvl);
            lut[iWvl] = readToaRad(lutFileName);
        }

    }

    public int getAerosolModel() {
        return aerosolModel;
    }

    public float[] getAlbDim() {
        return albDim;
    }

    public float[] getLogPresDim() {
        return logPresDim;
    }

    public float[] getPresDim() {
        return presDim;
    }

    public float[] getRaziDim() {
        return raziDim;
    }

    public float[] getSzaDim() {
        return szaDim;
    }

    public LookupTable[] getLut() {
        return lut;
    }

    public float[] getVzaDim() {
        return vzaDim;
    }

    public float[] getAotDim() {
        return aotDim;
    }

    public void subsecLUT(String instr, float pres, float o3, float vza, float vaa, float sza, float saa, float[] wvl, float[][][] a) {
        final int nWl = wvl.length;
        final int nAot = aotDim.length;
        final int nAlb = albDim.length;
        final boolean isAatsr = instr.equals("aatsr");
        final boolean isMeris = instr.equals("meris");
        
        float relAzi = Math.abs(saa - vaa);
        relAzi = (relAzi > 180.0f) ? 180 - (360 - relAzi) : 180 - relAzi;
        
        final double geomAMF = (1/Math.cos(Math.toRadians(sza))+1/Math.cos(Math.toRadians(vza)))/2;
        final double rad2rfl = Math.PI / Math.cos(Math.toRadians(sza));
        double o3Corr = 1.0;
        double wvCorr = 1.0;
        final double wvCol = 2.0; // constant water vapour column g/cm^2
        
        for (int iWl = 0; iWl < nWl; iWl++){
            if (isAatsr) {
                o3Corr = Math.exp(o3 / 1000 * SynergyConstants.o3CorrSlopeAatsr[iWl] * geomAMF);
                wvCorr = Math.exp(wvCol* SynergyConstants.wvCorrSlopeAatsr[iWl]);
            } else if (isMeris) {
                o3Corr = Math.exp(o3 / 1000 * SynergyConstants.o3CorrSlopeMeris[iWl] * geomAMF);
                wvCorr = Math.exp(wvCol* SynergyConstants.wvCorrSlopeMeris[iWl]);
            }
            for (int iAlb = 0; iAlb < nAlb; iAlb++) {
                for (int iAot = 0; iAot < nAot; iAot++) {
                    double[] x0 = {Math.log(pres), (double)vza, (double)relAzi, (double)sza,
                                    (double)aotDim[iAot], (double) albDim[iAlb]};
                    a[iWl][iAlb][iAot] = (float) (rad2rfl * lut[iWl].getValue(x0));
                    a[iWl][iAlb][iAot] *= o3Corr ;
                    a[iWl][iAlb][iAot] *= wvCorr;
                }
            }
        }
    }
    
    private int findWvlIndex(float lutWvl, float[] merisWvl) {
        int iWvl = 0;
        for (int i = 0; i < merisWvl.length; i++) {
            if (Math.abs(lutWvl - merisWvl[i]) < Math.abs(lutWvl - merisWvl[iWvl])) {
                iWvl = i;
            }
        }
        return iWvl;
    }

    private void readLutDims(String lutFileName) {
        BufferedReader inF = null;
        try {
            inF = new BufferedReader(new FileReader(lutFileName));
            String line;
            while ((line = inF.readLine()) != null) {
                String[] stmp = line.trim().split("[ ]+");
                if (stmp[0].toUpperCase().startsWith("PRES")) {
                    presDim = new float[stmp.length-2];
                    logPresDim = new float[stmp.length-2];
                    for (int i=2; i<stmp.length; i++) {
                        presDim[i-2] = Float.valueOf(stmp[i]);
                        logPresDim[i-2] = (float) Math.log(presDim[i-2]);
                    }
                }
                if (stmp[0].toUpperCase().startsWith("VIEWZ")) {
                    vzaDim = new float[stmp.length-2];
                    for (int i=2; i<stmp.length; i++) vzaDim[i-2] = Float.valueOf(stmp[i]);
                }
                if (stmp[0].toUpperCase().startsWith("RELAZ")) {
                    raziDim = new float[stmp.length-2];
                    for (int i=2; i<stmp.length; i++) raziDim[i-2] = Float.valueOf(stmp[i]);
                }
                if (stmp[0].toUpperCase().startsWith("SOLARZ")) {
                    szaDim = new float[stmp.length-2];
                    for (int i=2; i<stmp.length; i++) szaDim[i-2] = Float.valueOf(stmp[i]);
                }
                if (stmp[0].toUpperCase().startsWith("AOT")) {
                    aotDim = new float[stmp.length-2];
                    for (int i=2; i<stmp.length; i++) aotDim[i-2] = Float.valueOf(stmp[i]);
                }
                if (stmp[0].toUpperCase().startsWith("ALBE")) {
                    albDim = new float[stmp.length-2];
                    for (int i=2; i<stmp.length; i++) albDim[i-2] = Float.valueOf(stmp[i]);
                }
            }
        } catch (IOException ex) {
            throw new OperatorException("Could not read LUTs.");
//            Logger.getLogger(ReflectanceBinLUT.class.getName()).log(Level.SEVERE, "trying to open " + lutFileName, ex);
        } finally {
            try {
                if (inF != null) {
                    inF.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(MomoSynLut.class.getName()).log(Level.SEVERE, "trying to close " + lutFileName, ex);
            }
        }
    }

    private LookupTable readToaRad(String lutFileName) throws OperatorException {
        ObjectInputStream toaFile = null;
        LookupTable lut = null;
        try {

            toaFile = new ObjectInputStream(new FileInputStream(lutFileName));

            final int n = presDim.length*vzaDim.length*raziDim.length*szaDim.length*aotDim.length*albDim.length;
            float[] a = (float[]) toaFile.readObject();
            if (a.length != n) throw new OperatorException("Size of LUT array not equal size of Dimensions.");
            lut = new LookupTable(a, logPresDim, vzaDim, raziDim, szaDim, aotDim, albDim);
        } catch (Exception ex1) {
            String mess = "Could not open LUT file: \n" + ex1.getMessage();
            throw new OperatorException(mess, ex1);
        } finally {
            try {
                if (toaFile != null) toaFile.close();
            } catch (Exception ex2) {
                String mess = "trying to close " + lutFileName;
                //Logger.getLogger(ReflectanceBinLUT.class.getName()).log(Level.SEVERE, "trying to close " + lutFileName, ex);
                throw new OperatorException(mess, ex2);
            }
        }
        return lut;
    }
    
}

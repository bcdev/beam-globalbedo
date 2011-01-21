package org.esa.beam.globalbedo.bbdr;


import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.sdr.lutUtils.MomoLut;
import org.esa.beam.globalbedo.sdr.operators.InstrumentConsts;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.LookupTable;
import org.esa.beam.util.math.VectorLookupTable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility class for BBDR retrieval
 *
 * @author Olaf Danne
 */
public class BbdrUtils {

    public static final String aotLutPattern = "%INSTRUMENT%/%INSTRUMENT%_LUT_MOMO_ContinentalI_80_SDR_noG_v2.bin";
    public static final String cwvLutPattern = "%INSTRUMENT%/%INSTRUMENT%_LUT_6S_Tg_CWV_OZO.bin";

    /**
     * reads an AOT LUT (BBDR breadboard procedure GA_read_LUT_AOD)
     * uses {@link MomoLut} object of aerosol retrieval module
     *
     * currently not used
     *
     * @param instrument
     * @return
     */
//    public static MomoLut getAotLookupTable(String instrument) {
//        final String lutName = getAotLutName(instrument);
//        final int nLutBands = InstrumentConsts.getInstance().getnLutBands(instrument);
//        return new MomoLut(lutName, nLutBands);
//    }

    /**
     * reads a Water vapour / ozone vector LUT (BBDR breadboard procedure GA_read_LUT_WV_OZO)
     * The vector holds LUT values for each wavelength (instrument dependent)
     * This LUT is equivalent to the transposed IDL LUT:
     * - lut_cvw_ozo = transpose(lut_cvw_ozo)
     * --> lut_cvw_ozo[iAng,iCwv,iOzo,iBd] with wavelength (iBd) as most inner loop
     *
     * @param instrument
     * @return  VectorLookupTable
     */
    public static VectorLookupTable getTransposedCwvOzoVectorLookupTable(String instrument) {
        final String lutFileName = getCwvLutName(instrument);
        ByteBuffer bb = readLutFileToByteBuffer(lutFileName);
        int nAng = bb.getInt();
        int nCwv = bb.getInt();
        int nOzo = bb.getInt();
        int nBd = ((Integer) BbdrConstants.instrumentNumWavelengths.get(instrument)).intValue();

        float[] ang = readDimension(bb, nAng);
        float[] cwv = readDimension(bb, nCwv);
        float[] ozo = readDimension(bb, nOzo);
        float[] tgLut = new float[nAng * nCwv * nOzo * nBd];
        bb.asFloatBuffer().get(tgLut);

        // store in transposed sequence (see breadboard)
        VectorLookupTable vLut = new VectorLookupTable(nBd, tgLut, ang, cwv, ozo);
        return vLut;
    }

    /**
     * reads a Water vapour / ozone LUT (BBDR breadboard procedure GA_read_LUT_WV_OZO)
     * * This LUT is equivalent to the transposed IDL LUT:
     * - lut_cvw_ozo = transpose(lut_cvw_ozo)
     * --> lut_cvw_ozo[iAng,iCwv,iOzo,iBd] with wavelength (iBd) as most inner loop
     *
     * @param instrument
     * @return  LookupTable
     */
    public static LookupTable getTransposedCwvOzoLookupTable(String instrument) {
        final String lutFileName = getCwvLutName(instrument);
        ByteBuffer bb = readLutFileToByteBuffer(lutFileName);
        int nAng = bb.getInt();
        int nCwv = bb.getInt();
        int nOzo = bb.getInt();

        float[] ang = readDimension(bb, nAng);
        float[] cwv = readDimension(bb, nCwv);
        float[] ozo = readDimension(bb, nOzo);

        float[] wvl = getInstrumentWavelengths(instrument);
        final int nWvl = wvl.length;
        float[] tgLut = new float[nAng * nCwv * nOzo * nWvl];
        bb.asFloatBuffer().get(tgLut);

        // store in transposed sequence (see breadboard)
        return new LookupTable(tgLut, ang, cwv, ozo, wvl);
    }

    /**
     * reads an AOT LUT (BBDR breadboard procedure GA_read_LUT_AOD)
     * * This LUT is equivalent to the original IDL LUT:
     * for bd = 0, nm_bnd - 1 do  $
     *   for jj = 0, nm_aot - 1 do $
     *     for ii = 0, nm_hsf - 1 do $
     *       for k = 0, nm_azm - 1 do $
	 *         for j = 0, nm_asl - 1 do $
     *           for i = 0, nm_avs - 1 do begin
     *             readu, 1, aux
     *             lut[*, i, j, nm_azm - k - 1, ii, jj, bd] = aux
     *
     * @param instrument
     * @return  LookupTable
     */
    public static LookupTable getAotLookupTable(String instrument) {
        final String lutFileName = getAotLutName(instrument);

        ByteBuffer bb = readLutFileToByteBuffer(lutFileName);

        // read LUT dimensions and values
        float[] vza = readDimension(bb);
        int nVza = vza.length;
        float[] sza = readDimension(bb);
        int nSza = sza.length;
        float[] azi = readDimension(bb);
        int nAzi = azi.length;
        float[] hsf = readDimension(bb);
        int nHsf = hsf.length;
        // invert order to store stirctly increasing dimension in lut
        // swapping the actual values in the lut is taken care off in readValues()
        for (int i = 0; i < nHsf / 2; i++) {
            float swap = hsf[nHsf - 1 - i];
            hsf[nHsf - 1 - i] = hsf[i];
            hsf[i] = swap;
        }
        float[] aot = readDimension(bb);
        int nAot = aot.length;

        float[] parameters = new float[] {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
        int nParameters = parameters.length;

        float[] wvl = getInstrumentWavelengths(instrument);
        final int nWvl = wvl.length;

        float[] tgLut = new float[nParameters * nVza * nSza * nAzi * nHsf * nAot * nWvl];
        bb.asFloatBuffer().get(tgLut);

        // store in original sequence (see breadboard: loop over bd, jj, ii, k, j, i in GA_read_lut_AOD
        return new LookupTable(tgLut, wvl, aot, hsf, azi, sza, vza, parameters);
    }

    /**
     * converts ang values to geomAmf values (BBDR breadboard l.890)
     *
     * @param ang
     * @return
     */
    public static float[] convertAngArrayToAmfArray(float[] ang) {
        float[] geomAmf = new float[ang.length];
        for (int i=0; i<geomAmf.length; i++) {
            geomAmf[i] = (float) (2.0 / Math.cos(Math.toRadians(ang[i])));
        }
        return geomAmf;
    }

    //////////////////////////////////////////////////////////////////
    // end of public
    //////////////////////////////////////////////////////////////////

    private static float[] readDimension(ByteBuffer bb) {
        int len = bb.getInt();
        return readDimension(bb, len);
    }


    private static float[] readDimension(ByteBuffer bb, int len) {
        float[] dim = new float[len];
        for (int i = 0; i < len; i++) {
            dim[i] = bb.getFloat();
        }
        return dim;
    }

    private static String getAotLutName(String instrument) {
        final String aotLutPath = InstrumentConsts.getInstance().getLutPath() + File.separator + aotLutPattern;
        return aotLutPath.replace("%INSTRUMENT%", instrument);
    }

    private static String getCwvLutName(String instrument) {
        final String cwvLutPath = InstrumentConsts.getInstance().getLutPath() + File.separator + cwvLutPattern;
        return cwvLutPath.replace("%INSTRUMENT%", instrument);
    }

    private static float[] getInstrumentWavelengths(String instrument) {
        float[] wvl = null;
        if (instrument.equalsIgnoreCase("MERIS")) {
            wvl = new float[BbdrConstants.MERIS_WAVELENGHTS.length];
            for (int i = 0; i < wvl.length; i++) {
                wvl[i] = BbdrConstants.MERIS_WAVELENGHTS[i];
            }
        } else if (instrument.equalsIgnoreCase("AATSR")) {
            wvl = new float[BbdrConstants.MERIS_WAVELENGHTS.length];
            for (int i = 0; i < wvl.length; i++) {
                wvl[i] = BbdrConstants.MERIS_WAVELENGHTS[i];
            }
        } else if (instrument.equalsIgnoreCase("VGT")) {
            wvl = new float[BbdrConstants.MERIS_WAVELENGHTS.length];
            for (int i = 0; i < wvl.length; i++) {
                wvl[i] = BbdrConstants.MERIS_WAVELENGHTS[i];
            }
        }

        return wvl;
    }

    private static ByteBuffer readLutFileToByteBuffer(String lutName) {
        ByteBuffer bb = null;
        File lutFile = new File(lutName);
        Guardian.assertTrue("lookup table file exists", lutFile.exists());
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(lutFile);
            byte[] buffer = new byte[(int) lutFile.length()];
            fis.read(buffer, 0, (int) lutFile.length());
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
                }
            }
        }
        return bb;
    }

}

package org.esa.beam.globalbedo.bbdr;


import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.sdr.lutUtils.MomoLut;
import org.esa.beam.globalbedo.sdr.operators.InstrumentConsts;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.LookupTable;

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
    public static final String aotKxLutPattern = "%INSTRUMENT%/%INSTRUMENT%_LUT_MOMO_ContinentalI_80_SDR_noG_Kx-AOD_v2.bin";
    public static final String cwvLutPattern = "%INSTRUMENT%/%INSTRUMENT%_LUT_6S_Tg_CWV_OZO.bin";
    public static final String cwvKxLutPattern = "%INSTRUMENT%/%INSTRUMENT%_LUT_6S_Kx-CWV_OZO.bin";
    public static final String nskyLutDwPattern = "%INSTRUMENT%/%INSTRUMENT%_ContinentalI_80_Nsky_dw.bin";
    public static final String nskyLutDUpPattern = "%INSTRUMENT%/%INSTRUMENT%_ContinentalI_80_Nsky_up.bin";

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
     * reads an AOT LUT (BBDR breadboard procedure GA_read_LUT_AOD)
     * * This LUT is equivalent to the original IDL LUT:
     * for bd = 0, nm_bnd - 1 do  $
     * for jj = 0, nm_aot - 1 do $
     * for ii = 0, nm_hsf - 1 do $
     * for k = 0, nm_azm - 1 do $
     * for j = 0, nm_asl - 1 do $
     * for i = 0, nm_avs - 1 do begin
     * readu, 1, aux
     * lut[*, i, j, nm_azm - k - 1, ii, jj, bd] = aux
     * A LUT value can be accessed with
     * lut.getValue(new double[]{wvlValue, aotValue, hsfValue, aziValue, szaValue, vzaValue, parameterValue});
     *
     * @param instrument
     *
     * @return LookupTable
     */
    public static AotLookupTable getAotLookupTable(String instrument) {
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

        float[] parameters = new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
        int nParameters = parameters.length;

        float[] wvl = getInstrumentWavelengths(instrument);
        final int nWvl = wvl.length;

        float[] tgLut = new float[nParameters * nVza * nSza * nAzi * nHsf * nAot * nWvl];
        bb.asFloatBuffer().get(tgLut);

        bb.position(bb.position() + 4 * tgLut.length);
        readDimension(bb, nWvl); // skip wavelengths
        float[] solarIrradiances = readDimension(bb, nWvl);

        // store in original sequence (see breadboard: loop over bd, jj, ii, k, j, i in GA_read_lut_AOD
        AotLookupTable aotLut = new AotLookupTable();
        aotLut.setLut(new LookupTable(tgLut, wvl, aot, hsf, azi, sza, vza, parameters));
        aotLut.setWvl(wvl);
        aotLut.setSolarIrradiance(solarIrradiances);

        return aotLut;
//        return new LookupTable(tgLut, wvl, aot, hsf, azi, sza, vza, parameters);
    }

    /**
     * reads an AOT Kx LUT (BBDR breadboard procedure GA_read_LUT_AOD)
     * <p/>
     * A LUT value can be accessed with
     * lut.getValue(new double[]{wvlValue, aotValue, hsfValue, aziValue, szaValue, vzaValue, parameterValue});
     *
     * @param instrument
     *
     * @return LookupTable
     */
    public static LookupTable getAotKxLookupTable(String instrument) {
        final String lutFileName = getAotKxLutName(instrument);

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
        float[] aot = readDimension(bb);
        int nAot = aot.length;

        float[] kx = new float[]{1.0f, 2.0f};
        int nKx = kx.length;

        float[] wvl = getInstrumentWavelengths(instrument);
        final int nWvl = wvl.length;

        float[] tgLut = new float[nKx * nVza * nSza * nAzi * nHsf * nAot * nWvl];
        bb.asFloatBuffer().get(tgLut);

        // store in original sequence
        return new LookupTable(tgLut, wvl, aot, hsf, azi, sza, vza, kx);
    }


    /**
     * reads a Water vapour / ozone LUT (BBDR breadboard procedure GA_read_LUT_WV_OZO)
     * * This LUT is equivalent to the transposed IDL LUT:
     * - lut_cvw_ozo = transpose(lut_cvw_ozo)
     * --> lut_cvw_ozo[iAng,iCwv,iOzo,iBd] with wavelength (iBd) as most inner loop
     * * A LUT value can be accessed with
     * lut.getValue(new double[]{angValue, cwvValue, ozoValue, wvlValue});
     *
     * @param instrument
     *
     * @return LookupTable
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
     * reads a Water vapour / ozone Kx LUT (BBDR breadboard procedure GA_read_LUT_WV_OZO)
     * * This LUT is equivalent to the original (not transposed!) IDL LUT.
     * * A LUT value can be accessed with
     * lut.getValue(new double[]{wvlValue, ozoValue, cwvValue, angValue, kxValue, kxcaseValue});
     *
     * @param instrument
     *
     * @return LookupTable
     */
    public static LookupTable getCwvOzoKxLookupTable(String instrument) {
        final String lutFileName = getCwvKxLutName(instrument);

        ByteBuffer bb = readLutFileToByteBuffer(lutFileName);

        // read LUT dimensions and values
        int nAng = bb.getInt();
        float[] ang = readDimension(bb, nAng);
        int nCwv = bb.getInt();
        float[] cwv = readDimension(bb, nCwv);
        int nOzo = bb.getInt();
        float[] ozo = readDimension(bb, nOzo);

        float[] kx = new float[]{1.0f, 2.0f};
        int nKx = kx.length;

        float[] kxcase = new float[]{1.0f, 2.0f};
        int nKxcase = kx.length;

        float[] wvl = getInstrumentWavelengths(instrument);
        final int nWvl = wvl.length;

        float[] tgLut = new float[nKx * nKxcase * nWvl * nAng * nCwv * nOzo];
        bb.asFloatBuffer().get(tgLut);

        // store in original sequence
        // todo: sequence of kx and kxcase is unclear. try to find out in breadboard...
        return new LookupTable(tgLut, wvl, ozo, cwv, ang, kx, kxcase);
    }


    /**
     * converts ang values to geomAmf values (BBDR breadboard l.890)
     *
     * @param ang
     *
     * @return
     */
    public static float[] convertAngArrayToAmfArray(float[] ang) {
        float[] geomAmf = new float[ang.length];
        for (int i = 0; i < geomAmf.length; i++) {
            geomAmf[i] = (float) (2.0 / Math.cos(Math.toRadians(ang[i])));
        }
        return geomAmf;
    }

    /**
     * reads a NSky DW LUT (BBDR breadboard procedure GA_read_LUT_Nsky, first LUT)
     * * This LUT is equivalent to the original IDL LUT.
     * * A LUT value can be accessed with
     * lut.getValue(new double[]{wvlValue, ozoValue, cwvValue, angValue, kxValue, kxcaseValue});
     *
     * @param instrument
     *
     * @return LookupTable
     */
    public static NskyLookupTable getNskyLookupTableDw(String instrument) {
        final String lutFileName = getNskyDwLutName(instrument);

        ByteBuffer bb = readLutFileToByteBuffer(lutFileName);

        // read LUT dimensions and values
        float[] sza = readDimension(bb);
        int nSza = sza.length;
        float[] hsf = readDimension(bb);
        int nHsf = hsf.length;
        float[] aot = readDimension(bb);
        int nAot = aot.length;

        int nSpec = bb.getInt();
        float[] spec = new float[nSpec];
        for (int i = 0; i < spec.length; i++) {
            spec[i] = (i + 1) * 1.0f;
        }

        double kppVol = bb.getDouble();
        double kppGeo = bb.getDouble();

        // todo: again, we have an array of length 2 as innermost part of the LUT,
        // which has no meanigful name in breadboard --> clarify
        float[] values = new float[]{1.0f, 2.0f};
        int nValues = values.length;

        float[] tgLut = new float[nValues * nSza * nHsf * nAot * nSpec];
        bb.asFloatBuffer().get(tgLut);

        // store in original sequence (see breadboard: GA_read_LUT_Nsky)
        NskyLookupTable nskyLut = new NskyLookupTable();
        nskyLut.setLut(new LookupTable(tgLut, spec, aot, hsf, sza, values));
        nskyLut.setKppGeo(kppGeo);
        nskyLut.setKppVol(kppVol);

        return nskyLut;
    }

    /**
     * reads a NSky DW LUT (BBDR breadboard procedure GA_read_LUT_Nsky, first LUT)
     * * This LUT is equivalent to the original IDL LUT.
     * * A LUT value can be accessed with
     * lut.getValue(new double[]{wvlValue, ozoValue, cwvValue, angValue, kxValue, kxcaseValue});
     *
     * @param instrument
     *
     * @return LookupTable
     */
    public static NskyLookupTable getNskyLookupTableUp(String instrument) {
        final String lutFileName = getNskyUpLutName(instrument);

        ByteBuffer bb = readLutFileToByteBuffer(lutFileName);

        // read LUT dimensions and values
        float[] vza = readDimension(bb);
        int nVza = vza.length;
        float[] hsf = readDimension(bb);
        int nHsf = hsf.length;
        float[] aot = readDimension(bb);
        int nAot = aot.length;

        int nSpec = bb.getInt();
        float[] spec = new float[nSpec];
        for (int i = 0; i < spec.length; i++) {
            spec[i] = (i + 1) * 1.0f;
        }
        double kppVol = bb.getDouble();
        double kppGeo = bb.getDouble();

        // todo: again, we have an array of length 2 as innermost part of the LUT,
        // which has no meanigful name in breadboard --> clarify
        float[] values = new float[]{1.0f, 2.0f};
        int nValues = values.length;

        float[] tgLut = new float[nValues * nVza * nHsf * nAot * nSpec];
        bb.asFloatBuffer().get(tgLut);

        // store in original sequence (see breadboard: GA_read_LUT_Nsky)
        NskyLookupTable nskyLut = new NskyLookupTable();
        nskyLut.setLut(new LookupTable(tgLut, spec, aot, hsf, vza, values));
        nskyLut.setKppGeo(kppGeo);
        nskyLut.setKppVol(kppVol);

        return nskyLut;
//        return new LookupTable(tgLut, spec, aot, hsf, vza, values);
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

    private static String getAotKxLutName(String instrument) {
        final String aotLutPath = InstrumentConsts.getInstance().getLutPath() + File.separator + aotKxLutPattern;
        return aotLutPath.replace("%INSTRUMENT%", instrument);
    }

    private static String getCwvLutName(String instrument) {
        final String cwvLutPath = InstrumentConsts.getInstance().getLutPath() + File.separator + cwvLutPattern;
        return cwvLutPath.replace("%INSTRUMENT%", instrument);
    }

    private static String getCwvKxLutName(String instrument) {
        final String cwvLutPath = InstrumentConsts.getInstance().getLutPath() + File.separator + cwvKxLutPattern;
        return cwvLutPath.replace("%INSTRUMENT%", instrument);
    }

    private static String getNskyDwLutName(String instrument) {
        final String cwvLutPath = InstrumentConsts.getInstance().getLutPath() + File.separator + nskyLutDwPattern;
        return cwvLutPath.replace("%INSTRUMENT%", instrument);
    }

    private static String getNskyUpLutName(String instrument) {
        final String cwvLutPath = InstrumentConsts.getInstance().getLutPath() + File.separator + nskyLutDUpPattern;
        return cwvLutPath.replace("%INSTRUMENT%", instrument);
    }


    private static float[] getInstrumentWavelengths(String instrument) {
        return (float[]) BbdrConstants.instrumentWavelengths.get(instrument);
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

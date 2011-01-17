package org.esa.beam.globalbedo.bbdr;


import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.sdr.lutUtils.MomoLut;
import org.esa.beam.globalbedo.sdr.operators.InstrumentConsts;
import org.esa.beam.util.Guardian;
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

    public static MomoLut readAotLookupTable(String instrument) {
        final String lutName = getAotLutName(instrument);
        final int nLutBands = InstrumentConsts.getInstance().getnLutBands(instrument);
        return new MomoLut(lutName, nLutBands);
    }

    public static VectorLookupTable readCwvOzoLookupTable(String instrument) {

        final String lutFileName = getCwvLutName(instrument);
        ByteBuffer bb = readLutFileToByteBuffer(lutFileName);
        int nAng = bb.getInt();
        int nCwv = bb.getInt();
        int nOzo = bb.getInt();
        int nWvl = 15;

        float[] ang = readDimension(bb, nAng);
        float[] cwv = readDimension(bb, nCwv);
        float[] ozo = readDimension(bb, nOzo);
        float[] tgLut = new float[nAng * nCwv * nOzo * nWvl];
        bb.asFloatBuffer().get(tgLut);

        float[] geomAmf = new float[nAng];
        for (int i=0; i<nAng; i++) geomAmf[i] = (float) (2.0 / Math.cos(Math.toRadians(ang[i])));
        return new VectorLookupTable(nWvl, tgLut, geomAmf, cwv, ozo);
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

    public static ByteBuffer readLutFileToByteBuffer(String lutName) {
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

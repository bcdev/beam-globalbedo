package org.esa.beam.globalbedo.bbdr;

import junit.framework.TestCase;
import org.esa.beam.util.math.LookupTable;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class MerisLutTest extends TestCase {

    public void testLutAot() {
        AotLookupTable aotLut = BbdrUtils.getAotLookupTable("MERIS");
        assertNotNull(aotLut);
        LookupTable lut = aotLut.getLut();
        assertNotNull(lut);

        assertEquals(7, lut.getDimensionCount());

        final double[] parametersArray = lut.getDimension(6).getSequence();
        final int nParameters = parametersArray.length;
        assertEquals(5, nParameters);     //  Parameters
        assertEquals(2.0, parametersArray[1], 1.E-4);
        assertEquals(3.0, parametersArray[2], 1.E-4);
        assertEquals(5.0, parametersArray[4], 1.E-4);

        final double[] vzaArray = lut.getDimension(5).getSequence();
        final int nVza = vzaArray.length;
        assertEquals(13, nVza);     //  VZA
        assertEquals(12.76, vzaArray[3], 1.E-4);
        assertEquals(24.24, vzaArray[5], 1.E-4);
        assertEquals(35.68, vzaArray[7], 1.E-4);

        final double[] szaArray = lut.getDimension(4).getSequence();
        final int nSza = szaArray.length;
        assertEquals(14, nSza);     //  SZA
        assertEquals(6.97, szaArray[2], 1.E-4);
        assertEquals(18.51, szaArray[4], 1.E-4);
        assertEquals(29.96, szaArray[6], 1.E-4);

        final double[] aziArray = lut.getDimension(3).getSequence();
        final int nAzi = aziArray.length;
        assertEquals(19, nAzi);     //  AZI
        assertEquals(10.0, aziArray[1], 1.E-4);
        assertEquals(130.0, aziArray[13], 1.E-4);
        assertEquals(150.0, aziArray[15], 1.E-4);

        final double[] hsfArray = lut.getDimension(2).getSequence();
        final int nHsf = hsfArray.length;
        assertEquals(4, nHsf);     //  HSF
//        assertEquals(746.825, hsfArray[1], 1.E-3);
//        assertEquals(898.746, hsfArray[2], 1.E-3);
//        assertEquals(1013.25, hsfArray[3], 1.E-3);
        assertEquals(0.0, hsfArray[0], 1.E-3);
        assertEquals(1.0, hsfArray[1], 1.E-3);
        assertEquals(2.5, hsfArray[2], 1.E-3);
        assertEquals(8.0, hsfArray[3], 1.E-3);

        final double[] aotArray = lut.getDimension(1).getSequence();
        final int nAot = aotArray.length;
        assertEquals(9, nAot);     //  AOT
        assertEquals(0.1, aotArray[2], 1.E-3);
        assertEquals(0.2, aotArray[3], 1.E-3);
        assertEquals(1.5, aotArray[7], 1.E-3);

        final double[] wvlArray = lut.getDimension(0).getSequence();
        final int nWvl = wvlArray.length;
        assertEquals(15, nWvl);     //  AOT
        assertEquals(412.0, wvlArray[0], 1.E-3);
        assertEquals(442.0, wvlArray[1], 1.E-3);
        assertEquals(900.0, wvlArray[14], 1.E-3);

        // solar irradiances
        assertNotNull(aotLut.getSolarIrradiance());
        assertEquals(15, aotLut.getSolarIrradiance().length);
        assertEquals(1879.69f, aotLut.getSolarIrradiance()[1]);
        assertEquals(1803.06f, aotLut.getSolarIrradiance()[4]);
        assertEquals(958.059f, aotLut.getSolarIrradiance()[12]);

        // first values in LUT
        // iWvl=0, iAot0, iHsf0, iAzi=0, iSza=0, iVza=0, iParameters=0..4:
        double[] coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
        double value = lut.getValue(coord);
        assertEquals(0.03574, value, 1.E-4);

        coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0};
        value = lut.getValue(coord);
        assertEquals(0.74368, value, 1.E-4);

        coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.0};
        value = lut.getValue(coord);
        assertEquals(0.21518, value, 1.E-4);

        coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0};
        value = lut.getValue(coord);
        assertEquals(0.84468, value, 1.E-4);

        coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 5.0};
        value = lut.getValue(coord);
        assertEquals(0.84468, value, 1.E-4);

        // iWvl=0, iAot0, iHsf0, iAzi=0, iSza=0, iVza=1, iParameters=4:
        coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 2.56, 5.0};
        value = lut.getValue(coord);
        assertEquals(0.84454, value, 1.E-4);

        // values somewhere inside LUT:
        coord = new double[]{
                wvlArray[7], aotArray[2], hsfArray[1], aziArray[6], szaArray[9], vzaArray[11], parametersArray[3]
        };
        value = lut.getValue(coord);
        assertEquals(0.930663, value, 1.E-4);

        coord = new double[]{
                wvlArray[4], aotArray[1], hsfArray[2], aziArray[14], szaArray[5], vzaArray[3], parametersArray[2]
        };
        value = lut.getValue(coord);
        assertEquals(0.060401, value, 1.E-4);

        coord = new double[]{
                wvlArray[8], aotArray[0], hsfArray[1], aziArray[16], szaArray[3], vzaArray[10], parametersArray[1]
        };
        value = lut.getValue(coord);
        assertEquals(0.944405, value, 1.E-4);

        // last values in LUT:
        coord = new double[]{900.0, 2.0, 1013.25, 180.0, 69.989, 64.279, 3.0};
        value = lut.getValue(coord);
        assertEquals(0.192216, value, 1.E-4);

        coord = new double[]{900.0, 2.0, 1013.25, 180.0, 69.989, 64.279, 4.0};
        value = lut.getValue(coord);
        assertEquals(0.998259, value, 1.E-4);

        coord = new double[]{900.0, 2.0, 1013.25, 180.0, 69.989, 64.279, 5.0};
        value = lut.getValue(coord);
        assertEquals(0.998625, value, 1.E-4);
    }

    public void testLutAotKx() {
        LookupTable lut = BbdrUtils.getAotKxLookupTable("MERIS");
        assertNotNull(lut);

        assertEquals(7, lut.getDimensionCount());

        final double[] kxArray = lut.getDimension(6).getSequence();
        final int nKx = kxArray.length;
        assertEquals(2, nKx);     //  Parameters
        assertEquals(1.0, kxArray[0], 1.E-4);
        assertEquals(2.0, kxArray[1], 1.E-4);

        final double[] vzaArray = lut.getDimension(5).getSequence();
        final int nVza = vzaArray.length;
        assertEquals(13, nVza);     //  VZA
        assertEquals(12.76, vzaArray[3], 1.E-4);
        assertEquals(24.24, vzaArray[5], 1.E-4);
        assertEquals(35.68, vzaArray[7], 1.E-4);

        final double[] szaArray = lut.getDimension(4).getSequence();
        final int nSza = szaArray.length;
        assertEquals(14, nSza);     //  SZA
        assertEquals(6.97, szaArray[2], 1.E-4);
        assertEquals(18.51, szaArray[4], 1.E-4);
        assertEquals(29.96, szaArray[6], 1.E-4);

        final double[] aziArray = lut.getDimension(3).getSequence();
        final int nAzi = aziArray.length;
        assertEquals(19, nAzi);     //  AZI
        assertEquals(10.0, aziArray[1], 1.E-4);
        assertEquals(130.0, aziArray[13], 1.E-4);
        assertEquals(150.0, aziArray[15], 1.E-4);

        final double[] hsfArray = lut.getDimension(2).getSequence();
        final int nHsf = hsfArray.length;
        assertEquals(4, nHsf);     //  HSF
        assertEquals(1.0, hsfArray[1], 1.E-3);
        assertEquals(2.5, hsfArray[2], 1.E-3);
        assertEquals(7.998, hsfArray[3], 1.E-3);

        final double[] aotArray = lut.getDimension(1).getSequence();
        final int nAot = aotArray.length;
        assertEquals(9, nAot);     //  AOT
        assertEquals(0.1, aotArray[2], 1.E-3);
        assertEquals(0.2, aotArray[3], 1.E-3);
        assertEquals(1.5, aotArray[7], 1.E-3);

        final double[] wvlArray = lut.getDimension(0).getSequence();
        final int nWvl = wvlArray.length;
        assertEquals(15, nWvl);     //  AOT
        assertEquals(412.0, wvlArray[0], 1.E-3);
        assertEquals(442.0, wvlArray[1], 1.E-3);
        assertEquals(900.0, wvlArray[14], 1.E-3);

        // first values in LUT
        // iWvl=0, iAot0, iHsf0, iAzi=0, iSza=0, iVza=0..1, iKx=0..1:
        double[] coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0};
        double value = lut.getValue(coord);
        assertEquals(-0.06027, value, 1.E-4);

        coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.0};
        value = lut.getValue(coord);
        assertEquals(0.056184, value, 1.E-4);

        coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 2.56, 1.0};
        value = lut.getValue(coord);
        assertEquals(-0.059273, value, 1.E-4);

        coord = new double[]{412.0, 0.0, 0.0, 0.0, 0.0, 2.56, 2.0};
        value = lut.getValue(coord);
        assertEquals(0.055945, value, 1.E-4);

        // values somewhere inside LUT:
        coord = new double[]{wvlArray[7], aotArray[2], hsfArray[1], aziArray[6], szaArray[9], vzaArray[11], kxArray[0]};
        value = lut.getValue(coord);
        assertEquals(-0.082877, value, 1.E-4);

        coord = new double[]{wvlArray[4], aotArray[1], hsfArray[2], aziArray[14], szaArray[5], vzaArray[3], kxArray[1]};
        value = lut.getValue(coord);
        assertEquals(-0.3205, value, 1.E-4);

        coord = new double[]{
                wvlArray[8], aotArray[0], hsfArray[1], aziArray[16], szaArray[3], vzaArray[10], kxArray[0]
        };
        value = lut.getValue(coord);
        assertEquals(-0.01571, value, 1.E-4);

        // last values in LUT:
        coord = new double[]{900.0, 2.0, 1013.25, 180.0, 69.989, 64.279, 1.0};
        value = lut.getValue(coord);
        assertEquals(-0.197412, value, 1.E-4);

        coord = new double[]{900.0, 2.0, 1013.25, 180.0, 69.989, 64.279, 2.0};
        value = lut.getValue(coord);
        assertEquals(0.007395, value, 1.E-4);
    }

    public void testCwvOzoLut() {
        LookupTable lut = BbdrUtils.getTransposedCwvOzoLookupTable("MERIS");
        assertNotNull(lut);

        assertEquals(4, lut.getDimensionCount());

        final double[] angArray = lut.getDimension(0).getSequence();
        final int nAng = angArray.length;
        assertEquals(7, nAng);     //  ANG
        assertEquals(0.0, angArray[0], 1.E-4);
        assertEquals(50.0, angArray[3], 1.E-4);
        assertEquals(70.0, angArray[6], 1.E-4);

        final double[] cwvArray = lut.getDimension(1).getSequence();
        final int nCwv = cwvArray.length;
        assertEquals(4, nCwv);     //  CWV
        assertEquals(0.0, cwvArray[0], 1.E-4);
        assertEquals(3.0, cwvArray[2], 1.E-4);
        final double[] ozoArray = lut.getDimension(2).getSequence();
        final int nOzo = ozoArray.length;
        assertEquals(4, nOzo);     //  OZO
        assertEquals(0.3, ozoArray[1], 1.E-4);
        assertEquals(0.6, ozoArray[3], 1.E-4);
        final double[] wvlArray = lut.getDimension(3).getSequence();
        final int nWvl = wvlArray.length;
        assertEquals(15, nWvl);     //  WVL
        assertEquals(560.0f, wvlArray[4], 1.E-4);
        assertEquals(665.0f, wvlArray[6], 1.E-4);
        assertEquals(753.0f, wvlArray[9], 1.E-4);

        int index = 0;
        // print whole LUT
//        for (int iAng = 0; iAng < nAng; iAng++) {
//            for (int iCwv = 0; iCwv < nCwv; iCwv++) {
//                for (int iOzo = 0; iOzo < nOzo; iOzo++) {
//                    for (int iWvl = 0; iWvl < nWvl; iWvl++) {
//                        double[] coordinate = new double[]{
//                            angArray[iAng],
//                            cwvArray[iCwv],
//                            ozoArray[iOzo],
//                            wvlArray[iWvl]
//                        };
//                        System.out.println("LUT: " + index + ", " + lut.getValue(coordinate));
//                        index++;
//                    }
//                }
//            }
//        }

        // iAng=0, iCwv=0, iOzo=0, iWvl=0:
        double[] coord = new double[]{0.0, 0.0, 0.1, 412.0};
        assertEquals(1.0, lut.getValue(coord), 1.E-4);
        // The index in 1D LUT array (lut_cwv_ozo as retrieved in IDL breadboard) computes like this:
        // index = iAng*(nCwv*nOzo*nWvl) + iCwv*(nOzo*nWvl) + iOzo*nWvl + iWvl

        // iAng=2, iCwv=1, iOzo=2, iWvl=4:
        index = 2 * (nCwv * nOzo * nWvl) + 1 * (nOzo * nWvl) + 2 * nWvl + 4;
        assertEquals(574, index);
        coord = new double[]{40.0, 1.5, 0.5, 560.0};
        assertEquals(0.879466, lut.getValue(coord), 1.E-4);

        // iAng=4, iCwv=0, iOzo=3, iWvl=10:
        index = 4 * (nCwv * nOzo * nWvl) + 3 * nWvl + 10;
        assertEquals(1015, index);
        coord = new double[]{60.0, 0.0, 0.6, 760.0};
        assertEquals(0.275625, lut.getValue(coord), 1.E-4);

        // iAng=nAng-1, iCwv=nCwv-1, iOzo=nOzo-1, iWvl=nWvl-1:
        index = (nAng - 1) * (nCwv * nOzo * nWvl) + (nCwv - 1) * (nOzo * nWvl) + (nOzo - 1) * nWvl + nWvl - 1;
        assertEquals(1679, index);
        coord = new double[]{70.0, 4.5, 0.6, 900.0};
        assertEquals(0.42336, lut.getValue(coord), 1.E-4);
    }

    public void testCwvOzoLutArray() {
        float[][][][] lutArray = BbdrUtils.getCwvOzoLookupTableArray("MERIS");
        assertNotNull(lutArray);
        final int dimWvl = lutArray.length;
        final int dimCwv = lutArray[0].length;
        final int dimOzo = lutArray[0][0].length;
        final int dimAng = lutArray[0][0][0].length;

        assertEquals(15, dimWvl);
        assertEquals(4, dimOzo);
        assertEquals(4, dimCwv);
        assertEquals(7, dimAng);

        // todo: check sequence, values...
    }

    public void testCwvOzoLutKx() {
        LookupTable lut = BbdrUtils.getCwvOzoKxLookupTable("MERIS");
        assertNotNull(lut);

        assertEquals(6, lut.getDimensionCount());

        final double[] kxcaseArray = lut.getDimension(5).getSequence();
        final int nKxcase = kxcaseArray.length;
        assertEquals(2, nKxcase);     //  KXCASE
        assertEquals(1.0, kxcaseArray[0], 1.E-4);
        assertEquals(2.0, kxcaseArray[1], 1.E-4);

        final double[] kxArray = lut.getDimension(4).getSequence();
        final int nKx = kxArray.length;
        assertEquals(2, nKx);     //  KX
        assertEquals(1.0, kxArray[0], 1.E-4);
        assertEquals(2.0, kxArray[1], 1.E-4);

        final double[] angArray = lut.getDimension(3).getSequence();
        final int nAng = angArray.length;
        assertEquals(7, nAng);     //  ANG
        assertEquals(0.0, angArray[0], 1.E-4);
        assertEquals(50.0, angArray[3], 1.E-4);
        assertEquals(70.0, angArray[6], 1.E-4);

        final double[] cwvArray = lut.getDimension(2).getSequence();
        final int nCwv = cwvArray.length;
        assertEquals(4, nCwv);     //  CWV
        assertEquals(0.0, cwvArray[0], 1.E-4);
        assertEquals(3.0, cwvArray[2], 1.E-4);

        final double[] ozoArray = lut.getDimension(1).getSequence();
        final int nOzo = ozoArray.length;
        assertEquals(4, nOzo);     //  OZO
        assertEquals(0.3, ozoArray[1], 1.E-4);
        assertEquals(0.6, ozoArray[3], 1.E-4);

        final double[] wvlArray = lut.getDimension(0).getSequence();
        final int nWvl = wvlArray.length;
        assertEquals(15, nWvl);     //  WVL
        assertEquals(560.0f, wvlArray[4], 1.E-4);
        assertEquals(665.0f, wvlArray[6], 1.E-4);
        assertEquals(753.0f, wvlArray[9], 1.E-4);

        // first values in LUT
        // iWvl=0, iOzo=0, iCwv=0, iAng=0..1, iKx=0..1, iKxcase=0..1 :
        double[] coord = new double[]{412.0, 0.0, 0.0, 0.0, 1.0, 1.0};
        double value = lut.getValue(coord);
        assertEquals(-1.6943E-8, value, 1.E-11);

        coord = new double[]{412.0, 0.0, 0.0, 0.0, 1.0, 2.0};
        value = lut.getValue(coord);
        assertEquals(5.0774E-8, value, 1.E-11);

        coord = new double[]{412.0, 0.0, 0.0, 0.0, 2.0, 1.0};
        value = lut.getValue(coord);
        assertEquals(-1.2707E-7, value, 1.E-11);

        coord = new double[]{412.0, 0.0, 0.0, 0.0, 2.0, 2.0};
        value = lut.getValue(coord);
        assertEquals(3.808E-07, value, 1.E-11);

        // values somewhere inside LUT:
        coord = new double[]{wvlArray[10], ozoArray[2], cwvArray[1], angArray[3], kxArray[1], kxcaseArray[0]};
        value = lut.getValue(coord);
        assertEquals(0.00075, value, 1.E-4);

        coord = new double[]{wvlArray[12], ozoArray[1], cwvArray[1], angArray[5], kxArray[0], kxcaseArray[1]};
        value = lut.getValue(coord);
        assertEquals(0.001252, value, 1.E-4);

        coord = new double[]{wvlArray[3], ozoArray[1], cwvArray[0], angArray[2], kxArray[1], kxcaseArray[1]};
        value = lut.getValue(coord);
        assertEquals(0.086543, value, 1.E-4);

        // last values in LUT:
        coord = new double[]{wvlArray[14], ozoArray[3], cwvArray[3], angArray[6], kxArray[0], kxcaseArray[0]};
        value = lut.getValue(coord);
        assertEquals(0.00425, value, 1.E-5);

        coord = new double[]{wvlArray[14], ozoArray[3], cwvArray[3], angArray[6], kxArray[1], kxcaseArray[0]};
        value = lut.getValue(coord);
        assertEquals(-1.2831E-7, value, 1.E-11);

        coord = new double[]{wvlArray[14], ozoArray[3], cwvArray[3], angArray[6], kxArray[0], kxcaseArray[1]};
        value = lut.getValue(coord);
        assertEquals(0.110174, value, 1.E-5);

        coord = new double[]{wvlArray[14], ozoArray[3], cwvArray[3], angArray[6], kxArray[1], kxcaseArray[1]};
        value = lut.getValue(coord);
        assertEquals(6.2916E-7, value, 1.E-11);
    }

    public void testNskyLutDw() {
        NskyLookupTable nskyLut = BbdrUtils.getNskyLookupTableDw("MERIS");
        assertNotNull(nskyLut);
        LookupTable lut = nskyLut.getLut();
        assertNotNull(lut);

        assertEquals(5, lut.getDimensionCount());

        final double[] valueArray = lut.getDimension(4).getSequence();
        final int nValues = valueArray.length;
        assertEquals(2, nValues);     //  Parameters
        assertEquals(1.0, valueArray[0], 1.E-4);
        assertEquals(2.0, valueArray[1], 1.E-4);

        final double[] szaArray = lut.getDimension(3).getSequence();
        final int nSza = szaArray.length;
        assertEquals(17, nSza);     //  SZA
        assertEquals(6.97, szaArray[2], 1.E-4);
        assertEquals(29.96, szaArray[6], 1.E-4);
        assertEquals(75.71, szaArray[14], 1.E-4);

        final double[] hsfArray = lut.getDimension(2).getSequence();
        final int nHsf = hsfArray.length;
        assertEquals(4, nHsf);     //  HSF
        assertEquals(1.0, hsfArray[1], 1.E-3);
        assertEquals(2.5, hsfArray[2], 1.E-3);
        assertEquals(8.0, hsfArray[3], 1.E-3);

        final double[] aotArray = lut.getDimension(1).getSequence();
        final int nAot = aotArray.length;
        assertEquals(9, nAot);     //  AOT
        assertEquals(0.1, aotArray[2], 1.E-3);
        assertEquals(0.2, aotArray[3], 1.E-3);
        assertEquals(1.5, aotArray[7], 1.E-3);

        final double[] specArray = lut.getDimension(0).getSequence();
        final int nSpecs = specArray.length;
        assertEquals(3, nSpecs);     //  Parameters
        assertEquals(1.0, specArray[0], 1.E-4);
        assertEquals(2.0, specArray[1], 1.E-4);

        // Kpp coeffs:
        assertEquals(-1.289934, nskyLut.getKppGeo(), 1.E-4);
        assertEquals(0.10046, nskyLut.getKppVol(), 1.E-4);

        // first values in LUT
        // iSpec=0, iAot0, iHsf0, iSza=0, iValues=0..2:
        double[] coord = new double[]{0.0, 0.0, 0.0, 0.0, 1.0};
        double value = lut.getValue(coord);
        assertEquals(-0.02289, value, 1.E-4);

        coord = new double[]{0.0, 0.0, 0.0, 0.0, 2.0};
        value = lut.getValue(coord);
        assertEquals(-1.1780, value, 1.E-4);

        // iSpec=0, iAot0, iHsf0, iSza=1, iValues=0:
        coord = new double[]{0.0, 0.0, 0.0, 2.56, 1.0};
        value = lut.getValue(coord);
        assertEquals(-0.022719, value, 1.E-4);

        // values somewhere inside LUT:
        coord = new double[]{specArray[1], aotArray[1], hsfArray[2], szaArray[5], valueArray[1]};
        value = lut.getValue(coord);
        assertEquals(-1.49603, value, 1.E-4);

        coord = new double[]{specArray[2], aotArray[4], hsfArray[1], szaArray[9], valueArray[0]};
        value = lut.getValue(coord);
        assertEquals(0.038364, value, 1.E-4);

        // last values in LUT:
        // iSpec=0, iAot0, iHsf0, iSza=1, iValues=0:
        coord = new double[]{3.0, 2.0, 8.0, 87.14, 1.0};
        value = lut.getValue(coord);
        assertEquals(1.80323, value, 1.E-4);

        coord = new double[]{3.0, 2.0, 8.0, 87.14, 2.0};
        value = lut.getValue(coord);
        assertEquals(5.84008, value, 1.E-4);
    }

    public void testNskyLutUp() {
        NskyLookupTable nskyLut = BbdrUtils.getNskyLookupTableUp("MERIS");
        assertNotNull(nskyLut);
        LookupTable lut = nskyLut.getLut();
        assertNotNull(lut);

        assertEquals(5, lut.getDimensionCount());

        final double[] valueArray = lut.getDimension(4).getSequence();
        final int nValues = valueArray.length;
        assertEquals(2, nValues);     //  Parameters
        assertEquals(1.0, valueArray[0], 1.E-4);
        assertEquals(2.0, valueArray[1], 1.E-4);

        final double[] szaArray = lut.getDimension(3).getSequence();
        final int nSza = szaArray.length;
        assertEquals(17, nSza);     //  SZA
        assertEquals(6.97, szaArray[2], 1.E-4);
        assertEquals(29.96, szaArray[6], 1.E-4);
        assertEquals(75.71, szaArray[14], 1.E-4);

        final double[] hsfArray = lut.getDimension(2).getSequence();
        final int nHsf = hsfArray.length;
        assertEquals(4, nHsf);     //  HSF
        assertEquals(1.0, hsfArray[1], 1.E-3);
        assertEquals(2.5, hsfArray[2], 1.E-3);
        assertEquals(8.0, hsfArray[3], 1.E-3);

        final double[] aotArray = lut.getDimension(1).getSequence();
        final int nAot = aotArray.length;
        assertEquals(9, nAot);     //  AOT
        assertEquals(0.1, aotArray[2], 1.E-3);
        assertEquals(0.2, aotArray[3], 1.E-3);
        assertEquals(1.5, aotArray[7], 1.E-3);

        final double[] specArray = lut.getDimension(0).getSequence();
        final int nSpecs = specArray.length;
        assertEquals(3, nSpecs);     //  Parameters
        assertEquals(1.0, specArray[0], 1.E-4);
        assertEquals(2.0, specArray[1], 1.E-4);

        // Kpp coeffs:
        assertEquals(-1.289934, nskyLut.getKppGeo(), 1.E-4);
        assertEquals(0.10046, nskyLut.getKppVol(), 1.E-4);

        // first values in LUT
        // iSpec=0, iAot0, iHsf0, iSza=0, iValues=0..2:
        double[] coord = new double[]{0.0, 0.0, 0.0, 0.0, 1.0};
        double value = lut.getValue(coord);
        assertEquals(-0.0256047, value, 1.E-4);

        coord = new double[]{0.0, 0.0, 0.0, 0.0, 2.0};
        value = lut.getValue(coord);
        assertEquals(-0.863457, value, 1.E-4);

        // iSpec=0, iAot0, iHsf0, iSza=1, iValues=0:
        coord = new double[]{0.0, 0.0, 0.0, 2.56, 1.0};
        value = lut.getValue(coord);
        assertEquals(-0.025343, value, 1.E-4);

        // values somewhere inside LUT:
        coord = new double[]{specArray[1], aotArray[1], hsfArray[2], szaArray[5], valueArray[1]};
        value = lut.getValue(coord);
        assertEquals(-1.02195, value, 1.E-4);

        coord = new double[]{specArray[2], aotArray[4], hsfArray[1], szaArray[9], valueArray[0]};
        value = lut.getValue(coord);
        assertEquals(0.0856803, value, 1.E-4);

        // last values in LUT:
        // iSpec=0, iAot0, iHsf0, iSza=1, iValues=0:
        coord = new double[]{3.0, 2.0, 8.0, 87.14, 1.0};
        value = lut.getValue(coord);
        assertEquals(1.05181, value, 1.E-4);

        coord = new double[]{3.0, 2.0, 8.0, 87.14, 2.0};
        value = lut.getValue(coord);
        assertEquals(-9.34972, value, 1.E-4);
    }

    public void testLutInterpolation1D() {
        final double[] dimension = new double[]{0, 1, 2, 3, 4};
        final double[] values = new double[]{0, 2, 5, 10, 22};

        final LookupTable lut = new LookupTable(values, dimension);
        assertEquals(1, lut.getDimensionCount());

        assertEquals(0.0, lut.getDimension(0).getMin(), 0.0);
        assertEquals(4.0, lut.getDimension(0).getMax(), 0.0);

        assertEquals(0.0, lut.getValue(0.0), 0.0);
        assertEquals(2.0, lut.getValue(1.0), 0.0);
        assertEquals(5.0, lut.getValue(2.0), 0.0);
        assertEquals(7.5, lut.getValue(2.5), 0.0);
        assertEquals(0.2469, lut.getValue(0.12345), 0.0);
    }


    public void testConvertAngArrayToAmfArray() {
        float[] ang = new float[]{0.0f, 20.0f, 40.0f, 50.0f, 60.0f, 65.0f, 70.0f};
        float[] amf = BbdrUtils.convertAngArrayToAmfArray(ang);
        assertEquals(2.0f, amf[0], 1.E-4);
        assertEquals(2.12836f, amf[1], 1.E-4);
        assertEquals(2.61081f, amf[2], 1.E-4);
        assertEquals(3.11145f, amf[3], 1.E-4);
        assertEquals(4.0f, amf[4], 1.E-4);
        assertEquals(4.7324f, amf[5], 1.E-4);
        assertEquals(5.84761f, amf[6], 1.E-4);
    }
}

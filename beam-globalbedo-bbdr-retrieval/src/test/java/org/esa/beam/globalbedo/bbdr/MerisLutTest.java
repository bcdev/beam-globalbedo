/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.globalbedo.bbdr;

import junit.framework.TestCase;

import java.io.IOException;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class MerisLutTest extends TestCase {

    public void testLutAot() throws IOException {
        AotLookupTable aotLut = BbdrUtils.getAotLookupTable(Sensor.MERIS);
        assertNotNull(aotLut);
        LookupTable lut = aotLut.getLut();
        assertNotNull(lut);

        assertEquals(7, lut.getDimensionCount());

        final double[] parametersArray = lut.getDimension(6).getSequence();
        final int nParameters = parametersArray.length;
        assertEquals(5, nParameters);     //  Parameters
        assertEquals(1.0, parametersArray[0], 1.E-4);
        assertEquals(2.0, parametersArray[1], 1.E-4);
        assertEquals(3.0, parametersArray[2], 1.E-4);
        assertEquals(5.0, parametersArray[4], 1.E-4);

        final double[] vzaArray = lut.getDimension(5).getSequence();
        final int nVza = vzaArray.length;
        assertEquals(13, nVza);     //  VZA
        assertEquals(0.0, vzaArray[0], 1.E-4);
        assertEquals(12.76, vzaArray[3], 1.E-4);
        assertEquals(24.24, vzaArray[5], 1.E-4);
        assertEquals(35.68, vzaArray[7], 1.E-4);
        assertEquals(64.2799987, vzaArray[12], 1.E-4);

        final double[] szaArray = lut.getDimension(4).getSequence();
        final int nSza = szaArray.length;
        assertEquals(14, nSza);     //  SZA
        assertEquals(0.0, szaArray[0], 1.E-4);
        assertEquals(6.97, szaArray[2], 1.E-4);
        assertEquals(18.51, szaArray[4], 1.E-4);
        assertEquals(29.96, szaArray[6], 1.E-4);
        assertEquals(69.9899, szaArray[13], 1.E-4);

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

    public void testLutAotKx() throws IOException {
        LookupTable lut = BbdrUtils.getAotKxLookupTable(Sensor.MERIS);
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

    public void testNskyLutDw() throws IOException {
        NskyLookupTable nskyLut = BbdrUtils.getNskyLookupTableDw(Sensor.MERIS);
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

    public void testNskyLutUp() throws IOException {
        NskyLookupTable nskyLut = BbdrUtils.getNskyLookupTableUp(Sensor.MERIS);
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

    public void testLutGas() throws IOException {
        GasLookupTable gasLookupTable = new GasLookupTable(Sensor.MERIS);
        gasLookupTable.load(null);
        assertNotNull(gasLookupTable);
        // todo test sth.
//        assertEquals(0,gasLookupTable.getAmfArray().length;
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


}

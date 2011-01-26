/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GasLookupTableTest {

    private GasLookupTable gasLookupTable;

    @Before
    public void before() {
        gasLookupTable = new GasLookupTable(Sensor.MERIS);
        gasLookupTable.load();
    }

    @Test
    public void testLoading() {
        assertNotNull(gasLookupTable.getLutGas());
        assertNotNull(gasLookupTable.getKxLutGas());
    }

    @Test
    public void testDimensions() {
        final float[] amfArray = gasLookupTable.getAmfArray();
        final int nAng = amfArray.length;
        assertEquals(7, nAng);     //  AMF(ANG)
        assertEquals(2.0f, amfArray[0], 1.E-4);
        assertEquals(3.1114f, amfArray[3], 1.E-4);
        assertEquals(5.8476f, amfArray[6], 1.E-4);

        final float[] cwvArray = gasLookupTable.getCwvArray();
        final int nCwv = cwvArray.length;
        assertEquals(4, nCwv);     //  CWV
        assertEquals(0.0f, cwvArray[0], 1.E-4);
        assertEquals(3.0f, cwvArray[2], 1.E-4);

        final float[] ozoArray = gasLookupTable.getGasArray();
        final int nOzo = ozoArray.length;
        assertEquals(4, nOzo);     //  OZO
        assertEquals(0.3f, ozoArray[1], 1.E-4);
        assertEquals(0.6f, ozoArray[3], 1.E-4);
    }

    @Test
    public void testGetTg() {
        float[] tg = gasLookupTable.getTg(2.0f, 0.1f);
        assertEquals(0.9999f, tg[0], 1.E-4);
        assertEquals(0.9995f, tg[1], 1.E-4);

        tg = gasLookupTable.getTg(4.0f, 0.4f);
        assertEquals(0.9999f, tg[0], 1.E-4);
        assertEquals(0.9960f, tg[1], 1.E-4);
    }

    @Test
    public void testGetKxTg() {
        float[][][] kxtg = gasLookupTable.getKxTg(2.0f, 0.1f);
        assertEquals(-1.6943E-8f, kxtg[0][0][0], 1.E-4);
        assertEquals(5.07743E-8f, kxtg[0][0][1], 1.E-4);
        assertEquals(1.97026E-8f, kxtg[3][0][0], 1.E-4);
        assertEquals(-4.6359E-8f, kxtg[3][0][1], 1.E-4);
    }

    @Test
    public void testConvertAngArrayToAmfArray() {
        float[] ang = new float[]{0.0f, 20.0f, 40.0f, 50.0f, 60.0f, 65.0f, 70.0f};
        float[] amf = GasLookupTable.convertAngArrayToAmfArray(ang);
        assertEquals(2.0f, amf[0], 1.E-4);
        assertEquals(2.12836f, amf[1], 1.E-4);
        assertEquals(2.61081f, amf[2], 1.E-4);
        assertEquals(3.11145f, amf[3], 1.E-4);
        assertEquals(4.0f, amf[4], 1.E-4);
        assertEquals(4.7324f, amf[5], 1.E-4);
        assertEquals(5.84761f, amf[6], 1.E-4);
    }

}

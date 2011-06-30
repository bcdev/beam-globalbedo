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

package org.esa.beam.dataio.gmted2010;

import org.junit.Test;

import static org.junit.Assert.*;

public class GMTED2010ElevationModelDescriptorTest {

    private static final String GMTED_MEA300_TIF = "_20101117_gmted_mea300.tif";

    @Test
    public void testCreateTileFilename() throws Exception {
        assertEquals("E000/10N000E" + GMTED_MEA300_TIF, createTileFilename(+10, -0));
        assertEquals("E000/10S000E" + GMTED_MEA300_TIF, createTileFilename(-10, -0));

        assertEquals("W180/10N180W" + GMTED_MEA300_TIF, createTileFilename(+10, -180));
        assertEquals("W180/10S180W" + GMTED_MEA300_TIF, createTileFilename(-10, -180));

        assertEquals("E150/10N150E" + GMTED_MEA300_TIF, createTileFilename(+10, +150));
        assertEquals("E150/10S150E" + GMTED_MEA300_TIF, createTileFilename(-10, +150));

        assertEquals("E000/70N000E" + GMTED_MEA300_TIF, createTileFilename(+70, -0));
        assertEquals("E000/90S000E" + GMTED_MEA300_TIF, createTileFilename(-90, -0));
    }

    @Test
    public void testgetMinLat() throws Exception {
        assertEquals(-90, GMTED2010ElevationModelDescriptor.getMinLat(8));
        assertEquals(-70, GMTED2010ElevationModelDescriptor.getMinLat(7));
        assertEquals(-50, GMTED2010ElevationModelDescriptor.getMinLat(6));
        assertEquals(-30, GMTED2010ElevationModelDescriptor.getMinLat(5));
        assertEquals(-10, GMTED2010ElevationModelDescriptor.getMinLat(4));
        assertEquals(+10, GMTED2010ElevationModelDescriptor.getMinLat(3));
        assertEquals(+30, GMTED2010ElevationModelDescriptor.getMinLat(2));
        assertEquals(+50, GMTED2010ElevationModelDescriptor.getMinLat(1));
        assertEquals(+70, GMTED2010ElevationModelDescriptor.getMinLat(0));

    }

    @Test
    public void testgetMinLon() throws Exception {
        assertEquals(-180, GMTED2010ElevationModelDescriptor.getMinLon(0));
        assertEquals(-150, GMTED2010ElevationModelDescriptor.getMinLon(1));
        assertEquals(-120, GMTED2010ElevationModelDescriptor.getMinLon(2));
        assertEquals(-90, GMTED2010ElevationModelDescriptor.getMinLon(3));
        assertEquals(-60, GMTED2010ElevationModelDescriptor.getMinLon(4));
        assertEquals(-30, GMTED2010ElevationModelDescriptor.getMinLon(5));
        assertEquals(+0, GMTED2010ElevationModelDescriptor.getMinLon(6));
        assertEquals(+30, GMTED2010ElevationModelDescriptor.getMinLon(7));
        assertEquals(+60, GMTED2010ElevationModelDescriptor.getMinLon(8));
        assertEquals(+90, GMTED2010ElevationModelDescriptor.getMinLon(9));
        assertEquals(+120, GMTED2010ElevationModelDescriptor.getMinLon(10));
        assertEquals(+150, GMTED2010ElevationModelDescriptor.getMinLon(11));
    }

    private static String createTileFilename(int minLat, int minLon) {
        return GMTED2010ElevationModelDescriptor.createTileFilename(minLat, minLon);
    }

}

package org.esa.beam.globalbedo.mosaic;/*
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

import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class GlobAlbedoMosaicProductReaderTest {

    private GlobAlbedoMosaicProductReader mosaicGrid;

    @Before
    public void setUp() throws Exception {
        mosaicGrid = new GlobAlbedoMosaicProductReader(null);
    }

    @Test
    public void testGetMosaicFileRegex() {
        String dimRegex = mosaicGrid.getMosaicFileRegex("GlobAlbedo.2005129.h18v04.dim");
        assertEquals("GlobAlbedo.2005129.h\\d\\dv\\d\\d.dim", dimRegex);

        String enviRegex = mosaicGrid.getMosaicFileRegex("GlobAlbedo.2005129.h18v04_ENVI.bin");
        assertEquals("GlobAlbedo.2005129.h\\d\\dv\\d\\d_ENVI.bin", enviRegex);
    }

    @Test
    public void testCreateMosaicTile() {
        File mosaicFile = new File("GlobAlbedo.2005129.h18v04.dim");
        MosaicTile tile = mosaicGrid.createMosaicTile(mosaicFile);
        assertNotNull(tile);
        assertSame(mosaicFile, tile.getFile());
        assertEquals(18, tile.getTileX());
        assertEquals(4, tile.getTileY());
        assertEquals(162, tile.getIndex());
    }

    @Test
    public void testGetProductName() {
        File mosaicFile = new File("GlobAlbedo.2005129.h18v04.dim");
        String productName = mosaicGrid.getProductName(mosaicFile);
        assertEquals("GlobAlbedo.2005129", productName);

        mosaicFile = new File("GlobAlbedo.2005129.h18v04_ENVI.binm");
        productName = mosaicGrid.getProductName(mosaicFile);
        assertEquals("GlobAlbedo.2005129", productName);

    }
}

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


import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author MarcoZ
 */
public class ModisTileCoordinatesTest {

    @Test
    public void testInstance() throws Exception {
        ModisTileCoordinates instance = ModisTileCoordinates.getInstance();
        assertNotNull(instance);
        int tileCount = instance.getTileCount();
        assertEquals(326, tileCount);
    }

    @Test
    public void testGetTileIndexForUnkownTile() throws Exception {
        ModisTileCoordinates instance = ModisTileCoordinates.getInstance();

        assertEquals(-1, instance.findTileIndex("h55v55"));
        assertEquals(-1, instance.findTileIndex(""));
        assertEquals(-1, instance.findTileIndex(null));
    }

    @Test
    public void testGetUpperLeftCoordinatesForTile() throws Exception {
        ModisTileCoordinates instance = ModisTileCoordinates.getInstance();

        int tileIndex = instance.findTileIndex("h00v08");
        assertEquals(0, tileIndex);
        assertEquals(-20015109.354, instance.getUpperLeftX(tileIndex), 1e-5);
        assertEquals(1111950.520, instance.getUpperLeftY(tileIndex), 1e-5);

        tileIndex = instance.findTileIndex("h13v13");
        assertEquals(92, tileIndex);
        assertEquals(-5559752.598, instance.getUpperLeftX(tileIndex), 1e-5);
        assertEquals(-4447802.079, instance.getUpperLeftY(tileIndex), 1e-5);

        tileIndex = instance.findTileIndex("h35v10");
        assertEquals(325, tileIndex);
        assertEquals(18903158.834, instance.getUpperLeftX(tileIndex), 1e-5);
        assertEquals(-1111950.520, instance.getUpperLeftY(tileIndex), 1e-5);
    }
}

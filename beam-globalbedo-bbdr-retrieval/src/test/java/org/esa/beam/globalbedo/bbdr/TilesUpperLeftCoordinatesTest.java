package org.esa.beam.globalbedo.bbdr;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: olafd
 * Date: 12.07.11
 * Time: 16:04
 * To change this template use File | Settings | File Templates.
 */
public class TilesUpperLeftCoordinatesTest extends TestCase {

    public void testGetUpperLeftCoordinates() throws Exception {
        ModisTileUpperLeftCoordinates coordinates = new ModisTileUpperLeftCoordinates();
        ModisTileUpperLeftCoordinates.ULCoordinatesTable ulCoordinatesTable = coordinates.getUlCoordinatesTable();
        assertNotNull(ulCoordinatesTable);
        assertNotNull(ulCoordinatesTable.getTile());
        assertNotNull(ulCoordinatesTable.getUlX());
        assertNotNull(ulCoordinatesTable.getUlY());
        assertEquals(36 * 18, ulCoordinatesTable.getTile().length);
        assertEquals(36 * 18, ulCoordinatesTable.getUlX().length);
        assertEquals(36 * 18, ulCoordinatesTable.getUlY().length);
        assertEquals(326, coordinates.getUlCoordinatesTableLength());
        assertEquals("h00v08", ulCoordinatesTable.getTile()[0]);
        assertEquals(-20015109.354, ulCoordinatesTable.getUlX()[0]);
        assertEquals(1111950.520, ulCoordinatesTable.getUlY()[0]);
        assertEquals("h13v13", ulCoordinatesTable.getTile()[92]);
        assertEquals(-5559752.598, ulCoordinatesTable.getUlX()[92]);
        assertEquals(-4447802.079, ulCoordinatesTable.getUlY()[92]);
        assertEquals("h35v10", ulCoordinatesTable.getTile()[325]);
        assertEquals(18903158.834, ulCoordinatesTable.getUlX()[325]);
        assertEquals(-1111950.520, ulCoordinatesTable.getUlY()[325]);
    }

    public void testGetUpperLeftCoordinatesForTile() throws Exception {
        ModisTileUpperLeftCoordinates coordinates = new ModisTileUpperLeftCoordinates();

        String tile = "h00v08";
        assertEquals(-20015109.354, coordinates.getUpperLeftX(tile));
        assertEquals(1111950.520, coordinates.getUpperLeftY(tile));
        tile = "h13v13";
        assertEquals(-5559752.598, coordinates.getUpperLeftX(tile));
        assertEquals(-4447802.079, coordinates.getUpperLeftY(tile));
        tile = "h35v10";
        assertEquals(18903158.834, coordinates.getUpperLeftX(tile));
        assertEquals(-1111950.520, coordinates.getUpperLeftY(tile));
    }
}

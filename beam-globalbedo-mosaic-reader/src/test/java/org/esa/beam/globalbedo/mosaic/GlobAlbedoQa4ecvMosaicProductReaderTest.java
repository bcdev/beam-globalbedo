package org.esa.beam.globalbedo.mosaic;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GlobAlbedoQa4ecvMosaicProductReaderTest {

    @Test
    public void testGetAffectedSourceTiles() {
        Rectangle rect = new Rectangle(0, 0, 50, 50);
        Point[] affectedSourceTiles = MosaicGrid.getAffectedSourceTiles(rect, 18, 4, 200);
        assertNotNull(affectedSourceTiles);
        assertEquals(1, affectedSourceTiles.length);
        assertEquals(18, affectedSourceTiles[0].x);
        assertEquals(4, affectedSourceTiles[0].y);

        rect = new Rectangle(0, 0, 300, 300);
        affectedSourceTiles = MosaicGrid.getAffectedSourceTiles(rect, 18, 4, 200);
        assertNotNull(affectedSourceTiles);
        assertEquals(4, affectedSourceTiles.length);
        assertEquals(18, affectedSourceTiles[0].x);
        assertEquals(4, affectedSourceTiles[0].y);
        assertEquals(19, affectedSourceTiles[1].x);
        assertEquals(4, affectedSourceTiles[1].y);
        assertEquals(18, affectedSourceTiles[2].x);
        assertEquals(5, affectedSourceTiles[2].y);
        assertEquals(19, affectedSourceTiles[3].x);
        assertEquals(5, affectedSourceTiles[3].y);

    }

    @Test
    public void testGetTileBounds() {
        final Rectangle tileBounds = MosaicGrid.getTileBounds(18, 4, 18, 4, 200);
        assertNotNull(tileBounds);
        assertEquals(0, tileBounds.x);
        assertEquals(0, tileBounds.y);
        assertEquals(200, tileBounds.width);
        assertEquals(200, tileBounds.height);
    }
}

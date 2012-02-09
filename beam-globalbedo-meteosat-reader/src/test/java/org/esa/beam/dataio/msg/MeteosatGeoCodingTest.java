package org.esa.beam.dataio.msg;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.junit.Test;

import java.io.IOException;

import static java.lang.Float.NaN;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * Test class for MeteosatGeoCoding
 *
 * @author olafd
 * @author norman
 */
public class MeteosatGeoCodingTest {
    @Test
    public void testDefaults() throws Exception {
        final Product product = new Product("test", "test", 3, 3);
        final Band latBand = product.addBand("lat", "Y");
        final Band lonBand = product.addBand("lon", "X");
        latBand.loadRasterData();
        lonBand.loadRasterData();
        final MeteosatGeoCoding gc = new MeteosatGeoCoding(latBand, lonBand, "");

        assertTrue(gc.canGetPixelPos());
        assertTrue(gc.canGetGeoPos());
        assertEquals(Datum.WGS_84, gc.getDatum());
        assertFalse(gc.isCrossingMeridianAt180());
    }

    @Test
    public void testGetGeoPos() throws Exception {
        final MeteosatGeoCoding gc = createTestGC();

        assertTrue(gc.canGetGeoPos());
        assertFalse(gc.isCrossingMeridianAt180());

        final GeoPos geoPos = new GeoPos();
        assertSame(geoPos, gc.getGeoPos(new PixelPos(0.0F, 0.0F), geoPos));
        assertEquals(new GeoPos(2.0F, 0.0F), geoPos);

        assertEquals(new GeoPos(2.0F, 0.0F), gc.getGeoPos(new PixelPos(0.0F, 0.0F), null));
        assertEquals(new GeoPos(2.0F, 0.0F), gc.getGeoPos(new PixelPos(0.5F, 0.5F), null));

        assertEquals(new GeoPos(1.0F, 0.0F), gc.getGeoPos(new PixelPos(0.0F, 1.0F), null));
        assertEquals(new GeoPos(0.0F, 0.0F), gc.getGeoPos(new PixelPos(0.0F, 2.0F), null));

        assertEquals(new GeoPos(1.0F, 1.0F), gc.getGeoPos(new PixelPos(1.0F, 1.0F), null));
        assertEquals(new GeoPos(0.0F, 2.0F), gc.getGeoPos(new PixelPos(2.0F, 2.0F), null));

        assertFalse(gc.getGeoPos(new PixelPos(1.0F, 0.0F), null).isValid());
        assertFalse(gc.getGeoPos(new PixelPos(2.0F, 0.0F), null).isValid());
        assertFalse(gc.getGeoPos(new PixelPos(2.0F, 1.0F), null).isValid());
    }

    @Test
    public void testGetPixelPos() throws Exception {
        final MeteosatGeoCoding gc = createTestGC();

        assertTrue(gc.canGetPixelPos());
        assertFalse(gc.isCrossingMeridianAt180());

        final PixelPos pixelPos = new PixelPos();
        assertSame(pixelPos, gc.getPixelPos(new GeoPos(0.0F, 0.0F), pixelPos));
        assertEquals(new PixelPos(0.5F, 2.5F), pixelPos);

        assertEquals(new PixelPos(1.5F, 1.5F), gc.getPixelPos(new GeoPos(1.0F, 1.0F), pixelPos));
    }

    @Test
    public void testPixelBoxLut() throws Exception {
        MeteosatGeoCoding.PixelBoxLut pixelBoxLut = new MeteosatGeoCoding.PixelBoxLut(-1.0, 1.0, 1000);
        assertNull(pixelBoxLut.getPixelBox(0.0));

        assertEquals(0, pixelBoxLut.getIndex(-1.0));
        assertEquals(500, pixelBoxLut.getIndex(0.0));
        assertEquals(600, pixelBoxLut.getIndex(0.2));
        assertEquals(750, pixelBoxLut.getIndex(0.5));
        assertEquals(999, pixelBoxLut.getIndex(1.0));

        assertEquals(1000, pixelBoxLut.pixelBoxes.length);
        pixelBoxLut.add(0.7, 5, 6);
        assertNotNull(pixelBoxLut.getPixelBox(0.7));
        assertEquals(5, pixelBoxLut.getPixelBox(0.7).minX);
        assertEquals(5, pixelBoxLut.getPixelBox(0.7).maxX);
        assertEquals(6, pixelBoxLut.getPixelBox(0.7).minY);
        assertEquals(6, pixelBoxLut.getPixelBox(0.7).maxY);
        pixelBoxLut.add(0.7, 7, 8);
        assertEquals(5, pixelBoxLut.getPixelBox(0.7).minX);
        assertEquals(7, pixelBoxLut.getPixelBox(0.7).maxX);
        assertEquals(6, pixelBoxLut.getPixelBox(0.7).minY);
        assertEquals(8, pixelBoxLut.getPixelBox(0.7).maxY);
    }

    @Test
    public void testFillIncompletePixelBoxLut() throws Exception {
        MeteosatGeoCoding.PixelBoxLut lut = new MeteosatGeoCoding.PixelBoxLut(0.0, 1.0, 10);
        lut.add(0.0, 1, 2);
        lut.add(0.4, 7, 11);
        lut.add(0.6, 3, 4);
        lut.add(0.7, 6, 22);
        lut.add(1.0, 2, 16);
        assertEquals(new MeteosatGeoCoding.PixelBox(1, 2), lut.getPixelBox(0.0));
        assertNull(lut.getPixelBox(0.1));
        assertNull(lut.getPixelBox(0.2));
        assertNull(lut.getPixelBox(0.3));
        assertEquals(new MeteosatGeoCoding.PixelBox(7, 11), lut.getPixelBox(0.4));
        assertNull(lut.getPixelBox(0.5));
        assertEquals(new MeteosatGeoCoding.PixelBox(3, 4), lut.getPixelBox(0.6));
        assertEquals(new MeteosatGeoCoding.PixelBox(6, 22), lut.getPixelBox(0.7));
        assertNull(lut.getPixelBox(0.8));
        assertEquals(new MeteosatGeoCoding.PixelBox(2, 16), lut.getPixelBox(0.9));
        assertEquals(new MeteosatGeoCoding.PixelBox(2, 16), lut.getPixelBox(1.0));

        lut.complete();
        assertEquals(new MeteosatGeoCoding.PixelBox(1, 2, 1, 2), lut.getPixelBox(0.0));
        assertEquals(new MeteosatGeoCoding.PixelBox(1, 2, 7, 11), lut.getPixelBox(0.1));
        assertEquals(new MeteosatGeoCoding.PixelBox(1, 2, 7, 11), lut.getPixelBox(0.2));
        assertEquals(new MeteosatGeoCoding.PixelBox(1, 2, 7, 11), lut.getPixelBox(0.3));
        assertEquals(new MeteosatGeoCoding.PixelBox(7, 11, 7, 11), lut.getPixelBox(0.4));
        assertEquals(new MeteosatGeoCoding.PixelBox(3, 4, 7, 11), lut.getPixelBox(0.5));
        assertEquals(new MeteosatGeoCoding.PixelBox(3, 4, 3, 4), lut.getPixelBox(0.6));
        assertEquals(new MeteosatGeoCoding.PixelBox(6, 22, 6, 22), lut.getPixelBox(0.7));
        assertEquals(new MeteosatGeoCoding.PixelBox(2, 16, 6, 22), lut.getPixelBox(0.8));
        assertEquals(new MeteosatGeoCoding.PixelBox(2, 16, 2, 16), lut.getPixelBox(0.9));
        assertEquals(new MeteosatGeoCoding.PixelBox(2, 16, 2, 16), lut.getPixelBox(1.0));

    }

    private MeteosatGeoCoding createTestGC() throws IOException {
        final Product product = new Product("test", "test", 3, 3);
        final Band latBand = product.addBand("lat", ProductData.TYPE_FLOAT32);
        final Band lonBand = product.addBand("lon", ProductData.TYPE_FLOAT32);
        latBand.setRasterData(ProductData.createInstance(new float[] {
                2.0F, NaN, NaN,
                1.0F, 1.0F, NaN,
                0.0F, 0.0F, 0.0F,
        }));
        lonBand.setRasterData(ProductData.createInstance(new float[]{
                0.0F, NaN, NaN,
                0.0F, 1.0F, NaN,
                0.0F, 1.0F, 2.0F,
        }));
        latBand.setNoDataValue(NaN);
        latBand.setNoDataValueUsed(true);
        lonBand.setNoDataValue(NaN);
        lonBand.setNoDataValueUsed(true);
        return new MeteosatGeoCoding(latBand, lonBand, "");
    }
}

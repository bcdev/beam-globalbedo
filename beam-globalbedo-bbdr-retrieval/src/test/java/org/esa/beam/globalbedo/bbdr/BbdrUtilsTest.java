package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.util.math.MathUtils;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
public class BbdrUtilsTest {

    @Test
    public void testGetIndexBefore() {
        float[] values = {1.8f, 2.2f, 4.5f, 5.5f};
        assertEquals(0, BbdrUtils.getIndexBefore(1.2f, values));
        assertEquals(1, BbdrUtils.getIndexBefore(2.5f, values));
        assertEquals(2, BbdrUtils.getIndexBefore(4.6f, values));
        assertEquals(2, BbdrUtils.getIndexBefore(7.7f, values));
    }

    @Test
    public void testGetDoyFromYYYYMMDD() {
        String yyyymmdd = "20070101";
        int doy = BbdrUtils.getDoyFromYYYYMMDD(yyyymmdd);
        assertEquals(1, doy);

        yyyymmdd = "20071218";
        doy = BbdrUtils.getDoyFromYYYYMMDD(yyyymmdd);
        assertEquals(352, doy);
    }

    @Test
    public void testGetSzaFromUt() {
        double ut = 16.0;
        double doy = 111.0 + 4./24.;
        double lat = MathUtils.DTOR * 45;
        double sza = UtFromSzaOp.computeSzaFromUt(ut, lat, doy);

        assertEquals(31.73, sza, 1.E-2);
    }

    @Test
    public void testComputeConstantKernels() {
        double vzaRad = MathUtils.DTOR * 73.03463;
        double szaRad = MathUtils.DTOR * 92.6708;
        double phiRad = MathUtils.DTOR * 115.6619;
        final double[] kernels = BbdrUtils.computeConstantKernels(vzaRad, szaRad, phiRad);

        assertEquals(3.6714, kernels[0], 1.E-4);
        assertTrue(Double.isNaN(kernels[1]));
    }

    public void testGetModisTileFromLatLon() {
        // Texas
        float lat = 34.2f;
        float lon = -101.71f;
        assertEquals("h09v05", BbdrUtils.getModisTileFromLatLon(lat, lon));

        // MeckPomm
        lat = 53.44f;
        lon = 10.57f;
        assertEquals("h18v03", BbdrUtils.getModisTileFromLatLon(lat, lon));

        // Barrow (a left edge tile)
        lat = 65.0f;
        lon = -175.0f;
        assertEquals("h10v02", BbdrUtils.getModisTileFromLatLon(lat, lon));

        // New Zealand (a right edged tile)
        lat = -39.5f;
        lon = 176.71f;
        assertEquals("h31v12", BbdrUtils.getModisTileFromLatLon(lat, lon));


        // Antarctica
        lat = -84.2f;
        lon = 160.71f;
        assertEquals("h19v17", BbdrUtils.getModisTileFromLatLon(lat, lon));

        // Siberia
        lat = 65.2f;
        lon = 111.71f;
        assertEquals("h22v02", BbdrUtils.getModisTileFromLatLon(lat, lon));

        // Madagascar
        lat = -28.0f;
        lon = 46.1f;
        assertEquals("h22v11", BbdrUtils.getModisTileFromLatLon(lat, lon));

        // Railroad Valley (USA)
        lat = 38.497f;
        lon = -115.69f;
        assertEquals("h08v05", BbdrUtils.getModisTileFromLatLon(lat, lon));

        // Hainich (Germany)
        lat = 51.0792f;
        lon = 10.453f;
        assertEquals("h18v03", BbdrUtils.getModisTileFromLatLon(lat, lon));
    }

    @Test
    @Ignore
    public void testComputeConstantKernel_2() throws Exception {

        double phiRad = (103.90717 - 149.49255)*MathUtils.DTOR;
        double vzaRad = 21.1263*MathUtils.DTOR;
        float szaRad = (float) (41.34555*MathUtils.DTOR);
        final double[] kernels = BbdrUtils.computeConstantKernels_2(vzaRad, szaRad, phiRad);
        assertNotNull(kernels);
        assertEquals(2, kernels.length);
        assertEquals(0.050839, kernels[0], 1.E-6);
        assertEquals(-0.71918409, kernels[1], 1.E-6);
    }
}

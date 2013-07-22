package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.util.math.MathUtils;
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

//    @Test
//    public void testGetUtFromSza() {
//        double sza = 31.73 * MathUtils.DTOR;
//        double doy = 111.0 + 4./24.;
//        double lat = MathUtils.DTOR * 45;
//        double ut = UtFromSzaOp.computeUtFromSza(sza, lat, doy);
//
//        assertEquals(31.73, ut, 1.E-2);
//    }
}

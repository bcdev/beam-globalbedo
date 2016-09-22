package org.esa.beam.globalbedo.bbdr;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BbdrMeteosatTest {

    @Test
    public void testExtractInputParmsFromFilename() {
        String filename = "W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET7+MVIRI_C_BRF_EUMP_20050501000000_h18v04";
        double[] parmsFromFilename = MeteosatBbdrFromBrfOp.extractInputParmsFromFilename(filename);

        assertEquals(0.0, parmsFromFilename[0], 1.E-6);
        assertEquals(121, parmsFromFilename[1], 1.E-6);
        assertEquals(2005, parmsFromFilename[2], 1.E-6);
        assertEquals(12.0, parmsFromFilename[3], 1.E-6);

        filename = "W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET8+SEVIRI_HRVIS_000_C_BRF_EUMP_20060701000000_h18v06";
        parmsFromFilename = MeteosatBbdrFromBrfOp.extractInputParmsFromFilename(filename);

        assertEquals(0.0, parmsFromFilename[0], 1.E-6);
        assertEquals(182, parmsFromFilename[1], 1.E-6);
        assertEquals(2006, parmsFromFilename[2], 1.E-6);
        assertEquals(12.0, parmsFromFilename[3], 1.E-6);

        filename = "W_XX-EUMETSAT-Darmstadt,VIS+SATELLITE,MET5+MVIRI_063_C_BRF_EUMP_20051018000000_h19v06";
        parmsFromFilename = MeteosatBbdrFromBrfOp.extractInputParmsFromFilename(filename);

        assertEquals(63.0, parmsFromFilename[0], 1.E-6);
        assertEquals(291, parmsFromFilename[1], 1.E-6);
        assertEquals(2005, parmsFromFilename[2], 1.E-6);
        assertEquals(8.0, parmsFromFilename[3], 1.E-6);
    }

}

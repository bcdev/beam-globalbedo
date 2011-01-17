package org.esa.beam.globalbedo.bbdr;

import junit.framework.TestCase;
import org.esa.beam.globalbedo.sdr.lutUtils.MomoLut;
import org.esa.beam.util.math.VectorLookupTable;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class LUTAotTest extends TestCase {

    public void testMerisLutAot() {
        MomoLut lut = BbdrUtils.readAotLookupTable("MERIS");
        assertNotNull(lut);

        assertEquals(15, lut.getnWvl());

        assertEquals(5, lut.getnParameter());
        assertEquals(9, lut.getnAot());
        assertEquals(19, lut.getnAzi());
        assertEquals(4, lut.getnHsf());
        assertEquals(14, lut.getnSza());
        assertEquals(13, lut.getnVza());

        assertEquals(412.0f, lut.getWvl()[0]);
        assertEquals(665.0f, lut.getWvl()[6]);
        assertEquals(900.0f, lut.getWvl()[14]);

        assertEquals(0.1f, lut.getAot()[2], 1.E-3);
        assertEquals(0.2f, lut.getAot()[3], 1.E-3);
        assertEquals(1.5f, lut.getAot()[7], 1.E-3);

        assertEquals(746.825f, lut.getHsf()[1], 1.E-3);
        assertEquals(898.746f, lut.getHsf()[2], 1.E-3);
        assertEquals(1013.25f, lut.getHsf()[3], 1.E-3);

        assertEquals(10.0f, lut.getAzi()[1], 1.E-3);
        assertEquals(130.0f, lut.getAzi()[13], 1.E-3);
        assertEquals(150.0f, lut.getAzi()[15], 1.E-3);

        assertEquals(6.97f, lut.getSza()[2], 1.E-3);
        assertEquals(18.51f, lut.getSza()[4], 1.E-3);
        assertEquals(29.96f, lut.getSza()[6], 1.E-3);

        assertEquals(12.76f, lut.getVza()[3], 1.E-3);
        assertEquals(24.24f, lut.getVza()[5], 1.E-3);
        assertEquals(35.68f, lut.getVza()[7], 1.E-3);
    }

    public void testMerisCwvAot() {
        VectorLookupTable lut = BbdrUtils.readCwvOzoLookupTable("MERIS");
        assertNotNull(lut);

        assertEquals(3, lut.getDimensionCount());

        assertEquals(2.0, lut.getDimension(0).getMin(), 1.E-3);
        assertEquals(5.847, lut.getDimension(0).getMax(), 1.E-3);
        assertEquals(0.0, lut.getDimension(1).getMin(), 1.E-3);
        assertEquals(4.5, lut.getDimension(1).getMax(), 1.E-3);
        assertEquals(0.1, lut.getDimension(2).getMin(), 1.E-3);
        assertEquals(0.6, lut.getDimension(2).getMax(), 1.E-3);
    }
}

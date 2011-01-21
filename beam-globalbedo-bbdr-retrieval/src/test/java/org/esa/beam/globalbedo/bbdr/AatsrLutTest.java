package org.esa.beam.globalbedo.bbdr;

import junit.framework.TestCase;
import org.esa.beam.util.math.LookupTable;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AatsrLutTest extends TestCase {

    public void testLutAot() {
        LookupTable lut = BbdrUtils.getAotKxLookupTable("AATSR");
        assertNull(lut);
        // TODO write tests
    }

    public void testAotKx() {
        LookupTable lut = BbdrUtils.getAotKxLookupTable("AATSR");
        assertNull(lut);
        // TODO write tests
    }

    public void testCwvOzoLut() {
        LookupTable lut = BbdrUtils.getTransposedCwvOzoLookupTable("AATSR");
        assertNotNull(lut);
        // TODO write tests
    }

    public void testCwvOzoLutKx() {
        LookupTable lut = BbdrUtils.getCwvOzoKxLookupTable("AATSR");
        assertNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutDw() {
        LookupTable lut = BbdrUtils.getNskyLookupTableDw("AATSR");
        assertNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutUp() {
        LookupTable lut = BbdrUtils.getNskyLookupTableUp("AATSR");
        assertNull(lut);
        // TODO write tests
    }

}

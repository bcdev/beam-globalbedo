package org.esa.beam.globalbedo.bbdr;

import junit.framework.TestCase;
import org.esa.beam.util.math.LookupTable;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class VgtLutTest extends TestCase {

    public void testLutAot() {
        LookupTable lut = BbdrUtils.getAotKxLookupTable("VGT");
        assertNull(lut);
        // TODO write tests
    }

    public void testAotKx() {
        LookupTable lut = BbdrUtils.getAotKxLookupTable("VGT");
        assertNull(lut);
        // TODO write tests
    }

    public void testCwvOzoLut() {
        LookupTable lut = BbdrUtils.getTransposedCwvOzoLookupTable("VGT");
        assertNotNull(lut);
        // TODO write tests
    }

    public void testCwvOzoLutKx() {
        LookupTable lut = BbdrUtils.getCwvOzoKxLookupTable("VGT");
        assertNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutDw() {
        LookupTable lut = BbdrUtils.getNskyLookupTableDw("VGT");
        assertNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutUp() {
        LookupTable lut = BbdrUtils.getNskyLookupTableUp("VGT");
        assertNull(lut);
        // TODO write tests
    }

}

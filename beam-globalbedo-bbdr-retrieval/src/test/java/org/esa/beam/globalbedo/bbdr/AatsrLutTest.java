package org.esa.beam.globalbedo.bbdr;

import junit.framework.TestCase;
import org.esa.beam.util.math.LookupTable;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AatsrLutTest extends TestCase {

    public void testLutAot() {
        AotLookupTable lut = BbdrUtils.getAotLookupTable("AATSR");
        assertNotNull(lut);
        // TODO write tests
    }

    public void testAotKx() {
        LookupTable lut = BbdrUtils.getAotKxLookupTable("AATSR");
        assertNotNull(lut);
        // TODO write tests
    }

    public void testCwvOzoLut() {
        LookupTable lut = BbdrUtils.getTransposedCwvOzoLookupTable("AATSR");
        assertNotNull(lut);
        // TODO write tests
    }

    public void testCwvOzoLutKx() {
        LookupTable lut = BbdrUtils.getCwvOzoKxLookupTable("AATSR");
        assertNotNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutDw() {
        NskyLookupTable lut = BbdrUtils.getNskyLookupTableDw("AATSR");
        assertNotNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutUp() {
        NskyLookupTable lut = BbdrUtils.getNskyLookupTableUp("AATSR");
        assertNotNull(lut);
        // TODO write tests
    }

}

package org.esa.beam.globalbedo.bbdr;

import junit.framework.TestCase;
import org.esa.beam.util.math.LookupTable;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class VgtLutTest extends TestCase {

    public void testLutAot() {
        AotLookupTable lut = BbdrUtils.getAotLookupTable("VGT");
        assertNotNull(lut);
        // TODO write tests
    }

    public void testAotKx() {
        LookupTable lut = BbdrUtils.getAotKxLookupTable("VGT");
        assertNotNull(lut);
        // TODO write tests
    }

    public void testCwvOzoLut() {
        LookupTable lut = BbdrUtils.getTransposedCwvOzoLookupTable("VGT");
        assertNotNull(lut);
        // TODO write tests
    }

    public void testCwvOzoLutKx() {
        LookupTable lut = BbdrUtils.getCwvOzoKxLookupTable("VGT");
        assertNotNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutDw() {
        NskyLookupTable lut = BbdrUtils.getNskyLookupTableDw("VGT");
        assertNotNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutUp() {
        NskyLookupTable lut = BbdrUtils.getNskyLookupTableUp("VGT");
        assertNotNull(lut);
        // TODO write tests
    }

}

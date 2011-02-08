package org.esa.beam.globalbedo.bbdr;

import junit.framework.TestCase;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AatsrLutTest extends TestCase {

    public void testLutAot() {
        AotLookupTable lut = BbdrUtils.getAotLookupTable(Sensor.AATSR);
        assertNotNull(lut);
        // TODO write tests
    }

    public void testAotKx() {
        LookupTable lut = BbdrUtils.getAotKxLookupTable(Sensor.AATSR);
        assertNotNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutDw() {
        NskyLookupTable lut = BbdrUtils.getNskyLookupTableDw(Sensor.AATSR);
        assertNotNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutUp() {
        NskyLookupTable lut = BbdrUtils.getNskyLookupTableUp(Sensor.AATSR);
        assertNotNull(lut);
        // TODO write tests
    }

}

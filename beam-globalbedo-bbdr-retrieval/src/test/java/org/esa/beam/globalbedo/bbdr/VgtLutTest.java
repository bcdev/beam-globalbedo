package org.esa.beam.globalbedo.bbdr;

import junit.framework.TestCase;

import java.io.IOException;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class VgtLutTest extends TestCase {

    public void testLutAot() throws IOException {
        AotLookupTable lut = BbdrUtils.getAotLookupTable(Sensor.SPOT_VGT);
        assertNotNull(lut);
        // TODO write tests
    }

    public void testAotKx() throws IOException {
        LookupTable lut = BbdrUtils.getAotKxLookupTable(Sensor.SPOT_VGT);
        assertNotNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutDw() throws IOException {
        NskyLookupTable lut = BbdrUtils.getNskyLookupTableDw(Sensor.SPOT_VGT);
        assertNotNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutUp() throws IOException {
        NskyLookupTable lut = BbdrUtils.getNskyLookupTableUp(Sensor.SPOT_VGT);
        assertNotNull(lut);
        // TODO write tests
    }

}

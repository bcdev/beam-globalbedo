package org.esa.beam.globalbedo.bbdr;

import junit.framework.TestCase;
import org.esa.beam.util.math.LookupTable;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class VgtLutTest extends TestCase {

    public void testLutAot() {
        AotLookupTable lut = BbdrUtils.getAotLookupTable(Sensor.SPOT_VGT);
        assertNotNull(lut);
        // TODO write tests
    }

    public void testAotKx() {
        LookupTable lut = BbdrUtils.getAotKxLookupTable(Sensor.SPOT_VGT);
        assertNotNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutDw() {
        NskyLookupTable lut = BbdrUtils.getNskyLookupTableDw(Sensor.SPOT_VGT);
        assertNotNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutUp() {
        NskyLookupTable lut = BbdrUtils.getNskyLookupTableUp(Sensor.SPOT_VGT);
        assertNotNull(lut);
        // TODO write tests
    }

}

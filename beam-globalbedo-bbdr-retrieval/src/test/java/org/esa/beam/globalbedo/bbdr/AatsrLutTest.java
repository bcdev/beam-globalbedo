package org.esa.beam.globalbedo.bbdr;

import junit.framework.TestCase;
import org.esa.beam.util.math.LookupTable;

import java.io.IOException;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AatsrLutTest extends TestCase {

    public void testLutAot() throws IOException {
        AotLookupTable lut = BbdrUtils.getAotLookupTable(Sensor.AATSR_FWARD);
        assertNotNull(lut);
        // TODO write tests
    }

    public void testAotKx() throws IOException {
        LookupTable lut = BbdrUtils.getAotKxLookupTable(Sensor.AATSR_FWARD);
        assertNotNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutDw() throws IOException {
        NskyLookupTable lut = BbdrUtils.getNskyLookupTableDw(Sensor.AATSR_FWARD);
        assertNotNull(lut);
        // TODO write tests
    }

    public void testMerisNskyLutUp() throws IOException {
        NskyLookupTable lut = BbdrUtils.getNskyLookupTableUp(Sensor.AATSR_FWARD);
        assertNotNull(lut);
        // TODO write tests
    }

}

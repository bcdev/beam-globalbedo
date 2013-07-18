package org.esa.beam.globalbedo.inversion.util;

import Jama.Matrix;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.globalbedo.inversion.GlobalbedoLevel3MonthlyFrom8DayAlbedo;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class MonthlyAlbedoTest extends TestCase {

    public void testGetMonthlyWeighting() throws Exception {

        float[][] monthlyWeighting = AlbedoInversionUtils.getMonthlyWeighting();

        // expected results from breadboard
        assertEquals(12, monthlyWeighting.length);
        assertEquals(365, monthlyWeighting[0].length);
        assertEquals(0.21216308f, monthlyWeighting[0][0]);
        assertEquals(3.5346034E-8f, monthlyWeighting[7][28]);
        assertEquals(1.23058005E-11f, monthlyWeighting[9][0]);
        assertEquals(1.9015411E-5f, monthlyWeighting[10][188]);
        assertEquals(0.37752336f, monthlyWeighting[11][364]);
    }

    public void testGetMonthString() throws Exception {
        int month = 11;
        assertEquals("11", IOUtils.getMonthString(month));
        month = 7;
        assertEquals("07", IOUtils.getMonthString(month));
        month = -1;
        assertNull(IOUtils.getMonthString(month));
        month = 13;
        assertNull(IOUtils.getMonthString(month));
    }

}

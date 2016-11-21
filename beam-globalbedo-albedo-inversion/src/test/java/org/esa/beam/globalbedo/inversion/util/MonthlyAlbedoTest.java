package org.esa.beam.globalbedo.inversion.util;

import junit.framework.TestCase;
import org.esa.beam.globalbedo.inversion.GlobalbedoLevel3MonthlyFrom8DayAlbedo;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class MonthlyAlbedoTest extends TestCase {

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

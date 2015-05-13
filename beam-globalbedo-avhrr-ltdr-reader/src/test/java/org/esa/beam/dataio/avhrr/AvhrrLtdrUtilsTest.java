package org.esa.beam.dataio.avhrr;

import junit.framework.TestCase;

/**
 *
 * @author olafd
 */
public class AvhrrLtdrUtilsTest extends TestCase {

    public void testGetLtdrStartStopTimes()  {
        int year = 2005;
        int doy = 183;
        assertEquals("2005-07-02", AvhrrLtdrUtils.getDateFromDoy(year, doy));

        year = 2004; // a leap year
        doy = 176;
        assertEquals("2004-06-24", AvhrrLtdrUtils.getDateFromDoy(year, doy));
    }
}

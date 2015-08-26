package org.esa.beam.dataio.avhrr;

import junit.framework.TestCase;
import ncsa.hdf.object.Attribute;
import ncsa.hdf.object.Datatype;
import ncsa.hdf.object.FileFormat;
import ncsa.hdf.object.h4.H4Datatype;
import ncsa.hdf.object.h4.H4File;

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

    public void testGetAttributeValue() {
        Attribute attribute = new Attribute("test", new H4Datatype(Datatype.CLASS_STRING), new long[]{1});
        attribute.setValue(new String[]{"bla"});
        String attributeValue = AvhrrLtdrUtils.getAttributeValue(attribute);
        assertEquals("bla", attributeValue);

        attribute = new Attribute("test2", new H4Datatype(Datatype.CLASS_STRING), new long[]{2});
        attribute.setValue(new String[]{"bla", "blubb"});
        attributeValue = AvhrrLtdrUtils.getAttributeValue(attribute);
        assertEquals("bla blubb", attributeValue);
    }
}

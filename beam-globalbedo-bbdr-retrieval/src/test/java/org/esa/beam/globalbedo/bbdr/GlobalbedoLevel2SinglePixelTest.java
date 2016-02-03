package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.globalbedo.auxdata.AerosolClimatology;
import org.junit.Test;

import java.text.ParseException;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
public class GlobalbedoLevel2SinglePixelTest {

    @Test
    public void testGetProductTime() {
        String productName = "subset_0_of_MER_RR__1PNMAP20060511_095146_000003522047_00337_21934_0001_1x1";
        try {
            final ProductData.UTC productTime = GlobalbedoLevel2SinglePixel.getProductTime(Sensor.MERIS, productName);
            assert productTime != null;
            final String formatted = productTime.format();
            System.out.println("formatted = " + formatted);
            assertEquals(formatted, "11-MAY-2006 09:51:46.000000");
        } catch (ParseException e) {
            fail(e.getMessage());
        }

        productName = "subset_0_of_subset_saudiarabia_of_V2KRNP____20140511F025_1x1";
        try {
            final ProductData.UTC productTime = GlobalbedoLevel2SinglePixel.getProductTime(Sensor.VGT, productName);
            assert productTime != null;
            final String formatted = productTime.format();
            System.out.println("formatted = " + formatted);
            assertEquals(formatted, "11-MAY-2014 00:00:00.000000");
        } catch (ParseException e) {
            fail(e.getMessage());
        }

    }

}

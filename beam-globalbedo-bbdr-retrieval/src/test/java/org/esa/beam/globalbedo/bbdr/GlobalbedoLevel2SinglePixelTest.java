package org.esa.beam.globalbedo.bbdr;

import junit.framework.Assert;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
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

    @Test
    public void testAerosolClimatology() {
        final String filename = BbdrConstants.AEROSOL_CLIMATOLOGY_FILE;
        final String testFilePath = GlobalbedoLevel2SinglePixel.class.getResource(filename).getPath();
        assertNotNull(testFilePath);

        try {
            final Product aotProduct = ProductIO.readProduct(new File(testFilePath));
            assertNotNull(aotProduct);
            assertEquals("NetCDF", aotProduct.getProductType());
            assertEquals(360, aotProduct.getSceneRasterWidth());
            assertEquals(180, aotProduct.getSceneRasterHeight());
            assertNotNull(aotProduct.getGeoCoding());
            for (int i=1; i<=12; i++) {
                assertNotNull(aotProduct.getBand(BbdrConstants.AEROSOL_CLIMATOLOGY_MONTHLY_BAND_GROUP_NAME + i));
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

}

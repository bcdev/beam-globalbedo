package org.esa.beam.landcover;

import org.esa.beam.GlobalTestConfig;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ScripGeocodingWriterTest {

    private Product l1Product;
    private Band l1FlagBand;

    @Before
    public void setUp() throws Exception {
        l1Product = new Product("testName", "testType", 5, 5);
        Band latBand = l1Product.addBand("latitude", ProductData.TYPE_FLOAT32);
        Band lonBand = l1Product.addBand("longitude", ProductData.TYPE_FLOAT32);
        latBand.setRasterData(ProductData.createInstance(new float[] {
                58, 58, 58, 58, 58,
                57, 57, 57, 57, 57,
                56, 56, 56, 56, 56,
                55, 55, 55, 55, 55,
                54, 54, 54, 54, 54
        }));
        lonBand.setRasterData(ProductData.createInstance(new float[]{
                11, 12, 13, 14, 15,
                11, 12, 13, 14, 15,
                11, 12, 13, 14, 15,
                11, 12, 13, 14, 15,
                11, 12, 13, 14, 15
        }));
        l1Product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 3));
    }

    @After
    public void tearDown() throws Exception {
        l1Product.dispose();
    }

    @Test
    public void testWriter() throws Exception {
        ScripGeocodingWriterPlugIn plugIn = new ScripGeocodingWriterPlugIn();
        ProductWriter writer = plugIn.createWriterInstance();
        File outputFile = new File("scrip-geo.nc");
        writer.writeProductNodes(l1Product, outputFile );
    }
}
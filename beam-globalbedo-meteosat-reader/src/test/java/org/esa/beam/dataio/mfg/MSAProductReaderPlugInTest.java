package org.esa.beam.dataio.mfg;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;

import static org.junit.Assert.*;

public class MSAProductReaderPlugInTest {

    private org.esa.beam.dataio.mfg.MSAProductReaderPlugIn plugIn;

    @Before
    public void setup() {
        plugIn = new org.esa.beam.dataio.mfg.MSAProductReaderPlugIn();
    }

    @Test
    public void testIfPlugInIsLoaded() {
        ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();
        Iterator<ProductReaderPlugIn> readerPlugIns = ioPlugInManager.getReaderPlugIns(
                MSAProductReaderPlugIn.FORMAT_NAME_METEOSAT_MSA);
        assertTrue(readerPlugIns.hasNext());
        assertTrue(readerPlugIns.next() instanceof org.esa.beam.dataio.mfg.MSAProductReaderPlugIn);

    }

    @Test
    public void testCreateReaderInstanceReturnsNewInstanceEachTime() {
        ProductReader firstInstance = plugIn.createReaderInstance();
        assertNotNull(firstInstance);
        ProductReader secondInstance = plugIn.createReaderInstance();
        assertNotSame(secondInstance, firstInstance);
    }

}

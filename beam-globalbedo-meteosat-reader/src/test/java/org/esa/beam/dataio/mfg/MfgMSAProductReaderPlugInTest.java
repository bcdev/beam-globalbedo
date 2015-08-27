package org.esa.beam.dataio.mfg;

import org.esa.beam.dataio.msg.MsgMSAProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class MfgMSAProductReaderPlugInTest {

    private MfgMSAProductReaderPlugIn plugIn;

    @Before
    public void setup() {
        plugIn = new MfgMSAProductReaderPlugIn();
    }

    @Test
    public void testCreateReaderInstanceReturnsNewInstanceEachTime() {
        ProductReader firstInstance = plugIn.createReaderInstance();
        assertNotNull(firstInstance);
        ProductReader secondInstance = plugIn.createReaderInstance();
        assertNotSame(secondInstance, firstInstance);
    }

    @Test
    public void testMfgMSAReaderPlugInsAreLoaded() {
        testReaderPlugInLoading("GLOBALBEDO-METEOSAT-MFG-SURFACE-ALBEDO", MfgMSAProductReaderPlugIn.class);
    }

    private void testReaderPlugInLoading(String formatName, Class<? extends ProductReaderPlugIn> readerPlugInClass) {
        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        Iterator readerPlugIns = plugInManager.getReaderPlugIns(formatName);

        if (readerPlugIns.hasNext()) {
            ProductReaderPlugIn plugIn = (ProductReaderPlugIn) readerPlugIns.next();
            assertEquals(readerPlugInClass, plugIn.getClass());
        } else {
            fail(String.format("Where is %s?", readerPlugInClass.getSimpleName()));
        }
    }

}

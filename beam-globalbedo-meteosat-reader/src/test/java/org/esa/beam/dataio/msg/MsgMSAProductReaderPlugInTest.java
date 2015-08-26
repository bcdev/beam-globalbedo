package org.esa.beam.dataio.msg;

import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class MsgMSAProductReaderPlugInTest {

    private MsgMSAProductReaderPlugIn plugIn;

    @Before
    public void setup() {
        plugIn = new MsgMSAProductReaderPlugIn();
    }

    @Test
    public void testCreateReaderInstanceReturnsNewInstanceEachTime() {
        ProductReader firstInstance = plugIn.createReaderInstance();
        assertNotNull(firstInstance);
        ProductReader secondInstance = plugIn.createReaderInstance();
        assertNotSame(secondInstance, firstInstance);
    }

    @Test
    public void testMsgMSAReaderPlugInsAreLoaded() {
        testReaderPlugInLoading("GLOBALBEDO-METEOSAT-SURFACE-ALBEDO", MsgMSAProductReaderPlugIn.class);
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

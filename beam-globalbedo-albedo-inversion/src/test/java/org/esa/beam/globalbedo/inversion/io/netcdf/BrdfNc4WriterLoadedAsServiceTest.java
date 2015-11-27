package org.esa.beam.globalbedo.inversion.io.netcdf;

import junit.framework.TestCase;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.junit.Assert;

import java.util.Iterator;

public class BrdfNc4WriterLoadedAsServiceTest extends TestCase {

    public void testWriterIsLoaded() {
        int writerCount = 0;

        ProductIOPlugInManager plugInManager = ProductIOPlugInManager.getInstance();
        Iterator writerPlugIns = plugInManager.getWriterPlugIns("NetCDF4-GA-BRDF");

        while (writerPlugIns.hasNext()) {
            writerCount++;
            ProductWriterPlugIn plugIn = (ProductWriterPlugIn) writerPlugIns.next();
            System.out.println("writerPlugIn.Class = " + plugIn.getClass());
            System.out.println("writerPlugIn.Descr = " + plugIn.getDescription(null));
        }

        Assert.assertEquals(1, writerCount);
    }
}

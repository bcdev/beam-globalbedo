package org.esa.beam.globalbedo.sdr.operators;

import junit.framework.TestCase;


/**
 * Unit test for AerosolOp.
 */
public class MultiTileOperatorTest extends TestCase {

    public void testMultiTileOperator() {
/*
        int w = 32;
        int h = 16;
        RenderedImage imageA = ConstantDescriptor.create((float) w, (float) h, new Float[]{2.5f}, null);

        Product sourceProduct = new Product("A", "AType", w, h);
        Band bandA = sourceProduct.addBand("A", ProductData.TYPE_FLOAT32);
        bandA.setSourceImage(imageA);

        AerosolOp op = new AerosolOp();
        op.setSourceProduct(sourceProduct);
        op.setParameter("sourceBandName", "A");
        op.setParameter("targetBandName1", "X");
        op.setParameter("targetBandName2", "Y");

        Product targetProduct = op.getTargetProduct();
        assertNotNull(targetProduct);
        assertEquals(w, targetProduct.getSceneRasterWidth());
        assertEquals(h, targetProduct.getSceneRasterHeight());

        assertEquals(2, targetProduct.getNumBands());

        Band bandX = targetProduct.getBand("X");
        assertNotNull(bandX);
        RenderedImage imageX = bandX.getSourceImage();
        assertEquals(0.25f, imageX.getData().getSampleFloat(0, 0, 0));

        Band bandY = targetProduct.getBand("Y");
        assertNotNull(bandY);
        RenderedImage imageY = bandY.getSourceImage();
        assertEquals(0.5f, imageY.getData().getSampleFloat(0, 0, 0));

        op.dispose();
*/
    }
}

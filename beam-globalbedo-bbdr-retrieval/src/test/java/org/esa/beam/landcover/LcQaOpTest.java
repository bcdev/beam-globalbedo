package org.esa.beam.landcover;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.BitSetter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class LcQaOpTest {

    private Product l1Product;
    private Band l1FlagBand;

    @Before
    public void setUp() throws Exception {
        l1Product = new Product("testName", "testType", 5, 5);
        l1Product.addBand("radiance_13", ProductData.TYPE_FLOAT32);
        l1FlagBand = l1Product.addBand("l1_flags", ProductData.TYPE_INT8);
        FlagCoding l1FlagCoding = new FlagCoding("l1_flags");
        l1FlagCoding.addFlag("INVALID", BitSetter.setFlag(0, 0), "INVALID");
        l1Product.getFlagCodingGroup().add(l1FlagCoding);
        l1FlagBand.setSampleCoding(l1FlagCoding);
    }

    @After
    public void tearDown() throws Exception {
        l1Product.dispose();
    }

    @Test
    public void testQaOk_noInvalidRows() throws Exception {
        // all rows have at least one valid value
        l1FlagBand.setRasterData(ProductData.createInstance(new byte[]{
                1, 0, 0, 0, 0,
                1, 1, 0, 0, 0,
                1, 1, 0, 1, 1,
                1, 1, 0, 0, 0,
                0, 0, 0, 0, 0,
        }));

        LcQaOp lcQaOp = new LcQaOp();
        lcQaOp.setParameterDefaultValues();
        lcQaOp.setSourceProduct(l1Product);
        lcQaOp.setParameter("invalidMaskBitIndex", 0);
        Product lcQaResultProduct = lcQaOp.getTargetProduct();
        checkResult_qaOk(lcQaResultProduct);
    }

    @Test
    public void testQaOk_onlyTopRowsInvalid() throws Exception {
        // all except first two rows have at least one valid value
        l1FlagBand.setRasterData(ProductData.createInstance(new byte[]{
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                1, 1, 0, 1, 1,
                1, 1, 0, 0, 0,
                0, 0, 0, 0, 0,
        }));

        LcQaOp lcQaOp = new LcQaOp();
        lcQaOp.setParameterDefaultValues();
        lcQaOp.setSourceProduct(l1Product);
        lcQaOp.setParameter("invalidMaskBitIndex", 0);
        Product lcQaResultProduct = lcQaOp.getTargetProduct();
        checkResult_qaOk(lcQaResultProduct);
    }

    @Test
    public void testQaOk_onlyBottomRowsInvalid() throws Exception {
        // all except last two rows have at least one valid value
        l1FlagBand.setRasterData(ProductData.createInstance(new byte[]{
                1, 1, 0, 1, 1,
                1, 1, 0, 0, 0,
                0, 0, 0, 0, 0,
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
        }));

        LcQaOp lcQaOp = new LcQaOp();
        lcQaOp.setParameterDefaultValues();
        lcQaOp.setSourceProduct(l1Product);
        lcQaOp.setParameter("invalidMaskBitIndex", 0);
        Product lcQaResultProduct = lcQaOp.getTargetProduct();
        checkResult_qaOk(lcQaResultProduct);
    }


    @Test
    public void testQaFailed_innerRowInvalid() throws Exception {
        // product has one inner invalid row
        l1FlagBand.setRasterData(ProductData.createInstance(new byte[]{
                1, 0, 0, 0, 0,
                1, 1, 0, 0, 0,
                1, 1, 1, 1, 1,
                1, 1, 0, 0, 0,
                0, 0, 0, 0, 0,
        }));

        LcQaOp lcQaOp = new LcQaOp();
        lcQaOp.setParameterDefaultValues();
        lcQaOp.setSourceProduct(l1Product);
        lcQaOp.setParameter("invalidMaskBitIndex", 0);
        Product lcQaResultProduct = lcQaOp.getTargetProduct();
        checkResult_qaFailed(lcQaResultProduct);
    }

    @Test
    public void testQa_moreThanOneRowInvalid() throws Exception {
        // product has 3 invalid rows
        l1FlagBand.setRasterData(ProductData.createInstance(new byte[]{
                1, 1, 0, 0, 0,
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                1, 1, 1, 1, 1,
                0, 0, 0, 0, 0,
        }));

        LcQaOp lcQaOp = new LcQaOp();
        lcQaOp.setParameterDefaultValues();
        lcQaOp.setSourceProduct(l1Product);
        lcQaOp.setParameter("invalidMaskBitIndex", 0);
        lcQaOp.setParameter("badDataRowsThreshold", 0);   // no invalid row allowed - QA should fail
        Product lcQaResultProduct = lcQaOp.getTargetProduct();
        checkResult_qaFailed(lcQaResultProduct);

        lcQaOp = new LcQaOp();
        lcQaOp.setParameterDefaultValues();
        lcQaOp.setSourceProduct(l1Product);
        lcQaOp.setParameter("invalidMaskBitIndex", 0);
        lcQaOp.setParameter("badDataRowsThreshold", 3);   // 3 invalid rows allowed - QA should pass
        lcQaResultProduct = lcQaOp.getTargetProduct();
        checkResult_qaOk(lcQaResultProduct);
    }

    @Test
    public void testQa_oneRowPartiallyInvalid() throws Exception {
        // one row has 80% invalid pixels
        l1FlagBand.setRasterData(ProductData.createInstance(new byte[]{
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                1, 1, 1, 0, 1,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
        }));

        LcQaOp lcQaOp = new LcQaOp();
        lcQaOp.setParameterDefaultValues();
        lcQaOp.setSourceProduct(l1Product);
        lcQaOp.setParameter("invalidMaskBitIndex", 0);
        lcQaOp.setParameter("percentBadDataValuesThreshold", 79.0f);   // 79% invalid pixels allowed - QA should fail
        Product lcQaResultProduct = lcQaOp.getTargetProduct();
        checkResult_qaFailed(lcQaResultProduct);

        lcQaOp = new LcQaOp();
        lcQaOp.setParameterDefaultValues();
        lcQaOp.setSourceProduct(l1Product);
        lcQaOp.setParameter("invalidMaskBitIndex", 0);
        lcQaOp.setParameter("percentBadDataValuesThreshold", 81.0f);   // 81% invalid pixels allowed - QA should pass
        lcQaResultProduct = lcQaOp.getTargetProduct();
        checkResult_qaOk(lcQaResultProduct);

    }

    private void checkResult_qaOk(Product lcQaResultProduct) {
        assertEquals(l1Product.getName(), lcQaResultProduct.getName());
        assertEquals(l1Product.getProductType(), lcQaResultProduct.getProductType());
        assertEquals(0, lcQaResultProduct.getSceneRasterWidth());
        assertEquals(0, lcQaResultProduct.getSceneRasterHeight());
        assertNotNull(lcQaResultProduct.getBands());
        assertEquals(0, lcQaResultProduct.getBands().length);
    }

    private void checkResult_qaFailed(Product lcQaResultProduct) {
        assertEquals(l1Product.getSceneRasterWidth(), lcQaResultProduct.getSceneRasterWidth());
        assertEquals(l1Product.getSceneRasterHeight(), lcQaResultProduct.getSceneRasterHeight());
        assertEquals(l1Product.getName(), lcQaResultProduct.getName());
        assertEquals(l1Product.getProductType() + "_QA_FAILED", lcQaResultProduct.getProductType());
        assertNotNull(lcQaResultProduct.getBands());
        assertEquals(2, lcQaResultProduct.getBands().length);
        assertNotNull(lcQaResultProduct.getBand("radiance_13"));
        assertNotNull(lcQaResultProduct.getBand("l1_flags"));
        final FlagCoding lcQaFlagCoding = lcQaResultProduct.getFlagCodingGroup().get("l1_flags");
        assertNotNull(lcQaFlagCoding);
        assertNotNull(lcQaFlagCoding.getFlagNames());
        assertEquals(1, lcQaFlagCoding.getFlagNames().length);
        assertEquals("INVALID", lcQaFlagCoding.getFlagNames()[0]);
    }
}
package org.esa.beam.globalbedo.inversion.spectral;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.gpf.operators.standard.MergeOp;
import org.junit.Test;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class SpectralBrdfBandmergeTest extends TestCase {

    @Test
    public void testMergeOp_gaspectral() throws Exception {
        final Product product1 = new Product("dummy1", "mergeOpTest", 10, 10);

        product1.addBand("mean_b1_f0", ProductData.TYPE_FLOAT32);
        product1.addBand("mean_b1_f1", ProductData.TYPE_FLOAT32);
        product1.addBand("mean_b1_f2", ProductData.TYPE_FLOAT32);
        product1.addBand("var_b1_f0_b1_f0", ProductData.TYPE_FLOAT32);
        product1.addBand("var_b1_f1_b1_f1", ProductData.TYPE_FLOAT32);
        product1.addBand("var_b1_f2_b1_f2", ProductData.TYPE_FLOAT32);
        product1.addBand("Entropy", ProductData.TYPE_FLOAT32);
        product1.addBand("Relative_Entropy", ProductData.TYPE_FLOAT32);

        final Product product2 = new Product("dummy1", "mergeOpTest", 10, 10);

        product2.addBand("mean_b2_f0", ProductData.TYPE_FLOAT32);
        product2.addBand("mean_b2_f1", ProductData.TYPE_FLOAT32);
        product2.addBand("mean_b2_f2", ProductData.TYPE_FLOAT32);
        product2.addBand("var_b2_f0_b2_f0", ProductData.TYPE_FLOAT32);
        product2.addBand("var_b2_f1_b2_f1", ProductData.TYPE_FLOAT32);
        product2.addBand("var_b2_f2_b2_f2", ProductData.TYPE_FLOAT32);
        product2.addBand("Entropy", ProductData.TYPE_FLOAT32);
        product2.addBand("Relative_Entropy", ProductData.TYPE_FLOAT32);

        Product[] products = {product1, product2};
        final MergeOp mergeOp = new MergeOp();
        mergeOp.setSourceProducts(products);
        mergeOp.setSourceProduct("masterProduct", product1);
        final Product mergedProduct = mergeOp.getTargetProduct();
        assertNotNull(mergedProduct);
        assertEquals(14, mergedProduct.getNumBands());
        assertTrue(mergedProduct.containsBand("mean_b1_f0"));
        assertTrue(mergedProduct.containsBand("mean_b1_f1"));
        assertTrue(mergedProduct.containsBand("mean_b1_f2"));
        assertTrue(mergedProduct.containsBand("var_b1_f0_b1_f0"));
        assertTrue(mergedProduct.containsBand("var_b1_f1_b1_f1"));
        assertTrue(mergedProduct.containsBand("var_b1_f2_b1_f2"));
        assertTrue(mergedProduct.containsBand("Entropy"));
        assertTrue(mergedProduct.containsBand("Relative_Entropy"));
        assertTrue(mergedProduct.containsBand("mean_b2_f0"));
        assertTrue(mergedProduct.containsBand("mean_b2_f1"));
        assertTrue(mergedProduct.containsBand("mean_b2_f2"));
        assertTrue(mergedProduct.containsBand("var_b2_f0_b2_f0"));
        assertTrue(mergedProduct.containsBand("var_b2_f1_b2_f1"));
        assertTrue(mergedProduct.containsBand("var_b2_f2_b2_f2"));
    }

}

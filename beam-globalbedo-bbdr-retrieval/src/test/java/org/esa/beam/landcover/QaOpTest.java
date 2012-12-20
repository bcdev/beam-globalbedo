package org.esa.beam.landcover;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import static java.lang.Float.NaN;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 19.12.12
 * Time: 15:50
 *
 * @author olafd
 */
public class QaOpTest {

    @Test
    public void testQaOk() throws Exception {


    }

    @Test
    public void testQaFailed() throws Exception {
        final Product product = new Product("badName", "badType", 3, 3);
        final Band band1 = product.addBand("b1", ProductData.TYPE_FLOAT32);
        final Band band2 = product.addBand("b2", ProductData.TYPE_FLOAT32);
        band1.setRasterData(ProductData.createInstance(new float[]{
                2.0F, NaN, NaN,
                1.0F, 1.0F, NaN,
                0.0F, 0.0F, 0.0F,
        }));
        band2.setRasterData(ProductData.createInstance(new float[]{
                0.0F, NaN, NaN,
                0.0F, 1.0F, NaN,
                0.0F, 1.0F, 2.0F,
        }));
        band1.setNoDataValue(NaN);
        band1.setNoDataValueUsed(true);
        band2.setNoDataValue(NaN);
        band2.setNoDataValueUsed(true);

        QaOp qaOp = new QaOp();
        qaOp.setSourceProduct(product);
    }
}

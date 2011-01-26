/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.globalbedo.bbdr;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class BbdrOpTest {

    private static final int W = 3;
    private static final int H = 3;

//    @Test
    public void testMerisSdrPixel() {
        BbdrOp bbdrOp = new BbdrOp();
        bbdrOp.setSourceProduct(createSourceProduct());
        bbdrOp.setParameter("maskExpression", "true");
        Product targetProduct = bbdrOp.getTargetProduct();
        assertNotNull(targetProduct);
        int numBands = targetProduct.getNumBands();
        assertEquals(15 + 1, numBands);
        assertEquals(0.04042f, getSample(targetProduct, "sdr_1"), 1E-4);
        assertEquals(0.05604f, getSample(targetProduct, "sdr_2"), 1E-4);
        assertEquals(0.08070f, getSample(targetProduct, "sdr_3"), 1E-4);
        assertEquals(0.09621f, getSample(targetProduct, "sdr_4"), 1E-4);
        assertEquals(0.14354f, getSample(targetProduct, "sdr_5"), 1E-4);
        assertEquals(0.18669f, getSample(targetProduct, "sdr_6"), 1E-4);
        assertEquals(0.20817f, getSample(targetProduct, "sdr_7"), 1E-4);
        assertEquals(0.21411f, getSample(targetProduct, "sdr_8"), 1E-4);
        assertEquals(0.23358f, getSample(targetProduct, "sdr_9"), 1E-4);
        assertEquals(0.24567f, getSample(targetProduct, "sdr_10"), 1E-4);
        assertEquals(0.37566f, getSample(targetProduct, "sdr_11"), 1E-4);
        assertEquals(0.25639f, getSample(targetProduct, "sdr_12"), 1E-4);
        assertEquals(0.27614f, getSample(targetProduct, "sdr_13"), 1E-4);
        assertEquals(0.28028f, getSample(targetProduct, "sdr_14"), 1E-4);
        assertEquals(0.38764f, getSample(targetProduct, "sdr_15"), 1E-4);
        assertEquals(0.00322f, getSample(targetProduct, "sdr_error"), 1E-4);
    }

    private static Product createSourceProduct() {
        // data taken from himalaya test product x = 28, y = 2
        Product sourceProduct = new Product("TEST.N1", "MER_RR__1P", W, H);
        sourceProduct.addBand(addBand("view_zenith", 39.21092987060547));
        sourceProduct.addBand(addBand("view_azimuth", 100.31485748291016));
        sourceProduct.addBand(addBand("sun_zenith", 61.56242752075195));
        sourceProduct.addBand(addBand("sun_azimuth", 149.93161010742188));
        sourceProduct.addBand(addBand("elevation", 5090.0));
        sourceProduct.addBand(addBand("ozone", 257.1309509277344));
        sourceProduct.addBand(addBand("aot", 0.37025466561317444));
        sourceProduct.addBand(addBand("aot_err", 0.09551463276147842));
        sourceProduct.addBand(addBand("reflectance_1", 0.197262242436409));
        sourceProduct.addBand(addBand("reflectance_2", 0.1874786913394928));
        sourceProduct.addBand(addBand("reflectance_3", 0.1808471381664276));
        sourceProduct.addBand(addBand("reflectance_4", 0.18388484418392181));
        sourceProduct.addBand(addBand("reflectance_5", 0.2018040120601654));
        sourceProduct.addBand(addBand("reflectance_6", 0.2270776331424713));
        sourceProduct.addBand(addBand("reflectance_7", 0.2511415481567383));
        sourceProduct.addBand(addBand("reflectance_8", 0.25836682319641113));
        sourceProduct.addBand(addBand("reflectance_9", 0.2701571583747864));
        sourceProduct.addBand(addBand("reflectance_10", 0.2871195673942566));
        sourceProduct.addBand(addBand("reflectance_11", 0.1284395158290863));
        sourceProduct.addBand(addBand("reflectance_12", 0.29514235258102417));
        sourceProduct.addBand(addBand("reflectance_13", 0.3082677721977234));
        sourceProduct.addBand(addBand("reflectance_14", 0.30971765518188477));
        sourceProduct.addBand(addBand("reflectance_15", 0.2931019067764282));
        return sourceProduct;
    }

    private static float getSample(Product targetProduct, String bandName) {
        return targetProduct.getBand(bandName).getGeophysicalImage().getData().getSampleFloat(1, 1, 0);
    }

    private static Band addBand(String name, double value) {
        Band band = new Band(name, ProductData.TYPE_FLOAT32, W, H);
        band.setSourceImage(ConstantDescriptor.create(1.0f * W, 1.0f * H, new Double[]{value}, null));
        return band;
    }

    @Test
    public void testMatrixSquare() {
        double[] in = {2, 4, 6};
        Matrix squareMatrix = BbdrOp.matrixSquare(in);
        assertNotNull(squareMatrix);
        assertEquals(3, squareMatrix.getColumnDimension());
        assertEquals(3, squareMatrix.getRowDimension());

        assertEquals(4, squareMatrix.get(0,0), 1E-6);
        assertEquals(8, squareMatrix.get(1,0), 1E-6);
        assertEquals(12, squareMatrix.get(2,0), 1E-6);

        assertEquals(8, squareMatrix.get(0,1), 1E-6);
        assertEquals(16, squareMatrix.get(1,1), 1E-6);
        assertEquals(24, squareMatrix.get(2,1), 1E-6);

        assertEquals(12, squareMatrix.get(0,2), 1E-6);
        assertEquals(24, squareMatrix.get(1,2), 1E-6);
        assertEquals(36, squareMatrix.get(2,2), 1E-6);
    }

}

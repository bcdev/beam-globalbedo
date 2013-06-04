/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.globalbedo.bbdr.seaice;


import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.bbdr.BbdrConstants;
import org.esa.beam.globalbedo.bbdr.TileExtractor;
import org.esa.beam.util.ProductUtils;

/**
 * Reprojects a BBDR product onto one single PST 'quadrant'
 * if the MERIS and AATSR data intersects with it.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l2.bbdr.pst.single",
        description = "Reprojects a BBDR product onto one single PST 'quadrant' " + " " +
                "if the MERIS and AATSR data intersects with it.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2013 by Brockmann Consult")
public class BbdrToPstSingleQuadrantOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "90W_0", valueSet = {"180W_90W", "90W_0", "0_90E", "90E_180E"})
    private String quadrantName;

    @Override
    public void initialize() throws OperatorException {
        // first check with a reprojection of a small band subset if PST quadrant has data at all...
        // if it has not, set a dummy target product
        final Product subsetSourceProduct = getBandSubsetProduct();
        final double referencePixelX = getReferencePixelX();
        final double referencePixelY = getReferencePixelY();

        final Product subsetProduct = PolarStereographicOp.reprojectToPolarStereographic(subsetSourceProduct,
                referencePixelX, referencePixelY,
                BbdrConstants.SEAICE_PST_PIXEL_SIZE_X, BbdrConstants.SEAICE_PST_PIXEL_SIZE_Y,
                BbdrConstants.SEAICE_PST_QUADRANT_PRODUCT_WIDTH, BbdrConstants.SEAICE_PST_QUADRANT_PRODUCT_HEIGHT);

        final Product quadrantProduct = getQuadrantProduct(subsetProduct, referencePixelX, referencePixelY);
        if (quadrantProduct != null) {
            setTargetProduct(quadrantProduct);
        } else {
            setTargetProduct(new Product("dummy", "dummy", 0, 0));
        }
        getTargetProduct().setProductType(sourceProduct.getProductType() + "_PST");
    }

    private double getReferencePixelY() {
        if (quadrantName.equals("180W_90W") || quadrantName.equals("90E_180E")) {
            return (double) BbdrConstants.SEAICE_PST_QUADRANT_PRODUCT_HEIGHT;
        } else {
            return 0.0;
        }
    }

    private double getReferencePixelX() {
        if (quadrantName.equals("180W_90W") || quadrantName.equals("90W_0")) {
            return (double) BbdrConstants.SEAICE_PST_QUADRANT_PRODUCT_WIDTH;
        } else {
            return 0.0;
        }
    }

    private Product getQuadrantProduct(Product subsetProduct, double referencePixelX, double referencePixelY) {
        final Geometry sourceGeometry = TileExtractor.computeProductGeometry(sourceProduct);
        final Geometry quadGeometry = TileExtractor.computeProductGeometry(subsetProduct);
        if (quadGeometry != null && sourceGeometry.intersects(quadGeometry)) {
            // reproject full source product
            return PolarStereographicOp.reprojectToPolarStereographic(sourceProduct,
                    referencePixelX, referencePixelY,
                    BbdrConstants.SEAICE_PST_PIXEL_SIZE_X, BbdrConstants.SEAICE_PST_PIXEL_SIZE_Y,
                    BbdrConstants.SEAICE_PST_QUADRANT_PRODUCT_WIDTH, BbdrConstants.SEAICE_PST_QUADRANT_PRODUCT_HEIGHT);
        } else {
            return null;
        }
    }

    private Product getBandSubsetProduct() {
        Product bandSubsetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());
        ProductUtils.copyMetadata(sourceProduct, bandSubsetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, bandSubsetProduct);
        bandSubsetProduct.setStartTime(sourceProduct.getStartTime());
        bandSubsetProduct.setEndTime(sourceProduct.getEndTime());
        // just copy 1 band and lat/lon tpgs...
        ProductUtils.copyBand(sourceProduct.getBandAt(0).getName(), sourceProduct, bandSubsetProduct, true);
        if (!bandSubsetProduct.containsTiePointGrid("latitude")) {
            ProductUtils.copyTiePointGrid("latitude", sourceProduct, bandSubsetProduct);
        }
        if (!bandSubsetProduct.containsTiePointGrid("longitude")) {
            ProductUtils.copyTiePointGrid("longitude", sourceProduct, bandSubsetProduct);
        }

        return bandSubsetProduct;
    }

public static class Spi extends OperatorSpi {

    public Spi() {
        super(BbdrToPstSingleQuadrantOp.class);
    }
}
}

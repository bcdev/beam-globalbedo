package org.esa.beam.globalbedo.inversion.util;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;

/**
 * The merge operator allows copying raster data from other products to a specified product. The first product provided
 * is considered the 'master product', into which the raster data coming from the other products is copied. Existing
 * nodes are kept.
 * <p/>
 * It is mandatory that the products share the same scene, that is, their width and height need to match with those of
 * the master product as well as their geographic position.
 *
 * @author Olaf Danne
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Ralf Quast
 * @author Marco Zuehlke
 * @author Thomas Storm
 */
@OperatorMetadata(alias = "MergeTest",
                  description = "Allows copying raster data from any number of source products to a specified 'master'" +
                          " product.",
                  authors = "BEAM team",
                  version = "1.0",
                  copyright = "(c) 2012 by Brockmann Consult",
                  internal = false)
public class MergeTestOp extends Operator {

    @SourceProduct
    private Product masterProduct;

    @SourceProduct
    private Product slaveProduct;

    @Override
    public void initialize() throws OperatorException {
        Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);
    }

    private Product createTargetProduct() {

        Product targetProduct = new Product(masterProduct.getName(),
                                            masterProduct.getProductType(),
                                            masterProduct.getSceneRasterWidth(),
                                            masterProduct.getSceneRasterHeight());

        ProductUtils.copyMetadata(masterProduct, targetProduct);
        ProductUtils.copyFlagCodings(masterProduct, targetProduct);
        ProductUtils.copyGeoCoding(masterProduct, targetProduct);
        ProductUtils.copyFlagBands(masterProduct, targetProduct, true);
        ProductUtils.copyMasks(masterProduct, targetProduct);
        targetProduct.setStartTime(masterProduct.getStartTime());
        targetProduct.setEndTime(masterProduct.getEndTime());

        // merge all bands
        for (Band band : masterProduct.getBands()) {
            if (!targetProduct.containsBand(band.getName())) {
                ProductUtils.copyBand(band.getName(), masterProduct, targetProduct, true);
                ProductUtils.copyRasterDataNodeProperties(band, targetProduct.getBand(band.getName()));
            }
        }
        for (Band band : slaveProduct.getBands()) {
            if (!targetProduct.containsBand(band.getName())) {
                ProductUtils.copyBand(band.getName(), slaveProduct, targetProduct, true);
            }
        }
        // tie point grids...
        for (TiePointGrid tpg : masterProduct.getTiePointGrids()) {
            if (!targetProduct.containsTiePointGrid(tpg.getName())) {
                ProductUtils.copyTiePointGrid(tpg.getName(), masterProduct, targetProduct);
            }
        }

        return targetProduct;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MergeTestOp.class);
        }
    }
}
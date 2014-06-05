package org.esa.beam.dataio.netcdf.util;

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
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 05.06.2014
 * Time: 17:44
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Mod35.Merge",
                  description = "",
                  authors = "BEAM team",
                  version = "1.0",
                  copyright = "(c) 2012 by Brockmann Consult",
                  internal = true)
public class Mod35MergeOp extends Operator {
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
            super(Mod35MergeOp.class);
        }
    }
}

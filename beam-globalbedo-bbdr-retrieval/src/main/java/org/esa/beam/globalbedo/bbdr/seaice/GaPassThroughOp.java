package org.esa.beam.globalbedo.bbdr.seaice;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

/**
 * Clones a source product into a new target product.
 * Useful i.e. to convert product formats, such as BEAM-DIMAP to NetCDF-BEAM, as this does currently
 * not work correctly if WriteOp with -PformatName is directly called from gpt command line.
 * This works basically the same as the BEAM 'PassThroughOp', but that does not correctly copy the source images...
 *
 * @author olafd
 */
@OperatorMetadata(alias = "ga.passthrough",
                  description = "Sets target product to source product.")
public class GaPassThroughOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @Override
    public void initialize() throws OperatorException {
        Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);
    }

    private Product createTargetProduct() {
        Product targetProduct = new Product(sourceProduct.getName(),
                                            sourceProduct.getProductType(),
                                            sourceProduct.getSceneRasterWidth(),
                                            sourceProduct.getSceneRasterHeight());

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        // copy all bands
        for (Band band : sourceProduct.getBands()) {
            if (!targetProduct.containsBand(band.getName())) {
                ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, true);
                ProductUtils.copyRasterDataNodeProperties(band, targetProduct.getBand(band.getName()));
            }
        }
        // tie point grids...
        for (TiePointGrid tpg : sourceProduct.getTiePointGrids()) {
            if (!targetProduct.containsTiePointGrid(tpg.getName())) {
                ProductUtils.copyTiePointGrid(tpg.getName(), sourceProduct, targetProduct);
            }
        }

        return targetProduct;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GaPassThroughOp.class);
        }
    }
}
































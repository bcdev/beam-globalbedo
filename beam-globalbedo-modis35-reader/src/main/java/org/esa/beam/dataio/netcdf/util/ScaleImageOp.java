package org.esa.beam.dataio.netcdf.util;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.*;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 05.06.2014
 * Time: 17:38
 *
 * @author olafd
 */
@OperatorMetadata(alias = "Mod35.Scale",
                  description = "",
                  authors = "BEAM team",
                  version = "1.0",
                  copyright = "(c) 2012 by Brockmann Consult",
                  internal = true)
public class ScaleImageOp extends Operator {
    @SourceProduct
    private Product sourceProduct;

    @Parameter(description = "scaleFactor")
    private double scaleFactor ;

    @Override
    public void initialize() throws OperatorException {
        Product targetProduct = createTargetProduct();
        setTargetProduct(targetProduct);
    }

    private Product createTargetProduct() {

        if (scaleFactor <= 0.0) {
            throw new OperatorException("invalid scale factor: " + scaleFactor);
        }

        double scaleFactorToUse;
        if (scaleFactor >= 1.0) {
            scaleFactorToUse = Math.floor(scaleFactor);
        } else {
            scaleFactorToUse = Math.floor(1.0/scaleFactor);
        }

        int productWidth;
        int productHeight;
        if (scaleFactor >= 1.0) {
            productWidth = (int) (sourceProduct.getSceneRasterWidth() * scaleFactorToUse);
            productHeight = (int) (sourceProduct.getSceneRasterHeight() * scaleFactorToUse);
        } else {
            productWidth = (int) (sourceProduct.getSceneRasterWidth() / scaleFactorToUse);
            productHeight = (int) (sourceProduct.getSceneRasterHeight() / scaleFactorToUse);
        }

        Product targetProduct = new Product(sourceProduct.getName(),
                                            sourceProduct.getProductType(),
                                            productWidth,
                                            productHeight);

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());

        RenderingHints renderingHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER,
                                                           BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        // scale all bands
        for (Band band : sourceProduct.getBands()) {
            if (!targetProduct.containsBand(band.getName())) {
                ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, true);
                ProductUtils.copyRasterDataNodeProperties(band, targetProduct.getBand(band.getName()));

                final MultiLevelImage sourceImage = targetProduct.getBand(band.getName()).getSourceImage();
                RenderedOp scaledImg = ScaleDescriptor.create(sourceImage,
                                                              (float) scaleFactorToUse,
                                                              (float) scaleFactorToUse,
                                                              0.5f, 0.5f,
                                                              Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                                                              renderingHints);
                targetProduct.getBand(band.getName()).setSourceImage(scaledImg);
            }
        }
        // tie point grids...
        for (TiePointGrid tpg : sourceProduct.getTiePointGrids()) {
            if (!targetProduct.containsTiePointGrid(tpg.getName())) {
                ProductUtils.copyTiePointGrid(tpg.getName(), sourceProduct, targetProduct);
            }
        }

        final PixelGeoCoding pixelGeoCoding =
                new PixelGeoCoding(targetProduct.getBand("Latitude"), targetProduct.getBand("Longitude"), null, 6);
        targetProduct.setGeoCoding(pixelGeoCoding);

        return targetProduct;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ScaleImageOp.class);
        }
    }


}

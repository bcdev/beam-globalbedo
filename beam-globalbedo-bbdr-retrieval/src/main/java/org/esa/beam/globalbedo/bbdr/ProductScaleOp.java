package org.esa.beam.globalbedo.bbdr;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.image.RenderedImage;

/**
 * Scales an input product by a factor
 * // todo: move this class to a utility package!
 */
@OperatorMetadata(alias = "ga.scale",
        description = "Scales an input product by a factor.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2013 by Brockmann Consult")
public class ProductScaleOp extends Operator {

    @SourceProduct(alias = "source", description = "The source product to scale.")
    private Product sourceProduct;

    @Parameter(defaultValue = "1.0f", description = "The scale factor of the target product")
    private float scaleFactor;

    @Override
    public void initialize() throws OperatorException {

        Product scaledProduct = createScaledProduct();

        for (Band b : sourceProduct.getBands()) {
            RenderedImage targetImage;
            if (scaleFactor != 1.0f) {
                targetImage = scale(b.getSourceImage(), scaleFactor);
            } else {
                targetImage = b.getSourceImage();
            }

            if (!scaledProduct.containsBand(b.getName())) {
                scaledProduct.addBand(b.getName(), b.getDataType());
                scaledProduct.getBand(b.getName()).setSourceImage(targetImage);
                scaledProduct.getBand(b.getName()).setNoDataValue(b.getNoDataValue());
                scaledProduct.getBand(b.getName()).setNoDataValueUsed(true);
            }
        }

        setTargetProduct(scaledProduct);
    }

    public static RenderedOp scale(MultiLevelImage sourceImage, float scaleFactor) {
        return ScaleDescriptor.create(sourceImage,
                scaleFactor,
                scaleFactor,
                0.0f, 0.0f,
                Interpolation.getInstance(
                        Interpolation.INTERP_NEAREST),
                null);
    }

    private Product createScaledProduct() {
        final int width = (int) (sourceProduct.getSceneRasterWidth() * scaleFactor);
        final int height = (int) (sourceProduct.getSceneRasterHeight() * scaleFactor);

        Product scaledProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                width,
                height);

        ProductUtils.copyFlagCodings(sourceProduct, scaledProduct);
        ProductUtils.copyMasks(sourceProduct, scaledProduct);
        ProductUtils.copyMetadata(sourceProduct, scaledProduct);
        ProductUtils.copyOverlayMasks(sourceProduct, scaledProduct);
        ProductUtils.copyVectorData(sourceProduct, scaledProduct);

        return scaledProduct;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ProductScaleOp.class);
        }
    }
}


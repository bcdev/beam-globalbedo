package org.esa.beam.globalbedo.bbdr.seaice;

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

import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;

/**
 * Crops an input product
 * // todo: move this class to a utility package!
 */
@OperatorMetadata(alias = "ga.crop",
        description = "Crops an input product using JAI.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2013 by Brockmann Consult")
public class ProductCropOp extends Operator {

    @SourceProduct(alias = "source", description = "The source product to crop.")
    private Product sourceProduct;

    @Parameter(defaultValue = "0", description = "The x value to start cropped image")
    private float xStart;

    @Parameter(defaultValue = "0", description = "The y value to start cropped image")
    private float yStart;

    @Parameter(defaultValue = "0", description = "The width of cropped image")
    private float width;

    @Parameter(defaultValue = "0", description = "The height of cropped image")
    private float height;

    @Override
    public void initialize() throws OperatorException {

        Product croppedProduct = createCroppedProduct();

        for (Band b : sourceProduct.getBands()) {
            RenderedImage targetImage;
            if (xStart > 0.0f || yStart > 0.0f || width > 0.0f || height > 0.0f) {
                targetImage = crop(croppedProduct, b.getSourceImage());
            } else {
                targetImage = b.getSourceImage();
            }

            if (!croppedProduct.containsBand(b.getName())) {
                croppedProduct.addBand(b.getName(), b.getDataType());
                croppedProduct.getBand(b.getName()).setSourceImage(targetImage);
                croppedProduct.getBand(b.getName()).setNoDataValue(b.getNoDataValue());
                croppedProduct.getBand(b.getName()).setNoDataValueUsed(true);
            }
        }

        setTargetProduct(croppedProduct);
    }

    private RenderedOp crop(Product croppedProduct, MultiLevelImage sourceImage) {
        //Origin point where you want to start cropping
        //The size of the area that you want to crop
        Rectangle r = new Rectangle((int)xStart,(int) yStart,(int)width,(int)height);

        ParameterBlock pb = new ParameterBlock();
        pb.addSource(sourceImage);

        pb.add((float)r.getX());
        pb.add((float)r.getY());
        pb.add((float)r.getWidth());
        pb.add((float)r.getHeight());

        //Creates the cropped area
        RenderedOp image = JAI.create("crop", pb);
        return image;
//        return CropDescriptor.create(sourceImage,
//                xStart,
//                yStart,
//                (float)croppedProduct.getSceneRasterWidth(),
//                (float)croppedProduct.getSceneRasterHeight(),
//                null);
    }

    private Product createCroppedProduct() {
        int sourceWidth = sourceProduct.getSceneRasterWidth();
        final int symmetricWidth = (int) (sourceWidth - 2 * xStart);
        if (symmetricWidth <= 0) {
            throw new OperatorException("xStart too large - cropped product width would be negative!");
        }
        int sourceHeight = sourceProduct.getSceneRasterHeight();
        final int symmetricHeight = (int) (sourceHeight - 2 * yStart);
        if (symmetricHeight <= 0) {
            throw new OperatorException("yStart too large - cropped product height would be negative!");
        }
        final int croppedWidth = width > 0 ? (int) Math.min(width, sourceWidth-1) : symmetricWidth;
        final int croppedHeight = height > 0 ? (int) Math.min(height, sourceHeight-1) : symmetricHeight;

        Product croppedProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                croppedWidth,
                croppedHeight);

        ProductUtils.copyFlagCodings(sourceProduct, croppedProduct);
        ProductUtils.copyMasks(sourceProduct, croppedProduct);
        ProductUtils.copyMetadata(sourceProduct, croppedProduct);
        ProductUtils.copyOverlayMasks(sourceProduct, croppedProduct);
        ProductUtils.copyVectorData(sourceProduct, croppedProduct);

        return croppedProduct;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ProductCropOp.class);
        }
    }
}


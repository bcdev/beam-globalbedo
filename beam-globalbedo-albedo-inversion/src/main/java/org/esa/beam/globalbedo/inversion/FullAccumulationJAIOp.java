package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.AddDescriptor;
import javax.media.jai.operator.MultiplyConstDescriptor;
import java.awt.image.RenderedImage;

/**
 * Created by IntelliJ IDEA.
 * User: olafd
 * Date: 11.04.11
 * Time: 17:02
 * To change this template use File | Settings | File Templates.
 */
public class FullAccumulationJAIOp extends Operator {

    @SourceProducts(description = "source products")
    private Product[] sourceProducts;

    @SourceProduct(alias = "sp1")
    private Product sourceProduct1;
    @SourceProduct(alias = "sp2")
    private Product sourceProduct2;

    @Parameter(description = "Weight factor for full accumulation")
    private double weight;

    @Override
    public void initialize() throws OperatorException {

        Product targetProduct = new Product(getId(),
                getClass().getName(),
                sourceProduct1.getSceneRasterWidth(),
                sourceProduct1.getSceneRasterHeight());
        targetProduct.setStartTime(sourceProduct1.getStartTime());
        targetProduct.setEndTime(sourceProduct1.getEndTime());
        ProductUtils.copyTiePointGrids(sourceProduct1, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct1, targetProduct);

        double[] factor = new double[]{weight};
//        for (int i = 0; i < sourceProduct1.getNumBands(); i++) {
//            Band sourceBand1 = sourceProduct1.getBandAt(i);
//            String srcBandName = sourceBand1.getName();
//            Band sourceBand2 = sourceProduct2.getBand(srcBandName);
//
//            RenderedImage sourceImage1 = sourceBand1.getSourceImage();
//            RenderedImage sourceImage2 = sourceBand2.getSourceImage();
//            RenderedOp multipliedSourceImage2 = MultiplyConstDescriptor.create(sourceImage2, factor, null);
//            RenderedOp sumImage = AddDescriptor.create(sourceImage1, multipliedSourceImage2, null);
//
//            Band targetBand = targetProduct.addBand(srcBandName, sourceBand1.getDataType());
//            targetBand.setSourceImage(sumImage);
//        }

        Product sumProduct = sourceProducts[0];
        Product sourceProductInList = sourceProducts[1];
        for (int i = 0; i < sumProduct.getNumBands(); i++) {
            Band sumBand = sumProduct.getBandAt(i);
            String sumBandName = sumBand.getName();
            Band sourceBandToAccum = sourceProductInList.getBand(sumBandName);
            RenderedImage sumImage = sumBand.getSourceImage();
            RenderedImage sourceBandToAccumImage = sourceBandToAccum.getSourceImage();
            RenderedOp multipliedSourceImageToAccum = MultiplyConstDescriptor.create(sourceBandToAccumImage, factor, null);
            sumImage = AddDescriptor.create(sumImage, multipliedSourceImageToAccum, null);
            sumBand.setSourceImage(sumImage);
        }
        for (int i = 1; i < sourceProducts.length - 1; i++) {
            sourceProductInList = sourceProducts[i + 1];
            for (int j = 0; j < sumProduct.getNumBands(); j++) {
                Band sumBand = sumProduct.getBandAt(j);
                String sumBandName = sumBand.getName();
                Band sourceBandToAccum = sourceProductInList.getBand(sumBandName);
                RenderedImage sumImage = sumBand.getSourceImage();
                RenderedImage sourceBandToAccumImage = sourceBandToAccum.getSourceImage();
                RenderedOp multipliedSourceImageToAccum = MultiplyConstDescriptor.create(sourceBandToAccumImage, factor, null);
                sumImage = AddDescriptor.create(sumImage, multipliedSourceImageToAccum, null);
                sumBand.setSourceImage(sumImage);
            }
        }

        for (int i = 0; i < sumProduct.getNumBands(); i++) {
            Band sumBand = sumProduct.getBandAt(i);
            String sumBandName = sumBand.getName();
            Band targetBand = targetProduct.addBand(sumBandName, sumBand.getDataType());
            targetBand.setSourceImage(sumBand.getSourceImage());
        }

        setTargetProduct(targetProduct);

    }
}
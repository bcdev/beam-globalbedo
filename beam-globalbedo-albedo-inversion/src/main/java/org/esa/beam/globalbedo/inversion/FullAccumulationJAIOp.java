package org.esa.beam.globalbedo.inversion;

import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.util.ProductUtils;

import javax.media.jai.RenderedOp;
import javax.media.jai.operator.AddDescriptor;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.MultiplyConstDescriptor;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: olafd
 * Date: 11.04.11
 * Time: 17:02
 * To change this template use File | Settings | File Templates.
 */
public class FullAccumulationJAIOp extends Operator {

    private static final double HALFLIFE = 11.54;


    @Parameter(description = "Source filenames")
    private String[] sourceFilenames;

    @Parameter(description = "All DoYs for full accumulation")
    private int[] allDoys;

    @Override
    public void initialize() throws OperatorException {

        Product sourceProduct0 = null;
        try {
            sourceProduct0 = ProductIO.readProduct(new File(sourceFilenames[0]));
        } catch (IOException e) {
            // todo
            e.printStackTrace();
        }

        final String[] bandNames = sourceProduct0.getBandNames();

        double[] weight = new double[sourceFilenames.length];
        for (int i = 0; i < sourceFilenames.length; i++) {
            weight[i] = Math.exp(-1.0 * Math.abs(allDoys[i]) / HALFLIFE);
        }

        int[] bandDataTypes = new int[bandNames.length];
        for (int i = 0; i < bandDataTypes.length; i++) {
            bandDataTypes[i] = sourceProduct0.getBand(bandNames[i]).getDataType();
        }
        final int rasterWidth = sourceProduct0.getSceneRasterWidth();
        final int rasterHeight = sourceProduct0.getSceneRasterHeight();
        Product targetProduct = new Product(getId(),
                                            getClass().getName(),
                                            rasterWidth,
                                            rasterHeight);
        targetProduct.setStartTime(sourceProduct0.getStartTime());
        targetProduct.setEndTime(sourceProduct0.getEndTime());
        ProductUtils.copyTiePointGrids(sourceProduct0, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct0, targetProduct);
        sourceProduct0.dispose();

        BufferedImage[] accu = new BufferedImage[bandNames.length];
        for (int i = 0; i < accu.length; i++) {
            if (bandDataTypes[i] == ProductData.TYPE_FLOAT32) {
                accu[i] = ConstantDescriptor.create((float) rasterWidth, (float) rasterHeight, new Float[]{0f},
                                                    null).getAsBufferedImage();
            }
        }

        int fileIndex = 0;
        for (String sourceFileName : sourceFilenames) {
            Product product = null;
            try {
                product = ProductIO.readProduct(sourceFileName);
            } catch (IOException e) {
                // todo
                e.printStackTrace();
            }
            int bandIndex = 0;
            for (Band band : product.getBands()) {
                if (band.getName().equals("mask")) {
                    // todo: derive dayOfClosestSample
                    setDayOfClosestSampleImage(band, bandIndex);
                } else {
                    final MultiLevelImage geophysicalImage = band.getGeophysicalImage();
                    final RenderedImage image = geophysicalImage.getImage(0);

                    RenderedOp multipliedSourceImageToAccum = MultiplyConstDescriptor.create(image,
                                                                                             new double[]{weight[fileIndex]},
                                                                                             null);
                    final RenderedOp result = AddDescriptor.create(accu[bandIndex], multipliedSourceImageToAccum, null);
                    accu[bandIndex] = result.getAsBufferedImage();
                }
                bandIndex++;
            }
            product.dispose();
            fileIndex++;
        }

        for (int i = 0; i < bandNames.length; i++) {
            Band targetBand = targetProduct.addBand(bandNames[i], bandDataTypes[i]);
            targetBand.setSourceImage(accu[i]);
        }

        setTargetProduct(targetProduct);
    }

    private void setDayOfClosestSampleImage(Band docsBand, int bandIndex) {
        // todo: we need to implement dayOfClosestSample derival here because mask of each data file is needed :-( move from InversionOp!
        final MultiLevelImage geophysicalImage = docsBand.getGeophysicalImage();
        final RenderedImage image = geophysicalImage.getImage(0);
        for (int i = 0; i < image.getWidth(); i++) {
            for (int j = 0; j < image.getWidth(); j++) {
                final double aDouble_1 = image.getData().getSampleDouble(i, j, bandIndex); // does this work??
                final double aDouble_2 = docsBand.getPixelDouble(i, j);      // or this??
            }
        }
    }
}
package org.esa.beam.globalbedo.inversion;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import javax.media.jai.BorderExtender;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.AddDescriptor;
import javax.media.jai.operator.ConstantDescriptor;
import javax.media.jai.operator.MultiplyConstDescriptor;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Operator implementing the full accumulation part of python breadboard. Uses JAI image multiplication/addition.
 * <p/>
 *
 * todo: for performance reasons, we will probably use binary accumulator files instead od Dimaps in the final version.
 * Then this class is not needed any more!
 *
 * The breadboard file is 'AlbedoInversion_multisensor_FullAccum_MultiProcessing.py' provided by Gerardo Lopez Saldana.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.fullaccdimap",
        description = "Provides full accumulation of daily accumulators",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2011 by Brockmann Consult")
public class FullAccumulationFromDimapOp extends Operator {

    private static final double HALFLIFE = 11.54;

    private int[][] daysToTheClosestSample;

    private int rasterWidth;
    private int rasterHeight;

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
        for (int j = 0; j < sourceFilenames.length; j++) {
            weight[j] = Math.exp(-1.0 * Math.abs(allDoys[j]) / HALFLIFE);
        }

        int[] bandDataTypes = new int[bandNames.length];
        for (int i = 0; i < bandDataTypes.length; i++) {
            bandDataTypes[i] = sourceProduct0.getBand(bandNames[i]).getDataType();
        }
        rasterWidth = sourceProduct0.getSceneRasterWidth();
        rasterHeight = sourceProduct0.getSceneRasterHeight();
        Product targetProduct = new Product(getId(),
                getClass().getName(),
                rasterWidth,
                rasterHeight);
        targetProduct.setStartTime(sourceProduct0.getStartTime());
        targetProduct.setEndTime(sourceProduct0.getEndTime());
        targetProduct.setPreferredTileSize(100, 100);
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
        daysToTheClosestSample = new int[rasterWidth][rasterHeight];
        int[][] dayOfClosestSampleOld = new int[rasterWidth][rasterHeight];
        for (String sourceFileName : sourceFilenames) {
            System.out.println("sourceFileName = " + sourceFileName);
            Product product = null;
            try {
                BeamLogManager.getSystemLogger().log(Level.ALL, "Accumulating file " + sourceFileName + " ...");
                product = ProductIO.readProduct(sourceFileName);
                int bandIndex = 0;
                for (Band band : product.getBands()) {
                    // The determination of the 'daysToTheClosestSample' successively requires the masks from ALL single accumulators.
                    // To avoid opening several accumulators at the same time, store 'daysToTheClosestSample' as
                    // array[rasterWidth][rasterHeight] and update every time a single product is added to accumulation result
                    if (band.getName().equals("mask")) {
                        RasterDataNode maskRaster = product.getRasterDataNode(band.getName());
                        dayOfClosestSampleOld = daysToTheClosestSample;
                        daysToTheClosestSample = updateDoYOfClosestSampleArray(maskRaster, dayOfClosestSampleOld,
                                fileIndex);
                    }
                    final MultiLevelImage geophysicalImage = band.getGeophysicalImage();
                    final RenderedImage image = geophysicalImage.getImage(0);

                    // add weighted product to accumulation result...
                    final RenderedOp multipliedSourceImageToAccum = MultiplyConstDescriptor.create(image,
                            new double[]{weight[fileIndex]},
                            null);
                    final RenderedOp result = AddDescriptor.create(accu[bandIndex], multipliedSourceImageToAccum,
                            null);
                    accu[bandIndex] = result.getAsBufferedImage();

                    bandIndex++;
                }
            } catch (IOException e) {
                // todo : logging
                System.out.println("Could not accumulate product '" + product.getName() + "' - skipping.");
            }

            product.dispose();
            fileIndex++;
        }

        for (int i = 0; i < bandNames.length; i++) {
            Band targetBand = targetProduct.addBand(bandNames[i], bandDataTypes[i]);
            targetBand.setSourceImage(accu[i]);
        }

        Band daysToTheClosestSampleBand = targetProduct.addBand(AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME,
                ProductData.TYPE_INT16);
        daysToTheClosestSampleBand.setNoDataValue(-1);
        daysToTheClosestSampleBand.setNoDataValueUsed(true);

        setTargetProduct(targetProduct);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        // we need the computeTile only for the 'daysToTheClosestSample' band. All other bands are done imagewise
        // using JAI in initialize().
        if (targetBand.getName().equals(AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME)) {
            for (int y = targetTile.getMinY(); y <= targetTile.getMaxY(); y++) {
                for (int x = targetTile.getMinX(); x <= targetTile.getMaxX(); x++) {
                    targetTile.setSample(x, y, daysToTheClosestSample[x][y]);
                }
            }
        }
    }

    private int[][] updateDoYOfClosestSampleArray(RasterDataNode maskRaster, int[][] doyOfClosestSampleOld,
                                                  int productIndex) {
        // this is done at the end of 'Accumulator' routine in breadboard...
        int[][] doyOfClosestSample = new int[rasterWidth][rasterHeight];
        Rectangle rectangle = new Rectangle(0, 0, rasterWidth, rasterHeight);
        final Tile maskTile = getSourceTile(maskRaster, rectangle, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
                final double mask = maskTile.getSampleDouble(i, j);
                int doy = 0;
                final int bbdrDaysToDoY = Math.abs(allDoys[productIndex]) + 1;
                if (productIndex == 0) {
                    if (mask > 0.0) {
                        doy = bbdrDaysToDoY;
                    } else {
                        doy = doyOfClosestSampleOld[i][j];
                    }
                } else {
                    if (mask > 0 && doyOfClosestSampleOld[i][j] == 0) {
                        doy = bbdrDaysToDoY;
                    } else {
                        doy = doyOfClosestSampleOld[i][j];
                    }
                    if (mask > 0 && doy > 0) {
                        doy = Math.min(bbdrDaysToDoY, doy);
                    } else {
                        doy = doyOfClosestSampleOld[i][j];
                    }
                }
                doyOfClosestSample[i][j] = doy;
            }
        }
        return doyOfClosestSample;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(FullAccumulationFromDimapOp.class);
        }
    }
}
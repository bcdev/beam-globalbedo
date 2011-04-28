package org.esa.beam.globalbedo.inversion;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.ProductUtils;

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
import java.util.Vector;


/**
 * todo: implement. we need this class to avoid multiple I/O (reading of acc files) for each prior.
 * --> change implementation of FullAccumulationJAI: open acc file, do everything for ALL priors, dispose acc file !!
 * <p/>
 * Operator implementing the full accumulation part of python breadboard. Uses JAI image multiplication/addition.
 * <p/>
 * The breadboard file is 'AlbedoInversion_multisensor_FullAccum_MultiProcessing.py' provided by Gerardo López Saldaña.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.testfullaccjai",
        description = "Provides full accumulation of daily accumulators",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2011 by Brockmann Consult")
public class TestFullAccumulationJAIAllDoysOp extends Operator {

    private static final double HALFLIFE = 11.54;

    private int[][] daysToTheClosestSample;

    private int rasterWidth;
    private int rasterHeight;

    @Parameter(description = "Globalbedo root directory")
    private String gaRootDir;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(description = "Source filenames")
    private String[] sourceFilenames;

    // each element of this vector is an int array (related to a prior) holding the doy indices of
    // the accumulator files to be processed with proper weights for this prior day.
    // length of this vector is usually 45 (as we have 45 priors per year)
    @Parameter(description = "All DoYs for full accumulation")
    private Vector allDoys;

//    @Parameter(description = "Prior DoYs to process (45 each year)")
//    private int[] doysToProcess;

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

        Vector<double[]> allWeights = new Vector<double[]>();
        for (int i = 0; i < allDoys.size(); i++) {
            int[] allDoysPrior = (int[]) allDoys.elementAt(i);
            double[] weight = new double[allDoysPrior.length];
            for (int j = 0; j < weight.length; j++) {
                weight[j] = Math.exp(-1.0 * Math.abs(allDoysPrior[j]) / HALFLIFE);
            }
            allWeights.add(weight);
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
        ProductUtils.copyTiePointGrids(sourceProduct0, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct0, targetProduct);
        sourceProduct0.dispose();

        BufferedImage[][] accu = new BufferedImage[allDoys.size()][bandNames.length];
        for (int i = 0; i < allDoys.size(); i++) {
            for (int j = 0; j < bandNames.length; j++) {
                if (bandDataTypes[j] == ProductData.TYPE_FLOAT32) {
                    accu[i][j] = ConstantDescriptor.create((float) rasterWidth, (float) rasterHeight, new Float[]{0f},
                            null).getAsBufferedImage();
                }
            }
        }

        int fileIndex = 0;
        daysToTheClosestSample = new int[rasterWidth][rasterHeight];
        int[][] dayOfClosestSampleOld;

        for (String sourceFileName : sourceFilenames) {
            Product product = null;
            try {
                product = ProductIO.readProduct(sourceFileName);
                System.out.println("sourceFileName = " + sourceFileName);
                for (Band band : product.getBands()) {
                    // The determination of the 'daysToTheClosestSample' successively requires the masks from ALL single accumulators.
                    // To avoid opening several accumulators at the same time, store 'daysToTheClosestSample' as
                    // array[rasterWidth][rasterHeight] and update every time a single product is added to accumulation result
//                    if (band.getName().equals("mask")) {
//                        RasterDataNode maskRaster = product.getRasterDataNode(band.getName());
//                        dayOfClosestSampleOld = daysToTheClosestSample;
//                        daysToTheClosestSample = updateDoYOfClosestSampleArray(maskRaster, dayOfClosestSampleOld,
//                                fileIndex);
//                    }
                }

                for (int k = 0; k < allDoys.size(); k++) {
                    int bandIndex = 0;
                    double[] weight = allWeights.elementAt(k);
                    for (Band band : product.getBands()) {
                        final MultiLevelImage geophysicalImage = band.getGeophysicalImage();
                        final RenderedImage image = geophysicalImage.getImage(0);
//                        System.out.println("band = " + band.getName());

                        // add weighted product to accumulation result...
                        for (int i = 0; i < weight.length; i++) {
                            RenderedOp multipliedSourceImageToAccum = MultiplyConstDescriptor.create(image,
                                    new double[]{weight[i]},
                                    null);
                            final RenderedOp result = AddDescriptor.create(accu[k][bandIndex],
                                    multipliedSourceImageToAccum,
                                    null);
                            accu[k][bandIndex] = result.getAsBufferedImage();
                        }

                        bandIndex++;
                    }
                }
            } catch (IOException e) {
                // todo : logging
                System.out.println("Could not accumulate product '" + product.getName() + "' - skipping.");
            }

            product.dispose();
            fileIndex++;
        }

        // todo: do we need this: it is not supposed to be in final product, nor used in inversion code
        // --> it is needed in MergeBRDF.py :-(
        // todo: get as product from separate operator, copy the bands
//        Band daysToTheClosestSampleBand = targetProduct.addBand(
//                AlbedoInversionConstants.ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME,
//                ProductData.TYPE_INT16);
//        daysToTheClosestSampleBand.setNoDataValue(-1);
//        daysToTheClosestSampleBand.setNoDataValueUsed(true);


        for (int k = 0; k < allWeights.size(); k++) {
            // write full accumulation product to disk for each doy to be processed (45 per year)...
            for (int i = 0; i < bandNames.length; i++) {
                Band targetBand = targetProduct.addBand(bandNames[i], bandDataTypes[i]);
                targetBand.setSourceImage(accu[k][i]);
            }

            final String accumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles"
                    + File.separator + "full" + File.separator + year;    // todo: distinguish computeSnow
            final String targetFileName = "fullacc_test"; // todo
            final File targetFile = new File(accumulatorDir, targetFileName);
            final WriteOp writeOp = new WriteOp(targetProduct, targetFile, ProductIO.DEFAULT_FORMAT_NAME);
            writeOp.writeProduct(ProgressMonitor.NULL);
        }

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
        final Tile maskTile = getSourceTile(maskRaster, rectangle, BorderExtender.createInstance(
                BorderExtender.BORDER_COPY));
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
                final double mask = maskTile.getSampleDouble(i, j);
                int doy = 0;
                for (int k = 0; k < allDoys.size(); k++) {
                    int[] allDoysPrior = (int[]) allDoys.elementAt(k);
                    final int bbdrDaysToDoY = Math.abs(allDoysPrior[productIndex]) + 1;
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
                }
                doyOfClosestSample[i][j] = doy;
            }
        }
        return doyOfClosestSample;
    }
}
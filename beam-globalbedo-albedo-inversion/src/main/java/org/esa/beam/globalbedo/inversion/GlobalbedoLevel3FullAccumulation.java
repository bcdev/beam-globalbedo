package org.esa.beam.globalbedo.inversion;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.logging.BeamLogManager;

import javax.media.jai.BorderExtender;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * // todo: Class will probably not be needed (included in GlobalbedoLevel3Albedo) - remove later!
 *
 * Operator for the creation of full accumulators in Albedo Inversion
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.fullaccumulation")
public class GlobalbedoLevel3FullAccumulation extends Operator {

    private int[][][] daysToTheClosestSample;
    private double[][][][] sumMatrices;

    private int rasterWidth;
    private int rasterHeight;

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "540", description = "Wings")   // 540 # One year plus 3 months wings
    private int wings;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "30.0", description = "Prior scale factor")
    private double priorScaleFactor;

    // todo: do we need this configurable?
    private boolean usePrior = true;
    private Logger logger;
    private Vector<double[]> allWeights;

    @Override
    public void initialize() throws OperatorException {
//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        // STEP 1: get Prior input files...
        final String priorDir = gaRootDir + File.separator + "Priors" + File.separator + tile + File.separator +
                "background" + File.separator + "processed.p1.0.618034.p2.1.00000_java";

        Product[] allPriorProducts;
        Product[] priorProducts;
//        try {
//            allPriorProducts = IOUtils.getPriorProduct(priorDir, computeSnow);
            allPriorProducts = null;
            priorProducts = new Product[]{allPriorProducts[0]};
            System.out.println("priorProducts = " + priorProducts[0].getName());
//        } catch (IOException e) {
//            throw new OperatorException("Cannot load prior products: " + e.getMessage());
//        }

        // STEP 2: get Daily Accumulator input files...
        final String accumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles";

        AlbedoInput[] inputProducts = new AlbedoInput[priorProducts.length];
        Vector<int[]> allDoysVector = new Vector<int[]>();
        int priorIndex = 0;
        for (Product priorProduct : priorProducts) {
            final int doy = AlbedoInversionUtils.getDoyFromPriorName(priorProduct.getName(), false);
            try {
                inputProducts[priorIndex] = IOUtils.getAlbedoInputProduct(accumulatorDir, doy, year, tile,
                                                                          wings,
                                                                          computeSnow);
                final int[] allDoys = inputProducts[priorIndex].getProductDoys();
                allDoysVector.add(allDoys);
                priorIndex++;
            } catch (IOException e) {
                // todo: just skip product, but add appropriate logging here
                logger = BeamLogManager.getSystemLogger();
                logger.log(Level.ALL, "Could not process DoY " + doy + " - skipping.");
//                    System.out.println("Could not process DoY " + doy + " - skipping.");
            }
        }

        // STEP 3: we need to reproject the priors for further use...
        Product[] reprojectedPriorProducts;
//        try {
//            if (inputProducts != null) {
//                String somePriorProductFileName = inputProducts[0].getProductFilenames()[0]; // just its projection parms needed
//                reprojectedPriorProducts = IOUtils.getReprojectedPriorProduct(priorProducts, tile,
//                        somePriorProductFileName);
//            } else {
//                throw new OperatorException("No accumulator input products available - cannot proceed.");
//            }
//        } catch (IOException e) {
//            throw new OperatorException("Cannot reproject prior products: " + e.getMessage());
//        }

        // STEP 4: full accumulation...

        allWeights = new Vector<double[]>();
        for (int i = 0; i < allDoysVector.size(); i++) {       //  allDoysVector.size() is the number of prior files!
            // allDoysPrior are the BBDR doys to be processed for given prior. These doys are relative to given prior:
            // [..., -3, -2, -1, 0, 1, 2, 3,...]
            int[] allDoysPrior = allDoysVector.elementAt(i);
            double[] weight = new double[allDoysPrior.length];
            for (int j = 0; j < weight.length; j++) {
                // weight = 1.0 for prior day, decreases with distance from prior day...
                weight[j] = Math.exp(-1.0 * Math.abs(allDoysPrior[j]) / AlbedoInversionConstants.HALFLIFE);
            }
            allWeights.add(weight);
        }


        for (int k = 0; k < allWeights.size(); k++) {
            Product sourceProduct0 = null;
            try {
                sourceProduct0 = ProductIO.readProduct(new File(inputProducts[k].getProductFilenames()[0]));
            } catch (IOException e) {
                // todo
                e.printStackTrace();
            }

            System.out.println("inputProducts filenames = " + inputProducts[k].getProductFilenames());

            final String[] bandNames = sourceProduct0.getBandNames();


            int[] bandDataTypes = new int[bandNames.length];
            for (int i = 0; i < bandDataTypes.length; i++) {
                bandDataTypes[i] = sourceProduct0.getBand(bandNames[i]).getDataType();
            }
            rasterWidth = sourceProduct0.getSceneRasterWidth();
            rasterHeight = sourceProduct0.getSceneRasterHeight();

            sourceProduct0.dispose();

            int fileIndex = 0;
            daysToTheClosestSample = new int[allDoysVector.size()][rasterWidth][rasterHeight];
            sumMatrices = new double[allDoysVector.size()][bandNames.length][rasterWidth][rasterHeight];
            int[][][] dayOfClosestSampleOld;

            for (String dailyAccFileName : inputProducts[k].getProductFilenames()) {    // breadboard: for BBDR in AllFiles:
                Product product = null;
                try {
                    product = ProductIO.readProduct(dailyAccFileName);
                    System.out.println("dailyAccFileName = " + dailyAccFileName);
                    int bandIndex = 0;
                    for (Band band : product.getBands()) {
                        // The determination of the 'daysToTheClosestSample' successively requires the masks from ALL single accumulators.
                        // To avoid opening several accumulators at the same time, store 'daysToTheClosestSample' as
                        // array[rasterWidth][rasterHeight] and update every time a single product is added to accumulation result
                        RasterDataNode rasterDataNode = product.getRasterDataNode(band.getName());
                        Rectangle rectangle = new Rectangle(0, 0, rasterWidth, rasterHeight);
                        final Tile tileToProcess = getSourceTile(rasterDataNode, rectangle, BorderExtender.createInstance(
                                BorderExtender.BORDER_COPY));
                        for (int m = 0; m < allDoysVector.size(); m++) {
                            if (band.getName().equals("mask")) {
                                dayOfClosestSampleOld = daysToTheClosestSample;
                                int[] allDoysPrior = allDoysVector.elementAt(m);
                                daysToTheClosestSample[m] = updateDoYOfClosestSampleArray(tileToProcess, allDoysPrior,
                                        dayOfClosestSampleOld[m],
                                        m, fileIndex);
                            }

                            accumulateMatrixElement(tileToProcess, m, fileIndex, bandIndex);
                        }
                        bandIndex++;
                    }
                } catch (IOException e) {
                    // todo : logging
                    System.out.println("Could not accumulate product '" + product.getName() + "' - skipping.");
                }

                product.dispose();
                fileIndex++;
            }

            // write full accumulation product to disk for each doy to be processed (45 per year)...
            FullAccumulationWriteOp accumulationOp = new FullAccumulationWriteOp();
            accumulationOp.setSourceProduct(new Product("bla", "blubb", 1200, 1200)); // dummy

            Vector<double[][][]> sumMatricesVector = new Vector<double[][][]>();
            sumMatricesVector.addElement(sumMatrices[k]);

            accumulationOp.setParameter("sumMatricesVector", sumMatricesVector);
            accumulationOp.setParameter("daysToTheClosestSample", daysToTheClosestSample[k]);
            final Product accumulationProduct = accumulationOp.getTargetProduct();
//            setTargetProduct(accumulationProduct);

            final String fullAccumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles"
                    + File.separator + "full" + File.separator + year;    // todo: distinguish computeSnow
            final String targetFileName = "fullacc_test_" + k; // todo: proper name
            final File targetFile = new File(fullAccumulatorDir, targetFileName);
            final WriteOp writeOp = new WriteOp(accumulationProduct, targetFile, ProductIO.DEFAULT_FORMAT_NAME);
            writeOp.writeProduct(ProgressMonitor.NULL);
        }

        System.out.println("done");
        // todo: how to avoid that either no target product is set, or 'result.dim' is written which is not needed ?
    }

    private int[][] updateDoYOfClosestSampleArray(Tile maskTile, int[] allDoysPrior,
                                                  int[][] doyOfClosestSampleOld,
                                                  int priorIndex, int productIndex) {
        // this is done at the end of 'Accumulator' routine in breadboard...
        int[][] doyOfClosestSample = new int[rasterWidth][rasterHeight];
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
                final double mask = maskTile.getSampleDouble(i, j);
                int doy = 0;
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
                doyOfClosestSample[i][j] = doy;
            }
        }
        return doyOfClosestSample;
    }

    private void accumulateMatrixElement(Tile matrixTile, int priorIndex, int fileIndex, int bandIndex) {
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
                final double matrixElement = matrixTile.getSampleDouble(i, j);
                sumMatrices[priorIndex][bandIndex][i][j] += allWeights.elementAt(priorIndex)[fileIndex]*matrixElement;
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3FullAccumulation.class);
        }
    }
}

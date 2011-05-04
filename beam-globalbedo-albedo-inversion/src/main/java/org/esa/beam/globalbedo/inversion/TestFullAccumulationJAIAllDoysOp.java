package org.esa.beam.globalbedo.inversion;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */

import com.bc.ceres.core.ProgressMonitor;
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
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.Vector;


/**
 * todo: test class - remove later!!
 * this class does accumulation 'manually on int[][] arrays (what JAI does on whole images)
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

    private int[][][] daysToTheClosestSample;
    private double[][][][] sumMatrices;

    private int rasterWidth;
    private int rasterHeight;

    @Parameter(description = "Globalbedo root directory")
    private String gaRootDir;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(description = "Source filenames")
    private String[] dailyAccumulatorFilenames;

    // each element of this vector is an int array (related to a prior) holding the doy indices of
    // the accumulator files to be processed with proper weights for this prior day.
    // length of this vector is usually 45 (as we have 45 priors per year)
    @Parameter(description = "All DoYs for full accumulation")
    private Vector allPriors;

//    @Parameter(description = "Prior DoYs to process (45 each year)")
//    private int[] doysToProcess;

    @Override
    public void initialize() throws OperatorException {

        Product sourceProduct0 = null;
        try {
            sourceProduct0 = ProductIO.readProduct(new File(dailyAccumulatorFilenames[0]));
        } catch (IOException e) {
            // todo
            e.printStackTrace();
        }

        final String[] bandNames = sourceProduct0.getBandNames();

        Vector<double[]> allWeights = new Vector<double[]>();
        for (int i = 0; i < allPriors.size(); i++) {
            int[] allDoysPrior = (int[]) allPriors.elementAt(i);
            double[] weight = new double[allDoysPrior.length];
            for (int j = 0; j < weight.length; j++) {
                weight[j] = Math.exp(-1.0 * Math.abs(allDoysPrior[j]) / AlbedoInversionConstants.HALFLIFE);
            }
            allWeights.add(weight);
        }

        int[] bandDataTypes = new int[bandNames.length];
        for (int i = 0; i < bandDataTypes.length; i++) {
            bandDataTypes[i] = sourceProduct0.getBand(bandNames[i]).getDataType();
        }
        rasterWidth = sourceProduct0.getSceneRasterWidth();
        rasterHeight = sourceProduct0.getSceneRasterHeight();

        sourceProduct0.dispose();

        int fileIndex = 0;
        daysToTheClosestSample = new int[allPriors.size()][rasterWidth][rasterHeight];
        sumMatrices = new double[allPriors.size()][bandNames.length][rasterWidth][rasterHeight];
        int[][][] dayOfClosestSampleOld;

        for (String dailyAccFileName : dailyAccumulatorFilenames) {    // breadboard: for BBDR in AllFiles:
            Product product = null;
            try {
                product = ProductIO.readProduct(dailyAccFileName);
                System.out.println("dailyAccFileName = " + dailyAccFileName);
                int bandIndex = 0;
                for (Band band : product.getBands()) {
                    // The determination of the 'daysToTheClosestSample' successively requires the masks from ALL single accumulators.
                    // To avoid opening several accumulators at the same time, store 'daysToTheClosestSample' as
                    // array[rasterWidth][rasterHeight] and update every time a single product is added to accumulation result
                    for (int k = 0; k < allPriors.size(); k++) {
                        if (band.getName().equals("mask")) {
                            RasterDataNode maskRaster = product.getRasterDataNode(band.getName());
                            dayOfClosestSampleOld = daysToTheClosestSample;
                            daysToTheClosestSample[k] = updateDoYOfClosestSampleArray(maskRaster,
                                                                                      dayOfClosestSampleOld[k],
                                                                                      k, fileIndex);
                        }

                        RasterDataNode matrixElementRaster = product.getRasterDataNode(band.getName());
                        accumulateMatrixElement(matrixElementRaster, k, bandIndex);
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

        for (int k = 0; k < allWeights.size(); k++) {
            // write full accumulation product to disk for each doy to be processed (45 per year)...
            FullAccumulationWriteOp accumulationOp = new FullAccumulationWriteOp();
            accumulationOp.setParameter("sumMatrices", sumMatrices);
            accumulationOp.setParameter("daysToTheClosestSample", daysToTheClosestSample);
            final Product accumulationProduct = accumulationOp.getTargetProduct();
            setTargetProduct(accumulationProduct);

            final String accumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles"
                                          + File.separator + "full" + File.separator + year;    // todo: distinguish computeSnow
            final String targetFileName = "fullacc_test_" + k; // todo: proper name
            final File targetFile = new File(accumulatorDir, targetFileName);
            final WriteOp writeOp = new WriteOp(accumulationProduct, targetFile, ProductIO.DEFAULT_FORMAT_NAME);
            writeOp.writeProduct(ProgressMonitor.NULL);
        }
    }

    private int[][] updateDoYOfClosestSampleArray(RasterDataNode maskRaster, int[][] doyOfClosestSampleOld,
                                                  int priorIndex, int productIndex) {
        // this is done at the end of 'Accumulator' routine in breadboard...
        int[][] doyOfClosestSample = new int[rasterWidth][rasterHeight];
        Rectangle rectangle = new Rectangle(0, 0, rasterWidth, rasterHeight);
        final Tile maskTile = getSourceTile(maskRaster, rectangle, BorderExtender.createInstance(
                BorderExtender.BORDER_COPY));
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
                final double mask = maskTile.getSampleDouble(i, j);
                int doy = 0;
                int[] allDoysPrior = (int[]) allPriors.elementAt(priorIndex);
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

    private void accumulateMatrixElement(RasterDataNode maskRaster, int priorIndex, int bandIndex) {
        Rectangle rectangle = new Rectangle(0, 0, rasterWidth, rasterHeight);
        final Tile matrixTile = getSourceTile(maskRaster, rectangle, BorderExtender.createInstance(
                BorderExtender.BORDER_COPY));
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
                final double matrixElement = matrixTile.getSampleDouble(i, j);
                sumMatrices[priorIndex][bandIndex][i][j] += matrixElement;
            }
        }
    }
}
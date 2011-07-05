package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 'Master' operator for the full accumulation for a period of DoYs
 * --> basically makes 'full' (8-day) binary accumulator files from all the dailies, now
 * with reading the dailies only once!! However, this requires a lot of memory.
 * It seems that not more than 1/2 year (23 DoYs) can be properly processed.
 * Also, final setup will depend on final concept tp be provided by GL.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.combinedacc")
public class GlobalbedoLevel3CombinedAccumulation extends Operator {

    private static final int RASTER_WIDTH = AlbedoInversionConstants.MODIS_TILE_WIDTH;
    private static final int RASTER_HEIGHT = AlbedoInversionConstants.MODIS_TILE_HEIGHT;

    @Parameter(defaultValue = "", description = "GA root directory")
    private String gaRootDir;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "121", description = "Start Day of Year", interval = "[1,366]")
    private int startDoy;

    @Parameter(defaultValue = "129", description = "End Day of Year", interval = "[1,366]")
    private int endDoy;

    @Parameter(defaultValue = "540", description = "Wings")
    private int wings;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Override
    public void initialize() throws OperatorException {
        //        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose
        final Logger logger = BeamLogManager.getSystemLogger();

        final int numDoys = (endDoy - startDoy) / 8 + 1;
        int[] doys = new int[numDoys];
        for (int i = 0; i < doys.length; i++) {
            doys[i] = startDoy + 8 * i;
        }

        // STEP 1: get Daily Accumulator input files...
        final String accumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles";

        // todo: for this setup, write the daily acc files as empty files so we can get their names as before
        String dailyAccumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles"
                + File.separator + year + File.separator + tile;
        if (computeSnow) {
            dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "Snow" + File.separator);
        } else {
            dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "NoSnow" + File.separator);
        }
        for (int i = startDoy + 8 - wings; i <= endDoy + 8 + wings; i++) {
            File[] dailyAccFiles = IOUtils.getDailyAccumulatorFiles(dailyAccumulatorDir, year, i);
            for (File file : dailyAccFiles) {
                if (!file.exists()) {
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        // todo
                        e.printStackTrace();
                    }
                }
            }
        }

        AlbedoInput[] inputProducts = new AlbedoInput[doys.length];
        for (int i = 0; i < doys.length; i++) {
            try {
                inputProducts[i] = IOUtils.getAlbedoInputProduct(accumulatorDir, true, doys[i], year, tile,
                        wings,
                        computeSnow);
            } catch (IOException e) {
                // todo: just skip product, but add appropriate logging here
                logger.log(Level.ALL, "Could not process DoY " + doys[i] + " - skipping.");
            }
        }


        doAccumulate(dailyAccumulatorDir, inputProducts, doys); // accumulates matrices and extracts mask array

        // no target product needed here, define a dummy product
        Product dummyProduct = new Product("dummy", "dummy", 1, 1);
        setTargetProduct(dummyProduct);

        System.out.println("done");
    }


    private Integer[] mergeInputProductsFilenameDoys(AlbedoInput[] inputProducts) {
        List<Integer> filenameList = new ArrayList<Integer>();
        for (AlbedoInput input : inputProducts) {
            final String[] thisInputFilenames = input.getProductBinaryFilenames();
            int index = 0;
            while (index < thisInputFilenames.length) {
                final String thisInputFileDoyString = thisInputFilenames[index].substring(13, 16);
                final int thisInputFileDoy = Integer.parseInt(thisInputFileDoyString);
                if (!filenameList.contains(thisInputFileDoy)) {
                    filenameList.add(thisInputFileDoy);
                }
                index++;
            }
        }

        return filenameList.toArray(new Integer[filenameList.size()]);
    }

    private void doAccumulate(String dailyAccumulatorDir, AlbedoInput[] inputProducts,
                              int[] doys) {
        // merge input products to single object...
        final Integer[] dailyAccDoys = mergeInputProductsFilenameDoys(inputProducts);
        final String[] bandNames = IOUtils.getDailyAccumulatorBandNames();
        int numBands = bandNames.length;

        final int numDoys = doys.length;
        final int numDailyAccs = dailyAccDoys.length;

        float[][][] daysToTheClosestSample = new float[numDoys][RASTER_WIDTH][RASTER_HEIGHT];

        int size = 4 * RASTER_WIDTH * RASTER_HEIGHT;

        MatrixElementFullAccumulator[] accumulators = new MatrixElementFullAccumulator[numDoys];
        final File[][] fullAccumulatorBinaryFilesPerMatrixElement = new File[numDoys][numBands + 1];

        for (int i = 0; i < numDoys; i++) {
            accumulators[i] = new MatrixElementFullAccumulator(inputProducts[i].getReferenceYear(),
                    inputProducts[i].getReferenceDoy(), new float[RASTER_WIDTH][RASTER_HEIGHT]);
            fullAccumulatorBinaryFilesPerMatrixElement[i] =
                    IOUtils.getFullAccumulatorFiles(dailyAccumulatorDir, inputProducts[i].getReferenceYear(),
                            inputProducts[i].getReferenceDoy());
        }

        // for processing of max. one month --> numDoys <= 4 !!
        float[][][][] sumMatrices = new float[numDoys][numBands][RASTER_WIDTH][RASTER_HEIGHT];
        float[][][] mask = new float[numDoys][RASTER_WIDTH][RASTER_HEIGHT];

        final boolean[][] accumulate = new boolean[numDailyAccs][numDoys];
        final int[][] dayDifference = new int[numDailyAccs][numDoys];

        File[][] matrixFiles = new File[numDailyAccs][numBands];
        for (int i = 0; i < numDailyAccs; i++) {
            // todo: this works only if we process within the same 'year'
            matrixFiles[i] = IOUtils.getDailyAccumulatorFiles(dailyAccumulatorDir, year, dailyAccDoys[i]);
        }

        // loop over merged daily acc doys...
        float[][][] dailyAccResultArray = new float[numDailyAccs][RASTER_WIDTH][RASTER_HEIGHT];
        for (int dailyAccIndex = 0; dailyAccIndex < numDailyAccs; dailyAccIndex++) {
            System.out.println("dailyAccIndex = " + dailyAccIndex);
            // get daily acc for this day...
            Product dailyAccumulationProduct = getDailyAccumulationProduct(computeSnow, dailyAccDoys[dailyAccIndex]);

            if (dailyAccumulationProduct != null) {
                // loop over all bands...
                for (int bandIndex = 0; bandIndex < numBands; bandIndex++) {
//                    System.out.println("bandIndex = " + bandIndex);
                    dailyAccResultArray[dailyAccIndex] = getDailyAccResultArray(dailyAccumulationProduct, bandIndex);
                    for (int doyIndex = 0; doyIndex < doys.length; doyIndex++) {
                        final String filename = matrixFiles[dailyAccIndex][bandIndex].getName();
                        accumulate[dailyAccIndex][doyIndex] = doAccumulation(filename, inputProducts[doyIndex].getProductBinaryFilenames());
                        dayDifference[dailyAccIndex][doyIndex] = getDayDifference(filename, inputProducts[doyIndex]);
                        final float weight = getWeight(filename, inputProducts[doyIndex]);
                        if (accumulate[dailyAccIndex][doyIndex]) {
                            for (int jj = 0; jj < RASTER_WIDTH; jj++) {
                                for (int kk = 0; kk < RASTER_WIDTH; kk++) {
                                    mask[doyIndex][jj][kk] = dailyAccResultArray[dailyAccIndex][jj][kk];
                                    sumMatrices[doyIndex][bandIndex][jj][kk] += weight * dailyAccResultArray[dailyAccIndex][jj][kk];
                                }
                            }
                        }
                    }
                }

                dailyAccumulationProduct.dispose();

                for (int doyIndex = 0; doyIndex < doys.length; doyIndex++) {
                    if (accumulate[dailyAccIndex][doyIndex]) {
                        float[][] dayOfClosestSampleOld = daysToTheClosestSample[doyIndex];
                        daysToTheClosestSample[doyIndex] = updateDoYOfClosestSampleArray(dayOfClosestSampleOld,
                                mask[doyIndex],   // for last band index, this is the mask
                                dayDifference[dailyAccIndex][doyIndex],
                                dailyAccIndex);
                    }
                }
            }
        }

        for (int bandIndex = 0; bandIndex < numBands; bandIndex++) {
            for (int doyIndex = 0; doyIndex < doys.length; doyIndex++) {
                accumulators[doyIndex].accumulateSumMatrixElement(sumMatrices[doyIndex][bandIndex]);
                IOUtils.writeFullAccumulatorMatrixElementToFile(fullAccumulatorBinaryFilesPerMatrixElement[doyIndex][bandIndex],
                        accumulators[doyIndex].getSumMatrix());
                if (bandIndex == AlbedoInversionConstants.NUM_ACCUMULATOR_BANDS - 1) {
                    IOUtils.writeFullAccumulatorMatrixElementToFile(fullAccumulatorBinaryFilesPerMatrixElement[doyIndex][bandIndex + 1],
                            daysToTheClosestSample[doyIndex]);
                }
            }
        }
    }

    private float[][] getDailyAccResultArray(Product dailyAccumulationProduct, int bandIndex) {
        float[][] resultArray = new float[RASTER_WIDTH][RASTER_HEIGHT];

        Band thisBand = dailyAccumulationProduct.getBandAt(bandIndex);
        final int tileWidth = RASTER_WIDTH / 10;
        final int tileHeight = RASTER_HEIGHT / 10;
        for (int ii = 0; ii < 10; ii++) {
            for (int jj = 0; jj < 10; jj++) {
                Rectangle rect = new Rectangle(ii * tileWidth, jj * tileHeight, tileWidth, tileHeight);
                Tile thisTile = getSourceTile(thisBand, rect);
                for (int x = rect.x; x < rect.x + rect.width; x++) {
                    for (int y = rect.y; y < rect.y + rect.height; y++) {
                        resultArray[x][y] = thisTile.getSampleFloat(x, y);

                    }
                }
            }
        }

        return resultArray;
    }

    private Product getDailyAccumulationProduct(boolean computeSnow, int dailyAccDoy) {
        String bbdrRootDir = gaRootDir + File.separator + "BBDR";
        Product[] bbdrInputProducts;
        try {
            bbdrInputProducts = IOUtils.getAccumulationInputProducts(bbdrRootDir, tile, year, dailyAccDoy);
        } catch (IOException e) {
            throw new OperatorException("Daily Accumulator: Cannot get list of input products: " + e.getMessage());
        }

        if (bbdrInputProducts == null || bbdrInputProducts.length == 0) {
            return null;
        }

        DailyAccumulationToDimapOp accumulationOp = new DailyAccumulationToDimapOp();
        accumulationOp.setSourceProducts(bbdrInputProducts);
        accumulationOp.setParameter("computeSnow", computeSnow);
        return accumulationOp.getTargetProduct();
    }

    private static int getDayDifference(String filename, AlbedoInput inputProduct) {
        final int year = inputProduct.getReferenceYear();
        final int fileYear = Integer.parseInt(filename.substring(9, 13));  // 'matrices_yyyydoy_*'
        final int doy = inputProduct.getReferenceDoy() + 8;
        final int fileDoy = Integer.parseInt(filename.substring(13, 16)); // 'matrices_yyyydoy_*'

        return IOUtils.getDayDifference(fileDoy, fileYear, doy, year);
    }

    private static float getWeight(String filename, AlbedoInput inputProduct) {
        final int difference = getDayDifference(filename, inputProduct);
        return (float) Math.exp(-1.0 * Math.abs(difference) / AlbedoInversionConstants.HALFLIFE);
    }

    private static boolean doAccumulation(String filename, String[] doyFilenames) {
        for (String doyFilename : doyFilenames) {
            if (filename.substring(0, 16).equals(doyFilename.substring(0, 16))) {
                return true;
            }
        }
        return false;
    }

    private static float[][] updateDoYOfClosestSampleArray(float[][] doyOfClosestSampleOld,
                                                           float[][] mask,
                                                           int dayDifference,
                                                           int productIndex) {
        // this is done at the end of 'Accumulator' routine in breadboard...
        float[][] doyOfClosestSample = new float[RASTER_WIDTH][RASTER_HEIGHT];
        for (int i = 0; i < RASTER_WIDTH; i++) {
            for (int j = 0; j < RASTER_HEIGHT; j++) {
                float doy;
                final float bbdrDaysToDoY = (float) (Math.abs(dayDifference) + 1);
                if (productIndex == 0) {
                    if (mask[i][j] > 0.0) {
                        doy = bbdrDaysToDoY;
                    } else {
                        doy = doyOfClosestSampleOld[i][j];
                    }
                } else {
                    if (mask[i][j] > 0 && doyOfClosestSampleOld[i][j] == 0.0f) {
                        doy = bbdrDaysToDoY;
                    } else {
                        doy = doyOfClosestSampleOld[i][j];
                    }
                    if (mask[i][j] > 0 && doy > 0) {
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
            super(GlobalbedoLevel3CombinedAccumulation.class);
        }
    }
}

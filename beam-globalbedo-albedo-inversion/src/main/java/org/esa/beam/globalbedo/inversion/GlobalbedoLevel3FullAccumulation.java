package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.util.math.DoubleList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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
@OperatorMetadata(alias = "ga.l3.fullacc")
public class GlobalbedoLevel3FullAccumulation extends Operator {

    private static final double HALFLIFE = 11.54;
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


        // write accs to files...
        String dailyAccumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles"
                + File.separator + year + File.separator + tile;
        if (computeSnow) {
            dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "Snow" + File.separator);
        } else {
            dailyAccumulatorDir = dailyAccumulatorDir.concat(File.separator + "NoSnow" + File.separator);
        }
        FullAccumulator[] accsToWrite = getDailyAccFromBinaryFileAndAccumulate(dailyAccumulatorDir, inputProducts, doys); // accumulates matrices and extracts mask array
        for (FullAccumulator acc : accsToWrite) {
            String fullAccumulatorBinaryFilename = "matrices_full_" + acc.getYear() + acc.getDoy() + ".bin";
            final File fullAccumulatorBinaryFile = new File(dailyAccumulatorDir + fullAccumulatorBinaryFilename);
            IOUtils.writeFullAccumulatorToFile(fullAccumulatorBinaryFile, acc.getSumMatrices(), acc.getDaysToTheClosestSample());
        }

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

    private FullAccumulator[] getDailyAccFromBinaryFileAndAccumulate(String dailyAccumulatorDir, AlbedoInput[] inputProducts,
                                                                     int[] doys) {
        // merge input products to single object...
        final Integer[] sourceBinaryFileDoys = mergeInputProductsFilenameDoys(inputProducts);
        final String[] bandNames = IOUtils.getDailyAccumulatorBandNames();
        int numBands = bandNames.length;

        final int numDoys = doys.length;
        final int numFiles = sourceBinaryFileDoys.length;

        float[][][] daysToTheClosestSample = new float[numDoys][RASTER_WIDTH][RASTER_HEIGHT];

        int size = numBands * RASTER_WIDTH * RASTER_HEIGHT;

        FullAccumulator[] accumulators = new FullAccumulator[numDoys];
        for (int i = 0; i < numDoys; i++) {
            accumulators[i] = new FullAccumulator(inputProducts[i].getReferenceYear(),
                    inputProducts[i].getReferenceDoy(),
                    new float[numBands][RASTER_WIDTH][RASTER_HEIGHT], new float[RASTER_WIDTH][RASTER_HEIGHT]);
        }

        File[][] matrixFiles = new File[sourceBinaryFileDoys.length][AlbedoInversionConstants.NUM_ACCUMULATOR_BANDS];
        for (int i = 0; i < sourceBinaryFileDoys.length; i++) {
            // todo: this works only if we process within the same 'year'
            matrixFiles[i] = IOUtils.getDailyAccumulatorFiles(dailyAccumulatorDir, year, sourceBinaryFileDoys[i]);
        }

        final float[][][] weight = new float[numBands][numFiles][numDoys];
        for (int bandIndex = 0; bandIndex < AlbedoInversionConstants.NUM_ACCUMULATOR_BANDS; bandIndex++) {

            System.out.println("Processing band " + (bandIndex+1) + " of " +
                    AlbedoInversionConstants.NUM_ACCUMULATOR_BANDS + " ...");

            float[][][] sumMatrices = new float[numDoys][RASTER_WIDTH][RASTER_HEIGHT];
            float[][][] mask = new float[numDoys][RASTER_WIDTH][RASTER_HEIGHT];

            final boolean[][] accumulate = new boolean[numFiles][numDoys];
            final int[][] dayDifference = new int[numFiles][numDoys];

            for (int fileIndex = 0; fileIndex < sourceBinaryFileDoys.length; fileIndex++) {
                final String filename = matrixFiles[fileIndex][bandIndex].getName();
                final String filePath = matrixFiles[fileIndex][bandIndex].getAbsolutePath();
//                System.out.println("sourceFileName = " + filePath);

                final File dailyAccumulatorBinaryFile = new File(filePath);
                FileInputStream f = null;
                try {
                    f = new FileInputStream(dailyAccumulatorBinaryFile);
                } catch (FileNotFoundException e) {
                    // todo
                    e.printStackTrace();
                }
                FileChannel ch = f.getChannel();
                ByteBuffer bb = ByteBuffer.allocateDirect(size);

                for (int i = 0; i < doys.length; i++) {
                    accumulate[fileIndex][i] = doAccumulation(filename, inputProducts[i].getProductBinaryFilenames());
                    dayDifference[fileIndex][i] = getDayDifference(dailyAccumulatorBinaryFile.getName(), inputProducts[i]);
                    weight[bandIndex][fileIndex][i] = getWeight(dailyAccumulatorBinaryFile.getName(), inputProducts[i]);
                }

                int nRead;
                try {
                    int jj = 0;
                    int kk = 0;

                    while ((nRead = ch.read(bb)) != -1) {
                        if (nRead == 0) {
                            continue;
                        }
                        bb.position(0);
                        bb.limit(nRead);
                        while (bb.hasRemaining()) {
                            final float value = bb.getFloat();
                            for (int doyIndex = 0; doyIndex < doys.length; doyIndex++) {
                                if (accumulate[fileIndex][doyIndex]) {
                                    mask[doyIndex][jj][kk] = value;
                                    sumMatrices[doyIndex][jj][kk] += weight[bandIndex][fileIndex][doyIndex] * value;
                                }
                            }
                            // find the right indices for sumMatrices array...
                            kk++;
                            if (kk == RASTER_WIDTH) {
                                jj++;
                                kk = 0;
                            }
                        }
                        bb.clear();
                    }
                    ch.close();
                    f.close();

                } catch (IOException e) {
                    // todo
                    e.printStackTrace();
                }

                if (bandIndex == AlbedoInversionConstants.NUM_ACCUMULATOR_BANDS - 1) {
                    for (int doyIndex = 0; doyIndex < doys.length; doyIndex++) {
                        if (accumulate[fileIndex][doyIndex]) {
                            float[][] dayOfClosestSampleOld = daysToTheClosestSample[doyIndex];
                            daysToTheClosestSample[doyIndex] = updateDoYOfClosestSampleArray(dayOfClosestSampleOld,
                                    mask[doyIndex],   // for last band index, this is the mask
                                    dayDifference[fileIndex][doyIndex],
                                    fileIndex);
                        }
                    }
                }
            } // fileIndex
            for (int doyIndex = 0; doyIndex < doys.length; doyIndex++) {
                accumulators[doyIndex].accumulateSumMatrixElement(bandIndex, sumMatrices[doyIndex]);
            }
        } // bandIndex
        for (int doyIndex = 0; doyIndex < doys.length; doyIndex++) {
            accumulators[doyIndex].setDaysToTheClosestSample(daysToTheClosestSample[doyIndex]);
        }

        return accumulators;
    }


    private static int getDayDifference(String filename, AlbedoInput inputProduct) {
        final int year = inputProduct.getReferenceYear();
        final int fileYear = Integer.parseInt(filename.substring(9, 13));  // 'matrices_yyyydoy.bin'
        final int doy = inputProduct.getReferenceDoy() + 8;
        final int fileDoy = Integer.parseInt(filename.substring(13, 16)); // 'matrices_yyyydoy.bin'

        return IOUtils.getDayDifference(fileDoy, fileYear, doy, year);
    }

    private static float getWeight(String filename, AlbedoInput inputProduct) {
        final int difference = getDayDifference(filename, inputProduct);
        return (float) Math.exp(-1.0 * Math.abs(difference) / HALFLIFE);
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
            super(GlobalbedoLevel3FullAccumulation.class);
        }
    }
}

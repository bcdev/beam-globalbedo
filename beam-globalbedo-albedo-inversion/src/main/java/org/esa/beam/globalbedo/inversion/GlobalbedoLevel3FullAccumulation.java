package org.esa.beam.globalbedo.inversion;

import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

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
 * Main class for the full accumulation for a period of DoYs
 * --> basically makes 'full' (8-day) binary accumulator files from all the dailies, now
 * with reading the dailies only once!! However, this requires a lot of memory.
 * It seems that not more than 1/2 year (23 DoYs) can be properly processed.
 * Also, final setup will depend on final concept tp be provided by GL.
 * Therefore, this class is not yet used.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class GlobalbedoLevel3FullAccumulation {

    private static final double HALFLIFE = 11.54;
    private static final int RASTER_WIDTH = AlbedoInversionConstants.MODIS_TILE_WIDTH;
    private static final int RASTER_HEIGHT = AlbedoInversionConstants.MODIS_TILE_HEIGHT;

    public static void main(String[] args) throws IOException {
        //        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose
        final Logger logger = BeamLogManager.getSystemLogger();

        final String tile = args[0];
        final int year = Integer.parseInt(args[1]);
        final int startDoy = Integer.parseInt(args[2]);
        final int endDoy = Integer.parseInt(args[3]);
        final int wings = Integer.parseInt(args[4]);
        final int snowIndex = Integer.parseInt(args[5]);
        final String gaRootDir = args[6];

        // todo: validate input

        final boolean computeSnow = (snowIndex == 1) ? true : false;

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

        // merge input products to single object...
        final String[] sourceBinaryFilenames = mergeInputProductsFilenameLists(inputProducts);

        final String[] bandNames = IOUtils.getDailyAccumulatorBandNames();

        FullAccumulator[] accsToWrite = getDailyAccFromBinaryFileAndAccumulate(sourceBinaryFilenames,
                inputProducts,
                doys,
                bandNames.length); // accumulates matrices and extracts mask array

        // write accs to files...
        String fullAccumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles"
                + File.separator + year + File.separator + tile;
        if (computeSnow) {
            fullAccumulatorDir = fullAccumulatorDir.concat(File.separator + "Snow" + File.separator);
        } else {
            fullAccumulatorDir = fullAccumulatorDir.concat(File.separator + "NoSnow" + File.separator);
        }
        for (FullAccumulator acc : accsToWrite) {
            String fullAccumulatorBinaryFilename = "matrices_full_" + acc.getYear() + acc.getDoy() + ".bin";
            final File fullAccumulatorBinaryFile = new File(fullAccumulatorDir + fullAccumulatorBinaryFilename);
            IOUtils.writeFullAccumulatorToFile(fullAccumulatorBinaryFile, acc.getSumMatrices(), acc.getDaysToTheClosestSample());
        }

        System.out.println("done");
    }


    private static String[] mergeInputProductsFilenameLists(AlbedoInput[] inputProducts) {
        List<String> filenameList = new ArrayList<String>();
        for (AlbedoInput input : inputProducts) {
            final String[] thisInputFilenames = input.getProductBinaryFilenames();
            int index = 0;
            while (index < thisInputFilenames.length) {
                if (!filenameList.contains(thisInputFilenames[index])) {
                    filenameList.add(thisInputFilenames[index]);
                }
                index++;
            }
        }

        return filenameList.toArray(new String[filenameList.size()]);
    }

    private static FullAccumulator[] getDailyAccFromBinaryFileAndAccumulate(String[] sourceBinaryFilenames,
                                                                            AlbedoInput[] inputProducts,
                                                                            int[] doys,
                                                                            int numBands) {
        final int numDoys = doys.length;
        final int numFiles = sourceBinaryFilenames.length;

        float[][][] daysToTheClosestSample = new float[numDoys][RASTER_WIDTH][RASTER_HEIGHT];
        float[][][][] sumMatrices = new float[numDoys][numBands][RASTER_WIDTH][RASTER_HEIGHT];
        float[][][] mask = new float[numDoys][RASTER_WIDTH][RASTER_HEIGHT];

        int size = numBands * RASTER_WIDTH * RASTER_HEIGHT;

        final boolean[][] accumulate = new boolean[numFiles][numDoys];
        final int[][] dayDifference = new int[numFiles][numDoys];
        final float[][] weight = new float[numFiles][numDoys];

        int fileIndex = 0;
        for (String filename : sourceBinaryFilenames) {
            System.out.println("sourceFileName = " + filename);

            final File dailyAccumulatorBinaryFile = new File(filename);
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
                weight[fileIndex][i] = getWeight(dailyAccumulatorBinaryFile.getName(), inputProducts[i]);
            }

            int nRead;
            try {
                int ii = 0;
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
                                // last band is the mask. extract array mask[jj][kk] for determination of doyOfClosestSample...
                                if (ii == numBands - 1) {
                                    mask[doyIndex][jj][kk] = value;
                                }
                                sumMatrices[doyIndex][ii][jj][kk] += weight[fileIndex][doyIndex] * value;
                            }
                        }
                        // find the right indices for sumMatrices array...
                        kk++;
                        if (kk == RASTER_WIDTH) {
                            jj++;
                            kk = 0;
                            if (jj == RASTER_HEIGHT) {
                                ii++;
                                jj = 0;
                            }
                        }
                    }
                    bb.clear();
                }
                ch.close();
                f.close();

                // now update doy of closest sample...
                for (int doyIndex = 0; doyIndex < doys.length; doyIndex++) {
                    if (accumulate[fileIndex][doyIndex]) {
                        float[][] dayOfClosestSampleOld = daysToTheClosestSample[doyIndex];
                        daysToTheClosestSample[doyIndex] = updateDoYOfClosestSampleArray(dayOfClosestSampleOld,
                                mask[doyIndex],
                                dayDifference[fileIndex][doyIndex],
                                fileIndex);
                    }
                }
                fileIndex++;
            } catch (IOException e) {
                // todo
                e.printStackTrace();
            }
        }

        FullAccumulator[] accumulators = new FullAccumulator[numDoys];
        for (int i = 0; i < numDoys; i++) {
            accumulators[i] = new FullAccumulator(inputProducts[i].getReferenceYear(),
                    inputProducts[i].getReferenceDoy(),
                    sumMatrices[i], daysToTheClosestSample[i]);
        }
        return accumulators;
    }

    private static int getDayDifference(String filename, AlbedoInput inputProduct) {
        final int year = inputProduct.getReferenceYear();
        final int fileYear = Integer.parseInt(filename.substring(9, 13));  // 'matrices_yyyydoy.bin'
        final int doy = inputProduct.getReferenceDoy();
        final int fileDoy = Integer.parseInt(filename.substring(13, 16)); // 'matrices_yyyydoy.bin'

        return IOUtils.getDayDifference(fileDoy, fileYear, doy, year);
    }

    private static float getWeight(String filename, AlbedoInput inputProduct) {
        final int difference = getDayDifference(filename, inputProduct);
        return (float) Math.exp(-1.0 * Math.abs(difference) / HALFLIFE);
    }


    private static boolean doAccumulation(String filename, String[] doyFilenames) {
        for (String doyFilename : doyFilenames) {
            if (filename.equals(doyFilename)) {
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

}

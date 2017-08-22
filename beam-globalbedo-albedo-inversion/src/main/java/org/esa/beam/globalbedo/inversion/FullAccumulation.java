package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
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
 * Full accumulation (new implementation in QA4ECV) to be directly called from inversion.
 * The full accumulator is no longer written to binary file.
 *
 * @author Olaf Danne
 */
public class FullAccumulation {

    private int rasterWidth;
    private int rasterHeight;

    private String dailyAccRootDir;
    private String tile;
    private int year;
    private int doy;
    private int wings;

    private boolean computeSnow;

    private Logger logger;

    FullAccumulator result;

    public FullAccumulation(int rasterWidth, int rasterHeight,
                            String dailyAccRootDir, String tile,
                            int year, int doy,
                            int wings, boolean computeSnow) {
        this.rasterWidth = rasterWidth;
        this.rasterHeight = rasterHeight;
        this.dailyAccRootDir = dailyAccRootDir;
        this.tile = tile;
        this.year = year;
        this.doy = doy;
        this.wings = wings;
        this.computeSnow = computeSnow;

        accumulate();
    }

    public FullAccumulator getResult() {
        return result;
    }

    private void accumulate() throws OperatorException {
        logger = BeamLogManager.getSystemLogger();

        // STEP 1: get Daily Accumulator input files...
//        final String dailyAccDir = dailyAccRootDir + File.separator + "DailyAcc";
        AccumulatorHolder dailyAccumulators = IOUtils.getDailyAccumulator(dailyAccRootDir, doy, year, tile,
                                                                          wings,
                                                                          computeSnow, false);

        if (dailyAccumulators != null) {
            final String[] dailyAccBinaryFilenames = getDailyAccumulatorFilenameList(dailyAccumulators);
            final String[] bandNames = IOUtils.getDailyAccumulatorBandNames();

            result = accumulateAndWeightDailyAccs(dailyAccBinaryFilenames,
                    dailyAccumulators,
                    bandNames.length); // accumulates matrices and extracts mask array
        }
    }

    private String[] getDailyAccumulatorFilenameList(AccumulatorHolder accumulatorHolder) {
        List<String> filenameList = new ArrayList<>();
        if (accumulatorHolder != null) {
            final String[] thisInputFilenames = accumulatorHolder.getProductBinaryFilenames();
            final int numDailyAccBinFiles = accumulatorHolder.getProductBinaryFilenames().length;
            if (numDailyAccBinFiles == 0) {
                logger.log(Level.WARNING, "No daily accumulators found for DoY " +
                        IOUtils.getDoyString(accumulatorHolder.getReferenceDoy()) + " ...");
            } else {
                logger.log(Level.INFO, "Found " + numDailyAccBinFiles + " daily accumulators for DoY " +
                        IOUtils.getDoyString(accumulatorHolder.getReferenceDoy()) + " ...");
            }
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

    private FullAccumulator accumulateAndWeightDailyAccs(String[] sourceBinaryFilenames,
                                                         AccumulatorHolder accumulatorHolder,
                                                         int numBands) {
        final int numFiles = sourceBinaryFilenames.length;

        float[][] daysToTheClosestSample = new float[rasterWidth][rasterHeight];
        float[][][] sumMatrices = new float[numBands][rasterWidth][rasterHeight];
        float[][] mask = new float[rasterWidth][rasterHeight];

        int size = numBands * rasterWidth * rasterHeight;

        final boolean[] accumulate = new boolean[numFiles];
        final int[] dayDifference = new int[numFiles];
        final float[] weight = new float[numFiles];

        int fileIndex = 0;
        for (String filename : sourceBinaryFilenames) {
            BeamLogManager.getSystemLogger().log(Level.INFO, "Full accumulation: reading daily acc file = " + filename + " ...");

            final File dailyAccumulatorBinaryFile = new File(filename);
            FileInputStream f;
            try {
                f = new FileInputStream(dailyAccumulatorBinaryFile);
                FileChannel ch = f.getChannel();
                ByteBuffer bb = ByteBuffer.allocateDirect(size);

                accumulate[fileIndex] = doAccumulation(filename, accumulatorHolder.getProductBinaryFilenames());
                dayDifference[fileIndex] = getDayDifference(dailyAccumulatorBinaryFile.getName(), accumulatorHolder);
                weight[fileIndex] = getWeight(dailyAccumulatorBinaryFile.getName(), accumulatorHolder);

                try {
                    long t1 = System.currentTimeMillis();
                    int nRead;
                    int ii = 0;
                    int jj = 0;
                    int kk = 0;
                    while ((nRead = ch.read(bb)) != -1) {
                        if (nRead == 0) {
                            continue;
                        }
                        bb.position(0);
                        bb.limit(nRead);
                        while (bb.hasRemaining() && ii < numBands) {
                            final float value = bb.getFloat();
                            if (accumulate[fileIndex]) {
                                // last band is the mask. extract array mask[jj][kk] for determination of doyOfClosestSample...
                                if (ii == numBands - 1) {
                                    mask[jj][kk] = value;
                                }
                                sumMatrices[ii][jj][kk] += weight[fileIndex] * value;
                            }
                            // find the right indices for sumMatrices array...
                            kk++;
                            if (kk == rasterWidth) {
                                jj++;
                                kk = 0;
                                if (jj == rasterHeight) {
                                    ii++;
                                    jj = 0;
                                }
                            }
                        }
                        bb.clear();
                    }
                    ch.close();
                    f.close();

                    long t2 = System.currentTimeMillis();
                    BeamLogManager.getSystemLogger().log(Level.ALL, "daily acc read in: " + (t2 - t1) + " ms");

                    // now update doy of closest sample...
                    if (accumulate[fileIndex]) {
                        float[][] dayOfClosestSampleOld = daysToTheClosestSample;
                        daysToTheClosestSample = updateDoYOfClosestSampleArray(dayOfClosestSampleOld,
                                                                               mask,
                                                                               dayDifference[fileIndex],
                                                                               fileIndex);
                    }
                    fileIndex++;
                } catch (IOException e) {
                    BeamLogManager.getSystemLogger().log(Level.SEVERE, "Could not read daily acc " + filename + "  - skipping.");
                }
            } catch (FileNotFoundException e) {
                BeamLogManager.getSystemLogger().log(Level.SEVERE, "Could not find daily acc " + filename + "  - skipping.");
            }

        }

        return new FullAccumulator(accumulatorHolder.getReferenceYear(),
                                   accumulatorHolder.getReferenceDoy(),
                                   sumMatrices, daysToTheClosestSample);

    }

    private static int getDayDifference(String filename, AccumulatorHolder inputProduct) {
        final int year = inputProduct.getReferenceYear();
        final int fileYear = Integer.parseInt(filename.substring(9, 13));  // 'matrices_yyyydoy.bin'
//        final int doy = inputProduct.getReferenceDoy() + 8;
        final int doy = inputProduct.getReferenceDoy(); // take SAME day now, as we support daily albedos!
        final int fileDoy = Integer.parseInt(filename.substring(13, 16)); // 'matrices_yyyydoy.bin'

        return IOUtils.getDayDifference(fileDoy, fileYear, doy, year);
    }

    private static float getWeight(String filename, AccumulatorHolder inputProduct) {
        final int difference = getDayDifference(filename, inputProduct);
        return AlbedoInversionUtils.getWeight(difference);
    }

    private static boolean doAccumulation(String filename, String[] doyFilenames) {
        for (String doyFilename : doyFilenames) {
            if (filename.equals(doyFilename)) {
                return true;
            }
        }
        return false;
    }

    private float[][] updateDoYOfClosestSampleArray(float[][] doyOfClosestSampleOld,
                                                    float[][] mask,
                                                    int dayDifference,
                                                    int productIndex) {
        // this is done at the end of 'Accumulator' routine in breadboard...
        float[][] doyOfClosestSample = new float[rasterWidth][rasterHeight];
        for (int i = 0; i < rasterWidth; i++) {
            for (int j = 0; j < rasterHeight; j++) {
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

package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.experimental.Output;
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
 * 'Master' operator for the full accumulation for a period of DoYs
 * --> basically makes 'full' (8-day) binary accumulator files from all the dailies, now
 * with reading the dailies only once!! However, this requires a lot of memory.
 * It seems that not more than 1/2 year (23 DoYs) can be properly processed.
 * Also, final setup will depend on final concept to be provided by GL.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.fullacc")
public class GlobalbedoLevel3FullAccumulation extends Operator implements Output {

    private static final double HALFLIFE = 11.54;

    private int rasterWidth;
    private int rasterHeight;

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

    @Parameter(defaultValue = "false", description = "Computation for seaice mode (polar tiles)")
    private boolean computeSeaice;


    private Logger logger;

    @Override
    public void initialize() throws OperatorException {
        logger = BeamLogManager.getSystemLogger();

        final int numDoys = (endDoy - startDoy) / 8 + 1;
        int[] doys = new int[numDoys];
        for (int i = 0; i < doys.length; i++) {
            doys[i] = startDoy + 8 * i;
        }

        if (computeSeaice) {
            rasterWidth = AlbedoInversionConstants.SEAICE_TILE_WIDTH;
            rasterHeight = AlbedoInversionConstants.SEAICE_TILE_HEIGHT;
        } else {
            rasterWidth = AlbedoInversionConstants.MODIS_TILE_WIDTH;
            rasterHeight = AlbedoInversionConstants.MODIS_TILE_HEIGHT;
        }

        // STEP 1: get Daily Accumulator input files...
        final String accumulatorDir = gaRootDir + File.separator + "BBDR" + File.separator + "AccumulatorFiles";

        AlbedoInput[] inputProducts = new AlbedoInput[doys.length];
        for (int i = 0; i < doys.length; i++) {
            inputProducts[i] = IOUtils.getAlbedoInputProduct(accumulatorDir, true, doys[i], year, tile,
                                                             wings,
                                                             computeSnow, computeSeaice);
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
        } else if (computeSeaice) {
            fullAccumulatorDir = fullAccumulatorDir.concat(File.separator);
        } else {
            fullAccumulatorDir = fullAccumulatorDir.concat(File.separator + "NoSnow" + File.separator);
        }
        for (FullAccumulator acc : accsToWrite) {
            if (acc != null) {
                String fullAccumulatorBinaryFilename = "matrices_full_" + acc.getYear() + IOUtils.getDoyString(acc.getDoy()) + ".bin";
                final File fullAccumulatorBinaryFile = new File(fullAccumulatorDir + fullAccumulatorBinaryFilename);
                IOUtils.writeFullAccumulatorToFile(fullAccumulatorBinaryFile, acc.getSumMatrices(), acc.getDaysToTheClosestSample());
            }
        }

        // no target product needed here, define a dummy product
        Product dummyProduct = new Product("dummy", "dummy", 1, 1);
        setTargetProduct(dummyProduct);

        logger.log(Level.ALL, "Finished full accumulation process for tile: " + tile + ", year: " + year + ", DoYs: " +
                IOUtils.getDoyString(startDoy) + "-" + IOUtils.getDoyString(endDoy) + " , Snow = " + computeSnow);
    }


    private String[] mergeInputProductsFilenameLists(AlbedoInput[] inputProducts) {
        List<String> filenameList = new ArrayList<String>();
        for (AlbedoInput input : inputProducts) {
            if (input != null) {
                final String[] thisInputFilenames = input.getProductBinaryFilenames();
                if (input.getProductBinaryFilenames().length == 0) {
                    logger.log(Level.ALL, "No daily accumulators found for DoY " +
                            IOUtils.getDoyString(input.getReferenceDoy()) + " ...");
                }
                int index = 0;
                while (index < thisInputFilenames.length) {
                    if (!filenameList.contains(thisInputFilenames[index])) {
                        filenameList.add(thisInputFilenames[index]);
                    }
                    index++;
                }
            }
        }

        return filenameList.toArray(new String[filenameList.size()]);
    }

    private FullAccumulator[] getDailyAccFromBinaryFileAndAccumulate(String[] sourceBinaryFilenames,
                                                                            AlbedoInput[] inputProducts,
                                                                            int[] doys,
                                                                            int numBands) {
        final int numDoys = doys.length;
        final int numFiles = sourceBinaryFilenames.length;

        float[][][] daysToTheClosestSample = new float[numDoys][rasterWidth][rasterHeight];
        float[][][][] sumMatrices = new float[numDoys][numBands][rasterWidth][rasterHeight];
        float[][][] mask = new float[numDoys][rasterWidth][rasterHeight];

        int size = numBands * rasterWidth * rasterHeight;

        final boolean[][] accumulate = new boolean[numFiles][numDoys];
        final int[][] dayDifference = new int[numFiles][numDoys];
        final float[][] weight = new float[numFiles][numDoys];

        int fileIndex = 0;
        for (String filename : sourceBinaryFilenames) {
            BeamLogManager.getSystemLogger().log(Level.INFO, "Full accumulation: reading daily acc file = " + filename + " ...");

            final File dailyAccumulatorBinaryFile = new File(filename);
            FileInputStream f;
            try {
                f = new FileInputStream(dailyAccumulatorBinaryFile);
                FileChannel ch = f.getChannel();
                ByteBuffer bb = ByteBuffer.allocateDirect(size);

                for (int i = 0; i < doys.length; i++) {
                    accumulate[fileIndex][i] = doAccumulation(filename, inputProducts[i].getProductBinaryFilenames());
                    dayDifference[fileIndex][i] = getDayDifference(dailyAccumulatorBinaryFile.getName(), inputProducts[i]);
                    weight[fileIndex][i] = getWeight(dailyAccumulatorBinaryFile.getName(), inputProducts[i]);
                }

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
                    BeamLogManager.getSystemLogger().log(Level.INFO, "daily acc read in: " + (t2 - t1) + " ms");

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
                    BeamLogManager.getSystemLogger().log(Level.SEVERE, "Could not read daily acc " + filename + "  - skipping.");
                }
            } catch (FileNotFoundException e) {
                BeamLogManager.getSystemLogger().log(Level.SEVERE, "Could not find daily acc " + filename + "  - skipping.");
            }

        }

        FullAccumulator[] accumulators = new FullAccumulator[numDoys];
        for (int i = 0; i < numDoys; i++) {
            accumulators[i] = null;
            if (inputProducts[i] != null) {
                accumulators[i] = new FullAccumulator(inputProducts[i].getReferenceYear(),
                                                      inputProducts[i].getReferenceDoy(),
                                                      sumMatrices[i], daysToTheClosestSample[i]);
            }
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

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3FullAccumulation.class);
        }
    }
}

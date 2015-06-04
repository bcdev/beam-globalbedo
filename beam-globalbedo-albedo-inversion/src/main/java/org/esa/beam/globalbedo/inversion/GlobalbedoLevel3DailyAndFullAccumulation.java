package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * New operator for daily AND full accumulation part.
 * This version avoids daily accumulator file I/O, but still writes full accumulator binary files
 * due to memory constraints.
 * todo: to be tested...
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.dailyandfullacc")
public class GlobalbedoLevel3DailyAndFullAccumulation extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory")
    private String gaRootDir;

    @Parameter(defaultValue = "", description = "Sensors to use the BBDRs from")
    private String[] sensors;

    @Parameter(defaultValue = "", description = "MODIS tile to process")
    private String tile;

    @Parameter(defaultValue = "", description = "Year")
    private int year;

    @Parameter(defaultValue = "", description = "Day of Year", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "90", description = "Wings value")
    private int wings;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "false", description = "Computation for seaice mode (polar tiles)")
    private boolean computeSeaice;      // todo: check if we need this any more as it is (from GA CCN)


    private static final int[][] TRG_M =
            new int[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS]
                    [3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private static final int[] TRG_V =
            new int[3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS];

    private static final int TRG_E = 90;
    private static final int TRG_MASK = 91;
    private static final int TRG_DCS = 92;

//    Accumulator[][] fullAccumulator;         //  Accumulator[x][y]

    private Rectangle tileRectangle;

    private float[][][] resultArray;

    @Override
    public void initialize() throws OperatorException {

        Logger logger = BeamLogManager.getSystemLogger();

        logger.log(Level.ALL, "START!!!");

        final int rasterWidth = AlbedoInversionConstants.MODIS_TILE_WIDTH;
        final int rasterHeight = AlbedoInversionConstants.MODIS_TILE_HEIGHT;

        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            for (int j = 0; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                TRG_M[i][j] = 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * i + j;
            }
        }
        for (int i = 0; i < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; i++) {
            TRG_V[i] = 3 * 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS + i;
        }

        tileRectangle = new Rectangle(rasterWidth, rasterHeight);

        // set paths...
        final String bbdrString = computeSeaice ? "BBDR_PST" : "BBDR";
        final String bbdrRootDir = gaRootDir + File.separator + bbdrString;

        resultArray = new float[AlbedoInversionConstants.NUM_ACCUMULATOR_BANDS + 1]
                [AlbedoInversionConstants.MODIS_TILE_WIDTH]
                [AlbedoInversionConstants.MODIS_TILE_HEIGHT];

        logger.log(Level.ALL, "initializing accumulators for doy #" + doy + " ...");

        for (int j = 0; j < AlbedoInversionConstants.NUM_ACCUMULATOR_BANDS; j++) {
            for (int x = 0; x < AlbedoInversionConstants.MODIS_TILE_WIDTH; x++) {
                for (int y = 0; y < AlbedoInversionConstants.MODIS_TILE_HEIGHT; y++) {
                    resultArray[j][x][y] = 0.0f;
                }
            }
            for (int x = 0; x < AlbedoInversionConstants.MODIS_TILE_WIDTH; x++) {
                for (int y = 0; y < AlbedoInversionConstants.MODIS_TILE_HEIGHT; y++) {
                    resultArray[TRG_DCS][x][y] = Float.NaN;
                }
            }
        }

        // - loop in over all days in year +- wings.
        //     # read BBDRs for each day
        //     # inner loop over all 46 doys of year:
        //        ** inner loop over all pixel: (x,y)
        //            + compute a Daily accumulator D(i,j,BBDR(x,y)) for each day
        //            + accumulate full accumulator with proper weight:
        //                          F(x,y,i,j) += weight(|day - doy|)*D(i,j)
        //     # close BBDRs for the single day
        // - write full acc to binary file

        // - wings=90 (Oct-Dec, Jan-Mar)
        // -90, ..., -1, 0, 1, 2, ... , 365, 366(+1), 367(+2), ..., 455(+90)
        for (int i = -wings; i < 365 + wings; i++) {
            logger.log(Level.ALL, "BBDR DAY INDEX:  " + i);
            Product[] bbdrProducts = null;

            final int[] referenceDate = AlbedoInversionUtils.getReferenceDate(year, i);
            logger.log(Level.ALL, "bbdrProduct date:  " + referenceDate[0] + "," + referenceDate[1]);
            try {
                bbdrProducts = IOUtils.getAccumulationInputProducts(bbdrRootDir, sensors, tile, referenceDate);
                for (Product bbdrProduct : bbdrProducts) {
                    logger.log(Level.ALL, "bbdrProduct:  " + bbdrProduct.getName());
                }
                final Tile[][] bbdrTiles = extractTilesFromBBDRProducts(bbdrProducts);

                final int productYear = ((i < 0) ? (year - 1) : ((i > 365) ? (year + 1) : year));
                final int doyOffset = AlbedoInversionUtils.computeDayOffset(referenceDate, productYear, doy);
                final float weight = FullAccumulation.getWeight(doyOffset);

                // per pixel:
                if (bbdrProducts.length > 0) {
//                    accumulate(i, bbdrProducts, bbdrTiles, doyOffset, weight);
//                    accumulate_op(i, bbdrProducts, bbdrTiles, doyOffset, weight);
                    accumulate_2(i, bbdrProducts, bbdrTiles, doyOffset, weight);
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, "No BBDR products could be read for date " +
                        +referenceDate[0] + "," + referenceDate[1] + " : " + e.getMessage());
            } finally {
                // dispose the BBDR products of the single day:
                if (bbdrProducts != null) {
                    for (Product bbdrProduct : bbdrProducts) {
                        if (bbdrProduct != null) {
                            bbdrProduct.dispose();
                        }
                    }
                }
            }
        }

        // now write full accumulator to binary file...

        String fullAccDir = gaRootDir + File.separator + bbdrString + File.separator + "FullAcc"
                + File.separator + year + File.separator + tile;
        if (computeSnow) {
            fullAccDir = fullAccDir.concat(File.separator + "Snow" + File.separator);
        } else if (computeSeaice) {
            fullAccDir = fullAccDir.concat(File.separator);
        } else {
            fullAccDir = fullAccDir.concat(File.separator + "NoSnow" + File.separator);
        }
        String fullAccumulatorBinaryFilename = "matrices_full_" + year + IOUtils.getDoyString(doy) + ".bin";
        final File fullAccumulatorBinaryFile = new File(fullAccDir + fullAccumulatorBinaryFilename);

        IOUtils.writeFloatArrayToFile(fullAccumulatorBinaryFile, resultArray);

        // no target product needed here, define a 'success' dummy product
        // no target product needed here, define a 'success' dummy product
        final String productName = "SUCCESS_fullacc_" + year + "_" + doy;
        Product successProduct = new Product(productName, "SUCCESS", 1, 1);
        setTargetProduct(successProduct);

        logger.log(Level.ALL, "Finished full accumulation process for tile: " + tile + ", year: " + year + ", DoYs: " +
                IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
    }

    private void accumulate_op(int i, Product[] bbdrProducts, Tile[][] bbdrTiles, int doyOffset, float weight) {
        // first get result array from old DailyAccumulationOp for the single day
        //...
        final long t0 = System.nanoTime();
        DailyAccumulationOp dailyAccOp = new DailyAccumulationOp();
        dailyAccOp.setParameterDefaultValues();
        dailyAccOp.setSourceProducts(bbdrProducts);
        dailyAccOp.setParameter("computeSnow", computeSnow);
        dailyAccOp.setParameter("computeSeaice", computeSeaice);
        dailyAccOp.setParameter("writeBinaryFile", false);
        setTargetProduct(dailyAccOp.getTargetProduct());

        //... wait until resultArray is filled completely...
        long endWaitTime = System.currentTimeMillis() + 60 * 1000;
        boolean isConditionMet = false;
        float[][][] dailyAccOpResultArray = null;
        while (System.currentTimeMillis() < endWaitTime && !isConditionMet) {
            isConditionMet = dailyAccOp.getNumPixelsProcessed() == 1200 * 1200;
            if (isConditionMet) {
                dailyAccOpResultArray = dailyAccOp.getResultArray();
                break;
            } else {
                try {
                    System.out.println("dailyAccOp.getNumPixelsProcessed() = " + dailyAccOp.getNumPixelsProcessed());
                    Thread.sleep(100);
                    System.out.println("slept 100ms");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        final long t1 = System.nanoTime();
        System.out.println("t1-t0 (us) [dailyacc] = " + (t1 - t0) / 1000.);

        // then full accumulate:
        for (int x = 0; x < AlbedoInversionConstants.MODIS_TILE_WIDTH; x++) {
            for (int y = 0; y < AlbedoInversionConstants.MODIS_TILE_HEIGHT; y++) {
                // compute daily acc for single day:
                // now accumulate full accumulator: F(x,y,i,j) += weight(|doyOffset|)*D(i,j):
                float dayOfClosestSampleOld = resultArray[TRG_DCS][x][y];
                final int singleDay = i - doy;
                final long t2 = System.nanoTime();
                resultArray[TRG_DCS][x][y] = FullAccumulation.updateDayOfClosestSample(dayOfClosestSampleOld,
                                                                                       dailyAccOpResultArray[TRG_MASK][x][y],
                                                                                       doyOffset,
                                                                                       singleDay);
                final long t3 = System.nanoTime();
                if (x == 600 && y == 600) {
                    System.out.println("t3-t2 (us) = " + (t3 - t2) / 1000.);
                }

                for (int j = 0; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                    for (int k = 0; k < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; k++) {
                        resultArray[TRG_M[j][k]][x][y] += weight * dailyAccOpResultArray[TRG_M[j][k]][x][y];
                    }
                }
                final long t4 = System.nanoTime();
                if (x == 600 && y == 600) {
                    System.out.println("t4-t3 (us) = " + (t4 - t3) / 1000.);
                }
                for (int j = 0; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                    resultArray[TRG_V[j]][x][y] += weight * dailyAccOpResultArray[TRG_V[j]][x][y];
                }
                resultArray[TRG_E][x][y] += weight * resultArray[TRG_E][x][y];
                resultArray[TRG_MASK][x][y] += dailyAccOpResultArray[TRG_MASK][x][y];
                final long t5 = System.nanoTime();
                if (x == 600 && y == 600) {
                    System.out.println("t5-t4 (us) = " + (t5 - t4) / 1000.);
                }
            }
        }

//        FullAccumulationTestOp fullAccTestOp = new FullAccumulationTestOp();
//        fullAccTestOp.setParameterDefaultValues();
//        fullAccTestOp.setSourceProducts(bbdrProducts);
//        fullAccTestOp.setParameter("weight", weight);
//        fullAccTestOp.setParameter("doyOffset", doyOffset);
    }

    private void accumulate(int i, Product[] bbdrProducts, Tile[][] bbdrTiles, int doyOffset, float weight) {
        for (int x = 0; x < AlbedoInversionConstants.MODIS_TILE_WIDTH; x++) {
            for (int y = 0; y < AlbedoInversionConstants.MODIS_TILE_HEIGHT; y++) {
                // compute daily acc for single day:
                final long t1 = System.nanoTime();
                final Accumulator singleDailyAcc = DailyAccumulation.compute(x, y, bbdrProducts, bbdrTiles,
                                                                             computeSnow, computeSeaice);
                final long t2 = System.nanoTime();
                if (x == 600 && y == 600) {
                    System.out.println("t2-t1 (us) = " + (t2 - t1) / 1000.);
                }
                // now accumulate full accumulator: F(x,y,i,j) += weight(|doyOffset|)*D(i,j):
                float dayOfClosestSampleOld = resultArray[TRG_DCS][x][y];
                final int singleDay = i - doy;
                resultArray[TRG_DCS][x][y] = FullAccumulation.updateDayOfClosestSample(dayOfClosestSampleOld,
                                                                                       singleDailyAcc.getMask(),
                                                                                       doyOffset,
                                                                                       singleDay);
                final long t3 = System.nanoTime();
                if (x == 600 && y == 600) {
                    System.out.println("t3-t2 (us) = " + (t3 - t2) / 1000.);
                }

                for (int j = 0; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                    for (int k = 0; k < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; k++) {
                        resultArray[TRG_M[j][k]][x][y] += weight * singleDailyAcc.getM().get(j, k);
                    }
                }
                final long t4 = System.nanoTime();
                if (x == 600 && y == 600) {
                    System.out.println("t4-t3 (us) = " + (t4 - t3) / 1000.);
                }
                for (int j = 0; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                    resultArray[TRG_V[j]][x][y] += weight * singleDailyAcc.getV().get(j, 0);
                }
                resultArray[TRG_E][x][y] += weight * singleDailyAcc.getE().get(0, 0);
                resultArray[TRG_MASK][x][y] += singleDailyAcc.getMask();
                final long t5 = System.nanoTime();
                if (x == 600 && y == 600) {
                    System.out.println("t5-t4 (us) = " + (t5 - t4) / 1000.);
                }
            }
        }
    }

    private void accumulate_2(int i, Product[] bbdrProducts, Tile[][] bbdrTiles, int doyOffset, float weight) {
        for (int x = 0; x < AlbedoInversionConstants.MODIS_TILE_WIDTH; x++) {
            for (int y = 0; y < AlbedoInversionConstants.MODIS_TILE_HEIGHT; y++) {
                // compute daily acc for single day:
                final long t1 = System.nanoTime();
                final float[] dailyAccOpResultArray =
                        DailyAccumulation.computeResultArray(x, y, bbdrProducts, bbdrTiles, computeSnow, computeSeaice);
                final long t2 = System.nanoTime();
                if (x == 600 && y == 600) {
                    System.out.println("t2-t1 (us) = " + (t2 - t1) / 1000.);
                }
                // now accumulate full accumulator: F(x,y,i,j) += weight(|doyOffset|)*D(i,j):
                float dayOfClosestSampleOld = resultArray[TRG_DCS][x][y];
                final int singleDay = i - doy;
                resultArray[TRG_DCS][x][y] = FullAccumulation.updateDayOfClosestSample(dayOfClosestSampleOld,
                                                                                       dailyAccOpResultArray[TRG_MASK],
                                                                                       doyOffset,
                                                                                       singleDay);
                final long t3 = System.nanoTime();
                if (x == 600 && y == 600) {
                    System.out.println("t3-t2 (us) = " + (t3 - t2) / 1000.);
                }

                for (int j = 0; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                    for (int k = 0; k < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; k++) {
                        resultArray[TRG_M[j][k]][x][y] += weight * dailyAccOpResultArray[TRG_M[j][k]];
                    }
                }
                final long t4 = System.nanoTime();
                if (x == 600 && y == 600) {
                    System.out.println("t4-t3 (us) = " + (t4 - t3) / 1000.);
                }
                for (int j = 0; j < 3 * AlbedoInversionConstants.NUM_BBDR_WAVE_BANDS; j++) {
                    resultArray[TRG_V[j]][x][y] += weight * dailyAccOpResultArray[TRG_V[j]];
                }
                resultArray[TRG_E][x][y] += weight * resultArray[TRG_E][x][y];
                resultArray[TRG_MASK][x][y] += dailyAccOpResultArray[TRG_MASK];
                final long t5 = System.nanoTime();
                if (x == 600 && y == 600) {
                    System.out.println("t5-t4 (us) = " + (t5 - t4) / 1000.);
                }
            }
        }
    }


    private Tile[][] extractTilesFromBBDRProducts(Product[] bbdrProducts) {
        Tile[][] bbdrTiles = new Tile[bbdrProducts.length]
                [AlbedoInversionConstants.BBDR_BAND_NAMES.length];
        tileRectangle = new Rectangle(AlbedoInversionConstants.MODIS_TILE_WIDTH,
                                      AlbedoInversionConstants.MODIS_TILE_HEIGHT);
        for (int i = 0; i < bbdrProducts.length; i++) {
            for (int j = 0; j < AlbedoInversionConstants.BBDR_BAND_NAMES.length; j++) {
                final Band b = bbdrProducts[i].getBandAt(j);
                if (b != null) {
                    bbdrTiles[i][j] = getSourceTile(b, tileRectangle);
                }
            }
        }
        return bbdrTiles;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3DailyAndFullAccumulation.class);
        }
    }
}

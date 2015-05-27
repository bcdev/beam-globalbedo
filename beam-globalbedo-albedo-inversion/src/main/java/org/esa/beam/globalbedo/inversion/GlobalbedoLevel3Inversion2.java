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
 * New operator for the whole inversion, including daily/full accumulation part.
 * This version completely avoids intermediate file I/O.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.inversion2")
public class GlobalbedoLevel3Inversion2 extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "", description = "Sensors to use the BBDRs from")
    private String[] sensors;

    @Parameter(defaultValue = "", description = "MODIS tile to process")
    private String tile;

    @Parameter(defaultValue = "", description = "Year")
    private int year;

    @Parameter(defaultValue = "", description = "Reference DoY", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "180", description = "Wings value")
    private int wings;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "false", description = "Computation for seaice mode (polar tiles)")
    private boolean computeSeaice;      // todo: check if we need this any more as it is (from GA CCN)

    @Parameter(defaultValue = "false", description = "Debug - write more target bands")
    private boolean debug;

    @SuppressWarnings("FieldCanBeLocal")
    @Parameter(defaultValue = "true", description = "Decide whether MODIS priors shall be used in inversion")
    private boolean usePrior = true;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory") // e.g., /disk2/Priors
    private String priorRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory suffix")
    // e.g., background/processed.p1.0.618034.p2.1.00000
    private String priorRootDirSuffix;

    @Parameter(defaultValue = "kernel", description = "MODIS Prior file name prefix")
    private String priorFileNamePrefix;

    @Parameter(defaultValue = "30.0", description = "Prior scale factor")
    private double priorScaleFactor;


    Accumulator[][] fullAccumulator;
    float[][] daysToTheClosestSample;
    private Logger logger;

    @Override
    public void initialize() throws OperatorException {

        logger = BeamLogManager.getSystemLogger();

        logger.log(Level.ALL, "START!!!");

        Product priorProduct = null;
        if (usePrior) {
            // STEP 1: get Prior input file...
            String priorDir = priorRootDir + File.separator + tile;
            if (priorRootDirSuffix != null) {
                priorDir = priorDir.concat(File.separator + priorRootDirSuffix);
            }

            logger.log(Level.ALL, "Searching for prior file in directory: '" + priorDir + "'...");

            try {
                priorProduct = IOUtils.getPriorProduct(priorDir, priorFileNamePrefix, doy, computeSnow);
            } catch (IOException e) {
                throw new OperatorException("No prior file available for DoY " + IOUtils.getDoyString(doy) +
                                                    " - cannot proceed...: " + e.getMessage());
            }
        }

        if (computeSeaice || !usePrior || (usePrior && priorProduct != null)) {
            // STEP 2: set paths...
            final String bbdrString = computeSeaice ? "BBDR_PST" : "BBDR";
            final String bbdrRootDir = gaRootDir + File.separator + bbdrString;

            // STEP 3: we need to reproject the priors for further use...
            Product reprojectedPriorProduct = null;
            if (usePrior) {
                try {
                    reprojectedPriorProduct = IOUtils.getReprojectedPriorProduct(priorProduct, tile,
                                                                                 null);
                } catch (IOException e) {
                    throw new OperatorException("Cannot reproject prior products - cannot proceed: " + e.getMessage());
                }
            }


            // set constants
            final int nIntervals = 2 * wings / AlbedoInversionConstants.EIGHT_DAYS;
            final String[] bandNames = IOUtils.getDailyAccumulatorBandNames();
            final int numBands = bandNames.length;

            fullAccumulator = new Accumulator[AlbedoInversionConstants.MODIS_TILE_WIDTH]
                    [AlbedoInversionConstants.MODIS_TILE_HEIGHT];
            daysToTheClosestSample = new float[AlbedoInversionConstants.MODIS_TILE_WIDTH]
                    [AlbedoInversionConstants.MODIS_TILE_WIDTH];
            for (int x = 0; x < AlbedoInversionConstants.MODIS_TILE_WIDTH; x++) {
                for (int y = 0; y < AlbedoInversionConstants.MODIS_TILE_HEIGHT; y++) {
                    fullAccumulator[x][y] = DailyAccumulation.createZeroAccumulator();
                    daysToTheClosestSample[x][y] = Float.NaN;
                }
            }

            // loop over all 8-day intervals: In each interval:
            // - read BBDRs for the 8 consecutive days
            // - inner loop over these 8 days:
            //     # inner loop over all pixel: (x,y)
            //        ** initialize a Daily accumulator D(i,j)
            //        ** compute D(i,j) = D(i,j,BBDR(x,y))
            //        ** accumulate full accumulator arrays: F(x,y,i,j) += weight(|day - doy|)*D(i,j)
            // - close BBDRs for the 8 consecutive days

            for (int i = 0; i < nIntervals; i++) {
                logger.log(Level.ALL, "INTERVAL:  " + i + " of " + nIntervals);
                final int doyOffset = AlbedoInversionConstants.EIGHT_DAYS * (i - nIntervals / 2);
                Product[] bbdrProducts;
                try {
                    // the BBDR products for a single day:
                    for (int singleDay = 0; singleDay < 8; singleDay++) {
                        logger.log(Level.ALL, "DAY:  " + singleDay + " of " + 8);
//                        bbdrProducts = IOUtils.getAccumulationInputProducts(bbdrRootDir, sensors, tile,
//                                                                            year, doy, doyOffset);
                        bbdrProducts = IOUtils.getAccumulationInputProducts(bbdrRootDir, sensors, tile,
                                                                            year, doy, doy + doyOffset + singleDay);
                        for (Product bbdrProduct : bbdrProducts) {
                            logger.log(Level.ALL, "bbdrProduct:  " + bbdrProduct.getName());
                        }
                        Tile[][] bbdrTiles = extractTilesFromBBDRProducts(bbdrProducts);
                        if (bbdrProducts != null && bbdrProducts.length > 0) {
                            // get an array of 1200x1200-tiles for all input bands of all products:
                            final double weight = FullAccumulation.getWeight(doyOffset);
                            for (int x = 0; x < AlbedoInversionConstants.MODIS_TILE_WIDTH; x++) {
                                for (int y = 0; y < AlbedoInversionConstants.MODIS_TILE_HEIGHT; y++) {
                                    // one daily acc for each pixel:
                                    Accumulator singleDailyAcc =
                                            DailyAccumulation.compute(x, y, bbdrProducts, bbdrTiles,
                                                                      computeSnow, computeSeaice);
                                    // now accumulate full accumulator: F(x,y,i,j) += weight(|doyOffset|)*D(i,j):
                                    fullAccumulator[x][y].setM(fullAccumulator[x][y].getM().plus(singleDailyAcc.getM().times(weight)));
                                    fullAccumulator[x][y].setV(fullAccumulator[x][y].getV().plus(singleDailyAcc.getV().times(weight)));
                                    fullAccumulator[x][y].setE(fullAccumulator[x][y].getE().plus(singleDailyAcc.getE().times(weight)));
                                    fullAccumulator[x][y].setMask(fullAccumulator[x][y].getMask() + singleDailyAcc.getMask());
                                    float dayOfClosestSampleOld = daysToTheClosestSample[x][y];
                                    daysToTheClosestSample[x][y] = FullAccumulation.updateDayOfClosestSample(dayOfClosestSampleOld,
                                                                                                             singleDailyAcc.getMask(), doyOffset, singleDay);
                                }
                            }
                        }
                        // dispose the BBDR products of the single day:
                        for (Product bbdrProduct : bbdrProducts) {
                            bbdrProduct.dispose();
                        }
                    }
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Problem while accumulating for tile: " + tile + ", year: " + year + ", DoY: " +
                            IOUtils.getDoyString(doy) + " , Snow = " + computeSnow + " - skipping.");
                }
            }

            // after loop over all 8-day intervals we have:
            // - Accumulator fullAccumulator[x][y] for each pixel with M, V, E
            // - daysToTheClosestSample
            // --> we are ready for pixel-wise inversion:
            // todo: if fullAccumulator is put as parameter into an Op, check if is written as metadata into netcdf file

            Inversion2Op inversion2Op = new Inversion2Op();
            inversion2Op.setParameterDefaultValues();
            Product dummySourceProduct;
            if (reprojectedPriorProduct != null) {
                inversion2Op.setSourceProduct("priorProduct", reprojectedPriorProduct);
            } else {
                // the inversion Op needs a source product of proper size
                if (computeSeaice) {
                    dummySourceProduct = AlbedoInversionUtils.createDummySourceProduct(AlbedoInversionConstants.SEAICE_TILE_WIDTH,
                                                                                       AlbedoInversionConstants.SEAICE_TILE_HEIGHT);
                } else {
                    dummySourceProduct = AlbedoInversionUtils.createDummySourceProduct(AlbedoInversionConstants.MODIS_TILE_WIDTH,
                                                                                       AlbedoInversionConstants.MODIS_TILE_HEIGHT);
                }
                inversion2Op.setSourceProduct("priorProduct", dummySourceProduct);
            }
            inversion2Op.setParameter("fullAccumulator", fullAccumulator);
            inversion2Op.setParameter("daysToTheClosestSample", daysToTheClosestSample);
            inversion2Op.setParameter("year", year);
            inversion2Op.setParameter("tile", tile);
            inversion2Op.setParameter("doy", doy);
            inversion2Op.setParameter("computeSnow", computeSnow);
            inversion2Op.setParameter("computeSeaice", computeSeaice);
            inversion2Op.setParameter("usePrior", usePrior);
            inversion2Op.setParameter("priorScaleFactor", priorScaleFactor);
            Product inversionProduct = inversion2Op.getTargetProduct();

            if (computeSeaice) {
                for (int i = doy; i < doy + 8; i++) {
                    try {
                        Product[] bbdrpstProducts = IOUtils.getAccumulationInputProducts(bbdrRootDir, tile, year, i);
                        if (bbdrpstProducts.length > 0) {
                            inversionProduct.setGeoCoding(bbdrpstProducts[0].getGeoCoding());
                        }
                    } catch (IOException e) {
                        throw new OperatorException("Cannot attach geocoding from BBDR PST product: ", e);
                    }
                }
            } else if (reprojectedPriorProduct == null) {
                // same in the standard mode without using priors...
                inversionProduct.setGeoCoding(IOUtils.getModisTileGeocoding(tile));
            }

            setTargetProduct(inversionProduct);

            // correct for lost pixels due to extreme SIN angles near South Pole
            if (includesSouthPole(tile)) {
                SouthPoleCorrectionOp correctionOp = new SouthPoleCorrectionOp();
                correctionOp.setParameterDefaultValues();
                correctionOp.setSourceProduct("sourceProduct", inversionProduct);
                Product southPoleCorrectedProduct = correctionOp.getTargetProduct();
                setTargetProduct(southPoleCorrectedProduct);
            }

            if (computeSeaice) {
                // copy landmask into target product
                IOUtils.copyLandmask(gaRootDir, tile, getTargetProduct());
            }
        } else {
            logger.log(Level.ALL, "No prior file found for tile: " + tile + ", year: " + year + ", DoY: " +
                    IOUtils.getDoyString(doy) + " , Snow = " + computeSnow + " - no inversion performed.");
        }

        logger.log(Level.ALL, "Finished inversion process for tile: " + tile + ", year: " + year + ", DoY: " +
                IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
    }

    private Tile[][] extractTilesFromBBDRProducts(Product[] bbdrProducts) {
        Tile[][] bbdrTiles = new Tile[bbdrProducts.length]
                [AlbedoInversionConstants.BBDR_BAND_NAMES.length];
        final Rectangle rect = new Rectangle(AlbedoInversionConstants.MODIS_TILE_WIDTH,
                                             AlbedoInversionConstants.MODIS_TILE_HEIGHT);
//        logger.log(Level.ALL, "bbdrProducts.length:  " + bbdrProducts.length);
        for (int i = 0; i < bbdrProducts.length; i++) {
//            logger.log(Level.ALL, "bbdrProducts[i]:  " + bbdrProducts[i].getName());
            for (int j = 0; j < AlbedoInversionConstants.BBDR_BAND_NAMES.length; j++) {
//                logger.log(Level.ALL, "bbdrProducts[i] band j:  " + AlbedoInversionConstants.BBDR_BAND_NAMES[j]);
//                final Band b = bbdrProducts[i].getBand(AlbedoInversionConstants.BBDR_BAND_NAMES[j]);
                final Band b = bbdrProducts[i].getBandAt(j);
                if (b != null) {
                    bbdrTiles[i][j] = getSourceTile(b, rect);
                }
            }
        }
        return bbdrTiles;
    }

    private boolean includesSouthPole(String tile) {
        return (tile.equals("h17v17") || tile.equals("h18v17"));
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3Inversion2.class);
        }
    }
}

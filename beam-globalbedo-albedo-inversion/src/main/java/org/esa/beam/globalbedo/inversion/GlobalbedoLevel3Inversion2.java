package org.esa.beam.globalbedo.inversion;

import com.bc.ceres.core.ProgressMonitor;
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
import org.esa.beam.gpf.operators.standard.WriteOp;
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

    @Parameter(defaultValue = "90", description = "Wings value")
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


    Accumulator[][][] fullAccumulator;         //  Accumulator[doy][x][y]
    float[][][] daysToTheClosestSampleArray;   //  daysToTheClosestSampleArray[doy][x][y]
    private Logger logger;

    @Override
    public void initialize() throws OperatorException {

        logger = BeamLogManager.getSystemLogger();

        logger.log(Level.ALL, "START!!!");

        // set paths...
        final String bbdrString = computeSeaice ? "BBDR_PST" : "BBDR";
        final String bbdrRootDir = gaRootDir + File.separator + bbdrString;

        fullAccumulator = new Accumulator[AlbedoInversionConstants.DOYS_IN_YEAR]
                [AlbedoInversionConstants.MODIS_TILE_WIDTH]
                [AlbedoInversionConstants.MODIS_TILE_HEIGHT];
        daysToTheClosestSampleArray = new float[AlbedoInversionConstants.DOYS_IN_YEAR]
                [AlbedoInversionConstants.MODIS_TILE_WIDTH]
                [AlbedoInversionConstants.MODIS_TILE_WIDTH];
        for (int doyIndex = 0; doyIndex < AlbedoInversionConstants.DOYS_IN_YEAR; doyIndex++) {
            for (int x = 0; x < AlbedoInversionConstants.MODIS_TILE_WIDTH; x++) {
                for (int y = 0; y < AlbedoInversionConstants.MODIS_TILE_HEIGHT; y++) {
                    fullAccumulator[doyIndex][x][y] = DailyAccumulation.createZeroAccumulator();
                    daysToTheClosestSampleArray[doyIndex][x][y] = Float.NaN;
                }
            }
        }

        double[] weight = new double[AlbedoInversionConstants.DOYS_IN_YEAR];
        int[] doys = new int[AlbedoInversionConstants.DOYS_IN_YEAR];
        int[] doyOffset = new int[AlbedoInversionConstants.DOYS_IN_YEAR];
        for (int i = 0; i < doys.length; i++) {
            doys[i] = 1 + i * AlbedoInversionConstants.EIGHT_DAYS;
        }

        Product[] priorProducts = new Product[AlbedoInversionConstants.DOYS_IN_YEAR];
        Product[] reprojectedPriorProduct = new Product[AlbedoInversionConstants.DOYS_IN_YEAR];
        if (usePrior) {
            // STEP 1: get Prior input file...
            String priorDir = priorRootDir + File.separator + tile;
            if (priorRootDirSuffix != null) {
                priorDir = priorDir.concat(File.separator + priorRootDirSuffix);
            }

            logger.log(Level.ALL, "Searching for prior file in directorys: '" + priorDir + "'...");

            for (int i = 0; i < AlbedoInversionConstants.DOYS_IN_YEAR; i++) {
                try {
                    priorProducts[i] = IOUtils.getPriorProduct(priorDir, priorFileNamePrefix, doys[i], computeSnow);
                    reprojectedPriorProduct[i] = IOUtils.getReprojectedPriorProduct(priorProducts[i], tile,
                                                                                 null);
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "No reprojected prior file available for DoY " + IOUtils.getDoyString(doys[i]) + ": " +
                            e.getMessage());
                }
            }
        }

        // - loop in over all days in year +- wings.
        //     # read BBDRs for each day
        //     # inner loop over all 46 doys of year:
        //        ** inner loop over all pixel: (x,y)
        //            + compute a Daily accumulator D(i,j,BBDR(x,y)) for each day
        //            + accumulate full accumulator for each day with proper weight:
        //                          F(doy,x,y,i,j) += weight(|day - doy|)*D(i,j)
        //     # close BBDRs for the single day

        // - wings=90 (Oct-Dec, Jan-Mar)
        // -90, ..., -1, 0, 1, 2, ... , 365, 366(+1), 367(+2), ..., 455(+90)
        for (int i = -wings; i < 365+wings; i++) {
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

                int productYear = ((i < 0) ? (year - 1) : ((i > 365) ? (year + 1) : year));
                // for given day (i) get weights for all 46 DoYs in year (001, 009,..., 361):
                for (int j = 0; j < AlbedoInversionConstants.DOYS_IN_YEAR; j++) {
                    doyOffset[j] = AlbedoInversionUtils.computeDayOffset(referenceDate, productYear, doys[j]);
                    weight[j] = FullAccumulation.getWeight(doyOffset[j]);
                }

                // per pixel:
                if (bbdrProducts.length > 0) {
                    for (int x = 0; x < AlbedoInversionConstants.MODIS_TILE_WIDTH; x++) {
                        for (int y = 0; y < AlbedoInversionConstants.MODIS_TILE_HEIGHT; y++) {
                            // compute daily acc for single day:
                            final Accumulator singleDailyAcc = DailyAccumulation.compute(x, y, bbdrProducts, bbdrTiles,
                                                                                   computeSnow, computeSeaice);
                            // accumulate full accs of all 46 DoYs in year (001, 009,..., 361):
                            for (int j = 0; j < AlbedoInversionConstants.DOYS_IN_YEAR; j++) {
                                // now accumulate full accumulator: F(x,y,i,j) += weight(|doyOffset|)*D(i,j):
                                fullAccumulator[j][x][y].setM(fullAccumulator[j][x][y].getM().plus(singleDailyAcc.getM().times(weight[j])));
                                fullAccumulator[j][x][y].setV(fullAccumulator[j][x][y].getV().plus(singleDailyAcc.getV().times(weight[j])));
                                fullAccumulator[j][x][y].setE(fullAccumulator[j][x][y].getE().plus(singleDailyAcc.getE().times(weight[j])));
                                fullAccumulator[j][x][y].setMask(fullAccumulator[j][x][y].getMask() + singleDailyAcc.getMask());
                                float dayOfClosestSampleOld = daysToTheClosestSampleArray[j][x][y];
                                final int singleDay = i - doys[j];
                                daysToTheClosestSampleArray[j][x][y] = FullAccumulation.updateDayOfClosestSample(dayOfClosestSampleOld,
                                                                                                                 singleDailyAcc.getMask(),
                                                                                                                 doyOffset[j],
                                                                                                                 singleDay);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                // dispose the BBDR products of the single day:
                for (Product bbdrProduct : bbdrProducts) {
                    if (bbdrProduct != null) {
                        bbdrProduct.dispose();
                    }
                }
            }
        }

        // after loop over input products and all doys we have:
        // - Accumulators fullAccumulator[doy][x][y] for each doy and pixel with M, V, E
        // - daysToTheClosestSampleArray [doy][x][y] for each doy and pixel
        // --> we are ready for pixel-wise inversion and writing an output product for each doy:

        for (int j = 0; j < AlbedoInversionConstants.DOYS_IN_YEAR; j++) {
            Inversion2Op inversion2Op = new Inversion2Op();
            inversion2Op.setParameterDefaultValues();
            inversion2Op.setFullAccumulator(fullAccumulator[j]);
            inversion2Op.setDaysToTheClosestSampleArray(daysToTheClosestSampleArray[j]);
            inversion2Op.setParameterDefaultValues();
            Product dummySourceProduct;
            if (reprojectedPriorProduct[j] != null) {
                inversion2Op.setSourceProduct("priorProduct", reprojectedPriorProduct[j]);
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
            inversion2Op.setParameter("year", year);
            inversion2Op.setParameter("tile", tile);
            inversion2Op.setParameter("doy", doys[j]);
            inversion2Op.setParameter("computeSnow", computeSnow);
            inversion2Op.setParameter("computeSeaice", computeSeaice);
            inversion2Op.setParameter("usePrior", usePrior);
            inversion2Op.setParameter("priorScaleFactor", priorScaleFactor);
            Product inversionProduct = inversion2Op.getTargetProduct();

            if (computeSeaice) {
                for (int i = doys[j]; i < doys[j] + 8; i++) {
                    try {
                        Product[] bbdrpstProducts = IOUtils.getAccumulationInputProducts(bbdrRootDir, tile, year, i);
                        if (bbdrpstProducts.length > 0) {
                            inversionProduct.setGeoCoding(bbdrpstProducts[0].getGeoCoding());
                        }
                    } catch (IOException e) {
                        throw new OperatorException("Cannot attach geocoding from BBDR PST product: ", e);
                    }
                }
            } else if (reprojectedPriorProduct[j] == null) {
                // same in the standard mode without using priors...
                inversionProduct.setGeoCoding(IOUtils.getModisTileGeocoding(tile));
            }

            if (computeSeaice) {
                // copy landmask into target product
                IOUtils.copyLandmask(gaRootDir, tile, inversionProduct);
            }

            // correct for lost pixels due to extreme SIN angles near South Pole
            if (includesSouthPole(tile)) {
                SouthPoleCorrectionOp correctionOp = new SouthPoleCorrectionOp();
                correctionOp.setParameterDefaultValues();
                correctionOp.setSourceProduct("sourceProduct", inversionProduct);
                Product southPoleCorrectedProduct = correctionOp.getTargetProduct();
                writeInversionProduct(southPoleCorrectedProduct, doys[j]);
            } else {
                writeInversionProduct(inversionProduct, doys[j]);
            }
        }

        // set a dummy target product:
        Product dummyProduct = new Product("dummy", "dummy", 1, 1);
        setTargetProduct(dummyProduct);

        logger.log(Level.ALL, "Finished inversion process for tile: " + tile + ", year: " + year +
                 " , Snow = " + computeSnow);
    }

    private void writeInversionProduct(Product product, int doy) {
        final String inversionRootDir = gaRootDir + File.separator + "Inversion";
        final String snowString = computeSnow ? "Snow" : "NoSnow";
        final String productDir = inversionRootDir + File.separator + snowString + File.separator +
                year + File.separator + tile;
        final String fileName = "GlobAlbedo.brdf."+
                year + IOUtils.getDoyString(doy) + "." + tile + "." + snowString + ".nc";

        final File file = new File(productDir, fileName);
        WriteOp writeOp = new WriteOp(product, file, "NetCDF4-BEAM");
        writeOp.writeProduct(ProgressMonitor.NULL);
    }

    private Tile[][] extractTilesFromBBDRProducts(Product[] bbdrProducts) {
        Tile[][] bbdrTiles = new Tile[bbdrProducts.length]
                [AlbedoInversionConstants.BBDR_BAND_NAMES.length];
        final Rectangle rect = new Rectangle(AlbedoInversionConstants.MODIS_TILE_WIDTH,
                                             AlbedoInversionConstants.MODIS_TILE_HEIGHT);
        for (int i = 0; i < bbdrProducts.length; i++) {
            for (int j = 0; j < AlbedoInversionConstants.BBDR_BAND_NAMES.length; j++) {
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

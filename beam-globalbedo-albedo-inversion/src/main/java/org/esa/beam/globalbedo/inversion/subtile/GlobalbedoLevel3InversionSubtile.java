package org.esa.beam.globalbedo.inversion.subtile;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.Accumulator;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.singlepixel.DailyAccumulationSinglePixel;
import org.esa.beam.globalbedo.inversion.singlepixel.GlobalbedoLevel3InversionSinglePixel;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.*;


/**
 * 'Master' operator for the whole BRDF computation (daily acc, full acc, inversion) on a sub-tile of given size
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l3.inversion.subtile",
        description = "'Master' operator for the whole BRDF computation (daily acc, full acc, inversion) on a sub-tile",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")
public class GlobalbedoLevel3InversionSubtile extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory")
    private String gaRootDir;

    @Parameter(defaultValue = "", description = "BBDR root directory")
    private String bbdrRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory") // e.g., /disk2/Priors
    private String priorRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory suffix")
    // e.g., background/processed.p1.0.618034.p2.1.00000
    private String priorRootDirSuffix;

    @Parameter(defaultValue = "", description = "Inversion target directory")
    private String inversionDir;

    @Parameter(description = "MODIS tile")
    private String tile;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "Day of Year", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(description = "Start X pixel", interval = "[0,1199]")
    private int startX;

    @Parameter(description = "Start Y pixel", interval = "[0,1199]")
    private int startY;

    @Parameter(description = "End X pixel", interval = "[0,1199]")
    private int endX;

    @Parameter(description = "End Y pixel", interval = "[0,1199]")
    private int endY;

    private int width;
    private int height;
    private Rectangle targetRectangle;

    private static final String[] PARAMETER_BAND_NAMES = IOUtils.getInversionParameterBandNames();
    private static final String[][] UNCERTAINTY_BAND_NAMES = IOUtils.getInversionUncertaintyBandNames();
    private Accumulator[][][] allDailyAccs;


    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();
        width = endX - startX + 1;
        height = endY - startY + 1;
        targetRectangle = new Rectangle(startX, startY, width, height);

        if (inversionDir == null) {
            final String snowDir = computeSnow ? "Snow" : "NoSnow";
            inversionDir = gaRootDir + File.separator + "Inversion_single" + File.separator + year +
                    File.separator + tile + File.separator + snowDir;
        }

        Product targetProduct = createTargetProduct();
        targetProduct.setPreferredTileSize(width, height);

        allDailyAccs = new Accumulator[width][height][545];
        int allDailyAccsIndex = 0;

        for (int iDay = -90; iDay < 455; iDay++) {  // todo: introduce constants
            int currentYear = year;
            int currentDay = iDay;          // currentDay is in [0,365] !!
            if (iDay < 0) {
                currentYear--;
                currentDay = iDay + 365;
            } else if (iDay > 365) {
                currentYear++;
                currentDay = iDay - 365;
            }

            Product[] dailyBBDRProducts;
            try {
                // get BBDR input product list...
                dailyBBDRProducts = IOUtils.getAccumulationInputProducts(bbdrRootDir, tile, currentYear, currentDay);
            } catch (IOException e) {
                throw new OperatorException("Daily Accumulator: Cannot get list of input products: " + e.getMessage());
            }

            logger.log(Level.INFO, "Starting daily acc 'subtile': tile: " +
                    tile + ", year: " + currentYear + ", day: " + IOUtils.getDoyString(currentDay));
            for (int i = startX; i <= endX; i++) {
                for (int j = startY; j <= endY; j++) {
                    if (dailyBBDRProducts.length > 0) {
//                        Product[] dailyBbdrSubsetProducts = subsetBBDRProducts(dailyBBDRProducts);
                        // do daily accumulation for all pixels
                        DailyAccumulationSinglePixel dailyAccumulationSinglePixel =
                                new DailyAccumulationSinglePixel(dailyBBDRProducts, computeSnow, i, j);
//                                new DailyAccumulationSinglePixel(dailyBbdrSubsetProducts, computeSnow, i-startX, j-startY);
                        allDailyAccs[i-startX][j-startY][allDailyAccsIndex] = dailyAccumulationSinglePixel.accumulate();
                    } else {
                        allDailyAccs[i-startX][j-startY][allDailyAccsIndex] = Accumulator.createZeroAccumulator();
                    }
                }
            }
            logger.log(Level.INFO, "DONE daily acc 'subtile': tile: " +
                    tile + ", year: " + currentYear + ", day: " + IOUtils.getDoyString(currentDay));
            allDailyAccsIndex++;
        }

        setTargetProduct(targetProduct);
    }

    private Product[] subsetBBDRProducts(Product[] bbdrTileProducts) {

        Product[] bbdrSubsetProducts = new Product[bbdrTileProducts.length];
        int index = 0;
        for (Product bbdrTileProduct:bbdrTileProducts) {
            SubsetOp subsetOp = new SubsetOp();
            subsetOp.setParameterDefaultValues();
            subsetOp.setSourceProduct(bbdrTileProduct);
            subsetOp.setRegion(new Rectangle(startX, startY, width, height));
            bbdrSubsetProducts[index++] = subsetOp.getTargetProduct();
        }
        return bbdrSubsetProducts;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        for (int i = startX; i <= endX; i++) {
            for (int j = startY; j <= endY; j++) {
                GlobalbedoLevel3InversionSinglePixel inversionSinglePixel = new GlobalbedoLevel3InversionSinglePixel();
                inversionSinglePixel.setParameterDefaultValues();
                inversionSinglePixel.setParameter("year", year);
                inversionSinglePixel.setParameter("tile", tile);
                inversionSinglePixel.setParameter("doy", doy);
                inversionSinglePixel.setParameter("pixelX", String.format("%04d", i));
                inversionSinglePixel.setParameter("pixelY", String.format("%04d", j));
                inversionSinglePixel.setParameter("computeSnow", computeSnow);
                inversionSinglePixel.setParameter("usePrior", true);
                inversionSinglePixel.setParameter("gaRootDir", gaRootDir);
                inversionSinglePixel.setParameter("bbdrRootDir", bbdrRootDir);
                inversionSinglePixel.setParameter("priorRootDir", priorRootDir);
                inversionSinglePixel.setParameter("priorRootDirSuffix", priorRootDirSuffix);
                inversionSinglePixel.setParameter("allDailyAccs", allDailyAccs[i-startX][j-startY]);
                inversionSinglePixel.setParameter("writeInversionProductToCsv", false);

                Product inversionSinglePixelProduct = inversionSinglePixel.getTargetProduct();

                for (Map.Entry<Band, Tile> entry : targetTiles.entrySet()) {
                    checkForCancellation();
                    Band targetBand = entry.getKey();
                    Tile targetTile = entry.getValue();

                    Band sourceBand = inversionSinglePixelProduct.getBand(targetBand.getName());
                    final Tile sourceTile = getSourceTile(sourceBand, new Rectangle(0, 0, 1, 1));
                    final float singlePixelValue = sourceTile.getSampleFloat(0, 0);
                    targetTile.setSample(i-startX, j-startY, singlePixelValue);
                }
            }
        }


    }

    private Product createTargetProduct() {
        Product targetProduct = new Product("BRDF_subtile", "BRDF_subtile", width, height);

        for (String parameterBandName : PARAMETER_BAND_NAMES) {
            targetProduct.addBand(parameterBandName, ProductData.TYPE_FLOAT32);
        }

        for (int i = 0; i < 3 * NUM_BBDR_WAVE_BANDS; i++) {
            // add bands only for UR triangular matrix
            for (int j = i; j < 3 * NUM_BBDR_WAVE_BANDS; j++) {
                targetProduct.addBand(UNCERTAINTY_BAND_NAMES[i][j], ProductData.TYPE_FLOAT32);
            }
        }

        targetProduct.addBand(INV_ENTROPY_BAND_NAME, ProductData.TYPE_FLOAT32);
        targetProduct.addBand(INV_REL_ENTROPY_BAND_NAME, ProductData.TYPE_FLOAT32);
        targetProduct.addBand(INV_WEIGHTED_NUMBER_OF_SAMPLES_BAND_NAME, ProductData.TYPE_FLOAT32);
        targetProduct.addBand(ACC_DAYS_TO_THE_CLOSEST_SAMPLE_BAND_NAME, ProductData.TYPE_FLOAT32);
        targetProduct.addBand(INV_GOODNESS_OF_FIT_BAND_NAME, ProductData.TYPE_FLOAT32);

        for (Band b : targetProduct.getBands()) {
            b.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
            b.setNoDataValueUsed(true);
        }

        return targetProduct;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3InversionSubtile.class);
        }
    }
}

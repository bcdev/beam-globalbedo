package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 'Master' operator for the BRDF retrieval part (full accumulation and inversion)
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.inversion")
public class GlobalbedoLevel3Inversion extends Operator {

    @SourceProduct(optional = true)
    private Product seaiceGeocodingProduct;

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory") // e.g., /disk2/Priors
    private String priorRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory suffix") // e.g., background/processed.p1.0.618034.p2.1.00000
    private String priorRootDirSuffix;

    @Parameter(defaultValue = "kernel", description = "MODIS Prior file name prefix") // e.g., filename = kernel.001.006.h18v04.Snow.1km.nc
    private String priorFileNamePrefix;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "tileInfo_0.dim", description = "Name of tile info filename providing the geocoding")
    private String tileInfoFilename;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "001", description = "DoY")
    private int doy;

    @Parameter(defaultValue = "540", description = "Wings")   // 540 # One year plus 3 months wings
    private int wings;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "false", description = "Computation for seaice mode (polar tiles)")
    private boolean computeSeaice;

    @Parameter(defaultValue = "30.0", description = "Prior scale factor")
    private double priorScaleFactor;

    @Parameter(defaultValue = "true", description = "Use binary accumulator files instead of dimap (to be decided)")
    private boolean useBinaryAccumulators;

    @Parameter(defaultValue = "false", description = "Do accumulation only (no inversion)")
    private boolean accumulationOnly;

    @SuppressWarnings("FieldCanBeLocal")
    @Parameter(defaultValue = "true", description = "Decide whether MODIS priors shall be used in inversion")
    private boolean usePrior = true;

    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        Product priorProduct = null;
        if (usePrior) {
            // STEP 1: get Prior input file...
            final String priorDir = priorRootDir + File.separator + tile;
            if (priorRootDirSuffix != null) {
                priorDir.concat(File.separator + priorRootDirSuffix);
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
            String fullAccumulatorDir = bbdrRootDir + File.separator + "AccumulatorFiles"
                    + File.separator + year + File.separator + tile;

            // STEP 3: we need to reproject the priors for further use...
            Product reprojectedPriorProduct = null;
            if (usePrior) {
                try {
                    Product tileInfoProduct = IOUtils.getTileInfoProduct(fullAccumulatorDir, tileInfoFilename);
                    reprojectedPriorProduct = IOUtils.getReprojectedPriorProduct(priorProduct, tile,
                            tileInfoProduct);
                } catch (IOException e) {
                    throw new OperatorException("Cannot reproject prior products - cannot proceed: " + e.getMessage());
                }
            }

            // STEP 5: do inversion...
            String fullAccumulatorBinaryFilename = "matrices_full_" + year + IOUtils.getDoyString(doy) + ".bin";
            if (computeSnow) {
                fullAccumulatorDir = fullAccumulatorDir.concat(File.separator + "Snow" + File.separator);
            } else if (computeSeaice) {
                fullAccumulatorDir = fullAccumulatorDir.concat(File.separator);
            } else {
                fullAccumulatorDir = fullAccumulatorDir.concat(File.separator + "NoSnow" + File.separator);
            }

            String fullAccumulatorFilePath = fullAccumulatorDir + fullAccumulatorBinaryFilename;

            InversionOp inversionOp = new InversionOp();
            inversionOp.setParameterDefaultValues();
            Product dummySourceProduct;
            if (reprojectedPriorProduct != null) {
                inversionOp.setSourceProduct("priorProduct", reprojectedPriorProduct);
            } else {
                if (computeSeaice) {
                    dummySourceProduct = AlbedoInversionUtils.createDummySourceProduct(AlbedoInversionConstants.SEAICE_TILE_WIDTH,
                            AlbedoInversionConstants.SEAICE_TILE_HEIGHT);
                } else {
                    dummySourceProduct = AlbedoInversionUtils.createDummySourceProduct(AlbedoInversionConstants.MODIS_TILE_WIDTH,
                            AlbedoInversionConstants.MODIS_TILE_HEIGHT);
                }
                inversionOp.setSourceProduct("priorProduct", dummySourceProduct);
            }
            inversionOp.setParameter("fullAccumulatorFilePath", fullAccumulatorFilePath);
            inversionOp.setParameter("year", year);
            inversionOp.setParameter("tile", tile);
            inversionOp.setParameter("doy", doy);
            inversionOp.setParameter("computeSnow", computeSnow);
            inversionOp.setParameter("computeSeaice", computeSeaice);
            inversionOp.setParameter("usePrior", usePrior);
            inversionOp.setParameter("priorScaleFactor", priorScaleFactor);
            Product inversionProduct = inversionOp.getTargetProduct();

            if (computeSeaice) {
                // in this case we have no geocoding yet...
//                inversionProduct.setGeoCoding(IOUtils.getSeaicePstGeocoding(tile));
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
//            setTargetProduct(reprojectedPriorProduct);                  // test!!

            // correct for lost pixels due to extreme SIN angles near South Pole
            // todo: this is not a very nice hack. we should try to find a better solution...
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

    private boolean includesSouthPole(String tile) {
        return (tile.equals("h17v17") || tile.equals("h18v17"));
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3Inversion.class);
        }
    }
}

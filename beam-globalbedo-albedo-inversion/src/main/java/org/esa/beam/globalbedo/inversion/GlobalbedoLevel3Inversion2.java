package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.inversion.util.SouthPoleCorrectionOp;
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
@OperatorMetadata(alias = "ga.l3.inversion2")
public class GlobalbedoLevel3Inversion2 extends Operator {

    @SourceProduct(optional = true)
    private Product seaiceGeocodingProduct;

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory") // e.g., /disk2/Priors
    private String priorRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory suffix")
    // e.g., background/processed.p1.0.618034.p2.1.00000
    private String priorRootDirSuffix;

    @Parameter(defaultValue = "kernel", description = "MODIS Prior file name prefix")
    // e.g., filename = kernel.001.006.h18v04.Snow.1km.nc
    private String priorFileNamePrefix;

    //    @Parameter(defaultValue = "MEAN:_BAND_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    // Oct. 2015:
    @Parameter(defaultValue = "Mean_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    private String priorMeanBandNamePrefix;

    //    @Parameter(defaultValue = "SD:_BAND_", description = "Prefix of prior SD band (default fits to the latest prior version)")
    @Parameter(defaultValue = "Cov_", description = "Prefix of prior SD band (default fits to the latest prior version)")
    private String priorSdBandNamePrefix;


    @Parameter(description = "MODIS tile")
    private String tile;

    @Parameter(description = "Name of tile info filename providing the geocoding")
    private String tileInfoFilename;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "DoY")
    private int doy;

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
            String priorDir = priorRootDir + File.separator + tile;

            if (priorRootDirSuffix == null) {
                final int refDoy = 8 * ((doy - 1) / 8) + 1;
                priorRootDirSuffix = IOUtils.getDoyString(refDoy);
            }
            priorDir = priorDir.concat(File.separator + priorRootDirSuffix);

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
            String fullAccumulatorDir = bbdrRootDir + File.separator + "FullAcc"
                    + File.separator + year + File.separator + tile;

            // STEP 3: we need to attach a geocoding to the Prior product...
            if (usePrior) {
                try {
                    Product tileInfoProduct = null;
                    if (tileInfoFilename != null) {
                        tileInfoProduct = IOUtils.getTileInfoProduct(fullAccumulatorDir, tileInfoFilename);
                    }
                    IOUtils.attachGeoCodingToPriorProduct(priorProduct, tile, tileInfoProduct);
                } catch (IOException e) {
                    throw new OperatorException("Cannot reproject prior products - cannot proceed: " + e.getMessage());
                }
            }

            InversionOp2 inversionOp = new InversionOp2();
            inversionOp.setParameterDefaultValues();
            Product dummySourceProduct;
            if (priorProduct != null) {
                inversionOp.setSourceProduct("priorProduct", priorProduct);
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
            inversionOp.setParameter("gaRootDir", gaRootDir);
            inversionOp.setParameter("year", year);
            inversionOp.setParameter("tile", tile);
            inversionOp.setParameter("doy", doy);
            inversionOp.setParameter("computeSnow", computeSnow);
            inversionOp.setParameter("computeSeaice", computeSeaice);
            inversionOp.setParameter("usePrior", usePrior);
            inversionOp.setParameter("priorScaleFactor", priorScaleFactor);
            inversionOp.setParameter("priorMeanBandNamePrefix", priorMeanBandNamePrefix);
            inversionOp.setParameter("priorSdBandNamePrefix", priorSdBandNamePrefix);
            Product inversionProduct = inversionOp.getTargetProduct();

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
            } else if (priorProduct == null) {
                // same in the standard mode without using priors...
                inversionProduct.setGeoCoding(IOUtils.getSinusoidalTileGeocoding(tile));
            }

            // todo: adapt and activate if needed
            // MetadataUtils.addBrdfMetadata(inversionProduct);


            setTargetProduct(inversionProduct);

            // correct for lost pixels due to extreme SIN angles near South Pole
            // todo: this is not a very nice hack. we should try to find a better solution...
            if (includesSouthPole()) {
                SouthPoleCorrectionOp correctionOp = new SouthPoleCorrectionOp();
                correctionOp.setParameterDefaultValues();
                correctionOp.setSourceProduct("sourceProduct", inversionProduct);
                Product southPoleCorrectedProduct = correctionOp.getTargetProduct();
                ProductUtils.copyMetadata(inversionProduct, southPoleCorrectedProduct);
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

    private boolean includesSouthPole() {
        return (tile.equals("h17v17") || tile.equals("h18v17"));
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3Inversion2.class);
        }
    }
}

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
@OperatorMetadata(alias = "ga.l3.inversion")
public class GlobalbedoLevel3Inversion extends Operator {

    @SourceProduct(optional = true)
    private Product seaiceGeocodingProduct;

    @Parameter(defaultValue = "", description = "Globalbedo BBDR root directory") // e.g., /data/Globalbedo/BBDR
    private String bbdrRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory") // e.g., /disk2/Priors
    private String priorRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory suffix")
    // e.g., background/processed.p1.0.618034.p2.1.00000
    private String priorRootDirSuffix;

//    @Parameter(defaultValue = "kernel", description = "MODIS Prior file name prefix")
    // Oct. 2016:
    @Parameter(defaultValue = "prior.modis.c6", description = "MODIS Prior file name prefix")
    // e.g., filename = kernel.001.006.h18v04.Snow.1km.nc
    private String priorFileNamePrefix;

    //    @Parameter(defaultValue = "MEAN:_BAND_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    // Oct. 2015:
//    @Parameter(defaultValue = "Mean_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    // Oct. 2016:
    @Parameter(defaultValue = "BRDF_Albedo_Parameters_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    private String priorMeanBandNamePrefix;

    //    @Parameter(defaultValue = "SD:_BAND_", description = "Prefix of prior SD band (default fits to the latest prior version)")
//    @Parameter(defaultValue = "Cov_", description = "Prefix of prior SD band (default fits to the latest prior version)")
    // Oct. 2016:
    @Parameter(defaultValue = "BRDF_Albedo_Parameters_", description = "Prefix of prior SD band (default fits to the latest prior version)")
    private String priorSdBandNamePrefix;

    @Parameter(defaultValue = "7", description = "Prior broad bands start index (no longer needed for Collection 6 priors)")
    private int priorBandStartIndex;

    //    @Parameter(defaultValue = "Weighted_number_of_samples", description = "Prior NSamples band name (default fits to the latest prior version)")
//    @Parameter(defaultValue = "BRDF_Albedo_Parameters_bb_wns", description = "Prior NSamples band name (default fits to the latest prior version)")
    @Parameter(defaultValue = "BRDF_Albedo_Parameters_nir_wns", description = "Prior NSamples band name (default fits to the latest prior version)")
    // Oct. 2016:
    private String priorNSamplesBandName;

    //    @Parameter(defaultValue = "land_mask", description = "Prior data mask band name (default fits to the latest prior version)")
//    @Parameter(defaultValue = "Data_Mask", description = "Prior data mask band name (default fits to the latest prior version)")
    // Oct. 2016:
//    @Parameter(defaultValue = "snow", description = "Prior data mask band name (default fits to the latest prior version)")
    @Parameter(defaultValue = "snowFraction", description = "Prior data mask band name (default fits to the latest prior version)")
    private String priorDataMaskBandName;

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

    @Parameter(defaultValue = "false",
            description = "Computation for AVHRR and/or Meteosat (tiles usually have coarser resolution)")
    private boolean computeAvhrrGeo;

    @Parameter(defaultValue = "30.0", description = "Prior scale factor")
    private double priorScaleFactor;

    @Parameter(defaultValue = "true", description = "Use binary accumulator files instead of dimap (to be decided)")
    private boolean useBinaryAccumulators;

    @Parameter(defaultValue = "false", description = "Do accumulation only (no inversion)")
    private boolean accumulationOnly;

    @Parameter(defaultValue = "false", description = "Write only SW related outputs (usually in case of MVIRI/SEVIRI)")
    private boolean writeSwOnly;

    @SuppressWarnings("FieldCanBeLocal")
    @Parameter(defaultValue = "true", description = "Decide whether MODIS priors shall be used in inversion")
    private boolean usePrior = true;

    @Parameter(defaultValue = "6", description = "Prior version (MODIS collection)")
    private int priorVersion;

    @Parameter(defaultValue = "1.0",
            valueSet = {"0.5", "1.0", "2.0", "4.0", "6.0", "10.0", "12.0", "20.0", "60.0"},
            description = "Scale factor with regard to MODIS default 1200x1200. Values > 1.0 reduce product size." +
                    "Should usually be set to 6.0 for AVHRR/GEO (tiles of 200x200).")
    protected double modisTileScaleFactor;

    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        final String fullAccumulatorDir = bbdrRootDir + File.separator + "FullAcc"
                + File.separator + year + File.separator + tile;

        Product priorProduct = null;
        if (usePrior) {
            // STEP 1: get Prior input file...
            String priorDir = priorRootDir + File.separator + tile;

            if (priorRootDirSuffix == null) {
                if (priorVersion == 6) {
                    // daily priors available
                    priorRootDirSuffix = IOUtils.getDoyString(doy);
                } else {
                    final int refDoy = 8 * ((doy - 1) / 8) + 1;
                    priorRootDirSuffix = IOUtils.getDoyString(refDoy);
                }
            }
            priorDir = priorDir.concat(File.separator + priorRootDirSuffix);

            logger.log(Level.INFO, "Searching for prior file in directory: '" + priorDir + "'...");

            try {
                final Product tmpPriorProduct = IOUtils.getPriorProduct(priorVersion, priorDir, priorFileNamePrefix, doy, computeSnow);
                if (tmpPriorProduct != null) {
                    tmpPriorProduct.setGeoCoding(IOUtils.getSinusoidalTileGeocoding(tile));
                    if (modisTileScaleFactor != 1.0) {
                        priorProduct = AlbedoInversionUtils.reprojectToModisTile(tmpPriorProduct, tile, "Nearest", modisTileScaleFactor);
                    } else {
                        priorProduct = tmpPriorProduct;
                    }
                } else {
                    // if not available, continue without MODIS prior
                    usePrior = false;
                }
            } catch (IOException e) {
                throw new OperatorException("No prior file available for DoY " + IOUtils.getDoyString(doy) +
                        " - cannot proceed...: " + e.getMessage());
            }
        }

        if (computeSeaice || !usePrior || (usePrior && priorProduct != null)) {
            InversionOp inversionOp = new InversionOp();
            inversionOp.setParameterDefaultValues();
            Product dummySourceProduct;
            if (priorProduct != null) {
                inversionOp.setSourceProduct("priorProduct", priorProduct);
            } else {
                if (computeSeaice) {
                    final int width = (int) (AlbedoInversionConstants.SEAICE_TILE_WIDTH / modisTileScaleFactor);
                    final int height = (int) (AlbedoInversionConstants.SEAICE_TILE_HEIGHT / modisTileScaleFactor);
                    dummySourceProduct = AlbedoInversionUtils.createDummySourceProduct(width,height);
                } else {
                    final int width = (int) (AlbedoInversionConstants.MODIS_TILE_WIDTH / modisTileScaleFactor);
                    final int height = (int) (AlbedoInversionConstants.MODIS_TILE_HEIGHT / modisTileScaleFactor);
                    dummySourceProduct = AlbedoInversionUtils.createDummySourceProduct(width,height);
                }
                inversionOp.setSourceProduct("priorProduct", dummySourceProduct);
            }
            inversionOp.setParameter("bbdrRootDir", bbdrRootDir);
            inversionOp.setParameter("year", year);
            inversionOp.setParameter("tile", tile);
            inversionOp.setParameter("doy", doy);
            inversionOp.setParameter("computeSnow", computeSnow);
            inversionOp.setParameter("computeSeaice", computeSeaice);
            inversionOp.setParameter("writeSwOnly", writeSwOnly);
            inversionOp.setParameter("usePrior", usePrior);
            inversionOp.setParameter("priorScaleFactor", priorScaleFactor);
            inversionOp.setParameter("priorMeanBandNamePrefix", priorMeanBandNamePrefix);
            inversionOp.setParameter("priorSdBandNamePrefix", priorSdBandNamePrefix);
            inversionOp.setParameter("priorVersion", priorVersion);
            inversionOp.setParameter("priorNSamplesBandName", priorNSamplesBandName);
            inversionOp.setParameter("priorDataMaskBandName", priorDataMaskBandName);
            inversionOp.setParameter("modisTileScaleFactor", modisTileScaleFactor);
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
                inversionProduct.setGeoCoding(IOUtils.getSinusoidalTileGeocoding(tile, modisTileScaleFactor));
            }

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
                // todo: improve if still needed
                // IOUtils.copyLandmask(gaRootDir, tile, getTargetProduct());
            }

        } else {
            logger.log(Level.WARNING, "No prior file found for tile: " + tile + ", year: " + year + ", DoY: " +
                    IOUtils.getDoyString(doy) + " , Snow = " + computeSnow + " - no inversion performed.");
        }

        logger.log(Level.INFO, "Finished inversion process for tile: " + tile + ", year: " + year + ", DoY: " +
                IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
    }

    private boolean includesSouthPole() {
        return (tile.equals("h17v17") || tile.equals("h18v17"));
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3Inversion.class);
        }
    }
}

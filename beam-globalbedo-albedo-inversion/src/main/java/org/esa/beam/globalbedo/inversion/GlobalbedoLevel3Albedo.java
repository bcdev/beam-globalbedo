/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.inversion.util.SouthPoleCorrectionOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 'Master' operator for the final albedo retrieval part
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.albedo")
public class GlobalbedoLevel3Albedo extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory") // e.g., /disk2/Priors
    private String priorRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory suffix") // e.g., background/processed.p1.0.618034.p2.1.00000
    private String priorRootDirSuffix;

    @Parameter(defaultValue = "kernel", description = "MODIS Prior file name prefix") // e.g., filename = kernel.001.006.h18v04.Snow.1km.nc
    private String priorFileNamePrefix;

    @Parameter(defaultValue = "MEAN:_BAND_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    // Oct. 2015:
//    @Parameter(defaultValue = "Mean_", description = "Prefix of prior mean band (default fits to the latest prior version)")
    private String priorMeanBandNamePrefix;

    @Parameter(defaultValue = "SD:_BAND_", description = "Prefix of prior SD band (default fits to the latest prior version)")
//    @Parameter(defaultValue = "Cov_", description = "Prefix of prior SD band (default fits to the latest prior version)")
    private String priorSdBandNamePrefix;

    @Parameter(defaultValue = "true", description = "Decide whether MODIS priors shall be used in inversion")
    private boolean usePrior = true;

    @Parameter(description = "MODIS tile")
    private String tile;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "DoY")
    private int doy;

    @Parameter(defaultValue = "false", description = "Write merged BRDF product only (no albedo compuation)")
    private boolean mergedProductOnly;

    @Parameter(defaultValue = "false", description = "Computation for seaice mode (polar tiles)")
    private boolean computeSeaice;


    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();
//        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        // get BRDF Snow/NoSnow input files...
//        final String brdfDir = gaRootDir + File.separator + "Inversion" + File.separator + year +
//                File.separator + tile + File.separator;
//        final String brdfSnowDir = brdfDir + "snow";
//        final String brdfNoSnowDir = brdfDir + "nosnow";
        final String brdfDir = gaRootDir + File.separator + "Inversion" + File.separator;
        final String brdfSnowDir = brdfDir + "Snow" + File.separator + year + File.separator + tile;
        final String brdfNoSnowDir = brdfDir + "NoSnow" + File.separator + year + File.separator + tile;

        logger.log(Level.ALL, "Searching for BRDF SNOW file in directory: '" + brdfSnowDir + "'...");
        logger.log(Level.ALL, "Searching for BRDF NOSNOW file in directory: '" + brdfNoSnowDir + "'...");

        Product priorProduct = null;
        Product brdfMergedProduct = null;
        if (computeSeaice) {
            Product brdfSeaiceProduct;
            try {
                brdfSeaiceProduct = IOUtils.getBrdfSeaiceProduct(brdfDir, year, doy);
            } catch (IOException e) {
                throw new OperatorException("Cannot load BRDF Seaice product: " + e.getMessage());
            }
            brdfMergedProduct = copyFromSingleProduct(brdfSeaiceProduct, 0.0f);
        } else {
            // we need the SNOW Prior file for given DoY...
            String priorDir = priorRootDir + File.separator + tile;
            if (priorRootDirSuffix != null) {
                priorDir = priorDir.concat(File.separator + priorRootDirSuffix);
            }
            logger.log(Level.ALL, "Searching for SNOW prior file in directory: '" + priorDir + "'...");

            if (usePrior) {
                try {
                    priorProduct = IOUtils.getPriorProduct(priorDir, priorFileNamePrefix, doy, true);
                } catch (IOException e) {
                    throw new OperatorException("Cannot load prior product: " + e.getMessage());
                }
                if (priorProduct == null) {
                    logger.log(Level.ALL, "No 'snow' prior file available for DoY " + IOUtils.getDoyString(doy) + " - will compute albedos from 'NoSnow' BRDF product...");
                }
            }


            Product brdfSnowProduct;
            Product brdfNoSnowProduct;
            try {
                brdfSnowProduct = IOUtils.getBrdfProduct(brdfSnowDir, year, doy, true);
                brdfNoSnowProduct = IOUtils.getBrdfProduct(brdfNoSnowDir, year, doy, false);
            } catch (IOException e) {
                throw new OperatorException("Cannot load BRDF product: " + e.getMessage());
            }

            if (brdfSnowProduct != null && brdfNoSnowProduct != null && (!usePrior || priorProduct != null)) {
                // merge Snow/NoSnow products...
                MergeBrdfOp mergeBrdfOp = new MergeBrdfOp();
                mergeBrdfOp.setParameterDefaultValues();
                mergeBrdfOp.setParameter("priorMeanBandNamePrefix", priorMeanBandNamePrefix);
                mergeBrdfOp.setParameter("priorSdBandNamePrefix", priorSdBandNamePrefix);
                mergeBrdfOp.setSourceProduct("snowProduct", brdfSnowProduct);
                mergeBrdfOp.setSourceProduct("noSnowProduct", brdfNoSnowProduct);
                if (priorProduct != null) {
                    mergeBrdfOp.setSourceProduct("priorProduct", priorProduct);
                }
                brdfMergedProduct = mergeBrdfOp.getTargetProduct();
            } else if (brdfSnowProduct != null && brdfNoSnowProduct == null) {
                logger.log(Level.WARNING, "Found only 'Snow' BRDF product for tile:" + tile + ", year: " +
                        year + ", DoY: " + IOUtils.getDoyString(doy));
                // only use Snow product...
                brdfMergedProduct = copyFromSingleProduct(brdfSnowProduct, 1.0f);
            } else if (brdfNoSnowProduct != null && brdfSnowProduct == null) {
                logger.log(Level.WARNING, "Found only 'NoSnow' BRDF product for tile:" + tile + ", year: " +
                        year + ", DoY: " + IOUtils.getDoyString(doy));
                // only use NoSnow product...
                brdfMergedProduct = copyFromSingleProduct(brdfNoSnowProduct, 0.0f);
            } else {
                logger.log(Level.WARNING, "Neither 'Snow' nor 'NoSnow' BRDF product for tile:" + tile + ", year: " +
                        year + ", DoY: " + IOUtils.getDoyString(doy));
            }
        }

        if (brdfMergedProduct != null) {
            if (mergedProductOnly) {
                setTargetProduct(brdfMergedProduct);
            } else {
                // STEP 2: compute albedo from merged BRDF product...
                BrdfToAlbedoOp albedoOp = new BrdfToAlbedoOp();
                albedoOp.setParameterDefaultValues();
                albedoOp.setSourceProduct("brdfMergedProduct", brdfMergedProduct);
                albedoOp.setParameter("doy", doy);
                albedoOp.setParameter("computeSeaice", computeSeaice);
                setTargetProduct(albedoOp.getTargetProduct());
            }

            if (includesSouthPole(tile)) {
                SouthPoleCorrectionOp correctionOp = new SouthPoleCorrectionOp();
                correctionOp.setParameterDefaultValues();
                correctionOp.setSourceProduct("sourceProduct", getTargetProduct());
                Product southPoleCorrectedProduct = correctionOp.getTargetProduct();
                setTargetProduct(southPoleCorrectedProduct);
            }

            if (computeSeaice) {
                // copy landmask into target product
                IOUtils.copyLandmask(gaRootDir, tile, getTargetProduct());
            }

            logger.log(Level.ALL, "Finished albedo computation process for tile: " + tile + ", year: " + year + ", DoY: " +
                    IOUtils.getDoyString(doy));
        } else {
            logger.log(Level.WARNING, "No albedos computed for tile: " + tile + ", year: " + year +
                    ", Doy: " + IOUtils.getDoyString(doy));
        }
    }

    private boolean includesSouthPole(String tile) {
        return (tile.equals("h17v17") || tile.equals("h18v17"));
    }

    private Product copyFromSingleProduct(Product sourceProduct, float propNSampleConstantValue) {
        final int width = sourceProduct.getSceneRasterWidth();
        final int height = sourceProduct.getSceneRasterHeight();
        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), width, height);
        for (Band band : sourceProduct.getBands()) {
            ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, true);
        }
        // we need to fill the 'Proportion_NSamples' band: 1.0 if only snow, 0.0 if only no snow
        Band propNSamplesBand = targetProduct.addBand(AlbedoInversionConstants.MERGE_PROPORTION_NSAMPLES_BAND_NAME, ProductData.TYPE_FLOAT32);
        BufferedImage bi = ConstantDescriptor.create((float) width, (float) height, new Float[]{propNSampleConstantValue},
                null).getAsBufferedImage();
        propNSamplesBand.setSourceImage(bi);

        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        return targetProduct;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3Albedo.class);
        }
    }

}

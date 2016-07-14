package org.esa.beam.globalbedo.inversion.spectral;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.BrdfToAlbedoOp;
import org.esa.beam.globalbedo.inversion.MergeBrdfOp;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.inversion.util.ModisTileGeoCoding;
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
 * Computes spectral albedo from spectral BRDF products.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l3.albedo.spectral",
        description = "Computes spectral albedo from spectral BRDF products.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")
public class GlobalbedoLevel3SpectralAlbedo extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String inversionRootDir;

    @Parameter(description = "MODIS tile")
    private String tile;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "DoY")
    private int doy;

    @Parameter(defaultValue = "false", description = "Write merged BRDF product only (no albedo compuation)")
    private boolean mergedProductOnly;

    @Parameter(description = "Sub tile start X", valueSet = {"0", "300", "600", "900"})
    private int subStartX;

    @Parameter(description = "Sub tile start Y", valueSet = {"0", "300", "600", "900"})
    private int subStartY;



    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();
        final String brdfDir = inversionRootDir + File.separator;
        final String brdfSnowDir = brdfDir + "Snow" + File.separator + year + File.separator + tile;
        final String brdfNoSnowDir = brdfDir + "NoSnow" + File.separator + year + File.separator + tile;

        logger.log(Level.ALL, "Searching for BRDF SNOW file in directory: '" + brdfSnowDir + "'...");
        logger.log(Level.ALL, "Searching for BRDF NOSNOW file in directory: '" + brdfNoSnowDir + "'...");

        Product brdfMergedProduct = null;
        Product brdfSnowProduct;
        Product brdfNoSnowProduct;
        try {
            brdfSnowProduct = SpectralIOUtils.getSpectralBrdfProduct(brdfSnowDir, year, doy, true, subStartX, subStartY);
            brdfNoSnowProduct = SpectralIOUtils.getSpectralBrdfProduct(brdfNoSnowDir, year, doy, false, subStartX, subStartY);
        } catch (IOException e) {
            throw new OperatorException("Cannot load spectral BRDF product: " + e.getMessage());
        }

        if (brdfSnowProduct != null && brdfNoSnowProduct != null) {
            // merge Snow/NoSnow products...
            MergeSpectralBrdfOp mergeBrdfOp = new MergeSpectralBrdfOp();
            mergeBrdfOp.setParameterDefaultValues();
            mergeBrdfOp.setSourceProduct("snowProduct", brdfSnowProduct);
            mergeBrdfOp.setSourceProduct("noSnowProduct", brdfNoSnowProduct);
            brdfMergedProduct = mergeBrdfOp.getTargetProduct();
        } else if (brdfSnowProduct != null) {
            logger.log(Level.WARNING, "Found only 'Snow' BRDF product for tile:" + tile + ", year: " +
                    year + ", DoY: " + IOUtils.getDoyString(doy));
            // only use Snow product...
            brdfMergedProduct = copyFromSingleProduct(brdfSnowProduct, 1.0f);
        } else if (brdfNoSnowProduct != null) {
            logger.log(Level.WARNING, "Found only 'NoSnow' BRDF product for tile:" + tile + ", year: " +
                    year + ", DoY: " + IOUtils.getDoyString(doy));
            // only use NoSnow product...
            brdfMergedProduct = copyFromSingleProduct(brdfNoSnowProduct, 0.0f);
        } else {
            logger.log(Level.WARNING, "Neither 'Snow' nor 'NoSnow' BRDF product for tile:" + tile + ", year: " +
                    year + ", DoY: " + IOUtils.getDoyString(doy));
        }

        if (brdfMergedProduct != null) {
            if (brdfMergedProduct.getGeoCoding() == null) {
                final ModisTileGeoCoding sinusoidalSubtileGeocoding =
                        SpectralIOUtils.getSinusoidalSubtileGeocoding(tile, subStartX, subStartY);
                brdfMergedProduct.setGeoCoding(sinusoidalSubtileGeocoding);
            }

            if (mergedProductOnly) {
                setTargetProduct(brdfMergedProduct);
            } else {
                // STEP 2: compute albedo from merged BRDF product...
                BrdfToAlbedoOp albedoOp = new BrdfToAlbedoOp();
                albedoOp.setParameterDefaultValues();
                albedoOp.setSourceProduct("brdfMergedProduct", brdfMergedProduct);
                albedoOp.setParameter("doy", doy);
                setTargetProduct(albedoOp.getTargetProduct());
            }

            logger.log(Level.ALL, "Finished albedo computation process for tile: " + tile + ", year: " + year + ", DoY: " +
                    IOUtils.getDoyString(doy));
        } else {
            logger.log(Level.WARNING, "No albedos computed for tile: " + tile + ", year: " + year +
                    ", Doy: " + IOUtils.getDoyString(doy));
        }
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
            super(GlobalbedoLevel3SpectralAlbedo.class);
        }
    }

}

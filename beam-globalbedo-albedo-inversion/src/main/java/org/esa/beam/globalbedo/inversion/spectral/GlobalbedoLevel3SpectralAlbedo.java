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
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.inversion.util.ModisTileGeoCoding;
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

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;


    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        final String snowDirName = computeSnow ? "Snow" : "NoSnow";
        final String brdfDir = inversionRootDir + File.separator + snowDirName + File.separator + year + File.separator + tile;

        logger.log(Level.INFO, "Searching for BRDF file in directory: '" + brdfDir + "'...");

        Product brdfProduct;
        try {
            brdfProduct = SpectralIOUtils.getSpectralBrdfProduct(brdfDir, year, doy, true);
        } catch (IOException e) {
            throw new OperatorException("Cannot load spectral BRDF product: " + e.getMessage());
        }

        if (brdfProduct != null) {
            // compute albedo from BRDF product...
            SpectralBrdfToAlbedoOp albedoOp = new SpectralBrdfToAlbedoOp();
            albedoOp.setParameterDefaultValues();
            albedoOp.setSourceProduct("spectralBrdfProduct", brdfProduct);
            albedoOp.setParameter("doy", doy);
            final Product albedoProduct = albedoOp.getTargetProduct();
            ProductUtils.copyGeoCoding(brdfProduct, albedoProduct);
            setTargetProduct(albedoProduct);

            logger.log(Level.INFO, "Finished albedo computation process for tile: " + tile + ", year: " + year + ", DoY: " +
                    IOUtils.getDoyString(doy));
        } else {
            logger.log(Level.WARNING, "No albedos computed for tile: " + tile + ", year: " + year +
                    ", Doy: " + IOUtils.getDoyString(doy));
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3SpectralAlbedo.class);
        }
    }

}

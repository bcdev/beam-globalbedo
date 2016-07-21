package org.esa.beam.globalbedo.inversion.singlepixel;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.BrdfToAlbedoOp;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.globalbedo.inversion.util.ModisTileGeoCoding;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.logging.BeamLogManager;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 'Master' operator for the final albedo retrieval part for single pixel
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.l3.albedo.single",
        description = "''Master' operator for the final albedo retrieval part for single pixel",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")
public class GlobalbedoLevel3AlbedoSinglePixel extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "", description = "Inversion target directory")
    private String brdfDir;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(description = "MODIS tile")
    private String tile;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "DoY")
    private int doy;


    @SourceProduct(description = "BRDF input product")
    private Product brdfProduct;

    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        Product brdfProductToProcess = addProportionNSamplesBand(brdfProduct);

        if (brdfProductToProcess != null) {
            final ModisTileGeoCoding sinusoidalTileGeocoding = IOUtils.getSinusoidalTileGeocoding(tile);
            brdfProductToProcess.setGeoCoding(sinusoidalTileGeocoding);
            final GeoPos latLon = AlbedoInversionUtils.getLatLonFromProduct(brdfProduct);

            // compute albedo from BRDF product...
            BrdfToAlbedoOp albedoOp = new BrdfToAlbedoOp();
            albedoOp.setParameterDefaultValues();
            albedoOp.setSourceProduct("brdfMergedProduct", brdfProductToProcess);
            albedoOp.setParameter("doy", doy);
            albedoOp.setParameter("latLon", latLon);
            albedoOp.setParameter("singlePixelMode", true);
            final Product albedoProduct = albedoOp.getTargetProduct();

            setTargetProduct(albedoProduct);

            logger.log(Level.INFO, "Finished albedo computation process for tile: " + tile + ", year: " + year + ", DoY: " +
                    IOUtils.getDoyString(doy));
        } else {
            logger.log(Level.WARNING, "No single-pixel albedos computed for tile: " + tile + ", year: " + year +
                    ", Doy: " + IOUtils.getDoyString(doy));
        }
    }

    private Product addProportionNSamplesBand(Product sourceProduct) {
        final int width = sourceProduct.getSceneRasterWidth();
        final int height = sourceProduct.getSceneRasterHeight();
        Product targetProduct = new Product(sourceProduct.getName(), sourceProduct.getProductType(), width, height);
        for (Band band : sourceProduct.getBands()) {
            ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, true);
        }
        // we need to fill the 'Proportion_NSamples' band: 1.0 if only snow, 0.0 if only no snow
        final float propNSampleConstantValue = computeSnow ? 1.0f : 0.0f;
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
            super(GlobalbedoLevel3AlbedoSinglePixel.class);
        }
    }

}

package org.esa.beam.globalbedo.inversion.spectral;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.gpf.operators.standard.MergeOp;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Operator for merging spectral BRDF bands 1-7 (MODIS) products into one 'allbands' product.
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.albedo.brdf.spectral.bandmerge",
        description = "Merges spectral BRDF bands 1-7 (MODIS) products into one 'allbands' product.",
        authors = "Olaf Danne",
        version = "1.0",
        copyright = "(C) 2017 by Brockmann Consult")
public class SpectralBrdfBandmergeOp extends Operator {

    @Parameter(defaultValue = "", description = "Spectral inversion root directory")
    private String spectralInversionRootDir;

    @Parameter(description = "MODIS tile")
    private String tile;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "DoY", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;


    @Override
    public void initialize() throws OperatorException {
        try {
            final Product[] singleBandProducts =
                    SpectralIOUtils.getSpectralBrdfBandMergeInputProducts(spectralInversionRootDir,
                            computeSnow, tile, year, doy);
            if (singleBandProducts.length > 0) {
                setTargetProduct(createTargetProduct(singleBandProducts));
            } else {
                BeamLogManager.getSystemLogger().log(Level.WARNING, "No BRDF band products found for year/doy/tile " +
                        year + "/" + doy + "/" + tile + " ...");
                setTargetProduct(new Product("dummy", "dummy", 1, 1));
            }
        } catch (IOException e) {
            BeamLogManager.getSystemLogger().log(Level.WARNING, "Cannot get BRDF band products found for year/doy/tile " +
                    year + "/" + doy + "/" + tile + " ...");
            e.printStackTrace();
            setTargetProduct(new Product("dummy", "dummy", 1, 1));
        }

    }

    private Product createTargetProduct(Product[] singleBandProducts) {

        final MergeOp mergeOp = new MergeOp();
        mergeOp.setSourceProducts(singleBandProducts);
        mergeOp.setSourceProduct("masterProduct", singleBandProducts[0]);
        Product targetProduct = mergeOp.getTargetProduct();

        for (Band band : targetProduct.getBands()) {
            band.setNoDataValue(AlbedoInversionConstants.NO_DATA_VALUE);
            band.setNoDataValueUsed(true);
        }

        return targetProduct;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SpectralBrdfBandmergeOp.class);
        }
    }

}

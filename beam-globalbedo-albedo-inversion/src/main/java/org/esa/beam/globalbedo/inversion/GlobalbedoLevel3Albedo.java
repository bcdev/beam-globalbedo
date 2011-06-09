package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import javax.media.jai.JAI;
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

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "001", description = "DoY")
    private int doy;

    @Parameter(defaultValue = "false", description = "Write merged BRDF product only (no albedo compuation)")
    private boolean mergedProductOnly;


    private Logger logger;

    @Override
    public void initialize() throws OperatorException {
        logger = BeamLogManager.getSystemLogger();
        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        // STEP 1: we need the SNOW Prior file for given DoY...
        final String priorDir = gaRootDir + File.separator + "Priors" + File.separator + tile + File.separator +
                                "background" + File.separator + "processed.p1.0.618034.p2.1.00000";

        Product priorProduct;
        try {
            priorProduct = IOUtils.getPriorProduct(priorDir, doy, true);
        } catch (IOException e) {
            throw new OperatorException("Cannot load prior product: " + e.getMessage());
        }

        if (priorProduct == null) {
            logger.log(Level.ALL, "No prior file available for DoY " + doy + " - do inversion without prior...");
        }

        // STEP 2: get BRDF Snow/NoSnow input files...
        final String brdfDir = gaRootDir + File.separator + "inversion" + File.separator + tile + File.separator;

        Product brdfSnowProduct;
        Product brdfNoSnowProduct;
        Product brdfMergedProduct;
        try {
            brdfSnowProduct = IOUtils.getBrdfProduct(brdfDir, year, doy, true);
            brdfNoSnowProduct = IOUtils.getBrdfProduct(brdfDir, year, doy, false);
        } catch (IOException e) {
            throw new OperatorException("Cannot load BRDF product: " + e.getMessage());
        }

        if (brdfSnowProduct != null && brdfNoSnowProduct != null) {
            // merge Snow/NoSnow products...
            MergeBrdfOp mergeBrdfOp = new MergeBrdfOp();
            mergeBrdfOp.setSourceProduct("snowProduct", brdfSnowProduct);
            mergeBrdfOp.setSourceProduct("noSnowProduct", brdfNoSnowProduct);
            mergeBrdfOp.setSourceProduct("priorProduct", priorProduct);
            brdfMergedProduct = mergeBrdfOp.getTargetProduct();
        } else if (brdfSnowProduct != null && brdfNoSnowProduct == null) {
            logger.log(Level.WARNING, "Found only 'Snow' BRDF product for Year/Doy: " +
                                      Integer.toString(year) + "/" + Integer.toString(doy));
            // only use Snow product...
            brdfMergedProduct = brdfSnowProduct;
        } else if (brdfSnowProduct == null && brdfNoSnowProduct != null) {
            logger.log(Level.WARNING, "Found only 'Snow' BRDF product for Year/Doy: " +
                                      Integer.toString(year) + "/" + Integer.toString(doy));
            // only use NoSnow product...
            brdfMergedProduct = brdfNoSnowProduct;
        } else {
            throw new OperatorException("Neither 'Snow' nor 'NoSnow' BRDF product found for Year/Doy: " +
                                        Integer.toString(year) + "/" + Integer.toString(doy));
        }

        if (mergedProductOnly) {
            setTargetProduct(brdfMergedProduct);
        } else {
            // STEP 2: compute albedo from merged BRDF product...
            BrdfToAlbedoOp albedoOp = new BrdfToAlbedoOp();
            albedoOp.setSourceProduct("brdfMergedProduct", brdfMergedProduct);
            albedoOp.setParameter("doy", doy);
            setTargetProduct(albedoOp.getTargetProduct());
        }

        System.out.println("done");
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3Albedo.class);
        }
    }

}

package org.esa.beam.globalbedo.inversion.spectral;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 23.05.2016
 * Time: 14:40
 *
 * @author olafd
 */
@OperatorMetadata(alias = "ga.l3.inversion.spectral.uncertainties")
public class GlobalbedoLevel3SpectralUncertainties extends Operator {

    @Parameter(defaultValue = "", description = "Daily acc binary files root directory")
    private String dailyAccRootDir;

    @Parameter(defaultValue = "", description = "MODIS Prior root directory") // e.g., /disk2/Priors
    private String priorRootDir;

    @Parameter(description = "MODIS tile")
    private String tile;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "DoY")
    private int doy;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(interval = "[1,7]", description = "Band index in case only 1 SDR band is processed")
    private int singleBandIndex;    // todo: consider chemistry bands


    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        // STEP 1: get Prior input file...
        String priorDir = priorRootDir + File.separator + tile + File.separator + IOUtils.getDoyString(doy);
        logger.log(Level.INFO, "Searching for prior file in directory: '" + priorDir + "'...");

        Product priorProduct;
        try {
            priorProduct = IOUtils.getPriorProduct(6, priorDir, "prior.modis.c6", doy, computeSnow);
            if (priorProduct == null) {
                throw new OperatorException("Cannot get prior file for DoY " + IOUtils.getDoyString(doy) +
                                                    " - exiting. ");
            }
        } catch (IOException e) {
            throw new OperatorException("No prior file available for DoY " + IOUtils.getDoyString(doy) +
                                                " - cannot proceed...: " + e.getMessage());
        }

        SpectralUncertaintiesOp spectralUncertaintiesOp = new SpectralUncertaintiesOp();
        spectralUncertaintiesOp.setParameterDefaultValues();
        spectralUncertaintiesOp.setSourceProduct("priorProduct", priorProduct);
        spectralUncertaintiesOp.setParameter("dailyAccRootDir", dailyAccRootDir);
        spectralUncertaintiesOp.setParameter("year", year);
        spectralUncertaintiesOp.setParameter("tile", tile);
        spectralUncertaintiesOp.setParameter("doy", doy);
        spectralUncertaintiesOp.setParameter("computeSnow", computeSnow);
        spectralUncertaintiesOp.setParameter("singleBandIndex", singleBandIndex);
        Product uncertaintiesProduct = spectralUncertaintiesOp.getTargetProduct();

        uncertaintiesProduct.setGeoCoding(SpectralIOUtils.getSinusoidalGeocoding(tile));

        setTargetProduct(uncertaintiesProduct);

        logger.log(Level.INFO, "Finished uncertainties for tile: " + tile + ", year: " + year + ", DoY: " +
                IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3SpectralUncertainties.class);
        }
    }
}

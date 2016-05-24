package org.esa.beam.globalbedo.inversion.spectral;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.InversionOp;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
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
public class GlobalbedoLevel3SpectralInversion extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

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

    @Parameter(defaultValue = "false", description = "Do accumulation only (no inversion)")
    private boolean accumulationOnly;


    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        SpectralInversionOp inversionOp = new SpectralInversionOp();
        inversionOp.setParameterDefaultValues();
        Product dummySourceProduct = AlbedoInversionUtils.createDummySourceProduct(AlbedoInversionConstants.MODIS_TILE_WIDTH,
                                                                                   AlbedoInversionConstants.MODIS_TILE_HEIGHT);
        inversionOp.setSourceProduct("priorProduct", dummySourceProduct);
        inversionOp.setParameter("gaRootDir", gaRootDir);
        inversionOp.setParameter("year", year);
        inversionOp.setParameter("tile", tile);
        inversionOp.setParameter("doy", doy);
        inversionOp.setParameter("computeSnow", computeSnow);
        Product inversionProduct = inversionOp.getTargetProduct();

        inversionProduct.setGeoCoding(IOUtils.getSinusoidalTileGeocoding(tile));

        setTargetProduct(inversionProduct);

        logger.log(Level.ALL, "Finished inversion process for tile: " + tile + ", year: " + year + ", DoY: " +
                IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3SpectralInversion.class);
        }
    }
}

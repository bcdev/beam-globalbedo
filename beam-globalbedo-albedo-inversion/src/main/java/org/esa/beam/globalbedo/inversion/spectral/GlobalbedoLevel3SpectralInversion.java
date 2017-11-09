package org.esa.beam.globalbedo.inversion.spectral;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

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
@OperatorMetadata(alias = "ga.l3.inversion.spectral")
public class GlobalbedoLevel3SpectralInversion extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo SDR root directory")
    private String sdrRootDir;

    @Parameter(defaultValue = "", description = "Daily acc binary files root directory")
    private String dailyAccRootDir;

    @Parameter(description = "MODIS tile")
    private String tile;

    @Parameter(description = "Year")
    private int year;

    @Parameter(description = "DoY")
    private int doy;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    // for the moment we only accept original size (no division) or division into 4x4 subtiles
    @Parameter(description = "Sub tiling factor (e.g. 4 for 300x300 subtile size",
            defaultValue = "1", valueSet = {"1", "4"})
    private int subtileFactor;

    @Parameter(description = "Sub tile start X", defaultValue = "0", valueSet = {"0", "300", "600", "900"})
    private int subStartX;

    @Parameter(description = "Sub tile start Y", defaultValue = "0", valueSet = {"0", "300", "600", "900"})
    private int subStartY;

    @Parameter(defaultValue = "1", interval = "[1,7]",
            description = "Number of spectral bands (currently always 7 for standard MODIS spectral mapping")
    private int numSdrBands;

    @Parameter(defaultValue = "3", interval = "[1,7]", description = "Band index in case only 1 SDR band is processed")
    private int singleBandIndex;    // todo: consider chemistry bands

    int subtileWidth;
    int subtileHeight;

    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        subtileWidth = AlbedoInversionConstants.MODIS_TILE_WIDTH/subtileFactor;
        subtileHeight = AlbedoInversionConstants.MODIS_TILE_HEIGHT/subtileFactor;

        SpectralInversionOp inversionOp = new SpectralInversionOp();
        inversionOp.setParameterDefaultValues();
        Product dummySourceProduct = AlbedoInversionUtils.createDummySourceProduct(subtileWidth, subtileHeight);
        inversionOp.setSourceProduct("priorProduct", dummySourceProduct);
        inversionOp.setParameter("sdrRootDir", sdrRootDir);
        inversionOp.setParameter("dailyAccRootDir", dailyAccRootDir);
        inversionOp.setParameter("year", year);
        inversionOp.setParameter("tile", tile);
        inversionOp.setParameter("doy", doy);
        inversionOp.setParameter("computeSnow", computeSnow);
        inversionOp.setParameter("subStartX", subStartX);
        inversionOp.setParameter("subStartY", subStartY);
        inversionOp.setParameter("subtileFactor", subtileFactor);
        inversionOp.setParameter("numSdrBands", numSdrBands);
        inversionOp.setParameter("singleBandIndex", singleBandIndex);
        Product inversionProduct = inversionOp.getTargetProduct();

        inversionProduct.setGeoCoding(SpectralIOUtils.getSinusoidalSubtileGeocoding(tile, subStartX, subStartY));

        setTargetProduct(inversionProduct);

        logger.log(Level.INFO, "Finished inversion process for tile: " + tile + ", year: " + year + ", DoY: " +
                IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3SpectralInversion.class);
        }
    }
}

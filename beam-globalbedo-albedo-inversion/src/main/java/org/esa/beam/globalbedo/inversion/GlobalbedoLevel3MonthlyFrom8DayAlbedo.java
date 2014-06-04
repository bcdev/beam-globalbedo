package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.AlbedoInversionUtils;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 'Master' operator for getting monthly albedos from 8-day periods
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.albedo.monthly")
public class GlobalbedoLevel3MonthlyFrom8DayAlbedo extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "1", interval = "[1,12]", description = "Month index")
    private int monthIndex;

    @Parameter(defaultValue = "true", description = "True if input is 8-day mosaic, not single tile")
    private boolean isMosaicAlbedo;

    @Parameter(valueSet = {"05", "005", "025"}, description = "Scaling (05 = 1/2deg, 005 = 1/20deg, 025 = 1/4deg resolution", defaultValue = "05")
    private String mosaicScaling;

    @Parameter(valueSet = {"SIN", "PC"}, description = "Plate Carree or Sinusoidal", defaultValue = "PC")
    private String reprojection;

    private Logger logger;

    @Override
    public void initialize() throws OperatorException {
        logger = BeamLogManager.getSystemLogger();
        //        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        Product[] albedo8DayProducts;
        if (isMosaicAlbedo) {
            albedo8DayProducts = IOUtils.getAlbedo8DayMosaicProducts(gaRootDir, monthIndex, mosaicScaling, reprojection);
        } else {
            albedo8DayProducts = IOUtils.getAlbedo8DayTileProducts(gaRootDir, tile);
        }
        if (albedo8DayProducts != null && albedo8DayProducts.length > 0) {

            // get Albedo monthly product...
            if (isMosaicAlbedo) {
                // the standard now
                MonthlyFrom8DayAlbedoMosaicsOp monthlyAlbedoMosaicsOp = new MonthlyFrom8DayAlbedoMosaicsOp();
                monthlyAlbedoMosaicsOp.setParameterDefaultValues();
                monthlyAlbedoMosaicsOp.setSourceProducts(albedo8DayProducts);
                monthlyAlbedoMosaicsOp.setParameter("monthIndex", monthIndex);
                setTargetProduct(monthlyAlbedoMosaicsOp.getTargetProduct());
            } else {
                // get monthly weighting...
                float[][] monthlyWeighting = AlbedoInversionUtils.getMonthlyWeighting();
                MonthlyFrom8DayAlbedoOp monthlyAlbedoOp = new MonthlyFrom8DayAlbedoOp();
                monthlyAlbedoOp.setParameterDefaultValues();
                monthlyAlbedoOp.setSourceProducts(albedo8DayProducts);
                monthlyAlbedoOp.setParameter("monthlyWeighting", monthlyWeighting);
                monthlyAlbedoOp.setParameter("monthIndex", monthIndex);
                setTargetProduct(monthlyAlbedoOp.getTargetProduct());
            }

            logger.log(Level.ALL, "Finished monthly albedo computation process for tile: " + tile + ", year: " + year +
                    ", month: " + monthIndex);
        } else {
            logger.log(Level.WARNING, "\nNo input products found --> no monthly albedos computed for tile: " + tile + ", year: " + year +
                    ", month: " + monthIndex);
        }
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3MonthlyFrom8DayAlbedo.class);
        }
    }

}

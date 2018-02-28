package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.globalbedo.inversion.util.IOUtils;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 'Master' operator for getting monthly from daily albedo mosaics
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.l3.albedo.monthlyfromdaily")
public class GlobalbedoLevel3MonthlyFromDailyAlbedo extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "Merge", valueSet = {"Merge", "Snow", "NoSnow"},
            description = "Input BRDF type, either Merge, Snow or NoSnow.")
    private String snowMode;

    @Parameter(defaultValue = "2005", description = "Year")
    private String year;

    @Parameter(defaultValue = "1", interval = "[1,12]", description = "Month index")
    private int monthIndex;

    @Parameter(valueSet = {"05", "005", "025"},
            description = "Scaling (05 = 1/2deg, 005 = 1/20deg, 025 = 1/4deg resolution",
            defaultValue = "05")
    private String mosaicScaling;

    @Parameter(valueSet = {"SIN", "PC"}, description = "Plate Carree or Sinusoidal", defaultValue = "PC")
    private String reprojection;

    private Logger logger;

    @Override
    public void initialize() throws OperatorException {
        logger = BeamLogManager.getSystemLogger();
        //        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        final Product[] albedoDailyProducts =
                IOUtils.getAlbedoMonthlyMosaicDailyInputProducts(gaRootDir, snowMode, monthIndex, year, mosaicScaling);
        if (albedoDailyProducts != null && albedoDailyProducts.length > 0) {
            // get Albedo monthly product...
            MonthlyFromDailyAlbedoMosaicsOp monthlyAlbedoMosaicsOp = new MonthlyFromDailyAlbedoMosaicsOp();
            monthlyAlbedoMosaicsOp.setParameterDefaultValues();
            monthlyAlbedoMosaicsOp.setSourceProducts(albedoDailyProducts);
            monthlyAlbedoMosaicsOp.setParameter("monthIndex", monthIndex);
            setTargetProduct(monthlyAlbedoMosaicsOp.getTargetProduct());
            logger.log(Level.INFO, "Finished monthly albedo computation process for year: " + year +
                    ", month: " + monthIndex);
        } else {
            logger.log(Level.WARNING, "\nNo input products found --> no monthly albedos computed for  year: " + year +
                    ", month: " + monthIndex);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(GlobalbedoLevel3MonthlyFromDailyAlbedo.class);
        }
    }

}

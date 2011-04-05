package org.esa.beam.globalbedo.inversion;


import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.experimental.PixelOperator;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
/**
 * Pixel operator implementing the daily accumulation part of python breadboard.
 * The breadboard file is 'AlbedoInversionDailyAccumulator.py' provided by Gerardo López Saldaña.
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.fullaccum",
                  description = "Performs final inversion from fully accumulated optimal estimation matrices",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2011 by Brockmann Consult")

public class InversionOp extends PixelOperator {

    // BB line 597:
    private static final int xmin = 1;
    private static final int xmax = 1;
    private static final int ymin = 1;
    private static final int ymax = 1;

    @Override
    protected void configureTargetProduct(Product targetProduct) {

        readAuxdata();
    }

    private void readAuxdata() {
        //To change body of created methods use File | Settings | File Templates.
        readLandMask();
    }

    private void readObservationalData() {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void readLandMask() {
        // todo: define which land mask should be used - can we use the 'mask == 1' from the priors?
        // in any case, should go to IOUtils
    }

    @Override
    protected void configureSourceSamples(Configurator configurator) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void configureTargetSamples(Configurator configurator) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples,
                                WritableSample[] targetSamples) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(InversionOp.class);
        }
    }
}

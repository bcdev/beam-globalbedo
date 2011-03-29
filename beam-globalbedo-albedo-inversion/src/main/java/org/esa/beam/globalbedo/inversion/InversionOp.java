package org.esa.beam.globalbedo.inversion;


import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.experimental.PixelOperator;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class InversionOp extends PixelOperator {

    /* ------------------------------------------------------------------------------------------

    Perform albedo inversion in following two main steps:
        - optimal parameter estimation  (ATBD, section 6.3 (summary))
        - albedo estimation  (ATBD, section 6.3 (summary))

    Substeps:
        - read input data (ATBD section 7.1.1):
            * a set of prior estimates
            * land surface and land cover mask
            * set of BBDR observations to be accumulated

            python routines used:
                * Get_Observations (7.2.1.1)
                * Get_Prior (7.2.1.2)
                * Get_LandCoverMask (7.2.1.2)

        - accumulate observations:
            * accumulate all observations from a day into a single one (per-day accumulator file)
                --> AlbedoInversionDailyAccumulator.py
            * read ALL per-day accumulator files
            * apply a temporal weighting
                --> AlbedoInversion_multisensor_accum_v7.py

             python routines used:
                * Accumulate_OneStep (7.2.2.1)
                * Accumulate_TwoWay (7.2.2.2)

        - compute results:
            * model parameter estimates (7.1.2.1)
            * albedo (7.1.2.2)

            python routines used:
                *  Parameter_estimate (7.2.2.3)

        - write output data

            python routines used:
                * WriteDataset

        - external: verification against breadboard results
            * get python code running
            * compare results for GL testdata

     ---------------------------------------------------------------------------------------- */

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
        readPriors();
        readLandMask();
        readObservationalData();

    }

    private void readObservationalData() {
        //To change body of created methods use File | Settings | File Templates.
    }

    private void readLandMask() {
        // todo: define which land mask should be used - can we use the 'mask == 1' from the priors?
    }

    private void readPriors() {
        //To change body of created methods use File | Settings | File Templates.
        final double priorScaleFactor = 10.0;
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

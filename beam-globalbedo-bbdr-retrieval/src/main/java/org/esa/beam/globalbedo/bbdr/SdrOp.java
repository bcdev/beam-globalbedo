package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.PixelOperator;

import static java.lang.Math.*;
import static java.lang.StrictMath.toRadians;

/**
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
public class SdrOp extends PixelOperator {

    private static final int SRC_LAND_MASK = 0;
    private static final int SRC_VZA = 1;
    private static final int SRC_SZA = 2;
    private static final int SRC_PHI = 3;
    private static final int SRC_HSF = 4;
    private static final int SRC_AOT = 5;
    private static final int SRC_GAS = 6;
    private static final int SRC_TOA_RFL = 7;

    @SourceProduct
    private Product source;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    @Override
    protected void configureTargetProduct(Product targetProduct) {
        // TODO
    }

    @Override
    protected void configureSourceSamples(Configurator configurator) {
        // TODO
    }

    @Override
    protected void configureTargetSamples(Configurator configurator) {
        // TODO
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        if (!sourceSamples[SRC_LAND_MASK].getBoolean()) {
            // only compute over land
            // TODO set no-data-values
            return;
        }
        double vza = sourceSamples[SRC_VZA].getDouble();
        double sza = sourceSamples[SRC_SZA].getDouble();
        double phi = sourceSamples[SRC_PHI].getDouble();
        double hsf = sourceSamples[SRC_HSF].getDouble();
        double aot = sourceSamples[SRC_AOT].getDouble();
        double gas = sourceSamples[SRC_GAS].getDouble();

        double[] toa_rfl = new double[sensor.getNumBands()];
        for (int i = 0; i < toa_rfl.length; i++) {
            toa_rfl[i] = sourceSamples[SRC_TOA_RFL + i].getDouble();
        }

        double muv = cos(toRadians(vza));
        double mus = cos(toRadians(sza));
        double amf = 1.0 / muv + 1.0 / mus;

        //ind_amf = where(amf lt amf_arr) & ind_amf = ind_amf[0]-1
        // amf_p = (amf - amf_arr[ind_amf]) / (amf_arr[ind_amf + 1] - amf_arr[ind_amf])


    }
}

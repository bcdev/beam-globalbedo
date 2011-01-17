package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.PixelOperator;
import org.esa.beam.framework.gpf.experimental.PointOperator;
import org.esa.beam.framework.gpf.experimental.SampleOperator;

import java.util.Arrays;

import static java.lang.Math.*;
import static java.lang.StrictMath.toRadians;

/**
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
public class SdrOp extends SampleOperator {

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

    // auxdata
    private double[] amfArray;
    private double[] gasArray;
    private double[][][] lut_gas;
    private double[][][][][] kx_lut_gas;

    @Override
    protected void configureTargetProduct(Product targetProduct) {
        // TODO
        //read auxdata, LUTs
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
    protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
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
        int bandIndex = targetSample.getIndex();
        double toa_rfl = sourceSamples[SRC_TOA_RFL + bandIndex].getDouble();

        double muv = cos(toRadians(vza));
        double mus = cos(toRadians(sza));
        double amf = 1.0 / muv + 1.0 / mus;

        int ind_amf = getIndexBefore(amf, amfArray);
        double amf_p = (amf - amfArray[ind_amf]) / (amfArray[ind_amf + 1] - amfArray[ind_amf]);

        int ind_gas = getIndexBefore(gas, gasArray);
        double gas_p = (gas - gasArray[ind_gas]) / (gasArray[ind_gas + 1] - gasArray[ind_gas]);

        double tg = (1.0 - amf_p) * (1.0 - gas_p) * lut_gas[ind_amf][ind_gas][bandIndex] +
                    gas_p * (1 - amf_p) * lut_gas[ind_amf][ind_gas + 1][bandIndex] +
                    (1. - gas_p) * amf_p * lut_gas[ind_amf + 1][ind_gas][bandIndex] +
                    amf_p * gas_p * lut_gas[ind_amf + 1][ind_gas + 1][bandIndex];

        double[] f_int = interpol_lut_MOMO_kx(vza, sza, phi, hsf, aot, bandIndex);

        double rpw     = f_int[0] * Math.PI / mus; // Path Radiance
        double ttot    = f_int[1]/mus;    // Total TOA flux (Isc*Tup*Tdw)
        double sab     = f_int[2];        // Spherical Albedo
        double rat_tdw = 1.0 - f_int[3];  // tdif_dw / ttot_dw
        double rat_tup = 1.0 - f_int[4];  // tup_dw / ttot_dw

        toa_rfl = toa_rfl / tg;

        double x_term  = (toa_rfl  - rpw) / ttot;
        double rfl_pix = x_term / (1. + sab * x_term); //calculation of SDR
        targetSample.set(rfl_pix);
    }

    /**
     * 5-D linear interpolation:
     *   returns spectral array [rpw, ttot, sab, rat_tdw, rat_tup, Kx_1, Kx_2]
     *   as a function of [vza, sza, phi, hsf, aot] from the interpolation of the MOMO absorption-free LUTs
     */
    private double[] interpol_lut_MOMO_kx(double vza, double sza, double phi, double hsf, double aot, int bandIndex) {
        return new double[7]; //TODO
    }

    static int getIndexBefore(double value, double[] array) {
        for (int i = 0; i < array.length; i++) {
            if (value < array[i]) {
                if (i != 0) {
                    return i-1;
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
        throw new IllegalArgumentException();
    }
}

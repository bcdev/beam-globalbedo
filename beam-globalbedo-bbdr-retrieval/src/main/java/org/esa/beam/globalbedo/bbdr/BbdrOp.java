/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.PixelOperator;

import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.StrictMath.toRadians;

/**
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
public class BbdrOp extends PixelOperator {

    private static final int SRC_LAND_MASK = 0;
    private static final int SRC_VZA = 1;
    private static final int SRC_SZA = 2;
    private static final int SRC_PHI = 3;
    private static final int SRC_HSF = 4;
    private static final int SRC_AOT = 5;
    private static final int SRC_AOT_ERR = 6;
    private static final int SRC_GAS = 7;
    private static final int SRC_TOA_RFL = 8;

    @SourceProduct
    private Product source;

    @SourceProduct
    private Product sdrSource;

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

        int ind_amf = getIndexBefore(amf, amfArray);
        double amf_p = (amf - amfArray[ind_amf]) / (amfArray[ind_amf + 1] - amfArray[ind_amf]);

        int ind_gas = getIndexBefore(gas, gasArray);
        double gas_p = (gas - gasArray[ind_gas]) / (gasArray[ind_gas + 1] - gasArray[ind_gas]);

        double[] tg = new double[sensor.getNumBands()];
        for (int i = 0; i < tg.length; i++) {
            tg[i] = (1.0 - amf_p) * (1.0 - gas_p) * lut_gas[ind_amf][ind_gas][i] +
                    gas_p * (1 - amf_p) * lut_gas[ind_amf][ind_gas + 1][i] +
                    (1. - gas_p) * amf_p * lut_gas[ind_amf + 1][ind_gas][i] +
                    amf_p * gas_p * lut_gas[ind_amf + 1][ind_gas + 1][i];
        }
        double[][][] kx_tg = new double[1][1][1];
        for (int a1 = 0; a1 < kx_tg.length; a1++) {
            for (int a2 = 0; a2 < kx_tg[a1].length; a2++) {
                for (int a3 = 0; a3 < kx_tg[a1][a2].length; a3++) {
                    kx_tg[a1][a2][a3] = (1. - amf_p) * (1. - gas_p) * kx_lut_gas[a1][a2][ind_amf][ind_gas][a3] +
                            gas_p * (1 - amf_p) * kx_lut_gas[a1][a2][ind_amf][ind_gas + 1][a3] +
                            (1. - gas_p) * amf_p * kx_lut_gas[a1][a2][ind_amf + 1][ind_gas][a3] +
                            amf_p * gas_p * kx_lut_gas[a1][a2][ind_amf + 1][ind_gas + 1][a3];
                }
            }
        }
        double cwv = 0;//TODO
        double ozo = 0;//TODO

        if (sensor.getCwv_ozo_flag() == 1) {
            cwv = gas;
        }else{
            ozo = gas;
        }

        double[][] f_int_all = interpol_lut_MOMO_kx(vza, sza, phi, hsf, aot);

        double[] rfl_pix = new double[sensor.getNumBands()];
        for (int i = 0; i < f_int_all.length; i++) {
            double[] f_int = f_int_all[i];

            double rpw     = f_int[0] * Math.PI / mus; // Path Radiance
            double ttot    = f_int[1]/mus;    // Total TOA flux (Isc*Tup*Tdw)
            double sab     = f_int[2];        // Spherical Albedo
            double rat_tdw = 1.0 - f_int[3];  // tdif_dw / ttot_dw
            double rat_tup = 1.0 - f_int[4];  // tup_dw / ttot_dw

            toa_rfl[i] = toa_rfl[i] / tg[i];

            double x_term  = (toa_rfl[i]  - rpw) / ttot;
            rfl_pix[i] = x_term / (1. + sab * x_term); //calculation of SDR
        }

        double rfl_red = rfl_pix[sensor.getIndexRed()];
        double rfl_nir = rfl_pix[sensor.getIndexNIR()];
        double norm_ndvi = 1.0 / (rfl_nir + rfl_red);
        double ndvi_land = (sensor.getBndvi() * rfl_nir - sensor.getAndvi() * rfl_red) * norm_ndvi;

        double delta_aot = sourceSamples[SRC_AOT_ERR].getDouble();
        for (int i = 0; i < sensor.getNumBands(); i++) {
            double[] f_int = f_int_all[i];
            double err_rad = sensor.getRadiometricError() * toa_rfl[i];

            double delta_cwv = sensor.getCwvError() * cwv;
            double delta_ozo = sensor.getOzoError() * ozo;

            double err_aod = abs((f_int[5] + f_int[6] * rfl_pix[i]) * delta_aot);
            double err_cwv = abs((kx_tg[0][0][i] + kx_tg[1][0][i] * rfl_pix[i]) * delta_cwv);
            double err_ozo = abs((kx_tg[0][1][i] + kx_tg[1][1][i] * rfl_pix[i]) * delta_ozo);

//            err_coreg = reform(err_coreg_land[ind_land, *])
        }

    }

    /**
     * 5-D linear interpolation:
     *   returns spectral array [rpw, ttot, sab, rat_tdw, rat_tup, Kx_1, Kx_2]
     *   as a function of [vza, sza, phi, hsf, aot] from the interpolation of the MOMO absorption-free LUTs
     */
    private double[][] interpol_lut_MOMO_kx(double vza, double sza, double phi, double hsf, double aot) {
        return new double[1][1];
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

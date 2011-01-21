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

import Jama.Matrix;
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

    private static final int TRG_ERRORS = 0;
    private static final int TRG_KERN = 6;

    private static final int n_spc = 3; // VIS, NIR, SW ; Broadband albedos
    private static final int n_kernel = 2; //(geo & vol)

    @SourceProduct
    private Product source;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    // auxdata
    private double[] amfArray;
    private double[] gasArray;
    private double[][][] lut_gas;
    private double[][][][][] kx_lut_gas;
    private double[][] nb_coef_arr_all; // = fltarr(n_spc, num_bd)
    private double[] nb_intcp_arr_all; // = fltarr(n_spc)
    private double[] rmse_arr_all; // = fltarr(n_spc)
    private double[][] nb_coef_arr_D; // = fltarr(n_spc, num_bd)
    private double[] nb_intcp_arr_D; //= fltarr(n_spc)
    private double kpp_vol;
    private double kpp_geo;


    @Override
    protected void configureTargetProduct(Product targetProduct) {
        //read auxdata, LUTs
//        {"BB_VIS", "BB_NIR", "BB_SW", "sig_BB_VIS_VIS", "sig_BB_VIS_NIR", "sig_BB_VIS_SW", "sig_BB_NIR_NIR", "sig_BB_NIR_SW", "sig_BB_SW_SW", "Kvol_BRDF_VIS", "Kvol_BRDF_NIR", "Kvol_BRDF_SW", "Kgeo_BRDF_VIS", "Kgeo_BRDF_NIR", "Kgeo_BRDF_SW", $
//"AOD550", "NDVI", "sig_NDVI", "VZA", "SZA", "RAA", "DEM", "snow_mask", l1_flg_str};
//        targetProduct.addBand();
        // TODO
        readAuxdata();
    }
    
    void readAuxdata() {
        
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
        } else {
            ozo = gas;
        }

        double[][] f_int_all = interpol_lut_MOMO_kx(vza, sza, phi, hsf, aot);

        double[] sab = new double[sensor.getNumBands()];
        double[] rat_tdw = new double[sensor.getNumBands()];
        double[] rat_tup = new double[sensor.getNumBands()];
        double[] rfl_pix = new double[sensor.getNumBands()];
        for (int i = 0; i < sensor.getNumBands(); i++) {
            double[] f_int = f_int_all[i];

            double rpw = f_int[0] * Math.PI / mus; // Path Radiance
            double ttot = f_int[1] / mus;    // Total TOA flux (Isc*Tup*Tdw)
            sab[i] = f_int[2];        // Spherical Albedo
            rat_tdw[i] = 1.0 - f_int[3];  // tdif_dw / ttot_dw
            rat_tup[i] = 1.0 - f_int[4];  // tup_dw / ttot_dw

            toa_rfl[i] = toa_rfl[i] / tg[i];

            double x_term = (toa_rfl[i] - rpw) / ttot;
            rfl_pix[i] = x_term / (1. + sab[i] * x_term); //calculation of SDR
        }

        double rfl_red = rfl_pix[sensor.getIndexRed()];
        double rfl_nir = rfl_pix[sensor.getIndexNIR()];
        double norm_ndvi = 1.0 / (rfl_nir + rfl_red);
        double ndvi_land = (sensor.getBndvi() * rfl_nir - sensor.getAndvi() * rfl_red) * norm_ndvi;

        double delta_aot = sourceSamples[SRC_AOT_ERR].getDouble();

        double[] err_rad = new double[sensor.getNumBands()];
        double[] err_aod = new double[sensor.getNumBands()];
        double[] err_cwv = new double[sensor.getNumBands()];
        double[] err_ozo = new double[sensor.getNumBands()];
        double[] err_coreg = new double[sensor.getNumBands()];

        for (int i = 0; i < sensor.getNumBands(); i++) {
            double[] f_int = f_int_all[i];
            err_rad[i] = sensor.getRadiometricError() * toa_rfl[i];

            double delta_cwv = sensor.getCwvError() * cwv;
            double delta_ozo = sensor.getOzoError() * ozo;

            err_aod[i] = abs((f_int[5] + f_int[6] * rfl_pix[i]) * delta_aot);
            err_cwv[i] = abs((kx_tg[0][0][i] + kx_tg[1][0][i] * rfl_pix[i]) * delta_cwv);
            err_ozo[i] = abs((kx_tg[0][1][i] + kx_tg[1][1][i] * rfl_pix[i]) * delta_ozo);

            // err_coreg = reform(err_coreg_land[ind_land, *])
            err_coreg[i] = 0; // TODO
        }

        Matrix err_aod_cov = matrixSquare(err_aod);
        Matrix err_cwv_cov = matrixSquare(err_cwv);
        Matrix err_ozo_cov = matrixSquare(err_ozo);
        Matrix err_coreg_cov = matrixSquare(err_coreg);

        Matrix err_rad_cov = new Matrix(sensor.getNumBands(), sensor.getNumBands());
        for (int i = 0; i < sensor.getNumBands(); i++) {
            err_rad_cov.set(i, i, err_rad[i] * err_rad[i]);
        }

        Matrix err2_tot_cov = err_aod_cov.plusEquals(err_cwv_cov).plusEquals(err_ozo_cov).plusEquals(err_rad_cov).plusEquals(err_coreg_cov);


        double ndviSum = sensor.getAndvi() + sensor.getBndvi();
        double sig_ndvi_land = pow(
                (pow(ndviSum * rfl_nir * sqrt(err2_tot_cov.get(sensor.getIndexRed(), sensor.getIndexRed())) * norm_ndvi * norm_ndvi, 2) +
                        pow(ndviSum * rfl_red * sqrt(err2_tot_cov.get(sensor.getIndexNIR(), sensor.getIndexNIR())) * norm_ndvi * norm_ndvi, 2)
                ), 0.5);

        // BB conversion and error var-cov calculation

        Matrix nb_coef_arr_all_m = new Matrix(nb_coef_arr_all);
        Matrix rfl_pix_m = new Matrix(rfl_pix, rfl_pix.length);
        Matrix nb_intcp_arr_all_m = new Matrix(nb_intcp_arr_all, nb_intcp_arr_all.length);

        Matrix bdr_mat_all = nb_coef_arr_all_m.times(rfl_pix_m).plus(nb_intcp_arr_all_m);
        Matrix err2_mat_rfl = nb_coef_arr_all_m.times(err2_tot_cov).times(nb_coef_arr_all_m.transpose());
        Matrix err2_n2b_all = new Matrix(n_spc, n_spc);
        for (int i = 0; i < n_spc; i++) {
            err2_n2b_all.set(i, i, rmse_arr_all[i] * rmse_arr_all[i]);
        }
        Matrix err_sum = err2_mat_rfl.plus(err2_n2b_all);
        int[] relevantErrIndices = {0, 1, 2, 4, 7, 8};
        double[] columnPackedCopy = err_sum.getColumnPackedCopy();
        for (int i = 0; i < relevantErrIndices.length; i++) {
            double value = columnPackedCopy[relevantErrIndices[i]];
            targetSamples[TRG_ERRORS + i].set(value);
        }

        // calculation of kernels (kvol, kgeo) & weighting with (1-Dup)(1-Ddw)

        double[][] f_int_nsky = interpol_lut_Nsky(sza, vza, hsf, aot);

        double vza_r = toRadians(vza);
        double sza_r = toRadians(sza);
        double phi_r = toRadians(phi);

        double mu_phi = cos(phi_r);
        double mu_ph_ang = mus * muv + sin(vza_r) * sin(sza_r) * mu_phi;
        double ph_ang = acos(mu_ph_ang);

        double kvol = ((PI/2.0 - ph_ang) * cos(ph_ang) + sin(ph_ang))/(mus + muv) - PI/4.0;

        double br = 1.0;
        double hb = 2.0;

        double tan_vp = tan(vza_r);
        double tan_sp = tan(sza_r);
        double sec_vp = 1./muv;
        double sec_sp = 1./mus;

        double D2 = tan_vp * tan_vp + tan_sp * tan_sp - 2 * tan_vp * tan_sp * mu_phi;

        double cost = hb * (pow((D2 + pow((tan_vp * tan_sp * sin(phi_r)),2)),0.5)) / (sec_vp + sec_sp);
        cost = min(cost, 1.0);
        double t = acos(cost);

        double ocap = (t - sin(t) * cost) * (sec_vp + sec_sp) / PI;

        double kgeo = 0.5 * (1. + mu_ph_ang) * sec_sp * sec_vp + ocap - sec_vp - sec_sp;

        // Nsky-weighted kernels
        Matrix rat_tdw_m = new Matrix(rat_tdw, rat_tdw.length);
        Matrix rat_tup_m = new Matrix(rat_tup, rat_tup.length);
        for (int i_bb = 0; i_bb < n_spc; i_bb++) {
            Matrix nb_coef_arr_D_m = new Matrix(nb_coef_arr_D[i_bb], nb_coef_arr_D[i_bb].length).transpose();
            Matrix m1 = nb_coef_arr_D_m.times(rat_tdw_m);
            double rat_tdw_bb = m1.get(0, 0) + nb_intcp_arr_D[i_bb];

            Matrix m2 = nb_coef_arr_D_m.times(rat_tup_m);
            double rat_tup_bb = m2.get(0, 0) + nb_intcp_arr_D[i_bb];

            // 1/(1-Delta_bb)=(1-rho*S)^2
            Matrix sab_m = new Matrix(sab, sab.length);
            Matrix m3 = nb_coef_arr_D_m.times(sab_m);
            double delta_bb_inv = (1.- bdr_mat_all.get(0, 0) * (m3.get(0, 0) + nb_intcp_arr_D[i_bb]));

            double t0 = (1. - rat_tdw_bb) * (1. - rat_tup_bb) * delta_bb_inv;
            double t1 = (1. - rat_tdw_bb) * rat_tup_bb * delta_bb_inv;
            double t2 = rat_tdw_bb * (1. - rat_tup_bb) * delta_bb_inv;
            double t3 = (rat_tdw_bb * rat_tup_bb - (1. - 1. / delta_bb_inv)) * delta_bb_inv;
            double kernel_land_0 = t0 * kvol + t1 * f_int_nsky[0][i_bb] + t2 * f_int_nsky[2][i_bb] + t3 * kpp_vol; // targetSample
            double kernel_land_1= t0 * kgeo + t1 * f_int_nsky[1][i_bb] + t2 * f_int_nsky[3][i_bb] + t3 * kpp_geo; // targetSample
            targetSamples[TRG_KERN + i_bb].set(kernel_land_0);
            targetSamples[TRG_KERN + i_bb + 1].set(kernel_land_1);
        }
    }

    /**
     * 5-D linear interpolation:
     * returns spectral array [rpw, ttot, sab, rat_tdw, rat_tup, Kx_1, Kx_2]
     * as a function of [vza, sza, phi, hsf, aot] from the interpolation of the MOMO absorption-free LUTs
     */
    private double[][] interpol_lut_MOMO_kx(double vza, double sza, double phi, double hsf, double aot) {
        return new double[1][1];
    }

    private double[][] interpol_lut_Nsky(double sza, double vza, double hsf, double aot) {
        return new double[1][1];
    }

    private static Matrix matrixSquare(double[] doubles) {
        Matrix matrix = new Matrix(doubles, doubles.length);
        return matrix.times(matrix);
    }

    static int getIndexBefore(double value, double[] array) {
        for (int i = 0; i < array.length; i++) {
            if (value < array[i]) {
                if (i != 0) {
                    return i - 1;
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }
        throw new IllegalArgumentException();
    }
}

/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.WritableSample;

import static java.lang.Math.*;
import static java.lang.StrictMath.toRadians;

/**
 * Computes SDR/BBDR and kernel parameters for VGT
 *
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
@OperatorMetadata(alias = "ga.bbdr.vgt",
        description = "Computes BBDRs and kernel parameters for VGT",
        authors = "Marco Zuehlke, Olaf Danne",
        version = "1.1",
        copyright = "(C) 2015 by Brockmann Consult")
public class BbdrVgtOp extends BbdrMasterOp {

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        if (!sourceSamples[SRC_LAND_MASK].getBoolean()) {
            // only compute over land
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }

        double vza = sourceSamples[SRC_VZA].getDouble();
        double vaa = sourceSamples[SRC_VAA].getDouble();
        double sza = sourceSamples[SRC_SZA].getDouble();
        double saa = sourceSamples[SRC_SAA].getDouble();
        double aot = sourceSamples[SRC_AOT].getDouble();
        double delta_aot = sourceSamples[SRC_AOT_ERR].getDouble();
        double hsf = sourceSamples[SRC_DEM].getDouble();

        hsf *= 0.001; // convert m to km
        if (hsf <= 0.0 && hsf >= -0.45) {
            hsf = aux.getHsfMin();
        }

        if (vza < aux.getVzaMin() || vza > aux.getVzaMax() ||
                sza < aux.getSzaMin() || sza > aux.getSzaMax() ||
                aot < aux.getAotMin() || aot > aux.getAotMax() ||
                hsf < aux.getHsfMin() || hsf > aux.getHsfMax()) {
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }
        targetSamples[TRG_SNOW].set(sourceSamples[SRC_SNOW_MASK].getInt());
        targetSamples[TRG_VZA].set(vza);
        targetSamples[TRG_SZA].set(sza);
        targetSamples[TRG_DEM].set(hsf);
        targetSamples[TRG_AOD].set(aot);
        targetSamples[TRG_AODERR].set(delta_aot);

        double ozo;
        double cwv;
        double gas;

//      CWV & OZO - provided as a constant value and the other as pixel-based, depending on the sensor
//      MERIS: OZO per-pixel, CWV as constant value
//      AATSR: OZO and CWV as constant value
//      VGT: CWV per-pixel, OZO as constant value
        ozo = aux.getGasLookupTable().getGasMeanVal();   // mean value from whole image
        cwv = sourceSamples[SRC_WVP].getDouble();
        cwv = min(cwv, 4.45);
        gas = cwv;

        double vza_r = toRadians(vza);
        double sza_r = toRadians(sza);
        double muv = cos(vza_r);
        double mus = cos(sza_r);
        double amf = 1.0 / muv + 1.0 / mus;

        double[] toa_rfl = new double[Sensor.VGT.getNumBands()];
        for (int i = 0; i < toa_rfl.length; i++) {
            double toaRefl = sourceSamples[SRC_TOA_RFL + i].getDouble();
            toaRefl /= Sensor.VGT.getCal2Meris()[i];
            toa_rfl[i] = toaRefl;
        }

        double phi = abs(saa - vaa);
        if (phi > 180.0) {
            phi = 360.0 - phi;
        }
        phi = min(phi, 179);
        phi = max(phi, 1);
        targetSamples[TRG_RAA].set(phi);

        float[] tg = aux.getGasLookupTable().getTg((float) amf, (float) gas);
        float[][][] kx_tg = aux.getGasLookupTable().getKxTg((float) amf, (float) gas);

        double[][] f_int_all = aux.interpol_lut_MOMO_kx(vza, sza, phi, hsf, aot);

        double[] sab = new double[Sensor.VGT.getNumBands()];
        double[] rat_tdw = new double[Sensor.VGT.getNumBands()];
        double[] rat_tup = new double[Sensor.VGT.getNumBands()];
        double[] rfl_pix = new double[Sensor.VGT.getNumBands()];
        for (int i = 0; i < Sensor.VGT.getNumBands(); i++) {
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

        double rfl_red = rfl_pix[Sensor.VGT.getIndexRed()];
        double rfl_nir = rfl_pix[Sensor.VGT.getIndexNIR()];
        double norm_ndvi = 1.0 / (rfl_nir + rfl_red);
        double ndvi_land = (Sensor.VGT.getBndvi() * rfl_nir - Sensor.VGT.getAndvi() * rfl_red) * norm_ndvi;
        targetSamples[TRG_NDVI].set(ndvi_land);

        double[] err_rad = new double[Sensor.VGT.getNumBands()];
        double[] err_aod = new double[Sensor.VGT.getNumBands()];
        double[] err_cwv = new double[Sensor.VGT.getNumBands()];
        double[] err_ozo = new double[Sensor.VGT.getNumBands()];
        double[] err_coreg = new double[Sensor.VGT.getNumBands()];

        for (int i = 0; i < Sensor.VGT.getNumBands(); i++) {
            double[] f_int = f_int_all[i];
            err_rad[i] = Sensor.VGT.getRadiometricError() * toa_rfl[i];

            double delta_cwv = Sensor.VGT.getCwvError() * cwv;
            double delta_ozo = Sensor.VGT.getOzoError() * ozo;

            err_aod[i] = abs((f_int[5] + f_int[6] * rfl_pix[i]) * delta_aot);
            err_cwv[i] = abs((kx_tg[i][0][0] + kx_tg[i][0][1] * rfl_pix[i]) * delta_cwv);
            err_ozo[i] = abs((kx_tg[i][1][0] + kx_tg[i][1][1] * rfl_pix[i]) * delta_ozo);

            err_coreg[i] = sourceSamples[SRC_TOA_VAR + i].getDouble();
            err_coreg[i] *= Sensor.VGT.getErrCoregScale();
        }

        Matrix err_aod_cov = BbdrUtils.matrixSquare(err_aod);
        Matrix err_cwv_cov = BbdrUtils.matrixSquare(err_cwv);
        Matrix err_ozo_cov = BbdrUtils.matrixSquare(err_ozo);
        Matrix err_coreg_cov = BbdrUtils.matrixSquare(err_coreg);

        Matrix err_rad_cov = new Matrix(Sensor.VGT.getNumBands(), Sensor.VGT.getNumBands());
        for (int i = 0; i < Sensor.VGT.getNumBands(); i++) {
            err_rad_cov.set(i, i, err_rad[i] * err_rad[i]);
        }

        Matrix err2_tot_cov = err_aod_cov.plusEquals(err_cwv_cov).plusEquals(err_ozo_cov).plusEquals(
                err_rad_cov).plusEquals(err_coreg_cov);

        // end of implementation which is same as for SDR. Now BBDR computation...

        double ndviSum = Sensor.VGT.getAndvi() + Sensor.VGT.getBndvi();
        double sig_ndvi_land = pow(
                (pow(ndviSum * rfl_nir * sqrt(
                        err2_tot_cov.get(Sensor.VGT.getIndexRed(), Sensor.VGT.getIndexRed())) * norm_ndvi * norm_ndvi, 2) +
                        pow(ndviSum * rfl_red * sqrt(
                                err2_tot_cov.get(Sensor.VGT.getIndexNIR(), Sensor.VGT.getIndexNIR())) * norm_ndvi * norm_ndvi, 2)
                ), 0.5);
        targetSamples[TRG_NDVI + 1].set(sig_ndvi_land);

        // BB conversion and error var-cov calculation

        Matrix rfl_pix_m = new Matrix(rfl_pix, rfl_pix.length);
        Matrix bdr_mat_all = aux.getNb_coef_arr_all().times(rfl_pix_m).plus(aux.getNb_intcp_arr_all());

        double[] bbdrsData = bdr_mat_all.getColumnPackedCopy();
        for (int i = 0; i < bbdrsData.length; i++) {
            targetSamples[i].set(bbdrsData[i]);
        }

        Matrix err2_mat_rfl = aux.getNb_coef_arr_all().times(err2_tot_cov).times(aux.getNb_coef_arr_all().transpose());
        Matrix err2_n2b_all = new Matrix(BbdrConstants.N_SPC, BbdrConstants.N_SPC);
        for (int i = 0; i < BbdrConstants.N_SPC; i++) {
            err2_n2b_all.set(i, i, aux.getRmse_arr_all()[i] * aux.getRmse_arr_all()[i]);
        }
        Matrix err_sum = err2_mat_rfl.plus(err2_n2b_all);

        int[] relevantErrIndices = {0, 1, 2, 4, 7, 8};
        double[] columnPackedCopy = err_sum.getColumnPackedCopy();
        for (int i = 0; i < relevantErrIndices.length; i++) {
            final double err_final = sqrt(columnPackedCopy[relevantErrIndices[i]]);
            targetSamples[TRG_ERRORS + i].set(err_final);
        }

        // calculation of kernels (kvol, kgeo) & weighting with (1-Dup)(1-Ddw)

        double[][] f_int_nsky = aux.interpol_lut_Nsky(sza, vza, hsf, aot);

        double phi_r = toRadians(phi);

        double mu_phi = cos(phi_r);
        double mu_ph_ang = mus * muv + sin(vza_r) * sin(sza_r) * mu_phi;
        double ph_ang = acos(mu_ph_ang);

        double kvol = ((PI / 2.0 - ph_ang) * cos(ph_ang) + sin(ph_ang)) / (mus + muv) - PI / 4.0;

        double hb = 2.0;

        double tan_vp = tan(vza_r);
        double tan_sp = tan(sza_r);
        double sec_vp = 1. / muv;
        double sec_sp = 1. / mus;

        double D2 = tan_vp * tan_vp + tan_sp * tan_sp - 2 * tan_vp * tan_sp * mu_phi;

        double cost = hb * (pow((D2 + pow((tan_vp * tan_sp * sin(phi_r)), 2)), 0.5)) / (sec_vp + sec_sp);
        cost = min(cost, 1.0);
        double t = acos(cost);

        double ocap = (t - sin(t) * cost) * (sec_vp + sec_sp) / PI;

        double kgeo = 0.5 * (1. + mu_ph_ang) * sec_sp * sec_vp + ocap - sec_vp - sec_sp;

        // Nsky-weighted kernels
        Matrix rat_tdw_m = new Matrix(rat_tdw, rat_tdw.length);
        Matrix rat_tup_m = new Matrix(rat_tup, rat_tup.length);
        for (int i_bb = 0; i_bb < BbdrConstants.N_SPC; i_bb++) {
            Matrix nb_coef_arr_D_m = aux.getNb_coef_arr()[i_bb];
            Matrix m1 = nb_coef_arr_D_m.times(rat_tdw_m);
            double rat_tdw_bb = m1.get(0, 0) + aux.getNb_intcp_arr_D()[i_bb];

            Matrix m2 = nb_coef_arr_D_m.times(rat_tup_m);
            double rat_tup_bb = m2.get(0, 0) + aux.getNb_intcp_arr_D()[i_bb];

            // 1/(1-Delta_bb)=(1-rho*S)^2
            Matrix sab_m = new Matrix(sab, sab.length);
            Matrix m3 = nb_coef_arr_D_m.times(sab_m);
            double delta_bb_inv = pow((1. - bdr_mat_all.get(0, 0) * (m3.get(0, 0) + aux.getNb_intcp_arr_D()[i_bb])), 2);

            double t0 = (1. - rat_tdw_bb) * (1. - rat_tup_bb) * delta_bb_inv;
            double t1 = (1. - rat_tdw_bb) * rat_tup_bb * delta_bb_inv;
            double t2 = rat_tdw_bb * (1. - rat_tup_bb) * delta_bb_inv;
            double t3 = (rat_tdw_bb * rat_tup_bb - (1. - 1. / delta_bb_inv)) * delta_bb_inv;
            double kernel_land_0 = t0 * kvol + t1 * f_int_nsky[i_bb][0] + t2 * f_int_nsky[i_bb][2] + t3 * aux.getKpp_vol();
            double kernel_land_1 = t0 * kgeo + t1 * f_int_nsky[i_bb][1] + t2 * f_int_nsky[i_bb][3] + t3 * aux.getKpp_geo();
            targetSamples[TRG_KERN + (i_bb * 2)].set(kernel_land_0);
            targetSamples[TRG_KERN + (i_bb * 2) + 1].set(kernel_land_1);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BbdrVgtOp.class);
        }
    }
}

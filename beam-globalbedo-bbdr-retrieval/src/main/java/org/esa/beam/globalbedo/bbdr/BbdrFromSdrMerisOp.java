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
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.gpf.operators.standard.BandMathsOp;

import static java.lang.Math.*;
import static java.lang.StrictMath.toRadians;

/**
 * Computes SDR/BBDR and kernel parameters for MERIS
 *
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
@OperatorMetadata(alias = "ga.bbdr.from.sdr.meris",
        description = "Computes BBDRs and kernel parameters for MERIS, using previously computed SDR as input",
        authors = "Marco Zuehlke, Olaf Danne",
        version = "1.1",
        copyright = "(C) 2015 by Brockmann Consult")
public class BbdrFromSdrMerisOp extends BbdrMasterOp {

    private static final int SRC_VZA = 50;
    private static final int SRC_SZA = 51;
    private static final int SRC_VAA = 52;
    private static final int SRC_SAA = 53;
    private static final int SRC_DEM = 54;
    private static final int SRC_AOD = 55;
    private static final int SRC_AOD_ERR = 56;

    private static final int SRC_LAND_MASK = 60;
    private static final int SRC_SNOW_MASK = 61;

    private static final int SRC_STATUS = 70;

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) {
        final String commonLandExpr;
        if (landExpression != null && !landExpression.isEmpty()) {
            commonLandExpr = landExpression;
        } else {
            commonLandExpr = sensor.getLandExpr();
        }

        final String snowMaskExpression = "cloud_classif_flags.F_CLEAR_SNOW";

        BandMathsOp.BandDescriptor bdSnow = new BandMathsOp.BandDescriptor();
        bdSnow.name = "snow_mask";
        bdSnow.expression = snowMaskExpression;
        bdSnow.type = ProductData.TYPESTRING_INT8;

        BandMathsOp snowOp = new BandMathsOp();
        snowOp.setParameterDefaultValues();
        snowOp.setSourceProduct(sourceProduct);
        snowOp.setTargetBandDescriptors(bdSnow);
        Product snowMaskProduct = snowOp.getTargetProduct();

        configurator.defineSample(SRC_SNOW_MASK, snowMaskProduct.getBandAt(0).getName(), snowMaskProduct);

        BandMathsOp.BandDescriptor bdLand = new BandMathsOp.BandDescriptor();
        bdLand.name = "land_mask";
        bdLand.expression = commonLandExpr;
        bdLand.type = ProductData.TYPESTRING_INT8;

        BandMathsOp landOp = new BandMathsOp();
        landOp.setParameterDefaultValues();
        landOp.setSourceProduct(sourceProduct);
        landOp.setTargetBandDescriptors(bdLand);
        Product landMaskProduct = landOp.getTargetProduct();

        configurator.defineSample(SRC_LAND_MASK, landMaskProduct.getBandAt(0).getName(), landMaskProduct);


        for (int i = 0; i < sensor.getSdrBandNames().length; i++) {
            configurator.defineSample(i, sensor.getSdrBandNames()[i], sourceProduct);
        }

        int index = sensor.getSdrBandNames().length;
        for (int i = 0; i < sensor.getSdrErrorBandNames().length; i++) {
            configurator.defineSample(index + i, sensor.getSdrErrorBandNames()[i], sourceProduct);
        }

        index += sensor.getSdrErrorBandNames().length;
        configurator.defineSample(SRC_VZA, "VZA", sourceProduct);
        configurator.defineSample(SRC_SZA, "SZA", sourceProduct);
        configurator.defineSample(SRC_VAA, "VAA", sourceProduct);
        configurator.defineSample(SRC_SAA, "SAA", sourceProduct);
        configurator.defineSample(SRC_DEM, "DEM", sourceProduct);
        configurator.defineSample(SRC_AOD, "AOD550", sourceProduct);
        configurator.defineSample(SRC_AOD_ERR, "sig_AOD550", sourceProduct);
        configurator.defineSample(SRC_STATUS, "status", sourceProduct);
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        final int status = sourceSamples[SRC_STATUS].getInt();
        if (x == 500 && y == 1000) {
            System.out.println("status = " + status);
        }
        if (!singlePixelMode) {
            targetSamples[TRG_SNOW].set(sourceSamples[SRC_SNOW_MASK].getInt());

            if (!sourceSamples[SRC_LAND_MASK].getBoolean()) {
//            if (status != 1 && status != 3) {
                // only compute over land or snow
                BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
                return;
            }
        }

        double vza = sourceSamples[SRC_VZA].getDouble();
        if (Double.isNaN(vza)) {
            // todo: implement correct filtering of land/snow pixels only!!
            return;
        }
        double vaa = sourceSamples[SRC_VAA].getDouble();
        double sza = sourceSamples[SRC_SZA].getDouble();
        double saa = sourceSamples[SRC_SAA].getDouble();
        double aot;
        double delta_aot;
        if (useAotClimatology) {
            aot = 0.15;  // reasonable constant for the moment
            // todo: really use 'climatology_ratios.nc' from A.Heckel (collocated as slave with given source product)
            delta_aot = 0.0;
        } else {
            aot = sourceSamples[SRC_AOT].getDouble();
            delta_aot = sourceSamples[SRC_AOT_ERR].getDouble();
        }

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
        targetSamples[TRG_VZA].set(vza);
        targetSamples[TRG_SZA].set(sza);
        targetSamples[TRG_DEM].set(hsf);

        targetSamples[TRG_AOD].set(aot);
        targetSamples[TRG_AODERR].set(delta_aot);


        // end of implementation which is same as for SDR. Now BBDR computation...

        // prepare SDR --> BBDR computation...
        double[] rfl_pix = new double[Sensor.MERIS.getNumBands()];
         for (int i = 0; i < Sensor.MERIS.getNumBands(); i++) {
            rfl_pix[i] = sourceSamples[i].getDouble();
         }

        double rfl_red = rfl_pix[Sensor.MERIS.getIndexRed()];
        double rfl_nir = rfl_pix[Sensor.MERIS.getIndexNIR()];

        double norm_ndvi = 1.0 / (rfl_nir + rfl_red);
        double ndvi_land = (Sensor.MERIS.getBndvi() * rfl_nir - Sensor.MERIS.getAndvi() * rfl_red) * norm_ndvi;
        targetSamples[TRG_NDVI].set(ndvi_land);

        Matrix err2_tot_cov = BbdrUtils.matrixSquare(new double[Sensor.MERIS.getNumBands()]);
         for (int i = 0; i < Sensor.MERIS.getNumBands(); i++) {
             err2_tot_cov.set(i, i, sourceSamples[Sensor.MERIS.getNumBands() + i].getDouble());
         }

        double vza_r = toRadians(vza);
        double sza_r = toRadians(sza);
        double muv = cos(vza_r);
        double mus = cos(sza_r);

        double phi = abs(saa - vaa);
        if (phi > 180.0) {
            phi = 360.0 - phi;
        }
        phi = max(min(phi, 179), 1);

        double[] sab = new double[Sensor.MERIS.getNumBands()];
        double[] rat_tdw = new double[Sensor.MERIS.getNumBands()];
        double[] rat_tup = new double[Sensor.MERIS.getNumBands()];
        double[][] f_int_all = aux.interpol_lut_MOMO_kx(vza, sza, phi, hsf, aot);
        for (int i = 0; i < Sensor.MERIS.getNumBands(); i++) {
            double[] f_int = f_int_all[i];
            sab[i] = f_int[2];        // Spherical Albedo
            rat_tdw[i] = 1.0 - f_int[3];  // tdif_dw / ttot_dw
            rat_tup[i] = 1.0 - f_int[4];  // tup_dw / ttot_dw
        }

        // start SDR --> BBDR computation

        double ndviSum = Sensor.MERIS.getAndvi() + Sensor.MERIS.getBndvi();
        double sig_ndvi_land = pow(
                (pow(ndviSum * rfl_nir * sqrt(
                        err2_tot_cov.get(Sensor.MERIS.getIndexRed(), Sensor.MERIS.getIndexRed())) * norm_ndvi * norm_ndvi, 2) +
                        pow(ndviSum * rfl_red * sqrt(
                                err2_tot_cov.get(Sensor.MERIS.getIndexNIR(), Sensor.MERIS.getIndexNIR())) * norm_ndvi * norm_ndvi, 2)
                ), 0.5);
        targetSamples[TRG_NDVI + 1].set(sig_ndvi_land);

        // BB conversion and error var-cov calculation

        Matrix rfl_pix_m = new Matrix(rfl_pix, rfl_pix.length);
        Matrix bdr_mat_all = aux.getNb_coef_arr_all().times(rfl_pix_m).plus(aux.getNb_intcp_arr_all());

        double[] bbdrData = bdr_mat_all.getColumnPackedCopy();
        for (int i = 0; i < bbdrData.length; i++) {
            targetSamples[i].set(bbdrData[i]);
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
            super(BbdrFromSdrMerisOp.class);
        }
    }
}

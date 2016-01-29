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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.pointop.ProductConfigurer;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.WritableSample;
import org.esa.beam.landcover.StatusPostProcessOp;
import org.esa.beam.landcover.UclCloudDetection;

import java.io.IOException;

import static java.lang.Math.*;
import static java.lang.StrictMath.toRadians;

/**
 * Computes SDRs for VGT
 *
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
@OperatorMetadata(alias = "ga.sdr.vgt",
        description = "Computes SDRs for VGT",
        authors = "Marco Zuehlke, Olaf Danne",
        version = "1.1",
        copyright = "(C) 2015 by Brockmann Consult")
public class SdrVgtOp extends BbdrMasterOp {

    @Parameter(defaultValue = "false")
    private boolean writeGeometryAndAOT;


    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();
        if (writeGeometryAndAOT) {
            String[] bandNames = {
                    "VZA", "SZA", "VAA", "SAA",
                    "DEM", "AOD550", "sig_AOD550", "OG", "WVG"
            };
            for (String bandName : bandNames) {
                Band band = targetProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
                band.setNoDataValue(Float.NaN);
                band.setNoDataValueUsed(true);
            }
        }
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) {
        super.configureTargetSamples(configurator);

        if (writeGeometryAndAOT) {
            configurator.defineSample(TRG_VZA, "VZA");
            configurator.defineSample(TRG_SZA, "SZA");
            configurator.defineSample(TRG_VAA, "VAA");
            configurator.defineSample(TRG_SAA, "SAA");
            configurator.defineSample(TRG_DEM, "DEM");
            configurator.defineSample(TRG_AOD, "AOD550");
            configurator.defineSample(TRG_AODERR, "sig_AOD550");
            configurator.defineSample(TRG_OG, "OG");
            configurator.defineSample(TRG_WVG, "WVG");
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        if (x == 100 && y == 500 ) {
            System.out.println("x = " + x);
        }
        if (writeGeometryAndAOT) {
            // copy these source pixels in any case
            targetSamples[TRG_SZA].set(sourceSamples[SRC_SZA].getDouble());
            targetSamples[TRG_VZA].set(sourceSamples[SRC_VZA].getDouble());
            targetSamples[TRG_SAA].set(sourceSamples[SRC_SAA].getDouble());
            targetSamples[TRG_VAA].set(sourceSamples[SRC_VAA].getDouble());
            targetSamples[TRG_DEM].set(sourceSamples[SRC_DEM].getDouble());
            targetSamples[TRG_AOD].set(sourceSamples[SRC_AOT].getDouble());
            targetSamples[TRG_AODERR].set(sourceSamples[SRC_AOT_ERR].getDouble());
            targetSamples[TRG_OG].set(sourceSamples[SRC_OZO].getDouble());
            targetSamples[TRG_WVG].set(sourceSamples[SRC_WVP].getDouble());
        }

        final int status;
        status = sourceSamples[SRC_STATUS].getInt();
        if (status == StatusPostProcessOp.STATUS_WATER) {
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            // water, do simple atmospheric correction
            targetSamples[TRG_SDR_STATUS].set(status);
            return;
        } else if (status != StatusPostProcessOp.STATUS_LAND && status != StatusPostProcessOp.STATUS_SNOW) {
            // not land and not snow
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            targetSamples[TRG_SDR_STATUS].set(status);
            return;
        }
        targetSamples[TRG_SDR_STATUS].set(status);

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
            // write status
            targetSamples[TRG_SDR_STATUS].set(StatusPostProcessOp.STATUS_INVALID);
            return;
        }

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
            if (toaRefl == 0.0 || Double.isNaN(toaRefl)) {
                // if toa_refl look bad, set to invalid
                targetSamples[TRG_SDR_STATUS].set(StatusPostProcessOp.STATUS_INVALID);
            }
            toaRefl /= Sensor.VGT.getCal2Meris()[i];
            toa_rfl[i] = toaRefl;
        }

        double phi = abs(saa - vaa);
        if (phi > 180.0) {
            phi = 360.0 - phi;
        }
        phi = min(phi, 179);
        phi = max(phi, 1);

        float[] tg = aux.getGasLookupTable().getTg((float) amf, (float) gas);
        float[][][] kx_tg = aux.getGasLookupTable().getKxTg((float) amf, (float) gas);

        double[][] f_int_all = aux.interpol_lut_MOMO_kx(vza, sza, phi, hsf, aot);

        double[] sab = new double[Sensor.VGT.getNumBands()];
        double[] rfl_pix = new double[Sensor.VGT.getNumBands()];
        for (int i = 0; i < Sensor.VGT.getNumBands(); i++) {
            double[] f_int = f_int_all[i];

            double rpw = f_int[0] * Math.PI / mus; // Path Radiance
            double ttot = f_int[1] / mus;    // Total TOA flux (Isc*Tup*Tdw)
            sab[i] = f_int[2];        // Spherical Albedo

            toa_rfl[i] = toa_rfl[i] / tg[i];

            double x_term = (toa_rfl[i] - rpw) / ttot;
            rfl_pix[i] = x_term / (1. + sab[i] * x_term); //calculation of SDR
            targetSamples[i].set(rfl_pix[i]);
        }

        if (x == 900 && y == 100) {
            System.out.println("x = " + x);
        }

        double rfl_red = rfl_pix[Sensor.VGT.getIndexRed()];
        double rfl_nir = rfl_pix[Sensor.VGT.getIndexNIR()];
        double norm_ndvi = 1.0 / (rfl_nir + rfl_red);
        double ndvi_land = (Sensor.VGT.getBndvi() * rfl_nir - Sensor.VGT.getAndvi() * rfl_red) * norm_ndvi;
        targetSamples[TRG_SDR_NDVI].set(ndvi_land);

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

        for (int i = 0; i < Sensor.VGT.getNumBands(); i++) {
            targetSamples[Sensor.VGT.getNumBands() + i].set(err2_tot_cov.get(i, i));
        }

        // end of implementation needed for SDR

    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SdrVgtOp.class);
        }
    }
}

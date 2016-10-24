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
 * Computes SDRs for MERIS
 *
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
@OperatorMetadata(alias = "ga.sdr.meris",
        description = "Computes SDRs for MERIS",
        authors = "Marco Zuehlke, Olaf Danne",
        version = "1.1",
        copyright = "(C) 2015 by Brockmann Consult")
public class SdrMerisOp extends BbdrMasterOp {

    @Parameter(defaultValue = "true")
    private boolean doUclCloudDetection;

    @Parameter(defaultValue = "false")
    private boolean writeGeometryAndAOT;

    private UclCloudDetection uclCloudDetection;

    private static final double[] PATH_RADIANCE = new double[]{
            0.134, 0.103, 0.070, 0.059, 0.040,
            0.027, 0.022, 0.021, 0.018, 0.015,
            Double.NaN, 0.014, 0.010, 0.009, 0.008};
    private static final double[] TRANSMISSION = new double[]{
            0.65277, 0.71155, 0.77224, 0.78085, 0.78185,
            0.81036, 0.86705, 0.88244, 0.88342, 0.92075,
            Double.NaN, 0.93152, 0.9444, 0.9422, 0.58212
    };

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        if (doUclCloudDetection) {
            try {
                uclCloudDetection = UclCloudDetection.create();
            } catch (IOException e) {
                throw new OperatorException(e);
            }
        }
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) {
        super.configureSourceSamples(configurator);
        configurator.defineSample(SRC_STATUS + 1, "dem_alt");
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();
        if (writeGeometryAndAOT) {
            String[] bandNames = {
                    "VZA", "SZA", "VAA", "SAA",
                    "DEM", "AOD550", "sig_AOD550"
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
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        if (x == 60 && y ==1060) {
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
        }

        final int status = sourceSamples[SRC_STATUS].getInt();
        if (status == StatusPostProcessOp.STATUS_WATER) {
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            // water, do simple atmospheric correction
            double sdr13;
            if (computeSdrEverywhere || sourceSamples[SRC_STATUS + 1].getDouble() > -100) {
                // dem_alt from TP includes sea depth
                sdr13 = (sourceSamples[SRC_TOA_RFL + 12].getDouble() - PATH_RADIANCE[12]) / TRANSMISSION[12];
                for (int i = 0; i < Sensor.MERIS.getNumBands(); i++) {
                    double sdr = (sourceSamples[SRC_TOA_RFL + i].getDouble() - PATH_RADIANCE[i]) / TRANSMISSION[i];
                    sdr = sdr - sdr13;  // normalize
                    targetSamples[i].set(sdr);
                }
            } else {
                targetSamples[TRG_SDR_STATUS].set(status);
                return;
            }

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
        ozo = 0.001 * sourceSamples[SRC_OZO].getDouble();
        cwv = BbdrConstants.CWV_CONSTANT_VALUE;  // constant mean value of 1.5
        gas = ozo;

        double vza_r = toRadians(vza);
        double sza_r = toRadians(sza);
        double muv = cos(vza_r);
        double mus = cos(sza_r);
        double amf = 1.0 / muv + 1.0 / mus;

        double[] toa_rfl = new double[Sensor.MERIS.getNumBands()];
        for (int i = 0; i < toa_rfl.length; i++) {
            double toaRefl = sourceSamples[SRC_TOA_RFL + i].getDouble();
            if (toaRefl == 0.0 || Double.isNaN(toaRefl)) {
                // if toa_refl look bad, set to invalid
                targetSamples[TRG_SDR_STATUS].set(StatusPostProcessOp.STATUS_INVALID);
            }
            toaRefl /= Sensor.MERIS.getCal2Meris()[i];
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
        if (f_int_all == null) {
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }

        double[] sab = new double[Sensor.MERIS.getNumBands()];
        double[] rfl_pix = new double[Sensor.MERIS.getNumBands()];
        for (int i = 0; i < Sensor.MERIS.getNumBands(); i++) {
            double[] f_int = f_int_all[i];

            double rpw = f_int[0] * Math.PI / mus; // Path Radiance
            double ttot = f_int[1] / mus;    // Total TOA flux (Isc*Tup*Tdw)
            sab[i] = f_int[2];        // Spherical Albedo

            toa_rfl[i] = toa_rfl[i] / tg[i];

            double x_term = (toa_rfl[i] - rpw) / ttot;
            rfl_pix[i] = x_term / (1. + sab[i] * x_term); //calculation of SDR
            targetSamples[i].set(rfl_pix[i]);
        }

        if (status == StatusPostProcessOp.STATUS_LAND) {
            if (uclCloudDetection != null) {
                //do an additional cloud check on the SDRs (only over land)
                float sdrRed = (float) rfl_pix[6]; //sdr_7
                float sdrGreen = (float) rfl_pix[13]; //sdr_14
                float sdrBlue = (float) rfl_pix[2]; //sdr_3
                if (uclCloudDetection.isCloud(sdrRed, sdrGreen, sdrBlue)) {
                    targetSamples[TRG_SDR_STATUS].set(StatusPostProcessOp.STATUS_UCL_CLOUD);
                }
            }
        }

        double rfl_red = rfl_pix[Sensor.MERIS.getIndexRed()];
        double rfl_nir = rfl_pix[Sensor.MERIS.getIndexNIR()];
        double norm_ndvi = 1.0 / (rfl_nir + rfl_red);
        double ndvi_land = (Sensor.MERIS.getBndvi() * rfl_nir - Sensor.MERIS.getAndvi() * rfl_red) * norm_ndvi;
        targetSamples[TRG_SDR_NDVI].set(ndvi_land);

        double[] err_rad = new double[Sensor.MERIS.getNumBands()];
        double[] err_aod = new double[Sensor.MERIS.getNumBands()];
        double[] err_cwv = new double[Sensor.MERIS.getNumBands()];
        double[] err_ozo = new double[Sensor.MERIS.getNumBands()];
        double[] err_coreg = new double[Sensor.MERIS.getNumBands()];

        for (int i = 0; i < Sensor.MERIS.getNumBands(); i++) {
            double[] f_int = f_int_all[i];
            err_rad[i] = Sensor.MERIS.getRadiometricError() * toa_rfl[i];

            double delta_cwv = Sensor.MERIS.getCwvError() * cwv;
            double delta_ozo = Sensor.MERIS.getOzoError() * ozo;

            err_aod[i] = abs((f_int[5] + f_int[6] * rfl_pix[i]) * delta_aot);
            err_cwv[i] = abs((kx_tg[i][0][0] + kx_tg[i][0][1] * rfl_pix[i]) * delta_cwv);
            err_ozo[i] = abs((kx_tg[i][1][0] + kx_tg[i][1][1] * rfl_pix[i]) * delta_ozo);

            err_coreg[i] = sourceSamples[SRC_TOA_VAR + i].getDouble();
            err_coreg[i] *= Sensor.MERIS.getErrCoregScale();
        }

        Matrix err_aod_cov = BbdrUtils.matrixSquare(err_aod);
        Matrix err_cwv_cov = BbdrUtils.matrixSquare(err_cwv);
        Matrix err_ozo_cov = BbdrUtils.matrixSquare(err_ozo);
        Matrix err_coreg_cov = BbdrUtils.matrixSquare(err_coreg);

        Matrix err_rad_cov = new Matrix(Sensor.MERIS.getNumBands(), Sensor.MERIS.getNumBands());
        for (int i = 0; i < Sensor.MERIS.getNumBands(); i++) {
            err_rad_cov.set(i, i, err_rad[i] * err_rad[i]);
        }

        Matrix err2_tot_cov = err_aod_cov.plusEquals(err_cwv_cov).plusEquals(err_ozo_cov).plusEquals(
                err_rad_cov).plusEquals(err_coreg_cov);

        for (int i = 0; i < Sensor.MERIS.getNumBands(); i++) {
            targetSamples[Sensor.MERIS.getNumBands() + i].set(err2_tot_cov.get(i, i));
        }

        // end of implementation needed for SDR
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SdrMerisOp.class);
        }
    }
}

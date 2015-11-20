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
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.landcover.StatusPostProcessOp;
import org.esa.beam.landcover.UclCloudDetection;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.FracIndex;
import org.esa.beam.util.math.LookupTable;

import java.io.IOException;

import static java.lang.Math.*;
import static java.lang.StrictMath.toRadians;

/**
 * Computes SDR/BBDR and kernel parameters
 * // todo: make this an abstract class for BBDR computation. Implement for specific sensors.
 *
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
@OperatorMetadata(alias = "ga.sdr",
        description = "Computes BBDRs and kernel parameters",
        authors = "Marco Zuehlke, Olaf Danne",
        version = "1.1",
        copyright = "(C) 2015 by Brockmann Consult")
public class AbstractSdrOp extends PixelOperator {

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "")
    private Sensor sensor;

    @Parameter
    private String landExpression;

    private static final int SRC_LAND_MASK = 0;
    private static final int SRC_SNOW_MASK = 1;
    private static final int SRC_VZA = 2;
    private static final int SRC_VAA = 3;
    private static final int SRC_SZA = 4;
    private static final int SRC_SAA = 5;
    private static final int SRC_DEM = 6;
    private static final int SRC_AOT = 7;
    private static final int SRC_AOT_ERR = 8;
    private static final int SRC_OZO = 9;
    private static final int SRC_WVP = 10;
    private static final int SRC_TOA_RFL = 11;
    private int SRC_TOA_VAR;
    private int SRC_STATUS;

    private static final int TRG_ERRORS = 3;
    private static final int TRG_KERN = 9;
    private static final int TRG_NDVI = 15;
    private static final int TRG_VZA = 17;
    private static final int TRG_SZA = 18;
    private static final int TRG_RAA = 19;
    private static final int TRG_DEM = 20;
    private static final int TRG_SNOW = 21;
    private static final int TRG_AOD = 22;

    private static final int TRG_AODERR = 23;

    // Auxdata
//    private Matrix nb_coef_arr_all; // = fltarr(n_spc, num_bd)
//    private Matrix nb_intcp_arr_all; // = fltarr(n_spc)
//    private double[] rmse_arr_all; // = fltarr(n_spc)
//    private Matrix[] nb_coef_arr; // = fltarr(n_spc, num_bd)
//    private double[] nb_intcp_arr_D; //= fltarr(n_spc)
//    private double kpp_vol;
//    private double kpp_geo;
//
//    private AotLookupTable aotLut;
//    private LookupTable kxAotLut;
//    private GasLookupTable gasLookupTable;
//    private NskyLookupTable nskyDwLut;
//    private NskyLookupTable nskyUpLut;
//
//    private double vzaMin;
//    private double vzaMax;
//    private double szaMin;
//    private double szaMax;
//    private double aotMin;
//    private double aotMax;
//    private double hsfMin;
//    private double hsfMax;

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
    private Product varianceProduct;
    private String commonLandExpr;

    private BbdrAuxdata aux;

    @Override
    protected void prepareInputs() throws OperatorException {
        super.prepareInputs();
        aux = BbdrAuxdata.getInstance(sourceProduct, sensor);
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();

        // copy flag coding and flag images
        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        targetProduct.setAutoGrouping("sdr_error:sdr");
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) {
        commonLandExpr = landExpression;
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

        ImageVarianceOp imageVarianceOp = new ImageVarianceOp();
        imageVarianceOp.setParameterDefaultValues();
        imageVarianceOp.setSourceProduct(sourceProduct);
        imageVarianceOp.setParameter("sensor", sensor);
        varianceProduct = imageVarianceOp.getTargetProduct();

        String l1InvalidExpression = "";
        if (sensor == Sensor.MERIS) {
            // cloud_classif_flags.F_INVALID is triggered, id any radiance == zero
            l1InvalidExpression = "l1_flags.INVALID OR l1_flags.COSMETIC OR cloud_classif_flags.F_INVALID";
        } else if (sensor == Sensor.VGT) {
            l1InvalidExpression = "!SM.B0_GOOD OR !SM.B2_GOOD OR !SM.B3_GOOD OR (!SM.MIR_GOOD AND MIR > 0.65)";
        } else if (sensor == Sensor.PROBAV) {
            l1InvalidExpression = "!SM.B0_GOOD OR !SM.B2_GOOD OR !SM.B3_GOOD OR (!SM.MIR_GOOD AND MIR > 0.65)";
            l1InvalidExpression =
                    "!SM_FLAGS.GOOD_BLUE OR !SM_FLAGS.GOOD_RED OR !SM_FLAGS.GOOD_NIR OR (!SM_FLAGS.GOOD_SWIR AND TOA_REFL_SWIR > 0.65)";
        }

        String statusExpression = l1InvalidExpression + " ? 0 : (cloud_classif_flags.F_CLOUD ? 4 :" +
                "((cloud_classif_flags.F_CLEAR_SNOW) ? 3 :" +
                "((cloud_classif_flags.F_WATER) ? 2 : 1)))";
        BandMathsOp.BandDescriptor statusBd = new BandMathsOp.BandDescriptor();
        statusBd.name = "status";
        statusBd.expression = statusExpression;
        statusBd.type = ProductData.TYPESTRING_INT8;

        BandMathsOp bandMathsOp = new BandMathsOp();
        bandMathsOp.setParameterDefaultValues();
        bandMathsOp.setTargetBandDescriptors(statusBd);
        bandMathsOp.setSourceProduct(sourceProduct);
        Product statusProduct = bandMathsOp.getTargetProduct();

        configurator.defineSample(SRC_STATUS, "status", statusProduct);
        if (sensor == Sensor.MERIS) {
            configurator.defineSample(SRC_STATUS + 1, "dem_alt");
        }

    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) {
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        int status = StatusPostProcessOp.STATUS_INVALID;
        status = sourceSamples[SRC_STATUS].getInt();
        if (status == StatusPostProcessOp.STATUS_WATER) {
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            // water, do simple atmospheric correction
            targetSamples[sensor.getNumBands() * 2 + 2].set(status);
            return;
        } else if (status != StatusPostProcessOp.STATUS_LAND && status != StatusPostProcessOp.STATUS_SNOW) {
            // not land and not snow
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            targetSamples[sensor.getNumBands() * 2 + 2].set(status);
            return;
        }
        targetSamples[sensor.getNumBands() * 2 + 2].set(status);
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
            targetSamples[sensor.getNumBands() * 2 + 2].set(StatusPostProcessOp.STATUS_INVALID);
            return;
        }
        targetSamples[sensor.getNumBands() * 2 + 1].set(aot);

//      CWV & OZO - provided as a constant value and the other as pixel-based, depending on the sensor
//      MERIS: OZO per-pixel, CWV as constant value
//      AATSR: OZO and CWV as constant value
//      VGT: CWV per-pixel, OZO as constant value
        double ozo = BbdrConstants.OZO_CONSTANT_VALUE;  // constant mean value of 0.32
        double cwv = BbdrConstants.CWV_CONSTANT_VALUE;  // constant mean value of 1.5
        double gas = ozo;

        double vza_r = toRadians(vza);
        double sza_r = toRadians(sza);
        double muv = cos(vza_r);
        double mus = cos(sza_r);
        double amf = 1.0 / muv + 1.0 / mus;

        double[] toa_rfl = new double[sensor.getNumBands()];
        for (int i = 0; i < toa_rfl.length; i++) {
            double toaRefl = sourceSamples[SRC_TOA_RFL + i].getDouble();
            if (toaRefl == 0.0 || Double.isNaN(toaRefl)) {
                // if toa_refl look bad, set to invalid
                targetSamples[sensor.getNumBands() * 2 + 2].set(StatusPostProcessOp.STATUS_INVALID);
            }
            toaRefl /= sensor.getCal2Meris()[i];
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
            targetSamples[i].set(rfl_pix[i]);
        }

        double rfl_red = rfl_pix[sensor.getIndexRed()];
        double rfl_nir = rfl_pix[sensor.getIndexNIR()];
        double norm_ndvi = 1.0 / (rfl_nir + rfl_red);
        double ndvi_land = (sensor.getBndvi() * rfl_nir - sensor.getAndvi() * rfl_red) * norm_ndvi;
        targetSamples[sensor.getNumBands() * 2].set(ndvi_land);

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
            err_cwv[i] = abs((kx_tg[i][0][0] + kx_tg[i][0][1] * rfl_pix[i]) * delta_cwv);
            err_ozo[i] = abs((kx_tg[i][1][0] + kx_tg[i][1][1] * rfl_pix[i]) * delta_ozo);

            err_coreg[i] = sourceSamples[SRC_TOA_VAR + i].getDouble();
            err_coreg[i] *= sensor.getErrCoregScale();
        }

        Matrix err_aod_cov = BbdrUtils.matrixSquare(err_aod);
        Matrix err_cwv_cov = BbdrUtils.matrixSquare(err_cwv);
        Matrix err_ozo_cov = BbdrUtils.matrixSquare(err_ozo);
        Matrix err_coreg_cov = BbdrUtils.matrixSquare(err_coreg);

        Matrix err_rad_cov = new Matrix(sensor.getNumBands(), sensor.getNumBands());
        for (int i = 0; i < sensor.getNumBands(); i++) {
            err_rad_cov.set(i, i, err_rad[i] * err_rad[i]);
        }

        Matrix err2_tot_cov = err_aod_cov.plusEquals(err_cwv_cov).plusEquals(err_ozo_cov).plusEquals(
                err_rad_cov).plusEquals(err_coreg_cov);

        for (int i = 0; i < sensor.getNumBands(); i++) {
            targetSamples[sensor.getNumBands() + i].set(err2_tot_cov.get(i, i));
        }
    }

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(AbstractSdrOp.class);
        }
    }
}

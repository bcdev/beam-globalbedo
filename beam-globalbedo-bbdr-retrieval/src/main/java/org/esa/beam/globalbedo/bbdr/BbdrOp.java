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
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.experimental.PixelOperator;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.FracIndex;

import java.io.IOException;

import static java.lang.Math.*;
import static java.lang.StrictMath.toRadians;

/**
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
@OperatorMetadata(alias = "ga.bbdr",
                  description = "Computes BBDRs and kernel parameters",
                  authors = "Marco Zuehlke, Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2011 by Brockmann Consult")
public class BbdrOp extends PixelOperator {

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
//    private static final int SRC_TOA_VAR = 10 + 15;
    private int SRC_TOA_VAR;

    private static final int TRG_BBDR = 0;
    private static final int TRG_ERRORS = 3;
    private static final int TRG_KERN = 9;
    private static final int TRG_NDVI = 15;
    private static final int TRG_VZA = 17;
    private static final int TRG_SZA = 18;
    private static final int TRG_RAA = 19;
    private static final int TRG_DEM = 20;
    private static final int TRG_SNOW = 21;

    private static final int n_spc = 3; // VIS, NIR, SW ; Broadband albedos
    private static final int n_kernel = 2; //(geo & vol)

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "MERIS")
    private Sensor sensor;

    @Parameter(defaultValue = "false")
    private boolean sdrOnly;
    @Parameter
    private String landExpression;

    // Auxdata
    private double[][] nb_coef_arr_all; // = fltarr(n_spc, num_bd)
    private double[] nb_intcp_arr_all; // = fltarr(n_spc)
    private double[] rmse_arr_all; // = fltarr(n_spc)
    private double[][] nb_coef_arr_D; // = fltarr(n_spc, num_bd)
    private double[] nb_intcp_arr_D; //= fltarr(n_spc)
    private double kpp_vol;
    private double kpp_geo;

    private AotLookupTable aotLut;
    private LookupTable kxAotLut;
    private GasLookupTable gasLookupTable;
    private NskyLookupTable nskyDwLut;
    private NskyLookupTable nskyUpLut;

    private double vzaMin;
    private double vzaMax;
    private double szaMin;
    private double szaMax;
    private double aotMin;
    private double aotMax;
    private double hsfMin;
    private double hsfMax;

    @Override
    protected void configureTargetProduct(Product targetProduct) {
        if (sdrOnly) {
            for (int i = 0; i < sensor.getNumBands(); i++) {
                Band srcBand = sourceProduct.getBand("reflectance_" + (i + 1));
                Band band = targetProduct.addBand("sdr_" + (i + 1), ProductData.TYPE_FLOAT32);
                band.setNoDataValue(Float.NaN);
                band.setNoDataValueUsed(true);
                ProductUtils.copySpectralBandProperties(srcBand, band);
            }
            Band sdrError = targetProduct.addBand("sdr_error", ProductData.TYPE_FLOAT32);
            sdrError.setNoDataValue(Float.NaN);
            sdrError.setNoDataValueUsed(true);

            Band ndvi = targetProduct.addBand("ndvi", ProductData.TYPE_FLOAT32);
            ndvi.setNoDataValue(Float.NaN);
            ndvi.setNoDataValueUsed(true);

            // copy flag coding and flag images
            ProductUtils.copyFlagBands(sourceProduct, targetProduct);
            final Band[] bands = sourceProduct.getBands();
            for (Band band : bands) {
                if (band.getFlagCoding() != null) {
                    targetProduct.getBand(band.getName()).setSourceImage(band.getSourceImage());
                }
            }
        } else {
            String[] bandNames = {"BB_VIS", "BB_NIR", "BB_SW",
                "sig_BB_VIS_VIS", "sig_BB_VIS_NIR", "sig_BB_VIS_SW",
                "sig_BB_NIR_NIR", "sig_BB_NIR_SW", "sig_BB_SW_SW",
                "Kvol_BRDF_VIS", "Kvol_BRDF_NIR", "Kvol_BRDF_SW",
                "Kgeo_BRDF_VIS", "Kgeo_BRDF_NIR", "Kgeo_BRDF_SW",
                "AOD550",
                "NDVI", "sig_NDVI",
                "VZA", "SZA", "RAA", "DEM",
            };
            for (String bandName : bandNames) {
                Band band = targetProduct.addBand(bandName, ProductData.TYPE_FLOAT32);
                band.setNoDataValue(Float.NaN);
                band.setNoDataValueUsed(true);
            }
            targetProduct.addBand("snow_mask", ProductData.TYPE_INT8);
            Band targetAOT = targetProduct.getBand("AOD550");
            Band sourceAOT = sourceProduct.getBand("aot");
            ProductUtils.copyRasterDataNodeProperties(sourceAOT, targetAOT);
            targetAOT.setSourceImage(sourceAOT.getSourceImage());

            // copy flag coding and flag images
            ProductUtils.copyFlagBands(sourceProduct, targetProduct);
            final Band[] bands = sourceProduct.getBands();
            for (Band band : bands) {
                if (band.getFlagCoding() != null) {
                    targetProduct.getBand(band.getName()).setSourceImage(band.getSourceImage());
                }
            }
        }
        readAuxdata();
    }

    void readAuxdata() {
         N2Bconversion n2Bconversion = new N2Bconversion(sensor, 3);
        try {
            n2Bconversion.load();
            rmse_arr_all = n2Bconversion.getRmse_arr_all();
            nb_coef_arr_all = n2Bconversion.getNb_coef_arr_all();
            nb_intcp_arr_all = n2Bconversion.getNb_intcp_arr_all();
            nb_coef_arr_D = n2Bconversion.getNb_coef_arr_D();
            nb_intcp_arr_D = n2Bconversion.getNb_intcp_arr_D();

            aotLut = BbdrUtils.getAotLookupTable(sensor);
            kxAotLut = BbdrUtils.getAotKxLookupTable(sensor);
            nskyDwLut = BbdrUtils.getNskyLookupTableDw(sensor);
            nskyUpLut = BbdrUtils.getNskyLookupTableUp(sensor);
            kpp_geo = nskyDwLut.getKppGeo();
            kpp_vol = nskyDwLut.getKppVol();

            gasLookupTable = new GasLookupTable(sensor);
            gasLookupTable.load(sourceProduct);
        } catch (IOException e) {
            throw new OperatorException(e.getMessage());
        }

        LookupTable aotLut = this.aotLut.getLut();

        final double[] vzaArray = aotLut.getDimension(5).getSequence();
        vzaMin = vzaArray[0];
        vzaMax = vzaArray[vzaArray.length - 1];

        final double[] szaArray = aotLut.getDimension(4).getSequence();
        szaMin = szaArray[0];
        szaMax = szaArray[szaArray.length - 1];

        final double[] hsfArray = aotLut.getDimension(2).getSequence();
        hsfMin = hsfArray[0];
        hsfMax = hsfArray[hsfArray.length - 1];

        final double[] aotArray = aotLut.getDimension(1).getSequence();
        aotMin = aotArray[0];
        aotMax = aotArray[aotArray.length - 1];
    }

    @Override
    protected void configureSourceSamples(Configurator configurator) {
        String[] toaBandNames = null;

        String landExpr = null;
        final String commonLandExpr;
        if (landExpression != null && !landExpression.isEmpty()) {
            commonLandExpr = landExpression;
        } else {
            commonLandExpr = "cloud_classif_flags.F_CLEAR_LAND OR cloud_classif_flags.F_CLEAR_SNOW";
        }
        final String snowMaskExpression = "cloud_classif_flags.F_CLEAR_SNOW";

        BandMathsOp snowOp = BandMathsOp.createBooleanExpressionBand(snowMaskExpression, sourceProduct);
        Product snowMaskProduct = snowOp.getTargetProduct();
        configurator.defineSample(SRC_SNOW_MASK, snowMaskProduct.getBandAt(0).getName(), snowMaskProduct);

        if (sensor == Sensor.MERIS) {
            landExpr = "NOT l1_flags.INVALID AND (" + commonLandExpr + ")";

            configurator.defineSample(SRC_VZA, BbdrConstants.MERIS_VZA_TP_NAME);
            configurator.defineSample(SRC_VAA, BbdrConstants.MERIS_VAA_TP_NAME);
            configurator.defineSample(SRC_SZA, BbdrConstants.MERIS_SZA_TP_NAME);
            configurator.defineSample(SRC_SAA, BbdrConstants.MERIS_SAA_TP_NAME);
            configurator.defineSample(SRC_DEM, BbdrConstants.MERIS_DEM_BAND_NAME);
            configurator.defineSample(SRC_AOT, BbdrConstants.MERIS_AOT_BAND_NAME);
            configurator.defineSample(SRC_AOT_ERR, BbdrConstants.MERIS_AOTERR_BAND_NAME);
            configurator.defineSample(SRC_OZO, BbdrConstants.MERIS_OZO_TP_NAME);
//            configurator.defineSample(SRC_WVP, "ozone");

            toaBandNames = new String[BbdrConstants.MERIS_TOA_BAND_NAMES.length];
            System.arraycopy(BbdrConstants.MERIS_TOA_BAND_NAMES, 0, toaBandNames, 0,
                             BbdrConstants.MERIS_TOA_BAND_NAMES.length);
        } else if (sensor == Sensor.AATSR_NADIR) {
            landExpr = commonLandExpr;

            configurator.defineSample(SRC_VZA, "view_elev_nadir");
            configurator.defineSample(SRC_VAA, "view_azimuth_nadir");
            configurator.defineSample(SRC_SZA, "sun_elev_nadir");
            configurator.defineSample(SRC_SAA, "sun_azimuth_nadir");
            configurator.defineSample(SRC_DEM, "elevation");
            configurator.defineSample(SRC_AOT, "aot");
            configurator.defineSample(SRC_AOT_ERR, "aot_err");
//            configurator.defineSample(SRC_OZO, "ozone");
//            configurator.defineSample(SRC_WVP, "ozone");

            toaBandNames = new String[BbdrConstants.AATSR_TOA_BAND_NAMES_NADIR.length];
            System.arraycopy(BbdrConstants.AATSR_TOA_BAND_NAMES_NADIR, 0, toaBandNames, 0,
                             BbdrConstants.AATSR_TOA_BAND_NAMES_NADIR.length);

        } else if (sensor == Sensor.AATSR_FWARD) {
            landExpr = commonLandExpr;

            configurator.defineSample(SRC_VZA, "view_elev_fward");
            configurator.defineSample(SRC_VAA, "view_azimuth_fward");
            configurator.defineSample(SRC_SZA, "sun_elev_fward");
            configurator.defineSample(SRC_SAA, "sun_azimuth_fward");
            configurator.defineSample(SRC_DEM, "elevation");
            configurator.defineSample(SRC_AOT, "aot");
            configurator.defineSample(SRC_AOT_ERR, "aot_err");
//            configurator.defineSample(SRC_OZO, "ozone");
//            configurator.defineSample(SRC_WVP, "ozone");

            toaBandNames = new String[BbdrConstants.AATSR_TOA_BAND_NAMES_FWARD.length];
            System.arraycopy(BbdrConstants.AATSR_TOA_BAND_NAMES_FWARD, 0, toaBandNames, 0,
                             BbdrConstants.AATSR_TOA_BAND_NAMES_FWARD.length);
        } else if (sensor == Sensor.SPOT_VGT) {

            landExpr =
                    "SM.B0_GOOD AND SM.B2_GOOD AND SM.B3_GOOD AND (" + commonLandExpr + ")";

            configurator.defineSample(SRC_VZA, "VZA");
            configurator.defineSample(SRC_VAA, "VAA");
            configurator.defineSample(SRC_SZA, "SZA");
            configurator.defineSample(SRC_SAA, "SAA");
            configurator.defineSample(SRC_DEM, "elevation");
            configurator.defineSample(SRC_AOT, "aot");
            configurator.defineSample(SRC_AOT_ERR, "aot_err");
            configurator.defineSample(SRC_OZO, "OG");
            configurator.defineSample(SRC_WVP, "WVG");

            toaBandNames = new String[BbdrConstants.VGT_TOA_BAND_NAMES.length];
            System.arraycopy(BbdrConstants.VGT_TOA_BAND_NAMES, 0, toaBandNames, 0,
                             BbdrConstants.VGT_TOA_BAND_NAMES.length);
        }

        BandMathsOp landOp = BandMathsOp.createBooleanExpressionBand(landExpr, sourceProduct);
        Product landMaskProduct = landOp.getTargetProduct();
        configurator.defineSample(SRC_LAND_MASK, landMaskProduct.getBandAt(0).getName(), landMaskProduct);

        for (int i = 0; i < toaBandNames.length; i++) {
            configurator.defineSample(SRC_TOA_RFL + i, toaBandNames[i], sourceProduct);
        }
        SRC_TOA_VAR = SRC_TOA_RFL + toaBandNames.length;

        ImageVarianceOp imageVarianceOp = new ImageVarianceOp();
        imageVarianceOp.setSourceProduct(sourceProduct);
        imageVarianceOp.setParameter("sensor", sensor);
        Product varianceProduct = imageVarianceOp.getTargetProduct();

        for (int i = 0; i < toaBandNames.length; i++) {
            configurator.defineSample(SRC_TOA_VAR + i, toaBandNames[i], varianceProduct);
        }

    }

    @Override
    protected void configureTargetSamples(Configurator configurator) {
        if (sdrOnly) {
            for (int i = 0; i < sensor.getNumBands(); i++) {
                configurator.defineSample(i, "sdr_" + (i + 1));
            }
            configurator.defineSample(sensor.getNumBands(), "sdr_error");
            configurator.defineSample(sensor.getNumBands()+1, "ndvi");
        } else {
            configurator.defineSample(TRG_BBDR    , "BB_VIS");
            configurator.defineSample(TRG_BBDR + 1, "BB_NIR");
            configurator.defineSample(TRG_BBDR + 2, "BB_SW");

            configurator.defineSample(TRG_ERRORS    , "sig_BB_VIS_VIS");
            configurator.defineSample(TRG_ERRORS + 1, "sig_BB_VIS_NIR");
            configurator.defineSample(TRG_ERRORS + 2, "sig_BB_VIS_SW");
            configurator.defineSample(TRG_ERRORS + 3, "sig_BB_NIR_NIR");
            configurator.defineSample(TRG_ERRORS + 4, "sig_BB_NIR_SW");
            configurator.defineSample(TRG_ERRORS + 5, "sig_BB_SW_SW");

            configurator.defineSample(TRG_KERN    , "Kvol_BRDF_VIS");
            configurator.defineSample(TRG_KERN + 1, "Kgeo_BRDF_VIS");
            configurator.defineSample(TRG_KERN + 2, "Kvol_BRDF_NIR");
            configurator.defineSample(TRG_KERN + 3, "Kgeo_BRDF_NIR");
            configurator.defineSample(TRG_KERN + 4, "Kvol_BRDF_SW");
            configurator.defineSample(TRG_KERN + 5, "Kgeo_BRDF_SW");

            configurator.defineSample(TRG_NDVI, "NDVI");
            configurator.defineSample(TRG_NDVI + 1, "sig_NDVI");

            configurator.defineSample(TRG_VZA, "VZA");
            configurator.defineSample(TRG_SZA, "SZA");
            configurator.defineSample(TRG_RAA, "RAA");
            configurator.defineSample(TRG_DEM, "DEM");
            configurator.defineSample(TRG_SNOW, "snow_mask");
        }
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        if (!sourceSamples[SRC_LAND_MASK].getBoolean()) {
            // only compute over land
            fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }

        double vza = sourceSamples[SRC_VZA].getDouble();
        double vaa = sourceSamples[SRC_VAA].getDouble();
        double sza = sourceSamples[SRC_SZA].getDouble();
        double saa = sourceSamples[SRC_SAA].getDouble();
        double aot = sourceSamples[SRC_AOT].getDouble();
        double hsf = sourceSamples[SRC_DEM].getDouble();
        hsf *= 0.001;
        hsf = max(hsf, -0.45); // elevation up to -450m ASL

        if (vza < vzaMin || vza > vzaMax ||
                sza < szaMin || sza > szaMax ||
                aot < aotMin || aot > aotMax ||
                hsf < hsfMin || hsf > hsfMax) {
            fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }
        if (!sdrOnly) {
            targetSamples[TRG_SNOW].set(sourceSamples[SRC_SNOW_MASK].getInt());
            targetSamples[TRG_VZA].set(vza);
            targetSamples[TRG_SZA].set(sza);
            targetSamples[TRG_DEM].set(hsf);
        }

        double ozo;
        double cwv;
        double gas;

//      CWV & OZO - provided as a constant value and the other as pixel-based, depending on the sensor
//      MERIS: OZO per-pixel, CWV as constant value
//      AATSR: OZO and CWV as constant value
//      VGT: CWV per-pixel, OZO as constant value
        if (sensor == Sensor.MERIS) {
            ozo = 0.001 * sourceSamples[SRC_OZO].getDouble();
            cwv = BbdrConstants.CWV_CONSTANT_VALUE;  // constant mean value of 1.5
            gas = ozo;
        } else if (sensor == Sensor.AATSR_NADIR || sensor == Sensor.AATSR_FWARD) {
            ozo = BbdrConstants.OZO_CONSTANT_VALUE;  // constant mean value of 0.32
            cwv = BbdrConstants.CWV_CONSTANT_VALUE;  // constant mean value of 1.5
            gas = ozo;
        } else if (sensor == Sensor.SPOT_VGT) {
            ozo = gasLookupTable.getGasMeanVal();   // mean value from whole image
            cwv = sourceSamples[SRC_WVP].getDouble();
            cwv = min(cwv, 4.45);
            gas = cwv;
        } else {
            throw new IllegalArgumentException("Sensor '" + sensor.toString() + "' not supported.");
        }

        double[] toa_rfl = new double[sensor.getNumBands()];
        for (int i = 0; i < toa_rfl.length; i++) {
            toa_rfl[i] = sourceSamples[SRC_TOA_RFL + i].getDouble();
            toa_rfl[i] /= sensor.getCal2Meris()[i];
        }

        double phi = abs(saa - vaa);
        if (phi > 180.0) {
            phi = 360.0 - phi;
        }
        phi = min(phi, 179);
        phi = max(phi, 1);
        if (!sdrOnly) {
            targetSamples[TRG_RAA].set(phi);
        }

        double vza_r = toRadians(vza);
        double sza_r = toRadians(sza);
        double muv = cos(vza_r);
        double mus = cos(sza_r);
        double amf = 1.0 / muv + 1.0 / mus;


        float[] tg = gasLookupTable.getTg((float) amf, (float) gas);
        float[][][] kx_tg = gasLookupTable.getKxTg((float) amf, (float) gas);

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
            if (sdrOnly) {
                targetSamples[i].set(rfl_pix[i]);
            }
        }

        double rfl_red = rfl_pix[sensor.getIndexRed()];
        double rfl_nir = rfl_pix[sensor.getIndexNIR()];
        double norm_ndvi = 1.0 / (rfl_nir + rfl_red);
        double ndvi_land = (sensor.getBndvi() * rfl_nir - sensor.getAndvi() * rfl_red) * norm_ndvi;
        if (sdrOnly) {
            targetSamples[sensor.getNumBands()+1].set(ndvi_land);
        } else {
            targetSamples[TRG_NDVI].set(ndvi_land);
        }

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
            err_cwv[i] = abs((kx_tg[i][0][0] + kx_tg[i][0][1] * rfl_pix[i]) * delta_cwv);
            err_ozo[i] = abs((kx_tg[i][1][0] + kx_tg[i][1][1] * rfl_pix[i]) * delta_ozo);

            err_coreg[i] = sourceSamples[SRC_TOA_VAR + i].getDouble();
            err_coreg[i] *= sensor.getErrCoregScale();
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

        if (sdrOnly) {
            double sdrError = err2_tot_cov.trace();
            targetSamples[sensor.getNumBands()].set(sdrError);
            return;
        }
        // end of implementation needed for landcover cci

        double ndviSum = sensor.getAndvi() + sensor.getBndvi();
        double sig_ndvi_land = pow(
                (pow(ndviSum * rfl_nir * sqrt(err2_tot_cov.get(sensor.getIndexRed(), sensor.getIndexRed())) * norm_ndvi * norm_ndvi, 2) +
                        pow(ndviSum * rfl_red * sqrt(err2_tot_cov.get(sensor.getIndexNIR(), sensor.getIndexNIR())) * norm_ndvi * norm_ndvi, 2)
                ), 0.5);
        targetSamples[TRG_NDVI+1].set(sig_ndvi_land);

        // BB conversion and error var-cov calculation

        Matrix nb_coef_arr_all_m = new Matrix(nb_coef_arr_all);
        Matrix rfl_pix_m = new Matrix(rfl_pix, rfl_pix.length);
        Matrix nb_intcp_arr_all_m = new Matrix(nb_intcp_arr_all, nb_intcp_arr_all.length);

        Matrix bdr_mat_all = nb_coef_arr_all_m.times(rfl_pix_m).plus(nb_intcp_arr_all_m);

        double[] bbdrsData = bdr_mat_all.getColumnPackedCopy();
        for (int i = 0; i < bbdrsData.length; i++) {
            targetSamples[TRG_BBDR + i].set(bbdrsData[i]);
        }

        Matrix err2_mat_rfl = nb_coef_arr_all_m.times(err2_tot_cov).times(nb_coef_arr_all_m.transpose());
        Matrix err2_n2b_all = new Matrix(n_spc, n_spc);
        for (int i = 0; i < n_spc; i++) {
            err2_n2b_all.set(i, i, rmse_arr_all[i] * rmse_arr_all[i]);
        }
        Matrix err_sum = err2_mat_rfl.plus(err2_n2b_all);

        int[] relevantErrIndices = {0, 1, 2, 4, 7, 8};
        double[] columnPackedCopy = err_sum.getColumnPackedCopy();
        for (int i = 0; i < relevantErrIndices.length; i++) {
            final double err_final = sqrt(columnPackedCopy[relevantErrIndices[i]]);
            targetSamples[TRG_ERRORS + i].set(err_final);
        }

        // calculation of kernels (kvol, kgeo) & weighting with (1-Dup)(1-Ddw)

        double[][] f_int_nsky = interpol_lut_Nsky(sza, vza, hsf, aot);

        double phi_r = toRadians(phi);

        double mu_phi = cos(phi_r);
        double mu_ph_ang = mus * muv + sin(vza_r) * sin(sza_r) * mu_phi;
        double ph_ang = acos(mu_ph_ang);

        double kvol = ((PI / 2.0 - ph_ang) * cos(ph_ang) + sin(ph_ang)) / (mus + muv) - PI / 4.0;

        double br = 1.0;
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
        for (int i_bb = 0; i_bb < n_spc; i_bb++) {
            Matrix nb_coef_arr_D_m = new Matrix(nb_coef_arr_D[i_bb], nb_coef_arr_D[i_bb].length).transpose();
            Matrix m1 = nb_coef_arr_D_m.times(rat_tdw_m);
            double rat_tdw_bb = m1.get(0, 0) + nb_intcp_arr_D[i_bb];

            Matrix m2 = nb_coef_arr_D_m.times(rat_tup_m);
            double rat_tup_bb = m2.get(0, 0) + nb_intcp_arr_D[i_bb];

            // 1/(1-Delta_bb)=(1-rho*S)^2
            Matrix sab_m = new Matrix(sab, sab.length);
            Matrix m3 = nb_coef_arr_D_m.times(sab_m);
            double delta_bb_inv = pow((1. - bdr_mat_all.get(0, 0) * (m3.get(0, 0) + nb_intcp_arr_D[i_bb])), 2);

            double t0 = (1. - rat_tdw_bb) * (1. - rat_tup_bb) * delta_bb_inv;
            double t1 = (1. - rat_tdw_bb) * rat_tup_bb * delta_bb_inv;
            double t2 = rat_tdw_bb * (1. - rat_tup_bb) * delta_bb_inv;
            double t3 = (rat_tdw_bb * rat_tup_bb - (1. - 1. / delta_bb_inv)) * delta_bb_inv;
            double kernel_land_0 = t0 * kvol + t1 * f_int_nsky[i_bb][0] + t2 * f_int_nsky[i_bb][2] + t3 * kpp_vol;
            double kernel_land_1 = t0 * kgeo + t1 * f_int_nsky[i_bb][1]+ t2 * f_int_nsky[i_bb][3] + t3 * kpp_geo;
            targetSamples[TRG_KERN + (i_bb*2)].set(kernel_land_0);
            targetSamples[TRG_KERN + (i_bb*2) + 1].set(kernel_land_1);
        }
    }

    private void fillTargetSampleWithNoDataValue(WritableSample[] targetSamples) {
        for (WritableSample targetSample : targetSamples) {
            targetSample.set(Float.NaN);
        }
    }

    /**
     * 5-D linear interpolation:
     * returns spectral array [rpw, ttot, sab, rat_tdw, rat_tup, Kx_1, Kx_2]
     * as a function of [vza, sza, phi, hsf, aot] from the interpolation of the MOMO absorption-free LUTs
     */
    private double[][] interpol_lut_MOMO_kx(double vza, double sza, double phi, double hsf, double aot) {
        final LookupTable lut = aotLut.getLut();
        final float[] wvl = aotLut.getWvl();
        final double[] params = aotLut.getLut().getDimension(6).getSequence();
        final double[] kxParams = kxAotLut.getDimension(6).getSequence();
        double[][] result = new double[sensor.getNumBands()][7];

        int lutDimensionCount = lut.getDimensionCount();
        double[] coordinates = new double[lutDimensionCount];
        FracIndex[] fracIndexes = FracIndex.createArray(coordinates.length);
        double[] v = new double[1 << coordinates.length];

        lut.computeFracIndex(lut.getDimension(1), aot, fracIndexes[1]);
        lut.computeFracIndex(lut.getDimension(2), hsf, fracIndexes[2]);
        lut.computeFracIndex(lut.getDimension(3), phi, fracIndexes[3]);
        lut.computeFracIndex(lut.getDimension(4), sza, fracIndexes[4]);
        lut.computeFracIndex(lut.getDimension(5), vza, fracIndexes[5]);

        for (int i = 0; i < result.length; i++) {
            int index = 0;
            lut.computeFracIndex(lut.getDimension(0), wvl[i], fracIndexes[0]);
            for (double param : params) {
                lut.computeFracIndex(lut.getDimension(6), param, fracIndexes[6]);
                result[i][index++] = lut.getValue(fracIndexes, v);
            }
            for (double kxParam : kxParams) {
                lut.computeFracIndex(lut.getDimension(6), kxParam, fracIndexes[6]);
                result[i][index++] = kxAotLut.getValue(fracIndexes, v);
            }
        }
        return result;
    }

    private double[][] interpol_lut_Nsky(double sza, double vza, double hsf, double aot) {
        final LookupTable lut_dw = nskyDwLut.getLut();
        final LookupTable lut_up = nskyUpLut.getLut();
        final double[] broadBandSpecs = lut_dw.getDimension(0).getSequence();
        final double[] dw_params = lut_dw.getDimension(4).getSequence();
        final double[] up_params = lut_up.getDimension(4).getSequence();

        double[][] result = new double[broadBandSpecs.length][4];

        int lutDimensionCount = lut_dw.getDimensionCount();
        double[] coordinates = new double[lutDimensionCount];
        FracIndex[] fracIndexes = FracIndex.createArray(coordinates.length);
        double[] v = new double[1 << coordinates.length];

        lut_dw.computeFracIndex(lut_dw.getDimension(1), aot, fracIndexes[1]);
        lut_dw.computeFracIndex(lut_dw.getDimension(2), hsf, fracIndexes[2]);

        for (int i = 0; i < result.length; i++) {
            int index = 0;
            lut_dw.computeFracIndex(lut_dw.getDimension(0), broadBandSpecs[i], fracIndexes[0]);

            lut_dw.computeFracIndex(lut_dw.getDimension(3), sza, fracIndexes[3]);
            for (double param : dw_params) {
                lut_dw.computeFracIndex(lut_dw.getDimension(4), param, fracIndexes[4]);
                result[i][index++] = lut_dw.getValue(fracIndexes, v);
            }

            lut_up.computeFracIndex(lut_up.getDimension(3), vza, fracIndexes[3]);
            for (double param : up_params) {
                lut_up.computeFracIndex(lut_up.getDimension(4), param, fracIndexes[4]);
                result[i][index++] = lut_up.getValue(fracIndexes, v);
            }
        }
        return result;
    }

    static Matrix matrixSquare(double[] doubles) {
//        Matrix matrix = new Matrix(doubles, doubles.length);
//        return matrix.times(matrix.transpose());

        Matrix matrix = new Matrix(doubles.length, doubles.length);
        for (int i = 0; i < doubles.length; i++) {
           for (int j = 0; j < doubles.length; j++) {
              matrix.set(i, j, doubles[i] * doubles[j]);
           }
        }
        return matrix;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BbdrOp.class);
        }
    }
}

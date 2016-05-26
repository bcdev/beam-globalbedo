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
@OperatorMetadata(alias = "ga.bbdr.from.sdr",
        description = "Computes BBDRs and kernel parameters, using previously computed SDR as input",
        authors = "Marco Zuehlke, Olaf Danne",
        version = "1.1",
        copyright = "(C) 2015 by Brockmann Consult")
public class BbdrFromSdrOp extends BbdrMasterOp {

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
        if (x == 146 && y == 1961) {
            System.out.println("x = " + x);
        }

        final int status = sourceSamples[SRC_STATUS].getInt();
        if (!singlePixelMode) {
            targetSamples[TRG_SNOW].set(sourceSamples[SRC_SNOW_MASK].getInt());

            if (status != 1 && status != 3) {
                // only compute over clear land or snow
                BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
                return;
            }
        }

        double vza = sourceSamples[SRC_VZA].getDouble();
        double vaa = sourceSamples[SRC_VAA].getDouble();
        double sza = sourceSamples[SRC_SZA].getDouble();
        double saa = sourceSamples[SRC_SAA].getDouble();
        double aot;
        double deltaAot;
        if (useAotClimatology) {
            aot = 0.15;  // reasonable constant for the moment
            // todo: really use 'climatology_ratios.nc' from A.Heckel (collocated as slave with given source product)
            deltaAot = 0.0;
        } else {
            aot = sourceSamples[SRC_AOD].getDouble();
            deltaAot = sourceSamples[SRC_AOD_ERR].getDouble();
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
        targetSamples[TRG_AODERR].set(deltaAot);


        // end of implementation which is same as for SDR. Now BBDR computation...

        // prepare SDR --> BBDR computation...
        double[] sdr = new double[sensor.getNumBands()];
        for (int i = 0; i < sensor.getNumBands(); i++) {
            sdr[i] = sourceSamples[i].getDouble();
        }

        if (x == 900 && y == 100) {
            System.out.println("x = " + x);
        }

        double sdrRed = sdr[sensor.getIndexRed()];
        double sdrNir = sdr[sensor.getIndexNIR()];

        double normNdvi = 1.0 / (sdrNir + sdrRed);
        double ndviLand = (sensor.getBndvi() * sdrNir - sensor.getAndvi() * sdrRed) * normNdvi;
        targetSamples[TRG_NDVI].set(ndviLand);

        double vzaRad = toRadians(vza);
        double szaRad = toRadians(sza);
        double muv = cos(vzaRad);
        double mus = cos(szaRad);
        double amf = 1.0 / muv + 1.0 / mus;

        double phi = abs(saa - vaa);
        if (phi > 180.0) {
            phi = 360.0 - phi;
        }
        phi = max(min(phi, 179), 1);
        targetSamples[TRG_RAA].set(phi);

        double[] errRad = new double[sensor.getNumBands()];
        double[] errAod = new double[sensor.getNumBands()];
        double[] errCwv = new double[sensor.getNumBands()];
        double[] errOzo = new double[sensor.getNumBands()];
        double[] errCoreg = new double[sensor.getNumBands()];

        final float ozo = BbdrConstants.OZO_CONSTANT_VALUE;
        final float gas = ozo;
        final float cwv = BbdrConstants.CWV_CONSTANT_VALUE;

        float[][][] kxTg = aux.getGasLookupTable().getKxTg((float) amf, gas);

        double[] sab = new double[sensor.getNumBands()];
        double[] ratTdw = new double[sensor.getNumBands()];
        double[] ratTup = new double[sensor.getNumBands()];
        double[][] fIntAll = aux.interpol_lut_MOMO_kx(vza, sza, phi, hsf, aot);
        if (fIntAll == null) {
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }

        for (int i = 0; i < sensor.getNumBands(); i++) {
            double[] fInt = fIntAll[i];
            sab[i] = fInt[2];        // Spherical Albedo
            ratTdw[i] = 1.0 - fInt[3];  // tdif_dw / ttot_dw
            ratTup[i] = 1.0 - fInt[4];  // tup_dw / ttot_dw

            double rpw = fInt[0] * Math.PI / mus; // Path Radiance
            double ttot = fInt[1] / mus;    // Total TOA flux (Isc*Tup*Tdw)

            // compute back to toaRefl for original error estimation
            final double toaRfl = (rpw + ttot * sdr[i] - sab[i] * rpw * sdr[i]) / (1.0 - sab[i] * sdr[i]);
            errRad[i] = sensor.getRadiometricError() * toaRfl;

            double deltaCwv = sensor.getCwvError() * cwv;
            double deltaOzo = sensor.getOzoError() * ozo;

            errAod[i] = abs((fInt[5] + fInt[6] * sdr[i]) * deltaAot);
            errCwv[i] = abs((kxTg[i][0][0] + kxTg[i][0][1] * sdr[i]) * deltaCwv);
            errOzo[i] = abs((kxTg[i][1][0] + kxTg[i][1][1] * sdr[i]) * deltaOzo);

            errCoreg[i] = sourceSamples[SRC_TOA_VAR + i].getDouble();
            errCoreg[i] *= sensor.getErrCoregScale();
            errCoreg[i] = 0.05 * sdr[i]; // test
        }

        Matrix errAodCov = BbdrUtils.matrixSquare(errAod);
        Matrix errCwvCov = BbdrUtils.matrixSquare(errCwv);
        Matrix errOzoCov = BbdrUtils.matrixSquare(errOzo);
        Matrix errCoregCov = BbdrUtils.matrixSquare(errCoreg);

        Matrix errRadCov = new Matrix(sensor.getNumBands(), sensor.getNumBands());
        for (int i = 0; i < sensor.getNumBands(); i++) {
            errRadCov.set(i, i, errRad[i] * errRad[i]);
        }

        Matrix err2TotCov = errAodCov.plusEquals(errCwvCov).plusEquals(errOzoCov).plusEquals(
                errRadCov).plusEquals(errCoregCov);

        // start SDR --> BBDR computation

        double ndviSum = sensor.getAndvi() + sensor.getBndvi();
        double sigNdviLand = pow(
                (pow(ndviSum * sdrNir * sqrt(
                        err2TotCov.get(sensor.getIndexRed(), sensor.getIndexRed())) * normNdvi * normNdvi, 2) +
                        pow(ndviSum * sdrRed * sqrt(
                                err2TotCov.get(sensor.getIndexNIR(), sensor.getIndexNIR())) * normNdvi * normNdvi, 2)
                ), 0.5);
        targetSamples[TRG_NDVI + 1].set(sigNdviLand);

        // BB conversion and error var-cov calculation

        Matrix rflPixM = new Matrix(sdr, sdr.length);
        Matrix bdrMatAll = aux.getNb_coef_arr_all().times(rflPixM).plus(aux.getNb_intcp_arr_all());

        double[] bbdrData = bdrMatAll.getColumnPackedCopy();
        for (int i = 0; i < bbdrData.length; i++) {
            targetSamples[i].set(bbdrData[i]);
        }

        Matrix err2MatRfl = aux.getNb_coef_arr_all().times(err2TotCov).times(aux.getNb_coef_arr_all().transpose());
        Matrix err2N2BAll = new Matrix(BbdrConstants.N_SPC, BbdrConstants.N_SPC);
        for (int i = 0; i < BbdrConstants.N_SPC; i++) {
            err2N2BAll.set(i, i, aux.getRmse_arr_all()[i] * aux.getRmse_arr_all()[i]);
        }
        Matrix errSum = err2MatRfl.plus(err2N2BAll);

        int[] relevantErrIndices = {0, 1, 2, 4, 7, 8};
        double[] columnPackedCopy = errSum.getColumnPackedCopy();
        for (int i = 0; i < relevantErrIndices.length; i++) {
            // todo: there is no Math.abs here in the original BbdrOp, so we may get NaNs - is that a bug?!
            final double errFinal = sqrt(Math.abs(columnPackedCopy[relevantErrIndices[i]]));
            targetSamples[TRG_ERRORS + i].set(errFinal);
        }

        // calculation of kernels (kvol, kgeo) & weighting with (1-Dup)(1-Ddw)
        double[][] fIntNsky = aux.interpol_lut_Nsky(sza, vza, hsf, aot);
        if (fIntNsky == null) {
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }

        double phiR = toRadians(phi);

        double muPhi = cos(phiR);
        double muPhAng = mus * muv + sin(vzaRad) * sin(szaRad) * muPhi;
        double phAng = acos(muPhAng);

        double kvol = ((PI / 2.0 - phAng) * cos(phAng) + sin(phAng)) / (mus + muv) - PI / 4.0;

        double hb = 2.0;

        double tanVp = tan(vzaRad);
        double tanSp = tan(szaRad);
        double secVp = 1. / muv;
        double secSp = 1. / mus;

        double d2 = tanVp * tanVp + tanSp * tanSp - 2 * tanVp * tanSp * muPhi;

        double cost = hb * (pow((d2 + pow((tanVp * tanSp * sin(phiR)), 2)), 0.5)) / (secVp + secSp);
        cost = min(cost, 1.0);
        double t = acos(cost);

        double ocap = (t - sin(t) * cost) * (secVp + secSp) / PI;

        double kgeo = 0.5 * (1. + muPhAng) * secSp * secVp + ocap - secVp - secSp;

        // Nsky-weighted kernels
        Matrix ratTdwM = new Matrix(ratTdw, ratTdw.length);
        Matrix ratTupM = new Matrix(ratTup, ratTup.length);
        for (int i_bb = 0; i_bb < BbdrConstants.N_SPC; i_bb++) {
            Matrix nb_coef_arr_D_m = aux.getNb_coef_arr()[i_bb];
            Matrix m1 = nb_coef_arr_D_m.times(ratTdwM);
            double ratTdwBb = m1.get(0, 0) + aux.getNb_intcp_arr_D()[i_bb];

            Matrix m2 = nb_coef_arr_D_m.times(ratTupM);
            double ratTupBb = m2.get(0, 0) + aux.getNb_intcp_arr_D()[i_bb];

            // 1/(1-Delta_bb)=(1-rho*S)^2
            Matrix sabM = new Matrix(sab, sab.length);
            Matrix m3 = nb_coef_arr_D_m.times(sabM);
            double deltaBbInv = pow((1. - bdrMatAll.get(0, 0) * (m3.get(0, 0) + aux.getNb_intcp_arr_D()[i_bb])), 2);

            double t0 = (1. - ratTdwBb) * (1. - ratTupBb) * deltaBbInv;
            double t1 = (1. - ratTdwBb) * ratTupBb * deltaBbInv;
            double t2 = ratTdwBb * (1. - ratTupBb) * deltaBbInv;
            double t3 = (ratTdwBb * ratTupBb - (1. - 1. / deltaBbInv)) * deltaBbInv;
            double kernelLand0 = t0 * kvol + t1 * fIntNsky[i_bb][0] + t2 * fIntNsky[i_bb][2] + t3 * aux.getKpp_vol();
            double kernelLand1 = t0 * kgeo + t1 * fIntNsky[i_bb][1] + t2 * fIntNsky[i_bb][3] + t3 * aux.getKpp_geo();
            targetSamples[TRG_KERN + (i_bb * 2)].set(kernelLand0);
            targetSamples[TRG_KERN + (i_bb * 2) + 1].set(kernelLand1);
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BbdrFromSdrOp.class);
        }
    }
}

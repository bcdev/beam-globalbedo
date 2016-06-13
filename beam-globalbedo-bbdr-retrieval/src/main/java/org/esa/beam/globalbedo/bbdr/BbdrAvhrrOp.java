/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.util.BitSetter;
import org.esa.beam.util.ProductUtils;

import static java.lang.StrictMath.toRadians;

/**
 * Generates a GlobAlbedo conform BBDR product from AVHRR-LTDR BRF product provided by JRC (NG)
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.bbdr.avhrr",
        description = "Computes BBDRs and kernel parameters for AVHRR",
        authors = "Marco Zuehlke, Olaf Danne",
        version = "1.0",
        copyright = "(C) 2016 by Brockmann Consult")
public class BbdrAvhrrOp extends PixelOperator {

    protected static final int SRC_BRF_1 = 0;
    protected static final int SRC_BRF_2 = 1;
    protected static final int SRC_SIGMA_BRF_1 = 2;
    protected static final int SRC_SIGMA_BRF_2 = 3;
    protected static final int SRC_TS = 4;
    protected static final int SRC_TV = 5;
    protected static final int SRC_PHI = 6;
    protected static final int SRC_QA = 7;

    protected static final int TRG_BB_VIS = 0;
    protected static final int TRG_BB_NIR = 1;
    protected static final int TRG_BB_SW = 2;
    protected static final int TRG_sig_BB_VIS_VIS = 3;
    protected static final int TRG_sig_BB_VIS_NIR = 4;
    protected static final int TRG_sig_BB_VIS_SW = 5;
    protected static final int TRG_sig_BB_NIR_NIR = 6;
    protected static final int TRG_sig_BB_NIR_SW = 7;
    protected static final int TRG_sig_BB_SW_SW = 8;
    protected static final int TRG_VZA = 9;
    protected static final int TRG_SZA = 10;
    protected static final int TRG_RAA = 11;
    protected static final int TRG_KERN = 12;

    @SourceProduct
    protected Product sourceProduct;

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {

        final double brf1 = sourceSamples[SRC_BRF_1].getDouble();
        final double brf2 = sourceSamples[SRC_BRF_2].getDouble();
        final double sigmaBrf1 = sourceSamples[SRC_SIGMA_BRF_1].getDouble();
        final double sigmaBrf2 = sourceSamples[SRC_SIGMA_BRF_2].getDouble();
//        final double sza = sourceSamples[SRC_TS].getDouble();
//        final double vza = sourceSamples[SRC_TV].getDouble();
        // JRC swapped the angles in input products, so correct here. todo: remove this when JRC delivered correct data
        final double vza = sourceSamples[SRC_TS].getDouble();
        final double sza = sourceSamples[SRC_TV].getDouble();

        final double phi = sourceSamples[SRC_PHI].getDouble();
        final int qa = sourceSamples[SRC_QA].getInt();

        // decode qa flag and extract cloud info, see emails from Mirko Marioni, 20160513:
        final boolean isCloud = BitSetter.isFlagSet(qa, 1);
        final boolean isCloudShadow = BitSetter.isFlagSet(qa, 2);
        final boolean isSea = BitSetter.isFlagSet(qa, 3);
        if (isSea || isCloud || isCloudShadow) {
            // only compute over clear land
            BbdrUtils.fillTargetSampleWithNoDataValue(targetSamples);
            return;
        }

        // for conversion to broadband, use Liang coefficients (S.Liang, 2000, eq. (7)):
        double[] bb = new double[BbdrConstants.N_SPC];
        double[] sigmaBb = new double[BbdrConstants.N_SPC];
        for (int i = 0; i < sigmaBb.length; i++) {
            final double a = BbdrConstants.AVHRR_LIANG_COEFFS[i][0];
            final double b = BbdrConstants.AVHRR_LIANG_COEFFS[i][1];
            final double c = BbdrConstants.AVHRR_LIANG_COEFFS[i][2];
            final double d = BbdrConstants.AVHRR_LIANG_COEFFS[i][3];
            final double e = BbdrConstants.AVHRR_LIANG_COEFFS[i][4];
            final double f = BbdrConstants.AVHRR_LIANG_COEFFS[i][5];
            bb[i] = a * brf1 * brf1 + b * brf2 * brf2 + c * brf1 * brf2 + d * brf1 + e * brf2 + f;
            sigmaBb[i] = Math.abs(2.0 * a * brf1 + c * brf2 + d) * Math.abs(sigmaBrf1) +
                    Math.abs(2.0 * b * brf2 + c * brf1 + e) * Math.abs(sigmaBrf2);
        }

        final double sigmaBbVisVis = sigmaBb[0];
        final double sigmaBbVisNir = Math.sqrt(sigmaBb[0]*sigmaBb[1]);
        final double sigmaBbVisSw = Math.sqrt(sigmaBb[0]*sigmaBb[2]);
        final double sigmaBbNirNir = sigmaBb[1];
        final double sigmaBbNirSw = Math.sqrt(sigmaBb[1]*sigmaBb[2]);
        final double sigmaBbSwSw = sigmaBb[2];

        targetSamples[TRG_BB_VIS].set(bb[0]);
        targetSamples[TRG_BB_NIR].set(bb[1]);
        targetSamples[TRG_BB_SW].set(bb[2]);
        targetSamples[TRG_sig_BB_VIS_VIS].set(sigmaBbVisVis);
        targetSamples[TRG_sig_BB_VIS_NIR].set(sigmaBbVisNir);
        targetSamples[TRG_sig_BB_VIS_SW].set(sigmaBbVisSw);
        targetSamples[TRG_sig_BB_NIR_NIR].set(sigmaBbNirNir);
        targetSamples[TRG_sig_BB_NIR_SW].set(sigmaBbNirSw);
        targetSamples[TRG_sig_BB_SW_SW].set(sigmaBbSwSw);
        targetSamples[TRG_VZA].set(vza);
        targetSamples[TRG_SZA].set(sza);
        targetSamples[TRG_RAA].set(phi);

        // calculation of kernels (kvol, kgeo)

        final double vzaRad = toRadians(vza);
        final double szaRad = toRadians(sza);
        final double phiRad = toRadians(phi);

        final double[] kernels = BbdrUtils.computeConstantKernels(vzaRad, szaRad, phiRad);
        final double kvol = kernels[0];
        final double kgeo = kernels[1];

        for (int i_bb = 0; i_bb < BbdrConstants.N_SPC; i_bb++) {
            // for AVHRR, neglect Nsky, coupling terms and weighting (PL, 20160520)
            targetSamples[TRG_KERN + (i_bb * 2)].set(kvol);
            targetSamples[TRG_KERN + (i_bb * 2) + 1].set(kgeo);
        }
    }

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();

        for (String bbBandName : BbdrConstants.BB_BAND_NAMES) {
            targetProduct.addBand(bbBandName, ProductData.TYPE_FLOAT32);
        }
        for (String bbSigmaBandName : BbdrConstants.BB_SIGMA_BAND_NAMES) {
            targetProduct.addBand(bbSigmaBandName, ProductData.TYPE_FLOAT32);
        }
        for (String bbKernelBandName : BbdrConstants.BB_KERNEL_BAND_NAMES) {
            targetProduct.addBand(bbKernelBandName, ProductData.TYPE_FLOAT32);
        }
        targetProduct.addBand("VZA", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("SZA", ProductData.TYPE_FLOAT32);
        targetProduct.addBand("RAA", ProductData.TYPE_FLOAT32);

        for (Band band : targetProduct.getBands()) {
            band.setNoDataValue(Float.NaN);
            band.setNoDataValueUsed(true);
        }

        targetProduct.addBand("snow_mask", ProductData.TYPE_INT8);
        ProductUtils.copyBand("QA", sourceProduct, targetProduct, true);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) throws OperatorException {
        configurator.defineSample(SRC_BRF_1, "BRF_BAND_1", sourceProduct);
        configurator.defineSample(SRC_BRF_2, "BRF_BAND_2", sourceProduct);
        configurator.defineSample(SRC_SIGMA_BRF_1, "SIGMA_BRF_BAND_1", sourceProduct);
        configurator.defineSample(SRC_SIGMA_BRF_2, "SIGMA_BRF_BAND_2", sourceProduct);
        configurator.defineSample(SRC_TS, "TS", sourceProduct);
        configurator.defineSample(SRC_TV, "TV", sourceProduct);
        configurator.defineSample(SRC_PHI, "PHI", sourceProduct);
        configurator.defineSample(SRC_QA, "QA", sourceProduct);
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) throws OperatorException {
        configurator.defineSample(TRG_BB_VIS, "BB_VIS");
        configurator.defineSample(TRG_BB_NIR, "BB_NIR");
        configurator.defineSample(TRG_BB_SW, "BB_SW");

        configurator.defineSample(TRG_sig_BB_VIS_VIS, "sig_BB_VIS_VIS");
        configurator.defineSample(TRG_sig_BB_VIS_NIR, "sig_BB_VIS_NIR");
        configurator.defineSample(TRG_sig_BB_VIS_SW, "sig_BB_VIS_SW");
        configurator.defineSample(TRG_sig_BB_NIR_NIR, "sig_BB_NIR_NIR");
        configurator.defineSample(TRG_sig_BB_NIR_SW, "sig_BB_NIR_SW");
        configurator.defineSample(TRG_sig_BB_SW_SW, "sig_BB_SW_SW");

        configurator.defineSample(TRG_VZA, "VZA");
        configurator.defineSample(TRG_SZA, "SZA");
        configurator.defineSample(TRG_RAA, "RAA");

        configurator.defineSample(TRG_KERN, "Kvol_BRDF_VIS");
        configurator.defineSample(TRG_KERN + 1, "Kgeo_BRDF_VIS");
        configurator.defineSample(TRG_KERN + 2, "Kvol_BRDF_NIR");
        configurator.defineSample(TRG_KERN + 3, "Kgeo_BRDF_NIR");
        configurator.defineSample(TRG_KERN + 4, "Kvol_BRDF_SW");
        configurator.defineSample(TRG_KERN + 5, "Kgeo_BRDF_SW");

    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BbdrAvhrrOp.class);
        }
    }
}

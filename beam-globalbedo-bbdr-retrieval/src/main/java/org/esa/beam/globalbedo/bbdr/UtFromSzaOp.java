/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.util.math.MathUtils;

/**
 * Computes UT from SZA
 * // todo: check with MSSL if algo is correct
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.utfromsza",
                  description = "Computes UT from SZA",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2013 by Brockmann Consult")
public class UtFromSzaOp extends PixelOperator {

    private static final int SRC_SZA = 0;
    private static final int TRG_UT = 0;

    @SourceProduct
    private Product sourceProduct;

    @Parameter(defaultValue = "-1", description = "doy")
    private int doy;

    @Parameter(defaultValue = "", description = "yyyymmdd")
    private String yyyymmdd;

    @Override
    protected void configureTargetProduct(ProductConfigurer productConfigurer) {
        super.configureTargetProduct(productConfigurer);

        final Product targetProduct = productConfigurer.getTargetProduct();

        Band band = targetProduct.addBand("UT", ProductData.TYPE_FLOAT32);
        band.setNoDataValue(Float.NaN);
        band.setNoDataValueUsed(true);
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer configurator) {
        configurator.defineSample(SRC_SZA, BbdrConstants.MERIS_SZA_TP_NAME);
        if (doy == -1) {
            if (yyyymmdd != null && yyyymmdd.length() == 8) {
                doy = BbdrUtils.getDoyFromYYYYMMDD(yyyymmdd);
            } else {
                throw new OperatorException("Neither doy nor yyyymmdd specified.");
            }
        }
    }

    @Override
    protected void configureTargetSamples(SampleConfigurer configurator) {
        configurator.defineSample(TRG_UT, "UT");
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        double sza = sourceSamples[SRC_SZA].getDouble() * MathUtils.DTOR;

        final PixelPos pixelPos = new PixelPos(x, y);
        final GeoPos geoPos = sourceProduct.getGeoCoding().getGeoPos(pixelPos, null);
        double ut;
        ut = computeUtFromSza(sza, geoPos.getLat(), doy);

        targetSamples[TRG_UT].set(ut);
    }

    /**
     * Computes solar zenith angle at local noon as function of Geoposition and DoY
     *
     *
     * @param sza  - SZA
     * @param doy    - day of year
     * @return ut
     */
    public static double computeUtFromSza(double sza, double lat, double doy) {

//        final double latitude = geoPos.getLat() * MathUtils.DTOR;
//        final double delta = -23.45 * (Math.PI / 180.0) * Math.cos(2 * Math.PI / 365.0 * (doy + 10));
//
//        double acosArg = (Math.cos(sza) - Math.sin(delta) * Math.sin(latitude)) / (Math.cos(delta) * Math.cos(latitude));
//        final double ut = (12.0/Math.PI) * (1.0 - Math.acos(acosArg));
//
//        return ut;

        double doysAfterEquinoxe = doy - 80.0;
        if (doysAfterEquinoxe < 0) {
            doysAfterEquinoxe += 365.0;
        }
        double sinArg = MathUtils.DTOR * 360 * doysAfterEquinoxe / 365.25;
        final double psi =  MathUtils.DTOR * 23.5*Math.sin(sinArg);

        double acosArg = (Math.sin(sza) - Math.sin(psi) * Math.sin(lat)) / (Math.cos(psi) * Math.cos(lat));
        final double ut = 12.0 + Math.acos(acosArg)/15.0;

        return ut;
    }

    public static double computeSzaFromUt(double ut, double lat, double doy) {
        double doysAfterEquinoxe = doy - 80.0;
        if (doysAfterEquinoxe < 0) {
            doysAfterEquinoxe += 365.0;
        }
        double theta = MathUtils.DTOR * 360 * (ut-12.0) / 24.0;

        double sinArg = MathUtils.DTOR * 360 * (doysAfterEquinoxe)/365.25;
        final double psi =  MathUtils.DTOR * 23.5*Math.sin(sinArg);

        double acosArg = Math.cos(lat)*Math.cos(psi)*Math.cos(theta) + Math.sin(psi) * Math.sin(theta);
        final double sza = 90.0 - MathUtils.RTOD * Math.acos(acosArg);

        return sza;
    }


    public static class Spi extends OperatorSpi {

        public Spi() {
            super(UtFromSzaOp.class);
        }
    }
}

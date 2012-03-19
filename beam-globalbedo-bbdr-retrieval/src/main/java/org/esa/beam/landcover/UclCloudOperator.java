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

package org.esa.beam.landcover;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeFilter;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.PixelOperator;
import org.esa.beam.framework.gpf.pointop.ProductConfigurer;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.SampleConfigurer;
import org.esa.beam.framework.gpf.pointop.WritableSample;

import java.awt.Color;
import java.io.IOException;

@OperatorMetadata(alias = "lc.ucl")
public class UclCloudOperator extends PixelOperator {

    @SourceProduct
    Product sourceProduct;

    private UclCloudDetection uclCloudDetection;

    @Override
    protected void configureTargetProduct(ProductConfigurer pc) {
        super.configureTargetProduct(pc);

        pc.copyBands(new ProductNodeFilter<Band>() {
            @Override
            public boolean accept(Band productNode) {
                String name = productNode.getName();
                return name.startsWith("sdr_") && !name.startsWith("sdr_error_") || name.equals("status");
            }
        });

        pc.addBand("hue", ProductData.TYPE_FLOAT32, Float.NaN);
        pc.addBand("sat", ProductData.TYPE_FLOAT32, Float.NaN);
        pc.addBand("val", ProductData.TYPE_FLOAT32, Float.NaN);
        pc.addBand("cloudCoeff", ProductData.TYPE_FLOAT32, Float.NaN);
        pc.addBand("landCoeff", ProductData.TYPE_FLOAT32, Float.NaN);
        pc.addBand("townCoeff", ProductData.TYPE_FLOAT32, Float.NaN);
        pc.addBand("cloud", ProductData.TYPE_INT8);

        int w = sourceProduct.getSceneRasterWidth();
        int h = sourceProduct.getSceneRasterHeight();
        Mask cloudMask = Mask.BandMathsType.create("isCloud", "UCL Cloud", w, h, "cloud", Color.YELLOW, 0.5f);
        pc.getTargetProduct().getMaskGroup().add(cloudMask);

        try {
            uclCloudDetection = UclCloudDetection.create();
        } catch (IOException e) {
            throw new OperatorException("failed to initialize algorithm:", e);
        }
    }

    @Override
    protected void configureSourceSamples(SampleConfigurer sampleConfigurer) {
        sampleConfigurer.defineSample(0, "sdr_7");
        sampleConfigurer.defineSample(1, "sdr_14");
        sampleConfigurer.defineSample(2, "sdr_3");
    }

    @Override
    public void configureTargetSamples(SampleConfigurer sampleConfigurer) {
        sampleConfigurer.defineSample(0, "hue");
        sampleConfigurer.defineSample(1, "sat");
        sampleConfigurer.defineSample(2, "val");
        sampleConfigurer.defineSample(3, "cloudCoeff");
        sampleConfigurer.defineSample(4, "landCoeff");
        sampleConfigurer.defineSample(5, "townCoeff");
        sampleConfigurer.defineSample(6, "cloud");
    }

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        float sdrRed = sourceSamples[0].getFloat();
        float sdrGreen = sourceSamples[1].getFloat();
        float sdrBlue = sourceSamples[2].getFloat();

        float[] hsv = UclCloudDetection.rgb2hsv(sdrRed, sdrGreen, sdrBlue);
        targetSamples[0].set(hsv[0]);
        targetSamples[1].set(hsv[1]);
        targetSamples[2].set(hsv[2]);

        targetSamples[3].set(uclCloudDetection.cloudScatterData.getDensity(hsv));
        targetSamples[4].set(uclCloudDetection.landScatterData.getDensity(hsv));
        targetSamples[5].set(uclCloudDetection.townScatterData.getDensity(hsv));
        targetSamples[6].set(uclCloudDetection.isCloud(sdrRed, sdrGreen, sdrBlue));
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(UclCloudOperator.class);
        }
    }
}

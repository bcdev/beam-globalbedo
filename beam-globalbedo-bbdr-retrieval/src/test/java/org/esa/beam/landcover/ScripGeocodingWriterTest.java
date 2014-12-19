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

package org.esa.beam.landcover;

import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.PixelGeoCoding;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;


public class ScripGeocodingWriterTest {

    private Product l1Product;

    @Before
    public void setUp() throws Exception {
        l1Product = new Product("testName", "testType", 5, 5);
        Band latBand = l1Product.addBand("latitude", ProductData.TYPE_FLOAT32);
        Band lonBand = l1Product.addBand("longitude", ProductData.TYPE_FLOAT32);
        latBand.setRasterData(ProductData.createInstance(new float[] {
                58, 58, 58, 58, 58,
                57, 57, 57, 57, 57,
                56, 56, 56, 56, 56,
                55, 55, 55, 55, 55,
                54, 54, 54, 54, 54
        }));
        lonBand.setRasterData(ProductData.createInstance(new float[]{
                11, 12, 13, 14, 15,
                11, 12, 13, 14, 15,
                11, 12, 13, 14, 15,
                11, 12, 13, 14, 15,
                11, 12, 13, 14, 15
        }));
        l1Product.setGeoCoding(new PixelGeoCoding(latBand, lonBand, null, 3));
    }

    @After
    public void tearDown() throws Exception {
        l1Product.dispose();
    }

    @Test
    public void testWriter() throws Exception {
        ScripGeocodingWriterPlugIn plugIn = new ScripGeocodingWriterPlugIn();
        ProductWriter writer = plugIn.createWriterInstance();
        File outputFile = new File("scrip-geo.nc");
        writer.writeProductNodes(l1Product, outputFile );
    }
}
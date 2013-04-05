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

package org.esa.beam.globalbedo.bbdr.seaice;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SeaIceProcessingTest {

    private static final int W = 3;
    private static final int H = 3;

    @Test
    public void testGetCollocateMerisMasterProduct() {
        final Product targetProduct = MerisAatsrBbdrSeaiceOp.getCollocateMerisMasterProduct(createCollocAatsrMasterProduct(),
                                                                                                          createMerisL1bProduct());
        assertNotNull(targetProduct);
        int numBands = targetProduct.getNumBands();
        assertEquals(15 + 2 + 2, numBands);
        assertEquals("btemp_nadir_1200_S", targetProduct.getBandAt(0).getName());
        assertEquals("btemp_fward_0370_S", targetProduct.getBandAt(1).getName());
        for (int i=1; i<=15; i++) {
            assertEquals("radiance_" + i + "_M", targetProduct.getBandAt(i+1).getName());
        }
        assertEquals("l1_flags_M", targetProduct.getBandAt(17).getName());
        assertEquals("detector_index_M", targetProduct.getBandAt(18).getName());

        int numTpgs = targetProduct.getNumTiePointGrids();
        assertEquals(15, numTpgs);

        assertEquals("latitude", targetProduct.getTiePointGridAt(0).getName());
        assertEquals("longitude", targetProduct.getTiePointGridAt(1).getName());
        assertEquals("dem_alt", targetProduct.getTiePointGridAt(2).getName());
        assertEquals("dem_rough", targetProduct.getTiePointGridAt(3).getName());
        assertEquals("lat_corr", targetProduct.getTiePointGridAt(4).getName());
        assertEquals("lon_corr", targetProduct.getTiePointGridAt(5).getName());
        assertEquals("sun_zenith", targetProduct.getTiePointGridAt(6).getName());
        assertEquals("sun_azimuth", targetProduct.getTiePointGridAt(7).getName());
        assertEquals("view_zenith", targetProduct.getTiePointGridAt(8).getName());
        assertEquals("view_azimuth", targetProduct.getTiePointGridAt(9).getName());
        assertEquals("zonal_wind", targetProduct.getTiePointGridAt(10).getName());
        assertEquals("merid_wind", targetProduct.getTiePointGridAt(11).getName());
        assertEquals("atm_press", targetProduct.getTiePointGridAt(12).getName());
        assertEquals("ozone", targetProduct.getTiePointGridAt(13).getName());
        assertEquals("rel_hum", targetProduct.getTiePointGridAt(14).getName());

    }

    private static Product createCollocAatsrMasterProduct() {
        // data taken from himalaya test product x = 28, y = 2
        Product collocProduct = new Product("TEST.N1", "COLLOC", W, H);

        collocProduct.addBand(addBand("btemp_nadir_1200_M", 39.21092987060547));
        collocProduct.addBand(addBand("btemp_fward_0370_M", 100.31485748291016));

        collocProduct.addBand(addBand("radiance_1_S", 0.197262242436409));
        collocProduct.addBand(addBand("radiance_2_S", 0.1874786913394928));
        collocProduct.addBand(addBand("radiance_3_S", 0.1808471381664276));
        collocProduct.addBand(addBand("radiance_4_S", 0.18388484418392181));
        collocProduct.addBand(addBand("radiance_5_S", 0.2018040120601654));
        collocProduct.addBand(addBand("radiance_6_S", 0.2270776331424713));
        collocProduct.addBand(addBand("radiance_7_S", 0.2511415481567383));
        collocProduct.addBand(addBand("radiance_8_S", 0.25836682319641113));
        collocProduct.addBand(addBand("radiance_9_S", 0.2701571583747864));
        collocProduct.addBand(addBand("radiance_10_S", 0.2871195673942566));
        collocProduct.addBand(addBand("radiance_11_S", 0.1284395158290863));
        collocProduct.addBand(addBand("radiance_12_S", 0.29514235258102417));
        collocProduct.addBand(addBand("radiance_13_S", 0.3082677721977234));
        collocProduct.addBand(addBand("radiance_14_S", 0.30971765518188477));
        collocProduct.addBand(addBand("radiance_15_S", 0.2931019067764282));
        collocProduct.addBand(addBand("l1_flags_S", 0.2931019067764282));
        collocProduct.addBand(addBand("detector_index_S", 0.2931019067764282));

        collocProduct.addBand(addBand("latitude_S", 0.37025466561317444));
        collocProduct.addBand(addBand("longitude_S", 0.7025466561317444));
        collocProduct.addBand(addBand("dem_alt_S", 0.3705466561317444));
        collocProduct.addBand(addBand("dem_rough_S", 0.37025466561317444));
        collocProduct.addBand(addBand("lat_corr_S", 0.25466561317444));
        collocProduct.addBand(addBand("lon_corr_S", 0.5466561317444));
        collocProduct.addBand(addBand("sun_zenith_S", 0.36561317444));
        collocProduct.addBand(addBand("sun_azimuth_S", 0.3761317444));
        collocProduct.addBand(addBand("view_zenith_S", 1.66561317444));
        collocProduct.addBand(addBand("view_azimuth_S", 0.1317444));
        collocProduct.addBand(addBand("zonal_wind_S", 2.37025466561317444));
        collocProduct.addBand(addBand("merid_wind_S", 5.37025466561317444));
        collocProduct.addBand(addBand("atm_press_S", 7.37025466561317444));
        collocProduct.addBand(addBand("ozone_S", 8.37025466561317444));
        collocProduct.addBand(addBand("rel_hum_S", 10.37025466561317444));

        collocProduct.addTiePointGrid(addTiePointGrid("sun_elev_nadir", 0.37025466561317444));
        collocProduct.addTiePointGrid(addTiePointGrid("lon_corr_nadir", 0.37025466561317444));
        return collocProduct;
    }

    private static Product createMerisL1bProduct() {
        // data taken from himalaya test product x = 28, y = 2
        Product sourceProduct = new Product("TEST_MERIS.N1", "MER_RR__L1", W, H);

        sourceProduct.addBand(addBand("radiance_1", 0.197262242436409));
        sourceProduct.addBand(addBand("radiance_2", 0.1874786913394928));
        sourceProduct.addBand(addBand("radiance_3", 0.1808471381664276));
        sourceProduct.addBand(addBand("radiance_4", 0.18388484418392181));
        sourceProduct.addBand(addBand("radiance_5", 0.2018040120601654));
        sourceProduct.addBand(addBand("radiance_6", 0.2270776331424713));
        sourceProduct.addBand(addBand("radiance_7", 0.2511415481567383));
        sourceProduct.addBand(addBand("radiance_8", 0.25836682319641113));
        sourceProduct.addBand(addBand("radiance_9", 0.2701571583747864));
        sourceProduct.addBand(addBand("radiance_10", 0.2871195673942566));
        sourceProduct.addBand(addBand("radiance_11", 0.1284395158290863));
        sourceProduct.addBand(addBand("radiance_12", 0.29514235258102417));
        sourceProduct.addBand(addBand("radiance_13", 0.3082677721977234));
        sourceProduct.addBand(addBand("radiance_14", 0.30971765518188477));
        sourceProduct.addBand(addBand("radiance_15", 0.2931019067764282));
        sourceProduct.addBand(addBand("l1_flags", 0.2931019067764282));
        sourceProduct.addBand(addBand("detector_index", 0.2931019067764282));

        sourceProduct.addTiePointGrid(addTiePointGrid("latitude", 0.37025466561317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("longitude", 0.7025466561317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("dem_alt", 0.3705466561317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("dem_rough", 0.37025466561317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("lat_corr", 0.25466561317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("lon_corr", 0.5466561317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("sun_zenith", 0.36561317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("sun_azimuth", 0.3761317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("view_zenith", 1.66561317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("view_azimuth", 0.1317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("zonal_wind", 2.37025466561317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("merid_wind", 5.37025466561317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("atm_press", 7.37025466561317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("ozone", 8.37025466561317444));
        sourceProduct.addTiePointGrid(addTiePointGrid("rel_hum", 10.37025466561317444));

        return sourceProduct;
    }


    private static float getSample(Product targetProduct, String bandName) {
        return targetProduct.getBand(bandName).getGeophysicalImage().getData().getSampleFloat(1, 1, 0);
    }

    private static Band addBand(String name, double value) {
        Band band = new Band(name, ProductData.TYPE_FLOAT32, W, H);
        band.setSourceImage(ConstantDescriptor.create(1.0f * W, 1.0f * H, new Double[]{value}, null));
        return band;
    }

    private static TiePointGrid addTiePointGrid(String name, double value) {
        TiePointGrid tpg = new TiePointGrid(name,W, H, 0.0f, 0.0f, 1.0f, 1.0f, new float[W*H]);
        tpg.setSourceImage(ConstantDescriptor.create(1.0f * W, 1.0f * H, new Double[]{value}, null));
        return tpg;
    }

}

/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.globalbedo.auxdata;


import org.esa.beam.framework.datamodel.Product;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author MarcoZ
 */
public class AerosolClimatologyTest {

    @Test
    public void testInstance() throws Exception {
        final Product aotProduct = AerosolClimatology.getInstance().getAotProduct();
        assertNotNull(aotProduct);
        assertEquals("NetCDF", aotProduct.getProductType());
        assertEquals(360, aotProduct.getSceneRasterWidth());
        assertEquals(180, aotProduct.getSceneRasterHeight());
        assertNotNull(aotProduct.getGeoCoding());
        for (int i = 1; i <= 12; i++) {
            assertNotNull(aotProduct.getBand(AuxdataConstants.AEROSOL_CLIMATOLOGY_MONTHLY_BAND_GROUP_NAME + i));
        }
    }

}

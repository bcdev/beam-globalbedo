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


import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UclCloudDetectionTest {

    @Test
    public void testisCloudImpl() throws Exception {
        assertTrue(UclCloudDetection.iCloudImpl(Float.NaN, Float.NaN, Float.NaN));
        assertTrue(UclCloudDetection.iCloudImpl(1f, Float.NaN, Float.NaN));
        assertTrue(UclCloudDetection.iCloudImpl(1f, 0.5f, Float.NaN));
        assertFalse(UclCloudDetection.iCloudImpl(1f, 0.5f, 2f));
        assertFalse(UclCloudDetection.iCloudImpl(Float.NaN, 0.5f, 2f));
    }
}

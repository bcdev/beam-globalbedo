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

package org.esa.beam.globalbedo.auxdata.AVHRR;


import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author OlafD
 */
public class AvhrrBrfBlacklistTest {

    @Test
    public void testInstance() throws Exception {
        AvhrrBrfBlacklist instance = AvhrrBrfBlacklist.getInstance();
        assertNotNull(instance);
        int numBadDates = instance.getBrfBadDatesNumber();
        assertEquals(744, numBadDates);
    }

    @Test
    public void testBadDatesRead() throws Exception {
        AvhrrBrfBlacklist instance = AvhrrBrfBlacklist.getInstance();
        assertNotNull(instance);

        List<String> objectUnderTest = instance.getBrfBadDatesList();
        assertNotNull(objectUnderTest);
        assertEquals(744, objectUnderTest.size());
        assertEquals("19810625", objectUnderTest.get(0));
        assertEquals("19811029", objectUnderTest.get(21));
        assertEquals("19830525", objectUnderTest.get(132));
        assertEquals("19850305", objectUnderTest.get(254));
        assertEquals("19871126", objectUnderTest.get(401));
        assertEquals("19930919", objectUnderTest.get(486));
        assertEquals("19941128", objectUnderTest.get(574));
        assertEquals("19990318", objectUnderTest.get(633));
        assertEquals("20040420", objectUnderTest.get(688));
        assertEquals("20100620", objectUnderTest.get(741));
        assertEquals("20110710", objectUnderTest.get(743));
    }

}

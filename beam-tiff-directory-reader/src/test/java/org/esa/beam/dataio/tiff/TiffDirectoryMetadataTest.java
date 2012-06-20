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

package org.esa.beam.dataio.tiff;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.Test;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.junit.Assert.*;


public class TiffDirectoryMetadataTest {

    @Test
    public void testReadMetadata_HC() throws IOException {
        InputStream stream = TiffDirectoryMetadata.class.getResourceAsStream("test_HC_meta.txt");
        InputStreamReader reader = new InputStreamReader(stream);
        TiffDirectoryMetadata hcMetadata = new TiffDirectoryMetadata(reader);

        MetadataElement element = hcMetadata.getMetaDataElementRoot();
        assertNotNull(element);
        assertEquals("HC_METADATA_FILE", element.getName());
        MetadataElement[] childElements = element.getElements();
        assertEquals(2, childElements.length);
        MetadataElement firstChild = childElements[0];
        assertEquals("METADATA_FILE_INFO", firstChild.getName());
        assertEquals(0, firstChild.getElements().length);
        MetadataAttribute[] attributes = firstChild.getAttributes();
        assertEquals(3, attributes.length);
        MetadataAttribute originAttr = attributes[0];
        assertEquals("ORIGIN", originAttr.getName());
        assertEquals(ProductData.TYPESTRING_ASCII, originAttr.getData().getTypeString());
        assertEquals("from_JPM", originAttr.getData().getElemString());

        assertTrue(hcMetadata.isHyperspectralCamera());

        assertEquals("HC_test", hcMetadata.getProductType());

        ProductData.UTC cTime = hcMetadata.getCreationTime();
        assertNotNull(cTime);
        assertEquals("16-FEB-2012 00:05:17.123000", cTime.format());

        final Dimension dim = hcMetadata.getProductDim();
        assertNotNull(dim);
        assertEquals(7401, dim.getHeight(), 1e-8);
        assertEquals(8121, dim.getWidth(), 1e-8);
    }
}

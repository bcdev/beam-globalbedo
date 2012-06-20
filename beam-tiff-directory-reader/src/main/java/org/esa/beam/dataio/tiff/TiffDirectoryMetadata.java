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

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

/**
 * Class for metadata handling of 'TIFF directory' products.
 * todo: current implementation is preliminary and only applies for distinct format as used in
 * Landsat MTL metadata files.
 *
 * @author Olaf Danne
 */
class TiffDirectoryMetadata {

    private final MetadataElement root;

    TiffDirectoryMetadata(Reader hcMetaReader) throws IOException {
         root = parseHcMeta(hcMetaReader);
    }

    private MetadataElement parseHcMeta(Reader hcMetaReader) throws IOException {

        MetadataElement base = null;
        MetadataElement currentElement = null;
        BufferedReader reader = new BufferedReader(hcMetaReader);
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("GROUP")) {
                    int i = line.indexOf('=');
                    String groupName = line.substring(i+1).trim();
                    MetadataElement element = new MetadataElement(groupName);
                    if (base == null) {
                        base = element;
                        currentElement = element;
                    } else {
                        if (currentElement != null) {
                            currentElement.addElement(element);
                        }
                        currentElement = element;
                    }
                } else if (line.startsWith("END_GROUP") && currentElement != null) {
                    currentElement = currentElement.getParentElement();
                } else if (line.equals("END")) {
                    return base;
                } else if (currentElement != null) {
                    MetadataAttribute attribute = createAttribute(line);
                    currentElement.addAttribute(attribute);
                }
            }
        } finally {
            reader.close();
        }
        return base;
    }

    private MetadataAttribute createAttribute(String line) {
        int i = line.indexOf('=');
        String name = line.substring(0, i).trim();
        String value = line.substring(i+1).trim();
        ProductData pData;
        if (value.startsWith("\"")) {
            value = value.substring(1, value.length()-1);
            pData = ProductData.createInstance(value);
        } else if (value.contains(".")) {
            try {
                double d = Double.parseDouble(value);
                pData = ProductData.createInstance(new double[]{d});
            } catch (NumberFormatException e) {
                 pData = ProductData.createInstance(value);
            }
        } else {
            try {
                int integer = Integer.parseInt(value);
                pData = ProductData.createInstance(new int[]{integer});
            } catch (NumberFormatException e) {
                 pData = ProductData.createInstance(value);
            }
        }
        return new MetadataAttribute(name, pData, true);
    }

    MetadataElement getMetaDataElementRoot() {
        return root;
    }

    MetadataElement getMetadataFileInfo() {
        return root.getElement("METADATA_FILE_INFO");
    }

    MetadataElement getProductMetadata() {
        return root.getElement("PRODUCT_METADATA");
    }

    String getProductType() {
        return getProductMetadata().getAttribute("PRODUCT_TYPE").getData().getElemString();
    }

    Dimension getProductDim() {
        return getDimension("PRODUCT_SAMPLES", "PRODUCT_LINES");
    }

    private Dimension getDimension(String widthAttributeName, String heightAttributeName) {
        MetadataElement metadata = getProductMetadata();
        MetadataAttribute widthAttribute = metadata.getAttribute(widthAttributeName);
        MetadataAttribute heightAttribute = metadata.getAttribute(heightAttributeName);
        if (widthAttribute != null && heightAttribute != null) {
            int width = widthAttribute.getData().getElemInt();
            int height = heightAttribute.getData().getElemInt();
            return new Dimension(width, height);
        } else {
            return null;
        }
    }

    boolean isHyperspectralCamera() {
        final MetadataElement productMetadata = getProductMetadata();
        return "HC".equals(productMetadata.getAttribute("SENSOR_ID").getData().getElemString().toUpperCase());
    }

    public ProductData.UTC getCreationTime() {
        String timeString = getMetadataFileInfo().getAttributeString("CREATION_TIME");

        try {
            if  (timeString != null)  {
                final DateFormat dateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                final Date date = dateFormat.parse(timeString);
                String milliSeconds = timeString.substring(timeString.length()-3);
                return ProductData.UTC.create(date, Long.parseLong(milliSeconds)*1000);
            }
        } catch (ParseException ignored) {
            // ignore
        }
        return null;
    }

}
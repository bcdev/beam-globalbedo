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

package org.esa.beam.dataio.gmted2010;

import org.esa.beam.framework.datamodel.Product;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;

/**
 */
class ElevationTile {

    private final RenderedImage image;

    public ElevationTile(Product product) {
        image = product.getBandAt(0).getGeophysicalImage().getImage(0);
    }

    public float getSample(int x, int y) {
        Raster imageData = image.getData(new Rectangle(x, y, 1, 1));
        return imageData.getSampleFloat(x, y, 0);
    }
}

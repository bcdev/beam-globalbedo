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

package org.esa.beam.globalbedo.mosaic;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;

import java.io.File;
import java.io.IOException;

/**
 * A single tile of a product made up of a grid of products.
 */
class MosaicTile {
    private final int index;
    private final int tileX;
    private final int tileY;
    private final File file;
    private Product product;

    public MosaicTile(int tileX, int tileY, int index, File file) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.index = index;
        this.file = file;
    }

    public int getTileX() {
        return tileX;
    }

    public int getTileY() {
        return tileY;
    }

    public int getIndex() {
        return index;
    }

    public File getFile() {
        return file;
    }

    public synchronized Product getProduct() throws IOException {
        if (product == null) {
//            System.out.println("reading tile product from: " + file);
            product = ProductIO.readProduct(file);
        }
        return product;
    }

    public void dispose() {
        if (product != null) {
            product.dispose();
            product = null;
        }
    }
}

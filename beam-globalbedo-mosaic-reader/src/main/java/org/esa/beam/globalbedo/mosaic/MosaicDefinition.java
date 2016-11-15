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

/**
 * Defines the layout of a grid..
 */
class MosaicDefinition {
    private final int numTileX;
    private final int numTileY;
    private final int hStartIndex;
    private final int vStartIndex;
    private int tileSize;

    public MosaicDefinition(int numTileX, int numTileY, int tileSize) {
        this.numTileX = numTileX;
        this.numTileY = numTileY;
        this.hStartIndex = 0;
        this.vStartIndex = 0;
        this.tileSize = tileSize;
    }

    public MosaicDefinition(int numTileX, int numTileY, int hStartIndex, int vStartIndex, int tileSize) {
        this.numTileX = numTileX;
        this.numTileY = numTileY;
        this.hStartIndex = hStartIndex;
        this.vStartIndex = vStartIndex;
        this.tileSize = tileSize;
    }

    public int calculateIndex(int x, int y) {
        return (y - vStartIndex) * numTileX + (x - hStartIndex);
    }

    public int getNumTiles() {
        return numTileX * numTileY;
    }

    public int getWidth() {
        return numTileX * tileSize;
    }

    public int getHeight() {
        return numTileY * tileSize;
    }

    public int getHStartIndex() {
        return hStartIndex;
    }

    public int getVStartIndex() {
        return vStartIndex;
    }

    public int getTileSize() {
        return tileSize;
    }

    public void setTileSize(int tileSize) {
        this.tileSize = tileSize;
    }
}

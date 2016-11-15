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

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A grid of products forming a mosaic.
 */
class MosaicGrid {

    private final MosaicDefinition mosaicDefinition;
    private final MosaicTile[] mosaicTiles;

    public MosaicGrid(MosaicDefinition mosaicDefinition, Set<MosaicTile> mosaicTilesSet) {
        this.mosaicDefinition = mosaicDefinition;
        this.mosaicTiles = new MosaicTile[mosaicDefinition.getNumTiles()];
        for (MosaicTile mosaicTile : mosaicTilesSet) {
            int index = mosaicTile.getIndex();
            mosaicTiles[index] = mosaicTile;
        }
    }

    public MosaicTile getMosaicTile(int x, int y) {
        return mosaicTiles[mosaicDefinition.calculateIndex(x, y)];
    }

    public Rectangle getTileBounds(int x, int y) {
        final int tileSize = mosaicDefinition.getTileSize();
        return new Rectangle(x * tileSize, y * tileSize, tileSize, tileSize);
    }

    public static Rectangle getTileBounds(int x, int y, int hStart, int vStart, int tileSize) {
        return new Rectangle((x - hStart) * tileSize,
                             (y - vStart) * tileSize,
                             tileSize, tileSize);
    }

    public Point[] getAffectedSourceTiles(Rectangle rect) {
        int tileSize = mosaicDefinition.getTileSize();
        final int indexX0 = rect.x / tileSize;
        final int indexY0 = rect.y / tileSize;
        final int indexWidth = ((rect.x + rect.width) / tileSize) + 1;
        final int indexHeight = ((rect.y + rect.height) / tileSize) + 1;
        List<Point> indexes = new ArrayList<>();
        for (int y = indexY0; y < indexHeight; y++) {
            for (int x = indexX0; x < indexWidth; x++) {
                indexes.add(new Point(x, y));
            }
        }
        return indexes.toArray(new Point[indexes.size()]);
    }

    public static Point[] getAffectedSourceTiles(Rectangle rect, int hStart, int vStart, int tileSize) {
        final int indexX0 = hStart + rect.x / tileSize;
        final int indexY0 = vStart + rect.y / tileSize;
        final int indexWidth = ((rect.x + rect.width) / tileSize) + 1;
        final int indexHeight = ((rect.y + rect.height) / tileSize) + 1;
        List<Point> indexes = new ArrayList<>();
        for (int y = indexY0; y < vStart + indexHeight; y++) {
            for (int x = indexX0; x < hStart + indexWidth; x++) {
                indexes.add(new Point(x, y));
            }
        }
        return indexes.toArray(new Point[indexes.size()]);
    }

    public void dispose() {
        for (MosaicTile mosaicTile : mosaicTiles) {
            mosaicTile.dispose();
        }
    }
}

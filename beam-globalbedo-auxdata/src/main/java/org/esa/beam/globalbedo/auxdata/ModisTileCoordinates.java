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

import org.esa.beam.util.io.CsvReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Class providing the Modis tile upper left coordinated from auxdata
 *
 * @author MarcoZ
 */
public class ModisTileCoordinates {

    private static final String ULCOORDS_TABLE_DEFAULT_FILE_NAME = "Tiles_UpperLeftCorner_Coordinates.txt";
    private static final char[] SEPARATOR = new char[]{','};

    private final List<TileCoordinate> tiles;

    private ModisTileCoordinates() {
        this.tiles = loadAuxData();
    }

    public static ModisTileCoordinates getInstance() {
        return Holder.instance;
    }

    public int getTileCount() {
        return tiles.size();
    }

    public String getTileName(int tileIndex) {
        return tiles.get(tileIndex).name;
    }

    public double getUpperLeftX(int tileIndex) {
        return tiles.get(tileIndex).x;
    }

    public double getUpperLeftY(int tileIndex) {
        return tiles.get(tileIndex).y;
    }

    public int findTileIndex(String tileName) {
        for (int i = 0; i < tiles.size(); i++) {
            if (tiles.get(i).name.equals(tileName)) {
                return i;
            }
        }
        return -1;
    }

    // Initialization on demand holder idiom
    private List<TileCoordinate> loadAuxData() {
        InputStream inputStream = ModisTileCoordinates.class.getResourceAsStream(ULCOORDS_TABLE_DEFAULT_FILE_NAME);
        InputStreamReader streamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(streamReader);
        CsvReader csvReader = new CsvReader(streamReader, SEPARATOR);
        List<String[]> records = null;
        try {
            records = csvReader.readStringRecords();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load auxdata", e);
        }
        List<TileCoordinate> tiles = new ArrayList<TileCoordinate>(records.size());
        for (String[] record : records) {
            String name = record[0].trim();
            String xStr = record[1].trim();
            String yStr = record[2].trim();
            tiles.add(new TileCoordinate(name, Double.parseDouble(xStr), Double.parseDouble(yStr)));
        }
        return tiles;
    }

    private static class TileCoordinate {
        private final String name;
        private final double x;
        private final double y;

        TileCoordinate(String name, double x, double y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }

    private static class Holder {
        private static final ModisTileCoordinates instance = new ModisTileCoordinates();
    }
}

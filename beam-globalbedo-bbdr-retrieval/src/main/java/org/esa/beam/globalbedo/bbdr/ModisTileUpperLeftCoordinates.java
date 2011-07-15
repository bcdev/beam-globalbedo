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

package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.framework.gpf.OperatorException;

import java.io.*;
import java.util.StringTokenizer;

/**
 * Class providing the Modis tile upper left coordinated from auxdata
 */
public class ModisTileUpperLeftCoordinates {

    private int NUMBER_OF_TILES = 36 * 18;
    private String ULCOORDS_TABLE_DEFAULT_FILE_NAME = "Tiles_UpperLeftCorner_Coordinates.txt";

    ULCoordinatesTable ulCoordinatesTable;
    int ulCoordinatesTableLength = 0;

    public ModisTileUpperLeftCoordinates() throws IOException {
        loadUpperLeftCoordinates();
    }

    private void loadUpperLeftCoordinates() throws IOException {
        InputStream inputStream = GlobalbedoLevel2.class.getResourceAsStream(ULCOORDS_TABLE_DEFAULT_FILE_NAME);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        StringTokenizer st;
        try {
            ulCoordinatesTable = new ULCoordinatesTable();

            int i = 0;
            String line;
            while ((line = bufferedReader.readLine()) != null && i < NUMBER_OF_TILES) {
                st = new StringTokenizer(line, ", ", false);
                if (st.hasMoreTokens()) {
                    // tile
                    String tile = st.nextToken();
                    ulCoordinatesTable.setTile(i, tile);
                }
                if (st.hasMoreTokens()) {
                    // upper left x
                    ulCoordinatesTable.setUlX(i, Double.parseDouble(st.nextToken()));
                }
                if (st.hasMoreTokens()) {
                    // drift870
                    ulCoordinatesTable.setUlY(i, Double.parseDouble(st.nextToken()));
                }
                i++;
                ulCoordinatesTableLength++;
            }
        } catch (IOException e) {
            throw new OperatorException("Failed to load Tiles coordinates file: \n" + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new OperatorException("Failed to load Tiles coordinates file: \n" + e.getMessage(), e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    /**
     * Object providing the drift data for the 4 channels
     */
    class ULCoordinatesTable {
        private String[] tile = new String[NUMBER_OF_TILES];
        private double[] ulX = new double[NUMBER_OF_TILES];
        private double[] ulY = new double[NUMBER_OF_TILES];

        public String[] getTile() {
            return tile;
        }

        public void setTile(int index, String value) {
            tile[index] = value;
        }

        public double[] getUlX() {
            return ulX;
        }

        public void setUlX(int index, double value) {
            ulX[index] = value;
        }

        public double[] getUlY() {
            return ulY;
        }

        public void setUlY(int index, double value) {
            ulY[index] = value;
        }
    }

    public ULCoordinatesTable getUlCoordinatesTable() {
        return ulCoordinatesTable;
    }

    public int getUlCoordinatesTableLength() {
        return ulCoordinatesTableLength;
    }

    public double getUpperLeftX(String tile) {
        int tileIndex = 0;
        for (String tableTile: ulCoordinatesTable.getTile()) {
            if (tile.equals(tableTile)) {
               return (ulCoordinatesTable.getUlX()[tileIndex]);
            }
            tileIndex++;
        }
        return 0.0;
    }

    public double getUpperLeftY(String tile) {
        int tileIndex = 0;
        for (String tableTile: ulCoordinatesTable.getTile()) {
            if (tile.equals(tableTile)) {
               return (ulCoordinatesTable.getUlY()[tileIndex]);
            }
            tileIndex++;
        }
        return 0.0;
    }
}

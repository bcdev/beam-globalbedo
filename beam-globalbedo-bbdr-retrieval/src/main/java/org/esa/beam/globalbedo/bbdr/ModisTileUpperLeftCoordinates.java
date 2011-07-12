package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.framework.gpf.OperatorException;

import java.io.*;
import java.util.StringTokenizer;

/**
 * Class providing the Modis tile upper left coordinated from auxdata
 * <p/>
 * Created by IntelliJ IDEA.
 * User: olafd
 * Date: 12.07.11
 * Time: 15:38
 * To change this template use File | Settings | File Templates.
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

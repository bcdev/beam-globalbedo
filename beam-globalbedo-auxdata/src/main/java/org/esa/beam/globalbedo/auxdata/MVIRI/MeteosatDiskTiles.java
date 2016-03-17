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

package org.esa.beam.globalbedo.auxdata.MVIRI;

import org.esa.beam.util.io.CsvReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Class providing the list of tiles intersecting a Meteosat full disk with given ID (000, 057, 063)
 *
 * @author olafd
 */
public class MeteosatDiskTiles {

    private static MeteosatDiskTiles instance = null;
    private static final char[] SEPARATOR = new char[]{','};

    private final List<DiskTile> tiles;
    private final String diskId;

    private MeteosatDiskTiles(String diskId) {
        this.diskId = diskId;
        this.tiles = loadAuxData();
    }

    public static MeteosatDiskTiles getInstance(String diskId) {
        if(instance == null) {
            instance = new MeteosatDiskTiles(diskId);
        }
        return instance;
    }

    public int getTileCount() {
        return tiles.size();
    }

    public String getTileName(int tileIndex) {
        return tiles.get(tileIndex).name;
    }

    // Initialization on demand holder idiom
    private List<DiskTile> loadAuxData() {
        // tiles theoretically covered by disk:
//        InputStream inputStream = MeteosatDiskTiles.class.getResourceAsStream("tiles_disk_" + diskId + ".txt");
        // tiles covered by disk part which is really filled with data:
        InputStream inputStream = MeteosatDiskTiles.class.getResourceAsStream("tiles_disk_" + diskId + "_cut.txt");
        InputStreamReader streamReader = new InputStreamReader(inputStream);
        CsvReader csvReader = new CsvReader(streamReader, SEPARATOR);
        List<String[]> records;
        try {
            records = csvReader.readStringRecords();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load auxdata", e);
        }
        List<DiskTile> tiles = new ArrayList<>(records.size());
        for (String[] record : records) {
            String name = record[0].trim();
            tiles.add(new DiskTile(name));
        }
        return tiles;
    }

    private static class DiskTile {
        private final String name;

        DiskTile(String name) {
            this.name = name;
        }
    }

}

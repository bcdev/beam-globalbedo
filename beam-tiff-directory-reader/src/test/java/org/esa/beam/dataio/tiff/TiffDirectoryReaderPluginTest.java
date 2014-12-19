/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TiffDirectoryReaderPluginTest {

    @Test
    public void testIsCompressedFile() {
        assertTrue(TiffDirectoryReaderPlugin.isCompressedFile(new File("test.zip")));
        assertTrue(TiffDirectoryReaderPlugin.isCompressedFile(new File("test.tar")));
        assertTrue(TiffDirectoryReaderPlugin.isCompressedFile(new File("test.tgz")));
        assertTrue(TiffDirectoryReaderPlugin.isCompressedFile(new File("test.tar.gz")));
        assertTrue(TiffDirectoryReaderPlugin.isCompressedFile(new File("test.gz")));

        assertFalse(TiffDirectoryReaderPlugin.isCompressedFile(new File("test.txt")));
        assertFalse(TiffDirectoryReaderPlugin.isCompressedFile(new File("test.doc")));
        assertFalse(TiffDirectoryReaderPlugin.isCompressedFile(new File("test.xml")));
        assertFalse(TiffDirectoryReaderPlugin.isCompressedFile(new File("test")));
    }

    @Test
    public void testIsMetadataFile() {
        final File metaFile = new File("bla_HC_meta.txt");
        final File nonMetaFile = new File("L5043033_03319950627_B50.TIF");

        assertTrue(TiffDirectoryReaderPlugin.isMetadataFile(metaFile));
        assertFalse(TiffDirectoryReaderPlugin.isMetadataFile(nonMetaFile));
    }

    @Test
    public void testGetInputTypes() {
        final TiffDirectoryReaderPlugin plugin = new TiffDirectoryReaderPlugin();

        final Class[] inputTypes = plugin.getInputTypes();
        assertEquals(2, inputTypes.length);
        assertEquals(String.class, inputTypes[0]);
        assertEquals(File.class, inputTypes[1]);
    }

    @Test
    public void testIsMatchingArchiveFileName() {
         assertTrue(TiffDirectoryReaderPlugin.isMatchingArchiveFileName("Sample_30m19950627.tgz"));
         assertTrue(TiffDirectoryReaderPlugin.isMatchingArchiveFileName("Sample_L5_60m19950627.tgz"));
         assertTrue(TiffDirectoryReaderPlugin.isMatchingArchiveFileName("Sample_LE71810402006015ASN00.tar"));
         assertTrue(TiffDirectoryReaderPlugin.isMatchingArchiveFileName("Sample_L7_1810402006015ASN00.tar"));

        assertFalse(TiffDirectoryReaderPlugin.isMatchingArchiveFileName("ATS_TOA_1PPTOM20070110_192521_000000822054_00328_25432_0001.N1"));
        assertFalse(TiffDirectoryReaderPlugin.isMatchingArchiveFileName("SchnickSchnack.zip"));
        assertFalse(TiffDirectoryReaderPlugin.isMatchingArchiveFileName("hoppla.txt"));
        assertFalse(TiffDirectoryReaderPlugin.isMatchingArchiveFileName(""));
    }
}

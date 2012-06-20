package org.esa.beam.dataio.tiff;

import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
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

package org.esa.beam.dataio;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MisrProductReaderTest {

    @Test
    public void testMfgAlbedoFileNameMatches() {
        String filename = "bla.txt";
        try {
            MisrProductReader.mfgAlbedoFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Albedo_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }
        filename = "MSA_Albedo_.01_000_2006_001_010.jpg";
        try {
            MisrProductReader.mfgAlbedoFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Albedo_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }
        filename = "MSA_Albedo_L2.0_V2.01_000_2006_001_010.dim";
        try {
            MisrProductReader.mfgAlbedoFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Albedo_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }

        filename = "MSA_Albedo_L2.0_V2.01_000_2006_001_010.hdf";
        assertEquals(true, MisrProductReader.mfgAlbedoFileNameMatches(filename));
        filename = "MSA_Albedo_L2.0_V2.01_000_2006_001_010.HDF";
        assertEquals(true, MisrProductReader.mfgAlbedoFileNameMatches(filename));
    }

    @Test
    public void testAncillaryFileNameMatches() {
        String filename = "bla.txt";
        try {
            MisrProductReader.mfgAncillaryFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Ancillary_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }
        filename = "MSA_Ancillary_.01_000_2006_001_010.jpg";
        try {
            MisrProductReader.mfgAncillaryFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Ancillary_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }
        filename = "MSA_Ancillary_L2.0_V2.01_000_2006_001_010.dim";
        try {
            MisrProductReader.mfgAncillaryFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Ancillary_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }

        filename = "MSA_Ancillary_L2.0_V2.01_000_2006_001_010.hdf";
        assertEquals(true, MisrProductReader.mfgAncillaryFileNameMatches(filename));
        filename = "MSA_Ancillary_L2.0_V2.01_000_2006_001_010.HDF";
        assertEquals(true, MisrProductReader.mfgAncillaryFileNameMatches(filename));
    }

    @Test
    public void testStaticInputFileNameMatches() {
        String filename = "bla.txt";
        try {
            MisrProductReader.mfgStaticInputFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Static_L2.0_Vm.nn_sss_m.HDF'", e.getMessage());
        }
        filename = "MSA_Static_.01_000_2006_001_010.jpg";
        try {
            MisrProductReader.mfgStaticInputFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Static_L2.0_Vm.nn_sss_m.HDF'", e.getMessage());
        }
        filename = "MSA_Static_L2.0_V2.01_000_2006_001_010.dim";
        try {
            MisrProductReader.mfgStaticInputFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Static_L2.0_Vm.nn_sss_m.HDF'", e.getMessage());
        }

        filename = "MSA_Static_L2.0_V2.01_000_0.hdf";
        assertEquals(true, MisrProductReader.mfgStaticInputFileNameMatches(filename));
        filename = "MSA_Static_L2.0_V2.01_000_0.HDF";
        assertEquals(true, MisrProductReader.mfgStaticInputFileNameMatches(filename));
    }

}

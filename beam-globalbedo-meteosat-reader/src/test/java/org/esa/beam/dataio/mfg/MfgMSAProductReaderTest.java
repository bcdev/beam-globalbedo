package org.esa.beam.dataio.mfg;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MfgMSAProductReaderTest {

    @Test
    public void testMfgAlbedoFileNameMatches() {
        String filename = "bla.txt";
        try {
            MfgMSAProductReader.mfgAlbedoFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Albedo_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }
        filename = "MSA_Albedo_.01_000_2006_001_010.jpg";
        try {
            MfgMSAProductReader.mfgAlbedoFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Albedo_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }
        filename = "MSA_Albedo_L2.0_V2.01_000_2006_001_010.dim";
        try {
            MfgMSAProductReader.mfgAlbedoFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Albedo_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }

        filename = "MSA_Albedo_L2.0_V2.01_000_2006_001_010.hdf";
        assertEquals(true, MfgMSAProductReader.mfgAlbedoFileNameMatches(filename));
        filename = "MSA_Albedo_L2.0_V2.01_000_2006_001_010.HDF";
        assertEquals(true, MfgMSAProductReader.mfgAlbedoFileNameMatches(filename));
    }

    @Test
    public void testAncillaryFileNameMatches() {
        String filename = "bla.txt";
        try {
            MfgMSAProductReader.mfgAncillaryFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Ancillary_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }
        filename = "MSA_Ancillary_.01_000_2006_001_010.jpg";
        try {
            MfgMSAProductReader.mfgAncillaryFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Ancillary_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }
        filename = "MSA_Ancillary_L2.0_V2.01_000_2006_001_010.dim";
        try {
            MfgMSAProductReader.mfgAncillaryFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Ancillary_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }

        filename = "MSA_Ancillary_L2.0_V2.01_000_2006_001_010.hdf";
        assertEquals(true, MfgMSAProductReader.mfgAncillaryFileNameMatches(filename));
        filename = "MSA_Ancillary_L2.0_V2.01_000_2006_001_010.HDF";
        assertEquals(true, MfgMSAProductReader.mfgAncillaryFileNameMatches(filename));
    }

    @Test
    public void testStaticInputFileNameMatches() {
        String filename = "bla.txt";
        try {
            MfgMSAProductReader.mfgStaticInputFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Static_L2.0_Vm.nn_sss_m.HDF'", e.getMessage());
        }
        filename = "MSA_Static_.01_000_2006_001_010.jpg";
        try {
            MfgMSAProductReader.mfgStaticInputFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Static_L2.0_Vm.nn_sss_m.HDF'", e.getMessage());
        }
        filename = "MSA_Static_L2.0_V2.01_000_2006_001_010.dim";
        try {
            MfgMSAProductReader.mfgStaticInputFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Static_L2.0_Vm.nn_sss_m.HDF'", e.getMessage());
        }

        filename = "MSA_Static_L2.0_V2.01_000_0.hdf";
        assertEquals(true, MfgMSAProductReader.mfgStaticInputFileNameMatches(filename));
        filename = "MSA_Static_L2.0_V2.01_000_0.HDF";
        assertEquals(true, MfgMSAProductReader.mfgStaticInputFileNameMatches(filename));
    }

}

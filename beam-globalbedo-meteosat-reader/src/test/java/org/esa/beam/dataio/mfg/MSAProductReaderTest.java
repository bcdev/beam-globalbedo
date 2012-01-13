package org.esa.beam.dataio.mfg;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MSAProductReaderTest {

    @Test
    public void testAlbedoFileNameMatches() {
        String filename = "bla.txt";
        try {
            MSAProductReader.albedoFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Albedo_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }
        filename = "MSA_Albedo_.01_000_2006_001_010.jpg";
        try {
            MSAProductReader.albedoFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Albedo_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }
        filename = "MSA_Albedo_L2.0_V2.01_000_2006_001_010.dim";
        try {
            MSAProductReader.albedoFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Albedo_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }

        filename = "MSA_Albedo_L2.0_V2.01_000_2006_001_010.hdf";
        assertEquals(true, MSAProductReader.albedoFileNameMatches(filename));
        filename = "MSA_Albedo_L2.0_V2.01_000_2006_001_010.HDF";
        assertEquals(true, MSAProductReader.albedoFileNameMatches(filename));
    }

    @Test
    public void testAncillaryFileNameMatches() {
        String filename = "bla.txt";
        try {
            MSAProductReader.ancillaryFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Ancillary_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }
        filename = "MSA_Ancillary_.01_000_2006_001_010.jpg";
        try {
            MSAProductReader.ancillaryFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Ancillary_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }
        filename = "MSA_Ancillary_L2.0_V2.01_000_2006_001_010.dim";
        try {
            MSAProductReader.ancillaryFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Ancillary_L2.0_Vm.nn_sss_yyyy_fff_lll.HDF'", e.getMessage());
        }

        filename = "MSA_Ancillary_L2.0_V2.01_000_2006_001_010.hdf";
        assertEquals(true, MSAProductReader.ancillaryFileNameMatches(filename));
        filename = "MSA_Ancillary_L2.0_V2.01_000_2006_001_010.HDF";
        assertEquals(true, MSAProductReader.ancillaryFileNameMatches(filename));
    }

    @Test
    public void testStaticInputFileNameMatches() {
        String filename = "bla.txt";
        try {
            MSAProductReader.staticInputFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Static_L2.0_Vm.nn_sss_m.HDF'", e.getMessage());
        }
        filename = "MSA_Static_.01_000_2006_001_010.jpg";
        try {
            MSAProductReader.staticInputFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Static_L2.0_Vm.nn_sss_m.HDF'", e.getMessage());
        }
        filename = "MSA_Static_L2.0_V2.01_000_2006_001_010.dim";
        try {
            MSAProductReader.staticInputFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA_Static_L2.0_Vm.nn_sss_m.HDF'", e.getMessage());
        }

        filename = "MSA_Static_L2.0_V2.01_000_0.hdf";
        assertEquals(true, MSAProductReader.staticInputFileNameMatches(filename));
        filename = "MSA_Static_L2.0_V2.01_000_0.HDF";
        assertEquals(true, MSAProductReader.staticInputFileNameMatches(filename));
    }
}

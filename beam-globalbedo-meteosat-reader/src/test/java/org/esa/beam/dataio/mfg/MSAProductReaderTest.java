package org.esa.beam.dataio.mfg;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MSAProductReaderTest {

    @Test
    public void testPatchFileName() {
        String filename = "bla.txt";
        try {
            MSAProductReader.validateAlbedoInputFileName(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA Albedo L2.0 Vm.nn sss yyyy fff lll.HDF'", e.getMessage());
        }
        filename = "MSA_Albedo_.01_000_2006_001_010.jpg";
        try {
            MSAProductReader.validateAlbedoInputFileName(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA Albedo L2.0 Vm.nn sss yyyy fff lll.HDF'", e.getMessage());
        }
        filename = "MSA_Albedo_L2.0_V2.01_000_2006_001_010.dim";
        try {
            MSAProductReader.validateAlbedoInputFileName(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'MSA Albedo L2.0 Vm.nn sss yyyy fff lll.HDF'", e.getMessage());
        }

        filename = "MSA_Albedo_L2.0_V2.01_000_2006_001_010.hdf";
        assertEquals(true, MSAProductReader.validateAlbedoInputFileName(filename));
        filename = "MSA_Albedo_L2.0_V2.01_000_2006_001_010.HDF";
        assertEquals(true, MSAProductReader.validateAlbedoInputFileName(filename));
    }

    // todo: continue
}

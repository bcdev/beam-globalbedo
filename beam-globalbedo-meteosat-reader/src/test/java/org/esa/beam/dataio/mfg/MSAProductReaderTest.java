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

    @Test
    public void testSpectralBandsProperties() {
        // todo
//        final float[] wavelengths = new float[21];
//        final float[] bandwidths = new float[21];
//        MSAProductReader.getSpectralBandsProperties(wavelengths, bandwidths);
//
//        assertEquals(400.0, wavelengths[0], 0.0);
//        assertEquals(412.5, wavelengths[1], 0.0);
//        assertEquals(1020.0, wavelengths[20], 0.0);
//
//        assertEquals(400.0, wavelengths[0], 0.0);
//        assertEquals(412.5, wavelengths[1], 0.0);
//        assertEquals(1020.0, wavelengths[20], 0.0);
    }
}

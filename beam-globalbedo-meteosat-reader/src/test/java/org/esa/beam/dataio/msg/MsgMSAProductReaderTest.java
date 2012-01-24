package org.esa.beam.dataio.msg;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MsgMSAProductReaderTest {


    @Test
    public void testMsgAlbedoFileNameMatches() {
        String filename = "bla.txt";
        try {
            MsgMSAProductReader.msgAlbedoFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'HDF5_LSASAF_MSG_ALBEDO_<area>_yyyymmddhhmm'", e.getMessage());
        }
        filename = "HDF5_LSASAF_MSG_ALBEDO_Euro_200601070000.jpg";
        try {
            MsgMSAProductReader.msgAlbedoFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'HDF5_LSASAF_MSG_ALBEDO_<area>_yyyymmddhhmm'", e.getMessage());
        }
        filename = "HDF5_LSASAF_MSG_ALBEDO_Euro_200601070000.dim";
        try {
            MsgMSAProductReader.msgAlbedoFileNameMatches(filename);
        } catch (Exception e) {
            assertEquals(true, e instanceof IllegalArgumentException);
            assertEquals("Input file name '" + filename +
                    "' does not match naming convention: 'HDF5_LSASAF_MSG_ALBEDO_<area>_yyyymmddhhmm'", e.getMessage());
        }

        filename = "HDF5_LSASAF_MSG_ALBEDO_Euro_200601070000.hdf";
        assertEquals(true, MsgMSAProductReader.msgAlbedoFileNameMatches(filename));
    }

}

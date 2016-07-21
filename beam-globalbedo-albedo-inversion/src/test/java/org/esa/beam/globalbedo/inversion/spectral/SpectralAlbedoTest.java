package org.esa.beam.globalbedo.inversion.spectral;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class SpectralAlbedoTest extends TestCase {

    public void testGetSpectralAlbedoAlphaBandNames() {
        int numSdrBands = 7;
        final String[][] bhrAlphaBandNames =
                SpectralIOUtils.getSpectralAlbedoAlphaBandNames("BHR", numSdrBands, getSpectralWaveBandsMap(numSdrBands));
        assertNotNull(bhrAlphaBandNames);
        assertEquals(6, bhrAlphaBandNames[0].length);
        assertEquals("BHR_alpha_b1_b1", bhrAlphaBandNames[0][0]);
        assertEquals("BHR_alpha_b2_b2", bhrAlphaBandNames[1][1]);
        assertEquals("BHR_alpha_b3_b3", bhrAlphaBandNames[2][2]);
        assertEquals("BHR_alpha_b4_b4", bhrAlphaBandNames[3][3]);
        assertEquals("BHR_alpha_b5_b5", bhrAlphaBandNames[4][4]);
        assertEquals("BHR_alpha_b6_b6", bhrAlphaBandNames[5][5]);
        assertEquals("BHR_alpha_b2_b4", bhrAlphaBandNames[1][3]);
        assertEquals("BHR_alpha_b3_b5", bhrAlphaBandNames[2][4]);
        assertEquals("BHR_alpha_b5_b6", bhrAlphaBandNames[4][5]);
        assertNull(bhrAlphaBandNames[3][1]);
        assertNull(bhrAlphaBandNames[4][2]);
        assertNull(bhrAlphaBandNames[5][4]);

        final String[][] dhrAlphaBandNames =
                SpectralIOUtils.getSpectralAlbedoAlphaBandNames("DHR", numSdrBands, getSpectralWaveBandsMap(numSdrBands));
        assertNotNull(dhrAlphaBandNames);
        assertEquals(6, dhrAlphaBandNames[0].length);
        assertEquals("DHR_alpha_b1_b1", dhrAlphaBandNames[0][0]);
        assertEquals("DHR_alpha_b2_b4", dhrAlphaBandNames[1][3]);
    }

    private static Map<Integer, String> getSpectralWaveBandsMap(int numSdrBands) {
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < numSdrBands; i++) {
            map.put(i, "b" + (i + 1));
        }
        return map;
    }
}

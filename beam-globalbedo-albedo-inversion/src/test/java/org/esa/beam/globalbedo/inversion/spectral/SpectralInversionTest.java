package org.esa.beam.globalbedo.inversion.spectral;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class SpectralInversionTest extends TestCase {

    public void testGetSdrBandNames() {
        int numSdrBands = 7;
        final String[] sdrBandNames = SpectralInversionUtils.getSdrBandNames(numSdrBands);
        assertNotNull(sdrBandNames);
        assertEquals(numSdrBands, sdrBandNames.length);
        for (int i = 0; i < numSdrBands; i++) {
            assertEquals("SDR_" + i, sdrBandNames[i]);
        }
    }

    public void testGetSdrSigmaBandNames() {
        int numSdrBands = 7;
//        int numSigmaSdrBands = (numSdrBands * numSdrBands - numSdrBands) / 2 + numSdrBands;
        int numSigmaSdrBands = numSdrBands;
        final String[] sigmaSdrBandNames = SpectralInversionUtils.getSigmaSdrBandNames(numSigmaSdrBands);
        assertNotNull(sigmaSdrBandNames);
        assertEquals(7, sigmaSdrBandNames.length);
        assertEquals("SDR_SIGMA_0", sigmaSdrBandNames[0]);
        assertEquals("SDR_SIGMA_4", sigmaSdrBandNames[4]);
        assertEquals("SDR_SIGMA_6", sigmaSdrBandNames[6]);
    }

    public void testGetSigmaSdrDiagonalIndices() {
        int numSdrBands = 7;
        final int[] sigmaSdrDiagonalIndices = SpectralInversionUtils.getSigmaSdrDiagonalIndices(numSdrBands);
        assertNotNull(sigmaSdrDiagonalIndices);
        assertEquals(7, sigmaSdrDiagonalIndices.length);
        // for 7 bands, diagonal indices are 0, 7, 13, 18, 22, 25, 27
        assertEquals(0, sigmaSdrDiagonalIndices[0]);
        assertEquals(7, sigmaSdrDiagonalIndices[1]);
        assertEquals(13, sigmaSdrDiagonalIndices[2]);
        assertEquals(18, sigmaSdrDiagonalIndices[3]);
        assertEquals(22, sigmaSdrDiagonalIndices[4]);
        assertEquals(25, sigmaSdrDiagonalIndices[5]);
        assertEquals(27, sigmaSdrDiagonalIndices[6]);
    }

    public void testGetSigmaSdrURIndices() {
        int numSdrBands = 7;
        int numSigmaSdrBands = (numSdrBands * numSdrBands - numSdrBands) / 2 + numSdrBands;
        final int[] sigmaSdrURIndices = SpectralInversionUtils.getSigmaSdrURIndices(numSdrBands, numSigmaSdrBands);
        assertNotNull(sigmaSdrURIndices);
        assertEquals(21, sigmaSdrURIndices.length);
        // for 7 bands, UR indices are 1-6, 8-12, 14-17, 19-21, 23-24, 26
        assertEquals(1, sigmaSdrURIndices[0]);
        assertEquals(2, sigmaSdrURIndices[1]);
        assertEquals(5, sigmaSdrURIndices[4]);
        assertEquals(9, sigmaSdrURIndices[7]);
        assertEquals(14, sigmaSdrURIndices[11]);
        assertEquals(16, sigmaSdrURIndices[13]);
        assertEquals(20, sigmaSdrURIndices[16]);
        assertEquals(24, sigmaSdrURIndices[19]);
        assertEquals(26, sigmaSdrURIndices[20]);
    }


    public void testGetSpectralInversionParameterBandNames() {
        int numSdrBands = 3;
        String[] parameterBandNames = SpectralIOUtils.getSpectralInversionParameterBandNames(numSdrBands);
        assertNotNull(parameterBandNames);
        assertEquals(9, parameterBandNames.length);
        assertEquals("mean_b1_f0", parameterBandNames[0]);
        assertEquals("mean_b1_f1", parameterBandNames[1]);
        assertEquals("mean_b1_f2", parameterBandNames[2]);
        assertEquals("mean_b2_f0", parameterBandNames[3]);
        assertEquals("mean_b2_f1", parameterBandNames[4]);
        assertEquals("mean_b2_f2", parameterBandNames[5]);
        assertEquals("mean_b3_f0", parameterBandNames[6]);
        assertEquals("mean_b3_f1", parameterBandNames[7]);
        assertEquals("mean_b3_f2", parameterBandNames[8]);

        numSdrBands = 7;
        parameterBandNames = SpectralIOUtils.getSpectralInversionParameterBandNames(numSdrBands);
        assertNotNull(parameterBandNames);
        assertEquals(21, parameterBandNames.length);
        assertEquals("mean_b1_f0", parameterBandNames[0]);
        assertEquals("mean_b1_f1", parameterBandNames[1]);
        assertEquals("mean_b3_f2", parameterBandNames[8]);
        assertEquals("mean_b6_f2", parameterBandNames[17]);
        assertEquals("mean_b7_f2", parameterBandNames[20]);

    }

    public void testGetInversionUncertaintyBandNames() {
        int singleBandIndex = 3;
        String[][] uncertaintyBandNames =
                SpectralIOUtils.getSpectralInversionUncertaintySingleBandNames(getSingleSpectralWaveBandMap(singleBandIndex));
        assertNotNull(uncertaintyBandNames);
        assertEquals(3, uncertaintyBandNames.length);
        for (int i = 0; i < 3; i++) {
            assertEquals(3, uncertaintyBandNames[i].length);
        }
        assertEquals("VAR_b3_f0_b3_f0", uncertaintyBandNames[0][0]);
        assertEquals("VAR_b3_f1_b3_f1", uncertaintyBandNames[0][1]);
        assertEquals("VAR_b3_f2_b3_f2", uncertaintyBandNames[0][2]);
        assertNull(uncertaintyBandNames[1][0]);
        assertEquals("VAR_b3_f1_b3_f1", uncertaintyBandNames[1][1]);
        assertEquals("VAR_b3_f2_b3_f2", uncertaintyBandNames[1][2]);
        assertNull(uncertaintyBandNames[2][0]);
        assertNull(uncertaintyBandNames[2][1]);
        assertEquals("VAR_b3_f2_b3_f2", uncertaintyBandNames[2][2]);

        int numSdrBands = 7;
        uncertaintyBandNames =
                SpectralIOUtils.getSpectralInversionUncertaintyBandNames(numSdrBands, getSpectralWaveBandsMap(numSdrBands));
        assertNotNull(uncertaintyBandNames);
        assertEquals(21, uncertaintyBandNames.length);
        for (int i = 0; i < 21; i++) {
            assertEquals(21, uncertaintyBandNames[i].length);
        }
        assertEquals("VAR_b1_f0_b1_f0", uncertaintyBandNames[0][0]);
        assertEquals("VAR_b2_f2_b2_f2", uncertaintyBandNames[1][2]);
        assertEquals("VAR_b3_f2_b3_f2", uncertaintyBandNames[2][2]);
        assertEquals("VAR_b5_f1_b5_f1", uncertaintyBandNames[4][1]);
        assertEquals("VAR_b6_f0_b6_f0", uncertaintyBandNames[5][0]);
        assertEquals("VAR_b7_f2_b7_f2", uncertaintyBandNames[6][2]);

    }

    public void testCovarianceNames() throws Exception {
        int[] bandIndices = {4, 1, 3};
        Map<Integer, String> spectralWaveBandsMap = new HashMap<>();
        spectralWaveBandsMap.put(0, "b4");
        spectralWaveBandsMap.put(1, "b1");
        spectralWaveBandsMap.put(2, "b3");
        String[][] uncertaintyBandNames =
                SpectralIOUtils.getSpectralInversionUncertainty3BandNames(bandIndices, spectralWaveBandsMap);
        for (String[] uncertaintyBandName : uncertaintyBandNames) {
            for (String s : uncertaintyBandName) {
                System.out.println("uncertaintyBandName = " + s);
            }
        }
        System.out.println();
    }

    private static Map<Integer, String> getSpectralWaveBandsMap(int numSdrBands) {
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < numSdrBands; i++) {
            map.put(i, "b" + (i + 1));
        }
        return map;
    }

    private static Map<Integer, String> getSingleSpectralWaveBandMap(int singleBandIndex) {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "b" + singleBandIndex);
        return map;
    }
}

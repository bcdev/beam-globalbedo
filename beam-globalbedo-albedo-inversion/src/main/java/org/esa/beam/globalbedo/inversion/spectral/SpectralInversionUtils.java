package org.esa.beam.globalbedo.inversion.spectral;

import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;

/**
 * Utility class for spectral BRF(SDR) --> BRDF
 *
 * @author olafd
 */
public class SpectralInversionUtils {

    public static int[] getSigmaSdrDiagonalIndices(int numSdrBands) {
        // sigma_00 xx xx xx xx xx xx
        // sigma_   01 xx xx xx xx xx
        // sigma_      02 xx xx xx xx
        // sigma_         03 xx xx xx
        // sigma_            04 xx xx
        // sigma_               05 xx
        // sigma_                  06
        int[] sigmaSdrDiagonalIndices = new int[numSdrBands];
        int index = 0;
        for (int j = 0; j < numSdrBands; j++) {
            // for 7 bands, diagonal indices are 0, 7, 13, 18, 22, 25, 27
            sigmaSdrDiagonalIndices[j] = index;
            index += (7-j);
        }
        return sigmaSdrDiagonalIndices;
    }

    public static int[] getSigmaSdrURIndices(int numSdrBands, int numSigmaSdrBands) {
        // sigma_xx 01 02 03 04 05 06
        // sigma_   xx 02 03 04 05 06
        // sigma_      xx 03 04 05 06
        // sigma_         xx 04 05 06
        // sigma_            xx 05 06
        // sigma_               xx 06
        // sigma_                  xx
        int[] sigmaSdrURIndices = new int[numSigmaSdrBands - numSdrBands];
        int index = 0;
        // for 7 bands, UR indices are 1-6, 8-12, 14-17, 19-21, 23-24, 26
        int count = 0;
        for (int i = 0; i < numSdrBands; i++) {
            for (int j = i; j < numSdrBands; j++) {
                if (j > i) {
                    sigmaSdrURIndices[index] = count;
                    index++;
                }
                count++;
            }
        }
        return sigmaSdrURIndices;
    }

//    public static String[] getSigmaSdrBandNames(int numSdrBands, int numSigmaSdrBands) {
//        // sigma_00 01 02 03 04 05 06
//        // sigma_   01 02 03 04 05 06
//        // sigma_      02 03 04 05 06
//        // sigma_         03 04 05 06
//        // sigma_            04 05 06
//        // sigma_               05 06
//        // sigma_                  06
//        String[] sigmaSdrBandNames = new String[numSigmaSdrBands];
//        int index = 0;
//        for (int i = 0; i < numSdrBands; i++) {
//            for (int j = i; j < numSdrBands; j++) {
//                if (index < numSigmaSdrBands) {
//                    sigmaSdrBandNames[index++] = AlbedoInversionConstants.MODIS_SPECTRAL_SDR_SIGMA_NAME_PREFIX + i + "_" + j;
//                }
//            }
//        }
//        return sigmaSdrBandNames;
//    }

    public static String[] getSigmaSdrBandNames(int numSigmaSdrBands) {
        // we do not need the cross terms...
        String[] sdrBandNames = new String[numSigmaSdrBands];
        for (int i = 0; i < numSigmaSdrBands; i++) {
            sdrBandNames[i] = AlbedoInversionConstants.MODIS_SPECTRAL_SDR_SIGMA_NAME_PREFIX + i;
        }
        return sdrBandNames;
    }

    public static String[] getSdrBandNames(int numSdrBands) {
        String[] sdrBandNames = new String[numSdrBands];
        for (int i = 0; i < numSdrBands; i++) {
            sdrBandNames[i] = AlbedoInversionConstants.MODIS_SPECTRAL_SDR_NAME_PREFIX + i;
        }
        return sdrBandNames;
    }


}

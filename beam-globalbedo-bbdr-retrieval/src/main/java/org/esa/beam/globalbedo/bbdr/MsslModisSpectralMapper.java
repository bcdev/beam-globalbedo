package org.esa.beam.globalbedo.bbdr;

/**
 * Class for spectral mapping to MODIS from MERIS/VGT
 *
 * @author Said Kharbouche, Olaf Danne
 */
public class MsslModisSpectralMapper {

    private static double[] MSE = new double[]{
            0.000149339352436,
            0.000202888197683,
            0.000253645086521,
            0.000168781393185,
            0.000730908703564,
            0.00132911251515,
            0.00148779972301
    };

    private Sensor sensor;

    public MsslModisSpectralMapper(Sensor sensor) {
        this.sensor = sensor;
    }

    // map sdr_err to modis bands

    float[] getSpectralMappedSigmaSdr(float[] errs) {
        float vals[] = getSpectralMappedSdr(errs);
        for (int a = 0; a < BbdrConstants.MODIS_NUM_SPECTRAL_BANDS; a++){
            vals[a] += Math.sqrt((vals[a]*vals[a])+ MSE[a]);
            if(vals[a]>1f)
                vals[a]=1f;
            if(vals[a]<=0f)
                vals[a]=0.01f;
        }
        return vals;
    }

    // map sdr to modis bands
    float[] getSpectralMappedSdr(float[] sdrs) {
        float vals[] = new float[BbdrConstants.MODIS_NUM_SPECTRAL_BANDS];
        for (int a = 0; a < BbdrConstants.MODIS_NUM_SPECTRAL_BANDS; a++){
            for (int b = 0; b < sensor.getNumBands(); b++) {
                vals[a] += sdrs[b] * sensor.getSpectralMappingCoeffs()[a][b];
            }

            if(vals[a]>1f)
                vals[a]=1f;
            else
            if (vals[a]<0.0)
                vals[a]=0;
        }

        return vals;
    }

}

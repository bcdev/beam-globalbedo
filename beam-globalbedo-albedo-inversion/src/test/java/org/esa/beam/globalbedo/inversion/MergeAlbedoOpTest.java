package org.esa.beam.globalbedo.inversion;

import junit.framework.TestCase;

public class MergeAlbedoOpTest extends TestCase {

    public void testGetWeightedMergeValue() {

        double sza = 60.0;

        // valid snow and nosnow samples
        double noSnowValue = 0.32;
        double snowValue = 0.96;
        double propNoSnow = 0.25;
        double propSnow = 0.75;
        double mergeValue = MergeAlbedoOp.getWeightedMergeValue(noSnowValue, snowValue, propNoSnow, propSnow, sza);
        assertEquals(0.8, mergeValue, 1.E-3);

        noSnowValue = 0.2;
        snowValue = 0.8;
        propNoSnow = 0.9;
        propSnow = 0.1;
        mergeValue = MergeAlbedoOp.getWeightedMergeValue(noSnowValue, snowValue, propNoSnow, propSnow, sza);
        assertEquals(0.26, mergeValue, 1.E-3);

        // valid snow sample, no nosnow sample
        noSnowValue = 0.2;
        snowValue = 0.8;
        propNoSnow = 0.0;
        propSnow = 1.0;
        mergeValue = MergeAlbedoOp.getWeightedMergeValue(noSnowValue, snowValue, propNoSnow, propSnow, sza);
        assertEquals(0.8, mergeValue, 1.E-3);
        noSnowValue = Double.NaN;
        mergeValue = MergeAlbedoOp.getWeightedMergeValue(noSnowValue, snowValue, propNoSnow, propSnow, sza);
        assertEquals(0.8, mergeValue, 1.E-3);

        // valid nosnow sample, no snow sample
        noSnowValue = 0.2;
        snowValue = 0.8;
        propNoSnow = 1.0;
        propSnow = 0.0;
        mergeValue = MergeAlbedoOp.getWeightedMergeValue(noSnowValue, snowValue, propNoSnow, propSnow, sza);
        assertEquals(0.2, mergeValue, 1.E-3);
        snowValue = Double.NaN;
        mergeValue = MergeAlbedoOp.getWeightedMergeValue(noSnowValue, snowValue, propNoSnow, propSnow, sza);
        assertEquals(0.2, mergeValue, 1.E-3);

        // no valid samples, only snow and nosnow priors
        noSnowValue = 0.2;  // prior
        snowValue = 0.8;    // prior
        propNoSnow = 0.0;
        propSnow = 0.0;
        mergeValue = MergeAlbedoOp.getWeightedMergeValue(noSnowValue, snowValue, propNoSnow, propSnow, sza);
        assertEquals(0.5, mergeValue, 1.E-3);
        sza = 89.0;
        mergeValue = MergeAlbedoOp.getWeightedMergeValue(noSnowValue, snowValue, propNoSnow, propSnow, sza);
        assertEquals(0.8, mergeValue, 1.E-3);

        sza = 60.0;
        // no valid samples, only nosnow prior
        noSnowValue = 0.2;  // prior
        snowValue = Double.NaN;    // prior
        propNoSnow = 0.0;
        propSnow = 0.0;
        mergeValue = MergeAlbedoOp.getWeightedMergeValue(noSnowValue, snowValue, propNoSnow, propSnow, sza);
        assertEquals(0.2, mergeValue, 1.E-3);

        // no valid samples, only snow prior
        noSnowValue = Double.NaN;  // prior
        snowValue = 0.8;    // prior
        propNoSnow = 0.0;
        propSnow = 0.0;
        mergeValue = MergeAlbedoOp.getWeightedMergeValue(noSnowValue, snowValue, propNoSnow, propSnow, sza);
        assertEquals(0.8, mergeValue, 1.E-3);

        // no valid samples, no prior
        noSnowValue = Double.NaN;  // prior
        snowValue = Double.NaN;    // prior
        propNoSnow = 0.0;
        propSnow = 0.0;
        mergeValue = MergeAlbedoOp.getWeightedMergeValue(noSnowValue, snowValue, propNoSnow, propSnow, sza);
        assertEquals(AlbedoInversionConstants.NO_DATA_VALUE, mergeValue, 1.E-3);
    }

}

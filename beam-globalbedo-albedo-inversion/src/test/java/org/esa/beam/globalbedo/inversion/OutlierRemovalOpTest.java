package org.esa.beam.globalbedo.inversion;

import junit.framework.TestCase;
import org.junit.Test;

public class OutlierRemovalOpTest extends TestCase {

    @Test
    public void testCheckForOutliers() throws Exception {

        float bbdrValue = 0.09161f;
        float[] priorParms = new float[]{0.021484375f, 0.016601562f, 0.0029296875f};
        float[] priorSd = new float[]{0.00390625f, 0.0029296875f, 0.001953125f};
        float kvol = 0.03971f;
        float kgeo = -0.60837f;
        final float delta = OutlierRetrievalOp.checkForOutliers(bbdrValue, priorParms, priorSd, kvol, kgeo);
        System.out.println("delta = " + delta);
        System.out.println();
    }
}

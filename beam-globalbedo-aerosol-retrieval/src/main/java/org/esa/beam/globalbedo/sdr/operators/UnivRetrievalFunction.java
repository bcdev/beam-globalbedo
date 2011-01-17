/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import org.esa.beam.globalbedo.sdr.util.math.Function;

/**
 *
 * @author akheckel
 */
public interface UnivRetrievalFunction extends Function {

    float[] getModelReflec(float aot);

    double[] getpAtMin();

    double[] getSurfReflec(float aot);

}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.lutUtils;

import org.esa.beam.globalbedo.sdr.operators.InputPixelData;

/**
 *
 * @author akheckel
 */
public interface AerosolLookupTable {
    public void getSdrAndDiffuseFrac(InputPixelData inPixel, double tau);
}

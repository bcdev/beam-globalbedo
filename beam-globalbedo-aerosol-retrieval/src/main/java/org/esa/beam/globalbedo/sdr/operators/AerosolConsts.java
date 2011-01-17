/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import org.esa.beam.framework.datamodel.ProductData;

/**
 * Class defining constants needed by the Globalbedo Aerosol processor
 * should be mainly defining "output" constants
 * those needed for interpreting the input are in InstrumentConsts
 *
 * @author akheckel
 */
public class AerosolConsts {
    public static final String aotName = "aot";
    public static final String aotDescription = "aerosol optical thickness";
    public static final String aotUnit = "dl";
    public static final int aotType = ProductData.TYPE_FLOAT32;
    public static final double aotNoDataValue = -1;
    public static final boolean aotNoDataUsed = true;
    public static final double aotScale = 1;
    public static final double aotOffset = 0;

    public static final String aotErrName = "aot";
    public static final String aotErrDescription = "aerosol optical thickness";
    public static final String aotErrUnit = "dl";
    public static final int aotErrType = ProductData.TYPE_FLOAT32;
    public static final double aotErrNoDataValue = -1;
    public static final boolean aotErrNoDataUsed = true;
    public static final double aotErrScale = 1;
    public static final double aotErrOffset = 0;

    public static final String aotFlagsName = "aot";
    public static final String aotFlagsDescription = "aerosol optical thickness";
    public static final String aotFlagsUnit = "dl";
    public static final int aotFlagsType = ProductData.TYPE_FLOAT32;
    public static final double aotFlagsNoDataValue = -1;
    public static final boolean aotFlagsNoDataUsed = true;
    public static final double aotFlagsScale = 1;
    public static final double aotFlagsOffset = 0;
}

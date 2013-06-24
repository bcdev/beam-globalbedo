package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.globalbedo.auxdata.Luts;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
class GasLookupTable {

    private float gas2val = 1.5f; // keep variable name from breadboard
    private final Sensor sensor;

    private float[][][] lutGas;
    private float[][][][][] kxLutGas;

    private float[] amfArray;
    private float[] cwvArray;
    private float[] gasArray;
    private float[] ozoArray;

    GasLookupTable(Sensor sensor) {
        this.sensor = sensor;
    }

    void load(Product sourceProduct) throws IOException {
        if (sourceProduct != null && sensor == Sensor.VGT) {
            float ozoMeanValue = BbdrUtils.getImageMeanValue(sourceProduct.getBand(BbdrConstants.VGT_OZO_BAND_NAME).getGeophysicalImage());
            setGasVal(ozoMeanValue);
            // todo: check small deviation from breadboard mean value calculation (0.24251308 vs. 0.242677)
//            setGasVal(0.242677f); // test!!
        } else {
            setGasVal(BbdrConstants.CWV_CONSTANT_VALUE);
        }
        loadCwvOzoLookupTableArray(sensor);
        loadCwvOzoKxLookupTableArray(sensor);
    }

    void setGasVal(float value) {
        gas2val = value;
    }

    public float getGasMeanVal() {
        return gas2val;
    }

    float[][][] getLutGas() {
        return lutGas;
    }

    float[][][][][] getKxLutGas() {
        return kxLutGas;
    }

    float[] getAmfArray() {
        return amfArray;
    }

    float[] getCwvArray() {
        return cwvArray;
    }

    float[] getGasArray() {
        return gasArray;
    }

    private void loadCwvOzoLookupTableArray(Sensor sensor) throws IOException {
        // todo: test this method!
        ImageInputStream iis = Luts.getCwvLutData(sensor.getInstrument());
        try {
            int nAng = iis.readInt();
            int nCwv = iis.readInt();
            int nOzo = iis.readInt();

            final float[] angArr = Luts.readDimension(iis, nAng);
            cwvArray = Luts.readDimension(iis, nCwv);
            ozoArray = Luts.readDimension(iis, nOzo);

            float[] wvl = sensor.getWavelength();
            final int nWvl = wvl.length;
            float[][][][] cwvOzoLutArray = new float[nWvl][nOzo][nCwv][nAng];
            for (int iAng = 0; iAng < nAng; iAng++) {
                for (int iCwv = 0; iCwv < nCwv; iCwv++) {
                    for (int iOzo = 0; iOzo < nOzo; iOzo++) {
                        for (int iWvl = 0; iWvl < nWvl; iWvl++) {
                            cwvOzoLutArray[iWvl][iOzo][iCwv][iAng] = iis.readFloat();
                        }
                    }
                }
            }
            lutGas = new float[nWvl][nCwv][nAng];
            amfArray = convertAngArrayToAmfArray(angArr);

            if (this.sensor.equals(Sensor.VGT)) {
                int iOzo = BbdrUtils.getIndexBefore(gas2val, ozoArray);
                float term = (gas2val - ozoArray[iOzo]) / (ozoArray[iOzo + 1] - ozoArray[iOzo]);
                for (int iWvl = 0; iWvl < nWvl; iWvl++) {
                    for (int iCwv = 0; iCwv < nCwv; iCwv++) {
                        for (int iAng = 0; iAng < nAng; iAng++) {
                            lutGas[iWvl][iCwv][iAng] = cwvOzoLutArray[iWvl][iOzo][iCwv][iAng] + (cwvOzoLutArray[iWvl][iOzo + 1][iCwv][iAng] - cwvOzoLutArray[iWvl][iOzo][iCwv][iAng]) * term;
                        }
                    }
                }
                gasArray = cwvArray;
            } else {
                int iCwv = BbdrUtils.getIndexBefore(gas2val, cwvArray);
                float term = (gas2val - cwvArray[iCwv]) / (cwvArray[iCwv + 1] - cwvArray[iCwv]);
                for (int iWvl = 0; iWvl < nWvl; iWvl++) {
                    for (int iOzo = 0; iOzo < nOzo; iOzo++) {
                        for (int iAng = 0; iAng < nAng; iAng++) {
                            lutGas[iWvl][iOzo][iAng] = cwvOzoLutArray[iWvl][iOzo][iCwv][iAng] + (cwvOzoLutArray[iWvl][iOzo][iCwv + 1][iAng] - cwvOzoLutArray[iWvl][iOzo][iCwv][iAng]) * term;
                        }
                    }
                }
                gasArray = ozoArray;
            }
        } finally {
            iis.close();
        }
    }

    private void loadCwvOzoKxLookupTableArray(Sensor sensor) throws IOException {
        // todo: test this method!!
        ImageInputStream iis = Luts.getCwvKxLutData(sensor.getInstrument());
        try {
            // read LUT dimensions and values
            int nAng = iis.readInt();
            Luts.readDimension(iis, nAng);
            int nCwv = iis.readInt();
            Luts.readDimension(iis, nCwv);
            int nOzo = iis.readInt();
            Luts.readDimension(iis, nOzo);

            int nKx = 2;
            int nKxcase = 2;

            float[] wvl = sensor.getWavelength();
            final int nWvl = wvl.length;

            float[][][][][][] kxArray = new float[nWvl][nOzo][nCwv][nAng][nKxcase][nKx];
            for (int iWvl = 0; iWvl < nWvl; iWvl++) {
                for (int iOzo = 0; iOzo < nOzo; iOzo++) {
                    for (int iCwv = 0; iCwv < nCwv; iCwv++) {
                        for (int iAng = 0; iAng < nAng; iAng++) {
                            for (int iKxCase = 0; iKxCase < nKxcase; iKxCase++) {
                                for (int iKx = 0; iKx < nKx; iKx++) {
                                    kxArray[iWvl][iOzo][iCwv][iAng][iKxCase][iKx] = iis.readFloat();
                                }
                            }
                        }
                    }
                }
            }

            kxLutGas = new float[nWvl][nCwv][nAng][nKxcase][nKx];
            if (sensor.equals(Sensor.VGT)) {
                int iOzo = BbdrUtils.getIndexBefore(gas2val, ozoArray);
                for (int iWvl = 0; iWvl < nWvl; iWvl++) {
                    for (int iCwv = 0; iCwv < nCwv; iCwv++) {
                        for (int iAng = 0; iAng < nAng; iAng++) {
                            for (int iKxcase = 0; iKxcase < nKxcase; iKxcase++) {
                                for (int iKx = 0; iKx < nKx; iKx++) {
                                    float term = (gas2val - ozoArray[iOzo]) / (ozoArray[iOzo + 1] - ozoArray[iOzo]);
                                    kxLutGas[iWvl][iCwv][iAng][iKxcase][iKx] = kxArray[iWvl][iOzo][iCwv][iAng][iKxcase][iKx] + (kxArray[iWvl][iOzo + 1][iCwv][iAng][iKxcase][iKx] - kxArray[iWvl][iOzo][iCwv][iAng][iKxcase][iKx]) * term;
                                }
                            }
                        }
                    }
                }
            } else {
                int iCwv = BbdrUtils.getIndexBefore(gas2val, cwvArray);
                for (int iWvl = 0; iWvl < nWvl; iWvl++) {
                    for (int iOzo = 0; iOzo < nOzo; iOzo++) {
                        for (int iAng = 0; iAng < nAng; iAng++) {
                            for (int iKxcase = 0; iKxcase < nKxcase; iKxcase++) {
                                for (int iKx = 0; iKx < nKx; iKx++) {
                                    float term = (gas2val - cwvArray[iCwv]) / (cwvArray[iCwv + 1] - cwvArray[iCwv]);
                                    kxLutGas[iWvl][iOzo][iAng][iKxcase][iKx] = kxArray[iWvl][iOzo][iCwv][iAng][iKxcase][iKx] + (kxArray[iWvl][iOzo][iCwv + 1][iAng][iKxcase][iKx] - kxArray[iWvl][iOzo][iCwv][iAng][iKxcase][iKx]) * term;
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            iis.close();
        }
    }

    float[] getTg(float amf, float gas) {
        int ind_amf = BbdrUtils.getIndexBefore(amf, amfArray);
        float amf_p = (amf - amfArray[ind_amf]) / (amfArray[ind_amf + 1] - amfArray[ind_amf]);

        int ind_gas = BbdrUtils.getIndexBefore(gas, gasArray);
        float gas_p = (gas - gasArray[ind_gas]) / (gasArray[ind_gas + 1] - gasArray[ind_gas]);

        float[] tg = new float[sensor.getNumBands()];
        for (int iWvl = 0; iWvl < tg.length; iWvl++) {
            tg[iWvl] = (1.0f - amf_p) * (1.0f - gas_p) * lutGas[iWvl][ind_gas][ind_amf] +
                    gas_p * (1.0f - amf_p) * lutGas[iWvl][ind_gas + 1][ind_amf] +
                    (1.0f - gas_p) * amf_p * lutGas[iWvl][ind_gas][ind_amf + 1] +
                    amf_p * gas_p * lutGas[iWvl][ind_gas + 1][ind_amf + 1];
        }
        return tg;
    }

//    float[] get2(float amf, float gas) {
//        LookupTable lookupTable = new LookupTable(lutGas, amfArray, gasArray);
//        lookupTable.getValue(amf, gas);
//    }

    float[][][] getKxTg(float amf, float gas) {
        int ind_amf = BbdrUtils.getIndexBefore(amf, amfArray);
        float amf_p = (amf - amfArray[ind_amf]) / (amfArray[ind_amf + 1] - amfArray[ind_amf]);

        int ind_gas = BbdrUtils.getIndexBefore(gas, gasArray);
        float gas_p = (gas - gasArray[ind_gas]) / (gasArray[ind_gas + 1] - gasArray[ind_gas]);

        float[][][] kx_tg = new float[sensor.getNumBands()][2][2];         // todo: introduce constants for 2,2
        for (int iWvl = 0; iWvl < sensor.getNumBands(); iWvl++) {
            for (int iKxcase = 0; iKxcase < kx_tg[iWvl].length; iKxcase++) {
                for (int iKx = 0; iKx < kx_tg[iWvl][iKxcase].length; iKx++) {
                    kx_tg[iWvl][iKxcase][iKx] = (1.0f - amf_p) * (1.0f - gas_p) * kxLutGas[iWvl][ind_gas][ind_amf][iKxcase][iKx] +
                            gas_p * (1.0f - amf_p) * kxLutGas[iWvl][ind_gas + 1][ind_amf][iKxcase][iKx] +
                            (1.0f - gas_p) * amf_p * kxLutGas[iWvl][ind_gas][ind_amf + 1][iKxcase][iKx] +
                            amf_p * gas_p * kxLutGas[iWvl][ind_gas + 1][ind_amf + 1][iKxcase][iKx];
                }
            }
        }
        return kx_tg;
    }

    /**
     * converts ang values to geomAmf values (BBDR breadboard l.890)
     *
     * @param ang
     * @return
     */
    static float[] convertAngArrayToAmfArray(float[] ang) {
        float[] geomAmf = new float[ang.length];
        for (int i = 0; i < geomAmf.length; i++) {
            geomAmf[i] = (float) (2.0 / Math.cos(Math.toRadians(ang[i])));
        }
        return geomAmf;
    }


}

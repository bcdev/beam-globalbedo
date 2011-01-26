package org.esa.beam.globalbedo.bbdr;

import java.nio.ByteBuffer;

import static java.lang.Math.*;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
class GasLookupTable {

    private final float gas2val = 1.5f; //TODO this is MERIS and AATSTR only
    private final Sensor sensor;

    private float[][][] lutGas;
    private float[][][][][] kxLutGas;

    private float[] amfArray;
    private float[] cwvArray;
    private float[] gasArray;

    GasLookupTable(Sensor sensor) {
        this.sensor = sensor;
    }

    void load() {
        loadCwvOzoLookupTableArray(sensor.toString());
        loadCwvOzoKxLookupTableArray(sensor.toString());
    }

    float getGas2val() {
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

    private void loadCwvOzoLookupTableArray(String instrument) {
        // todo: test this method!
        final String lutFileName = BbdrUtils.getCwvLutName(instrument);
        ByteBuffer bb = BbdrUtils.readLutFileToByteBuffer(lutFileName);
        int nAng = bb.getInt();
        int nCwv = bb.getInt();
        int nOzo = bb.getInt();

        final float[] angArr = BbdrUtils.readDimension(bb, nAng);
        cwvArray = BbdrUtils.readDimension(bb, nCwv);
        final float[] ozoArr = BbdrUtils.readDimension(bb, nOzo);

        float[] wvl = BbdrUtils.getInstrumentWavelengths(instrument);
        final int nWvl = wvl.length;
        float[][][][] cwvOzoLutArray = new float[nWvl][nOzo][nCwv][nAng];
        for (int iAng = 0; iAng < nAng; iAng++) {
            for (int iCwv = 0; iCwv < nCwv; iCwv++) {
                for (int iOzo = 0; iOzo < nOzo; iOzo++) {
                    for (int iWvl = 0; iWvl < nWvl; iWvl++) {
                        cwvOzoLutArray[iWvl][iOzo][iCwv][iAng] = bb.getFloat();
                    }
                }
            }
        }
        lutGas = new float[nWvl][nCwv][nAng];
        amfArray = convertAngArrayToAmfArray(angArr);

        if (sensor.equals(Sensor.SPOT)) {
            //TODO
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
            gasArray = ozoArr;
        }
    }

    private void loadCwvOzoKxLookupTableArray(String instrument) {
        // todo: test this method!!
        final String lutFileName = BbdrUtils.getCwvKxLutName(instrument);

        ByteBuffer bb = BbdrUtils.readLutFileToByteBuffer(lutFileName);

        // read LUT dimensions and values
        int nAng = bb.getInt();
        BbdrUtils.readDimension(bb, nAng);
        int nCwv = bb.getInt();
        BbdrUtils.readDimension(bb, nCwv);
        int nOzo = bb.getInt();
        BbdrUtils.readDimension(bb, nOzo);

        int nKx = 2;
        int nKxcase = 2;

        float[] wvl = BbdrUtils.getInstrumentWavelengths(instrument);
        final int nWvl = wvl.length;

        float[][][][][][] kxArray = new float[nWvl][nOzo][nCwv][nAng][nKxcase][nKx];
        for (int iWvl = 0; iWvl < nWvl; iWvl++) {
            for (int iOzo = 0; iOzo < nOzo; iOzo++) {
                for (int iCwv = 0; iCwv < nCwv; iCwv++) {
                    for (int iAng = 0; iAng < nAng; iAng++) {
                        for (int iKxCase = 0; iKxCase < nKxcase; iKxCase++) {
                            for (int iKx = 0; iKx < nKx; iKx++) {
                                kxArray[iWvl][iOzo][iCwv][iAng][iKxCase][iKx] = bb.getFloat();
                            }
                        }
                    }
                }
            }
        }

        kxLutGas = new float[nWvl][nCwv][nAng][nKxcase][nKx];
        if (sensor.equals(Sensor.SPOT)) {
            //TODO
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
     *
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

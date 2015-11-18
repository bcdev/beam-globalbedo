/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.globalbedo.auxdata.Luts;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * The narrow to broadband conversion.
 */
public class N2Bconversion {

    private final Sensor sensor;
    private final int n_spc;
    private final int num_bd;

    private final double[][] nb_coef_arr_all;
    private final double[] nb_intcp_arr_all;
    private final double[] rmse_arr_all;

    private final double[][] nb_coef_arr_D;
    private final double[] nb_intcp_arr_D;

    public N2Bconversion(Sensor sensor, int n_spc) {
        this.sensor = sensor;
        this.n_spc = n_spc;
        this.num_bd = sensor.getNumBands();

        this.nb_coef_arr_all = new double[n_spc][num_bd];
        this.nb_intcp_arr_all = new double[n_spc];
        this.rmse_arr_all = new double[n_spc];

        this.nb_coef_arr_D = new double[n_spc][num_bd];
        this.nb_intcp_arr_D = new double[n_spc];
    }

    public void load() throws IOException {
        final String instrument = sensor.getInstrument().equals("PROBAV") ? "VGT" : sensor.getInstrument();
        BufferedReader reader = Luts.getN2BCoeffReader(instrument);
        try {
            for (int ind_spc = 0; ind_spc < n_spc; ind_spc++) {
                reader.readLine(); // skip this
                String headerLine = reader.readLine();
                String[] headerElems = headerLine.split(" ");
                for (String headerElem : headerElems) {
                    final String trim = headerElem.trim();
                    if (trim.isEmpty()) {
                        continue;
                    }
                    int ind_wvl_arr = Integer.parseInt(trim);
                    nb_coef_arr_all[ind_spc][ind_wvl_arr] = readDouble(reader);
                }
                nb_intcp_arr_all[ind_spc] = readDouble(reader);
                rmse_arr_all[ind_spc] = readDouble(reader);
            }
        } finally {
            reader.close();
        }

        reader = Luts.getN2BCoeffDReader(instrument);
        try {
            for (int ind_spc = 0; ind_spc < n_spc; ind_spc++) {
                reader.readLine(); // skip this
                String headerLine = reader.readLine();
                String[] headerElems = headerLine.split(" ");
                for (String headerElem : headerElems) {
                    final String trim = headerElem.trim();
                    if (trim.isEmpty()) {
                        continue;
                    }
                    int ind_wvl_arr = Integer.parseInt(trim);
                    nb_coef_arr_D[ind_spc][ind_wvl_arr] = readDouble(reader);
                }
                nb_intcp_arr_D[ind_spc] = readDouble(reader);
            }
        } finally {
            reader.close();
        }
    }

    private static double readDouble(BufferedReader reader) throws IOException {
        return Double.parseDouble(reader.readLine().trim());
    }

    public double[][] getNb_coef_arr_all() {
        return nb_coef_arr_all;
    }

    public double[] getNb_intcp_arr_all() {
        return nb_intcp_arr_all;
    }

    public double[] getRmse_arr_all() {
        return rmse_arr_all;
    }

    public double[][] getNb_coef_arr_D() {
        return nb_coef_arr_D;
    }

    public double[] getNb_intcp_arr_D() {
        return nb_intcp_arr_D;
    }
}

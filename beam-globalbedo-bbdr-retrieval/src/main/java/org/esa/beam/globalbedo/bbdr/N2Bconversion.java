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

import org.esa.beam.globalbedo.sdr.operators.InstrumentConsts;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * The narrow to broadband conversion.
 */
public class N2Bconversion {
    private static final String coeffPattern = "%INSTRUMENT%/N2B_coefs_%INSTRUMENT%_rmse_v2.txt";
    private static final String coeffDPattern = "%INSTRUMENT%/N2B_coefs_%INSTRUMENT%_Ddw_Dup.txt";

    private final Sensor instrument;
    private final int n_spc;
    private final int num_bd;

    private final double[][] nb_coef_arr_all;
    private final double[] nb_intcp_arr_all;
    private final double[] rmse_arr_all;

    private final double[][] nb_coef_arr_D;
    private final double[] nb_intcp_arr_D;

    public N2Bconversion(Sensor instrument, int n_spc) {
        this.instrument = instrument;
        this.n_spc = n_spc;
        this.num_bd = instrument.getNumBands();

        this.nb_coef_arr_all = new double[n_spc][num_bd];
        this.nb_intcp_arr_all = new double[n_spc];
        this.rmse_arr_all = new double[n_spc];

        this.nb_coef_arr_D = new double[n_spc][num_bd];
        this.nb_intcp_arr_D = new double[n_spc];
    }

    public void load() throws IOException {
        String coeffFileName = getFileName(instrument.getName(), coeffPattern);
        BufferedReader reader = new BufferedReader(new FileReader(coeffFileName));
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

        String coeffDFileName = getFileName(instrument.getName(), coeffDPattern);
        reader = new BufferedReader(new FileReader(coeffDFileName));
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

    private static String getFileName(String instrument, String pattern) {
        final String pathPattern = InstrumentConsts.getInstance().getLutPath() + File.separator + pattern;
        return pathPattern.replace("%INSTRUMENT%", instrument);
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

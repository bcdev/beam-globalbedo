package org.esa.beam.globalbedo.bbdr;

import org.esa.beam.util.io.CsvReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 13.06.2016
 * Time: 15:09
 *
 * @author olafd
 */
public class MsslModisSpectralMapper {

    String coeffFile = "sm_meris_coef.txt";

    String refBands[] = { "sur_refl_b01_1", "sur_refl_b02_1", "sur_refl_b03_1", "sur_refl_b04_1", "sur_refl_b05_1", "sur_refl_b06_1", "sur_refl_b07_1" };
    String satBands[] = { "sdr_1", "sdr_2", "sdr_3", "sdr_4", "sdr_5", "sdr_6", "sdr_7", "sdr_8", "sdr_9", "sdr_10", "sdr_11", "sdr_12", "sdr_13", "sdr_14", "sdr_15" };


    int numSatBands = satBands.length;
    int numRefBands = refBands.length;

    String landCoverClass = "all_lc_classes";
    String snowType = "SnowNoSnow";  // for now we consider one snowType
    String poly = "polynom_1st_order"; //or polynon_1st_order

    float[][] coeff1;
    float[][][] coeff2;
    float[] intercept;
    float[] mse;

    // map sdr_err to modis bands

    float[] getSpectralMappedSigmaSdr(float[] errs) {
        float vals[] = getSpectralMappedSdr(errs);
        for (int a = 0; a < numRefBands; a++){
            vals[a] += Math.sqrt((vals[a]*vals[a])+mse[a]);
            if(vals[a]>1f)
                vals[a]=1f;
            if(vals[a]<=0f)
                vals[a]=0.01f;
        }
        return vals;
    }


    // map sdr to modis bands
    float[] getSpectralMappedSdr(float[] sdrs) {
        float vals[] = new float[numRefBands];
        for (int a = 0; a < numRefBands; a++){
            for (int b = 0; b < numSatBands; b++) {
                vals[a] += sdrs[b] * coeff1[a][b];
                for (int c = 0; c < numSatBands; c++)
                    vals[a] += (sdrs[b] * sdrs[c] * coeff2[a][b][c]);
            }

            if(vals[a]>1f)
                vals[a]=1f;
            else
            if (vals[a]<0.0)
                vals[a]=0;

        }


        return vals;
    }


    // read coefficients from a text file
    void readCoeff() {

        this.coeff1 = new float[numRefBands][numSatBands];
        this.coeff2 = new float[numRefBands][numSatBands][numSatBands];
        this.intercept = new float[numRefBands];
        this.mse = new float[numRefBands];

        try {

            InputStream inputStream = getClass().getResourceAsStream("sm_meris_coef.txt");
            InputStreamReader streamReader = new InputStreamReader(inputStream);
            BufferedReader bufferReader = new BufferedReader(streamReader);

//            FileReader file = new FileReader(coeffFile);
//            BufferedReader bufferReader = new BufferedReader(file);

            String line;
            int idx_ref = -1;
            boolean t = false;
            while ((line = bufferReader.readLine()) != null) {
                if (!t && line.startsWith(">>>")) {
                    // System.out.println(line);

                    String tab[] = line.replace(">>>", "").replaceAll(" ", "")
                            .split(",");
                    // System.out.println(tab[0]+", "+tab[1]+", "+tab[2]);
                    if (tab[0].equalsIgnoreCase(poly)
                            && tab[2].equalsIgnoreCase(landCoverClass)
                            && tab[1].equalsIgnoreCase(snowType)) {
                        t = true;


                        for (int i = 0; i < this.numRefBands; i++)
                            if (tab[3].equalsIgnoreCase(this.refBands[i])) {
                                idx_ref = i;
                                break;
                            }

                        continue;
                    }
                }
                if (t) {

                    if (line.startsWith("Intercept"))
                        intercept[idx_ref] = Float
                                .parseFloat(line.split(" ")[1]);
                    else if (line.startsWith("MSE:"))
                        mse[idx_ref] = Float.parseFloat(line
                                                                .replace("MSE:", ""));
                    else {
                        for (int a = 0; a < this.numSatBands; a++) {
                            if (!line.contains(":")
                                    && line.contains(this.satBands[a] + " "))
                                this.coeff1[idx_ref][a] = Float.parseFloat(line
                                                                                   .split(" ")[1]);

                            for (int b = 0; b < this.numSatBands; b++)
                                if (line.contains(this.satBands[a] + ":"
                                                          + this.satBands[b] + " "))
                                    this.coeff2[idx_ref][a][b] = Float
                                            .parseFloat(line.split(" ")[1]);

                        }
                    }

                    if (line.startsWith("<<<")) {
                        t = false;
                        idx_ref = -1;
                    }

                }

            }
            bufferReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}

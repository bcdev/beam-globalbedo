package org.esa.beam.globalbedo.bbdr;

import java.io.BufferedReader;
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

//    String coeffFile = "sm_meris_coef.txt";

    String refBands[] = { "sur_refl_b01_1", "sur_refl_b02_1", "sur_refl_b03_1", "sur_refl_b04_1",
            "sur_refl_b05_1", "sur_refl_b06_1", "sur_refl_b07_1" };
    String satBands[] = { "sdr_1", "sdr_2", "sdr_3", "sdr_4", "sdr_5", "sdr_6", "sdr_7", "sdr_8",
            "sdr_9", "sdr_10", "sdr_11", "sdr_12", "sdr_13", "sdr_14", "sdr_15" };

    // meris_stat_coef.csv (SK, 20161118), all_classes:
//    public static final double[][] COEFF_1 = new double[][] {
//            {0.0, 0.0, 0.0, 0.0, 0.0686935348872, 0.0, 0.317039377819, 0.471876876565, 0.136010778976,
//                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0} ,
//            {0.117929898321, 0.0, -0.100350567427, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.145218662908,
//                    0.0179775300448, 0.855922547954, 0.0, 0.0},
//            {-0.244711707703, 0.0, 0.0, 0.936984912811, 0.0, 0.0, 0.0, 0.0, 0.0, -0.0378280138546, 0.0,
//                    0.0, 0.0, 0.0, 0.0},
//            {-0.235927215748, 0.0, 0.0, 0.318329950239, 0.790374005848, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
//                    0.0, 0.0, 0.0269013022799, 0.0269013022799},
//            {0.42274348004, 0.0, 0.0, -1.05933169333, 0.0, 0.0, 0.0, 0.0, 0.914892566036, -0.605256825945,
//                    0.0, 0.0, 1.35086894047, -0.19698419591, -0.19698419591},
//            {0.540254921442, 0.0, 0.0, -1.20929374247, 0.0, 0.0, 0.0, 0.164593497983, 1.95758407036, -1.48558749155,
//                    0.0, 0.0, 1.48228762628, -0.491304016928, -0.491304016928},
//            {0.866623679649, 0.0, -1.1550246423, -0.263974311816, 0.0, 0.0, 0.0, 1.56754833459, 0.12039074206,
//                    -0.137673885865, 0.0, 0.0, 0.0, 0.0552984349493, 0.0552984349493}
//    };

    // meris_stat_coef.csv (SK, 20161222), all_classes, bands 1-4 only positive coeffs:
    public static final double[][] COEFF_1 = new double[][] {
            {0.0, 0.0, 0.0, 0.0381683132278, 0.0, 0.0706581570918, 0.727938603627, 0.16365834236, 0.0,
                    0.0, 0.0, 0.0, 0.0, 0.0, 0.0} ,
            {0.0108626196304, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0269109404428, 0.0, 0.0, 0.0,
                    0.11219885911, 0.399278981479, 0.463244499785, 0.0},
            {0.0, 0.0, 0.52091545188, 0.298259211541, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0},
            {0.0, 0.0, 0.0, 0.138467743664, 0.867733708452, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                    0.0, 0.0, 0.0, 0.0},
            {0.42274348004, 0.0, 0.0, -1.05933169333, 0.0, 0.0, 0.0, 0.0, 0.914892566036, -0.605256825945,
                    0.0, 0.0, 1.35086894047, -0.19698419591, -0.19698419591},
            {0.540254921442, 0.0, 0.0, -1.20929374247, 0.0, 0.0, 0.0, 0.164593497983, 1.95758407036, -1.48558749155,
                    0.0, 0.0, 1.48228762628, -0.491304016928, -0.491304016928},
            {0.866623679649, 0.0, -1.1550246423, -0.263974311816, 0.0, 0.0, 0.0, 1.56754833459, 0.12039074206,
                    -0.137673885865, 0.0, 0.0, 0.0, 0.0552984349493, 0.0552984349493}
    };


    public static double[] MSE = new double[]{
            0.000149339352436,
            0.000202888197683,
            0.000253645086521,
            0.000168781393185,
            0.000730908703564,
            0.00132911251515,
            0.00148779972301
    };

    int numSatBands = satBands.length;
    int numRefBands = refBands.length;

//    String landCoverClass = "all_lc_classes";
//    String snowType = "SnowNoSnow";  // for now we consider one snowType
//    String poly = "polynom_1st_order"; //or polynon_1st_order

//    float[][] coeff1;
//    float[][][] coeff2;
//    float[] intercept;
//    float[] mse;

    // map sdr_err to modis bands

    float[] getSpectralMappedSigmaSdr(float[] errs) {
        float vals[] = getSpectralMappedSdr(errs);
        for (int a = 0; a < numRefBands; a++){
            vals[a] += Math.sqrt((vals[a]*vals[a])+ MSE[a]);
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
                vals[a] += sdrs[b] * COEFF_1[a][b];
//                for (int c = 0; c < numSatBands; c++)
//                    vals[a] += (sdrs[b] * sdrs[c] * coeff2[a][b][c]);
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
//    void readCoeff() {
//
////        this.coeff_1 = new float[numRefBands][numSatBands];
//        this.coeff2 = new float[numRefBands][numSatBands][numSatBands];
//        this.intercept = new float[numRefBands];
////        this.mse = new float[numRefBands];
//
//        try {
//
//            InputStream inputStream = getClass().getResourceAsStream("sm_meris_coef.txt");
//            InputStreamReader streamReader = new InputStreamReader(inputStream);
//            BufferedReader bufferReader = new BufferedReader(streamReader);
//
////            FileReader file = new FileReader(coeffFile);
////            BufferedReader bufferReader = new BufferedReader(file);
//
//            String line;
//            int idx_ref = -1;
//            boolean t = false;
//            while ((line = bufferReader.readLine()) != null) {
//                System.out.println("line: " + line);
//                if (!t && line.startsWith(">>>")) {
//
//                    String tab[] = line.replace(">>>", "").replaceAll(" ", "")
//                            .split(",");
//                    // System.out.println(tab[0]+", "+tab[1]+", "+tab[2]);
//                    if (tab[0].equalsIgnoreCase(poly)
//                            && tab[2].equalsIgnoreCase(landCoverClass)
//                            && tab[1].equalsIgnoreCase(snowType)) {
//                        t = true;
//
//
//                        for (int i = 0; i < this.numRefBands; i++)
//                            if (tab[3].equalsIgnoreCase(this.refBands[i])) {
//                                idx_ref = i;
//                                break;
//                            }
//
//                        continue;
//                    }
//                }
//                if (t) {
//
//                    if (line.startsWith("Intercept"))
//                        intercept[idx_ref] = Float
//                                .parseFloat(line.split(" ")[1]);
//                    else if (line.startsWith("MSE:"))
//                        MSE[idx_ref] = Float.parseFloat(line
//                                                                .replace("MSE:", ""));
//                    else {
//                        for (int a = 0; a < this.numSatBands; a++) {
//                            if (!line.contains(":")
//                                    && line.contains(this.satBands[a] + " "))
//                                this.COEFF_1[idx_ref][a] = Float.parseFloat(line
//                                                                                   .split(" ")[1]);
//
//                            for (int b = 0; b < this.numSatBands; b++)
//                                if (line.contains(this.satBands[a] + ":"
//                                                          + this.satBands[b] + " "))
//                                    this.coeff2[idx_ref][a][b] = Float
//                                            .parseFloat(line.split(" ")[1]);
//
//                        }
//                    }
//
//                    if (line.startsWith("<<<")) {
//                        t = false;
//                        idx_ref = -1;
//                    }
//
//                }
//
//            }
//            bufferReader.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

}

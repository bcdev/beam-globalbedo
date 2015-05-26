package org.esa.beam.globalbedo.staging;

import Jama.Matrix;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * This program create netcdf mosaics (stage2) from 1km tiles and text files resulted from matching program (stage1)
 * It fills mosaics parts one by one
 * the aggregation methods (energy conservation, nearest neighbor, average) for each layer are givens as input within a text file
 *
 * @author Said Kharbouche, MSSL.UCL(2014)
 */
public class Mosaicking {
    //##################################################################################################################
    /**
     * lon resolution: 0.05, 0.5
     */
    private float resDegX;
    /**
     * lat resolution: 0.05, 0.5
     */
    private float resDegY;
    /**
     * resname: 005, 05
     */
    private String resName;
    /**
     * year
     */
    private String year;
    /**
     * DoY
     */
    private String doy;
    /**
     * Snow, NoSmow, merge
     */
    private String mod;

    /**
     * folder that contains tiles: uncompressed brdf files for target year.doy.mod
     */
    private String inTilesDIR;
    /**
     * output directory
     */
    private String outfolder;
    /**
     * path of the text file that contains layer names and their appropriate aggregation methods
     */
    private String pathbands;
    /**
     * position of tile name in brdf file name (split by .)
     */
    private int idxTile;
    /**
     * netdcf version: 3 or 4
     */
    private int ncVersion;
    /**
     * fill value in brdf files 0
     */
    private float nodata;
    /**
     * number of mosaic subdivisions by axis (number of parts=div*div)
     */
    private int div;
    /**
     * tile width
     */
    private int wTile;
    /**
     * tile height
     */
    private int hTile;

    /**
     * directory of text files of tile1km-grid matching (stage1's output)
     */
    private String matchFolder;
    /**
     * Threshold on common area grid-tile1km
     */
    private float alpha;
    /**
     * to decrease threshold on ratio of common area with altitude
     */
    private float lamda;
    /**
     * from where high latitude starts
     */
    private float highLat;
    /**
     * Threshold on number of Obs to take into account in high altitude for 0.05 or lower only to reduce artifacts in uncertainty matrix in high altitude.
     * It can be disabled by (-1) which is recommended for res=0.5 and upper
     */
    private float obs;
    /**
     * To smooth "obs" curve in the beginning of high latitude: fixed to 0.1 for 0.05deg an disabled for 0.5deg by obs=-1
     */
    private float beta;

    /**
     * transpose input files
     */
    private boolean transpose;

    /**
     * min lat of mosaic
     */
    private float latMin;
    /**
     * max lat of mosaic
     */
    private float latMax;
    /**
     * min lon of mosaic
     */
    private float lonMin;
    /**
     * max lon of mosaic
     */
    private float lonMax;

    //##################################################################################################################


    private int hG;

    private int wG;

    private String outFile;

    private String bandsNN[] = null;

    private String bandsMF[] = null;

    private String bandsAV[] = null;

    private String latBandName = "lat";

    private String lonBandName = "lon";

    //String pathtilesbox1;

    private Version netcdfVersion;

    private String[] meanBands = null;

    private String[] varBands = null;

    private int coordsMatch[][];

    private float distMatch[][];

    private float interFracMatch[][];


    /**
     * Launch the creation of netcdf files of mosaics
     */
    public void compute() {
        try {

            System.out.println("start..");

            readbands();
            System.out.println(" ------- ");
            System.out.println("Mean Bands:");
            for (int i = 0; i < this.meanBands.length; i++)
                System.out.print(meanBands[i] + ",");
            System.out.println("\nVar Bands:");
            for (int i = 0; i < this.varBands.length; i++)
                System.out.print(varBands[i] + ",");
            System.out.println("\nNearest Neighbour Bnads:");
            for (int i = 0; i < this.bandsNN.length; i++)
                System.out.print(bandsNN[i] + ",");
            System.out.println("\nMajority Filter Bands:");
            for (int i = 0; i < this.bandsMF.length; i++)
                System.out.print(bandsMF[i] + ",");
            System.out.println("\nAverage Bands:");
            for (int i = 0; i < this.bandsAV.length; i++)
                System.out.print(bandsAV[i] + ",");

            System.out.println("\n------- ");

            this.netcdfVersion = Version.netcdf3;
            if (this.ncVersion == 4)
                this.netcdfVersion = Version.netcdf4;

            this.wG = Math.round(((lonMax - lonMin) / this.resDegX));
            this.hG = Math.round(((latMax - latMin) / this.resDegY));

            System.out.println("resDegX, resDegY: " + resDegX + ", " + resDegY);
            System.out.println("Mosaic (W, H): (" + wG + ", " + hG + ")");
            System.out.println(" --------------------------- ");
            int wg = wG / div;
            int hg = hG / div;
            int ng = wg * hg;
            // String res=(""+this.resDegX).replace(".", "");
            outFile = this.outfolder + "/GlobAlbedo.brdf." + mod + "."
                    + resName + "." + this.year + this.doy + ".nc";
            createGrid();

            for (int b = 0, di = 0; b < div; b++)
                for (int a = 0; a < div; a++, di++)
                // if(b==div-1)
                {

                    int x0 = a * wg;
                    int y0 = b * hg;

                    System.out.println("offset: (" + x0 + ", " + y0 + ")");

                    NetcdfFileWriter grid = NetcdfFileWriter
                            .openExisting(this.outFile);
                    float dist[] = null;

                    Matrix Csum[] = null;
                    Matrix FC[] = null;
                    Matrix F[] = null;

                    float numbAvr[] = null;

                    Array[] arrayOut = null;
                    Array[] arrayFout = null;

                    Array[] arrayNN = null;
                    Array[] arrayMF = null;
                    Array[] arrayAV = null;
                    byte mf[][][] = null;

                    int numSamples[] = new int[wg * hg];

                    File tiles[] = new File(this.inTilesDIR).listFiles();
                    boolean intersects = false;
                    for (int t = 0; t < tiles.length; t++)
                        if (tiles[t].getName().endsWith(".nc"))
                            try {

                                int hi = (int) this.getTileH(
                                        tiles[t].getName(), this.idxTile);
                                int vi = (int) this.getTileV(
                                        tiles[t].getName(), this.idxTile);

                                int nt = this.wTile * this.hTile;
                                NetcdfFile tile = NetcdfFile.open(tiles[t]
                                                                          .getAbsolutePath());
                                // if((hi==18 && vi==17) || (hi==17 && vi==17))
                                if (hi > -1 && vi > -1) {
                                    readMatch(hi, vi, di);
                                    if (this.coordsMatch != null
                                            && arrayOut == null) {

                                        if (this.meanBands.length > 0) {
                                            Csum = new Matrix[ng];
                                            FC = new Matrix[ng];
                                            F = new Matrix[ng];
                                        }

                                        dist = new float[ng];

                                        if (this.bandsAV.length > 0)
                                            numbAvr = new float[ng];

                                        arrayOut = new Array[45];
                                        arrayFout = new Array[9];

                                        arrayNN = new Array[this.bandsNN.length];
                                        arrayMF = new Array[this.bandsMF.length];
                                        arrayAV = new Array[this.bandsAV.length];

                                        for (int i = 0; i < 9; i++)
                                            arrayFout[i] = new ArrayFloat.D2(
                                                    wg, hg);

                                        for (int i = 0; i < 45; i++)
                                            arrayOut[i] = new ArrayFloat.D2(wg,
                                                                            hg);

                                        for (int i = 0; i < arrayNN.length; i++)
                                            arrayNN[i] = new ArrayFloat.D2(wg,
                                                                           hg);

                                        for (int i = 0; i < arrayMF.length; i++)
                                            arrayMF[i] = new ArrayFloat.D2(wg,
                                                                           hg);

                                        for (int i = 0; i < arrayAV.length; i++)
                                            arrayAV[i] = new ArrayFloat.D2(wg,
                                                                           hg);

                                        for (int i = 0; i < ng; i++) {
                                            dist[i] = Float.MAX_VALUE;
                                            if (this.meanBands.length > 0) {
                                                Csum[i] = new Matrix(9, 9, 0f);
                                                FC[i] = new Matrix(1, 9, 0f);
                                                F[i] = new Matrix(1, 9, 0.0);
                                            }
                                            for (int j = 0; j < arrayAV.length; j++)
                                                arrayAV[j].setFloat(i, 0f);
                                        }

                                        mf = new byte[this.bandsMF.length][wg
                                                * hg][400];

                                    }

                                    Array[] array = new Array[45];
                                    Array[] arrayF = new Array[9];

                                    Array[] arrayNNtile = new Array[this.bandsNN.length];
                                    Array[] arrayMFtile = new Array[this.bandsMF.length];
                                    Array[] arrayAVtile = new Array[this.bandsAV.length];

                                    if (this.meanBands.length > 0) {
                                        for (int i = 0; i < 9; i++)
                                            arrayF[i] = tile.findVariable(
                                                    this.meanBands[i]).read();

                                        for (int i = 0; i < 45; i++)
                                            array[i] = tile.findVariable(
                                                    this.varBands[i]).read();
                                    }

                                    for (int i = 0; i < this.bandsNN.length; i++) {
                                        arrayNNtile[i] = tile.findVariable(
                                                this.bandsNN[i]).read();

                                    }
                                    for (int i = 0; i < this.bandsMF.length; i++)
                                        arrayMFtile[i] = tile.findVariable(
                                                this.bandsMF[i]).read();

                                    for (int i = 0; i < this.bandsAV.length; i++)
                                        arrayAVtile[i] = tile.findVariable(
                                                this.bandsAV[i]).read();

                                    if (this.coordsMatch != null)
                                        for (int p0 = 0; p0 < nt; p0++) {
                                            int p = p0;

                                            if (this.coordsMatch[p] != null)
                                                for (int f = 0; f < this.coordsMatch[p].length; f++) {

                                                    int pG = this.coordsMatch[p][f];

                                                    int xG = pG / hg;
                                                    int yG = pG - (hg * xG);

                                                    float latG = Math
                                                            .abs(90f - ((y0
                                                                    + yG + 0.5f) * this.resDegY));

                                                    double threshold = computeThreshold(
                                                            latG, this.highLat,
                                                            this.alpha,
                                                            this.lamda);

                                                    double maxObs = Float.MAX_VALUE;
                                                    if (this.obs > 0)
                                                        maxObs = Math
                                                                .exp(-this.beta
                                                                             * (latG - this.highLat))
                                                                + this.obs;

                                                    if (this.interFracMatch[p][f] > threshold)
                                                        if (numSamples[pG] <= maxObs) {
                                                            numSamples[pG]++;

                                                            intersects = true;
                                                            if (this.meanBands.length > 0) {
                                                                boolean t0 = false;
                                                                Matrix C = new Matrix(
                                                                        9,
                                                                        9,
                                                                        Float.NaN);
                                                                for (int j = 0, c = 0; j < 9; j++)
                                                                    for (int i = j; i < 9; c++, i++) {
                                                                        double val = Double.NaN;
                                                                        val = array[c]
                                                                                .getFloat(p);
                                                                        C.set(i,
                                                                              j,
                                                                              val);
                                                                        C.set(j,
                                                                              i,
                                                                              val);
                                                                        if (val != 0f)
                                                                            t0 = true;
                                                                    }

                                                                for (int i = 0; i < 9; i++)
                                                                    F[pG].set(
                                                                            0,
                                                                            i,
                                                                            arrayF[i]
                                                                                    .getFloat(p));

                                                                if (t0
                                                                        && C.det() == 0)
                                                                    System.out
                                                                            .println("det==0 ");
                                                                if (C.det() != 0) {

                                                                    Csum[pG].plusEquals(C
                                                                                                .inverse());
                                                                    FC[pG].plusEquals(F[pG]
                                                                                              .times(C.inverse()));
                                                                }

                                                            }

                                                            for (int i = 0; i < this.bandsMF.length; i++) {
                                                                int v = Math
                                                                        .round(arrayMFtile[i]
                                                                                       .getFloat(p));
                                                                if (v > 0) {
                                                                    if (v >= mf[i][pG].length)
                                                                        v = mf[i][pG].length - 1;

                                                                    mf[i][pG][v] = (byte) (mf[i][pG][v] + 1);
                                                                    if (mf[i][pG][v] < 0)
                                                                        mf[i][pG][v] = 125;
                                                                }

                                                            }

                                                            boolean masked = false;
                                                            for (int i = 0; i < this.bandsAV.length; i++) {
                                                                if (arrayAVtile[i]
                                                                        .getFloat(p) != this.nodata
                                                                        && !Float
                                                                        .isNaN(arrayAVtile[i]
                                                                                       .getFloat(p)))
                                                                    masked = true;

                                                                arrayAV[i]
                                                                        .setFloat(
                                                                                pG,
                                                                                arrayAVtile[i]
                                                                                        .getFloat(p)
                                                                                        + arrayAV[i]
                                                                                        .getFloat(pG));
                                                            }
                                                            if (masked)
                                                                numbAvr[pG] += 1f;

                                                        }

                                                    if (this.distMatch[p][f] < dist[pG]) {
                                                        for (int e = 0; e < bandsNN.length; e++)
                                                            arrayNN[e]
                                                                    .setFloat(
                                                                            pG,
                                                                            arrayNNtile[e]
                                                                                    .getFloat(p));
                                                        dist[pG] = this.distMatch[p][f];
                                                    }

                                                }

                                        }

                                }

                                tile.close();

                            } catch (Exception e) {
                                System.out.println("errorInTile: " + tiles[t]);
                                e.printStackTrace();
                            }

                    if (intersects) {
                        for (int p_out = 0; p_out < ng; p_out++) {
                            if (this.meanBands.length > 0) {
                                Matrix Fs = null;
                                // snowFractionOut.setFloat(p_out, Float.NaN);

                                if (Csum[p_out].det() != 0.0) {
                                    Fs = FC[p_out].times(Csum[p_out].inverse());
                                    // System.out.println("inverse");
                                    Matrix CsumInv = Csum[p_out].inverse();

                                    for (int j = 0, c = 0; j < 9; j++)
                                        for (int i = j; i < 9; c++, i++)
                                            arrayOut[c].setFloat(p_out,
                                                                 (float) CsumInv.get(i, j));

                                    for (int i = 0; i < 9; i++)
                                        arrayFout[i].setFloat(p_out,
                                                              (float) Fs.get(0, i));

                                }
                            }
                            for (int i = 0; i < this.bandsMF.length; i++) {
                                float v = Float.NaN;
                                int num = -1;
                                for (int j = 1; j < mf[i][p_out].length; j++) {
                                    // System.out.println(j+" > "+mf[i][p_out][j]);
                                    if (mf[i][p_out][j] > num) {
                                        num = mf[i][p_out][j];
                                        v = (float) j;
                                    }

                                }
                                // System.out.println("----------> "+v);
                                arrayMF[i].setFloat(p_out, v);
                            }

                            for (int i = 0; i < this.bandsAV.length; i++) {
                                if (numbAvr[p_out] != 0f)
                                    arrayAV[i].setFloat(p_out,
                                                        arrayAV[i].getFloat(p_out)
                                                                / numbAvr[p_out]);
                                else
                                    arrayAV[i].setFloat(p_out, 0f);
                            }

                        }


                        //if (intersects) {
                        System.out.println("insert arrays");

                        for (int i = 0; i < this.meanBands.length; i++)
                            // arrayFout0[i]=insert(x0,y0,wG,hG,arrayFout0[i],arrayFout[i],wg,hg).copy();
                            grid.write(
                                    grid.findVariable(this.meanBands[i]),
                                    insert(x0,
                                           y0,
                                           wG,
                                           hG,
                                           grid.findVariable(this.meanBands[i])
                                                   .read(), arrayFout[i], wg,
                                           hg));

                        for (int i = 0; i < this.varBands.length; i++)
                            // arrayOut0[i]=insert(x0,y0,wG,hG,arrayOut0[i],arrayOut[i],wg,hg).copy();
                            grid.write(
                                    grid.findVariable(this.varBands[i]),
                                    insert(x0, y0, wG, hG,
                                           grid.findVariable(this.varBands[i])
                                                   .read(), arrayOut[i], wg,
                                           hg));

                        for (int i = 0; i < this.bandsMF.length; i++)
                            // arrayMF0[i]=insert(x0,y0,wG,hG,arrayMF0[i],arrayMF[i],wg,hg).copy();
                            grid.write(
                                    grid.findVariable(this.bandsMF[i]),
                                    insert(x0, y0, wG, hG,
                                           grid.findVariable(this.bandsMF[i])
                                                   .read(), arrayMF[i], wg, hg));

                        for (int i = 0; i < this.bandsNN.length; i++)
                            // arrayNN0[i]=insert(x0,y0,wG,hG,arrayNN0[i],arrayNN[i],wg,hg).copy();
                            grid.write(
                                    grid.findVariable(this.bandsNN[i]),
                                    insert(x0, y0, wG, hG,
                                           grid.findVariable(this.bandsNN[i])
                                                   .read(), arrayNN[i], wg, hg));

                        for (int i = 0; i < this.bandsAV.length; i++)
                            // arrayNN0[i]=insert(x0,y0,wG,hG,arrayNN0[i],arrayNN[i],wg,hg).copy();
                            grid.write(
                                    grid.findVariable(this.bandsAV[i]),
                                    insert(x0, y0, wG, hG,
                                           grid.findVariable(this.bandsAV[i])
                                                   .read(), arrayAV[i], wg, hg));

                    }

                    grid.close();

                }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * reduce threshold on ration of common area with latitude = alpha/(1+e^(lamda*|x|-highLaitude))
     *
     * @param x     latitude
     * @param m     high_latitude value
     * @param alpha
     * @param lamda
     * @return
     */
    private double computeThreshold(float x, float m, float alpha, float lamda) {

        return (alpha / (1 + Math.exp(lamda * (Math.abs(x) - m))));
    }

    /**
     * read match text file of the target tile and subdivision number
     *
     * @param h  tile h-index
     * @param v  tile v-index
     * @param di mosaic subsivision number
     */
    private void readMatch(int h, int v, int di) {
        this.coordsMatch = null;
        this.distMatch = null;
        this.interFracMatch = null;
        String diStr = (di < 10) ? ("0" + di) : ("" + di);
        String hStr = (h > 9) ? ("" + h) : ("0" + h);
        String vStr = (v > 9) ? ("" + v) : ("0" + v);
        String matchFile = this.matchFolder + "/" + "h" + hStr + "v" + vStr
                + "." + this.resName + "." + diStr + ".txt";

        int n = this.hTile * this.wTile;

        BufferedReader reader = null;
        if (new File(matchFile).exists())
            try {
                System.out.println("part: " + di + ", -->read matchFile: "
                                           + matchFile);
                reader = new BufferedReader(new FileReader(matchFile));
                this.coordsMatch = new int[n][];
                this.distMatch = new float[n][];
                this.interFracMatch = new float[n][];

                String line = null;
                while ((line = reader.readLine()) != null) {

                    line = line.replaceAll(" ", "");
                    String tab[] = line.split(":");
                    int coor[] = null;
                    float dis[] = null;
                    float fra[] = null;

                    String tab1[] = null;

                    if (tab.length == 2) {
                        tab1 = tab[1].split(";");
                        coor = new int[tab1.length];
                        dis = new float[tab1.length];
                        fra = new float[tab1.length];
                    } else {
                        coor = new int[0];
                        dis = new float[0];
                        fra = new float[0];
                    }

                    for (int i = 0; i < coor.length; i++) {
                        if (tab1[i] != "") {
                            String tab2[] = tab1[i].split(",");
                            coor[i] = Integer.parseInt(tab2[0]);
                            fra[i] = Float.parseFloat(tab2[1]);
                            dis[i] = Float.parseFloat(tab2[2]);
                            // System.out.println(i+"- "+fra[i]);
                        }
                    }

                    int p = Integer.parseInt(tab[0]);
                    if (this.transpose) {
                        int y = p / this.wTile;
                        int x = p - (y * wTile);
                        p = (x * this.hTile) + y;
                    }
                    this.coordsMatch[p] = coor;
                    this.interFracMatch[p] = fra;
                    this.distMatch[p] = dis;
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

    }

    /**
     * read text file that contains layer names and their appropriate aggregation methods
     * line starts with "MEAN::" contains layers of MEAN_F0/1/2. It will be  aggregated by Energy conservation method
     * line starts with "VAR::" contains layers of VAR_F0/1/2. It will be  aggregated by Energy conservation method
     * line starts with "NN::" contains layers that will be aggregated by Nearest-Neighbor
     * line starts with "MF::" contains layers that will be aggregated by Majority-Filter
     * line starts with "AV::" contains layers that will be aggregated by Average
     */
    private void readbands() {
        System.out.println("read bands ....");

        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(this.pathbands));

            String line = null;
            while ((line = br.readLine()) != null) {
                line = line.replaceAll(" ", "");
                if (line.split(":").length > 1) {
                    if (line.startsWith("MEAN::"))
                        this.meanBands = line.split("::")[1].split(",");
                    if (line.startsWith("VAR::"))
                        this.varBands = line.split("::")[1].split(",");
                    if (line.startsWith("NN::"))
                        this.bandsNN = line.split("::")[1].split(",");
                    if (line.startsWith("MF::"))
                        this.bandsMF = line.split("::")[1].split(",");
                    if (line.startsWith("AV::"))
                        this.bandsAV = line.split("::")[1].split(",");
                }
            }
            br.close();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (this.meanBands == null
                || (this.meanBands.length == 1 && this.meanBands[0] == ""))
            this.meanBands = new String[0];
        if (this.varBands == null
                || (this.varBands.length == 1 && this.varBands[0] == ""))
            this.varBands = new String[0];
        if (this.bandsNN == null
                || (this.bandsNN.length == 1 && this.bandsNN[0] == ""))
            this.bandsNN = new String[0];
        if (this.bandsMF == null
                || (this.bandsMF.length == 1 && this.bandsMF[0] == ""))
            this.bandsMF = new String[0];
        if (this.bandsAV == null
                || (this.bandsAV.length == 1 && this.bandsAV[0] == ""))
            this.bandsAV = new String[0];
    }

    /**
     * insert array section into an array
     *
     * @param x0     x-offset in array2
     * @param y0     y-offset in array1
     * @param w2     width of array2
     * @param h2     height of array2
     * @param array1 inserted array
     * @param array2 absorbing array
     * @param w1     width of array1
     * @param h1     height of array1
     * @return
     */
    private Array insert(int x0, int y0, int w2, int h2, Array array2, Array array1,
                         int w1, int h1) {

        for (int i = 0, p = 0; i < w1; i++)
            for (int j = 0; j < h1; j++, p++) {
                int pout = ((i + x0) * h2) + (j + y0);
                // if(!Float.isNaN(in.getFloat(p)))
                array2.setFloat(pout, array1.getFloat(p));

            }

        return array2.copy();
    }

    /**
     * create empty mosaic netcdf grid
     */
    private void createGrid() {
        System.out.println("create empty grid");
        NetcdfFileWriter grid = null;
        try {
            System.out.println("outFile: " + this.outFile);
            grid = NetcdfFileWriter.createNew(this.netcdfVersion, this.outFile);
            Dimension dimX = new Dimension("x", this.wG);
            Dimension dimY = new Dimension("y", this.hG);

            List<Dimension> dim = new ArrayList<Dimension>();
            dim.add(0, dimX);
            dim.add(1, dimY);
            grid.getNetcdfFile().addDimension(null, dimX);
            grid.getNetcdfFile().addDimension(null, dimY);

            for (int b = 0; b < this.meanBands.length; b++)
                grid.addVariable(grid.getNetcdfFile().getRootGroup(),
                                 this.meanBands[b], DataType.FLOAT, dim);

            for (int b = 0; b < this.varBands.length; b++)
                grid.addVariable(grid.getNetcdfFile().getRootGroup(),
                                 this.varBands[b], DataType.FLOAT, dim);

            for (int b = 0; b < this.bandsNN.length; b++)
                grid.addVariable(grid.getNetcdfFile().getRootGroup(),
                                 this.bandsNN[b], DataType.FLOAT, dim);

            for (int b = 0; b < this.bandsMF.length; b++)
                grid.addVariable(grid.getNetcdfFile().getRootGroup(),
                                 this.bandsMF[b], DataType.FLOAT, dim);

            for (int b = 0; b < this.bandsAV.length; b++)
                grid.addVariable(grid.getNetcdfFile().getRootGroup(),
                                 this.bandsAV[b], DataType.FLOAT, dim);

            grid.addVariable(null, this.latBandName, DataType.FLOAT, dim);
            grid.addVariable(null, this.lonBandName, DataType.FLOAT, dim);

            grid.addGroupAttribute(grid.getNetcdfFile().getRootGroup(),
                                   new Attribute("product", "brdf " + this.mod));
            grid.addGroupAttribute(grid.getNetcdfFile().getRootGroup(),
                                   new Attribute("grid", this.resName));
            grid.addGroupAttribute(grid.getNetcdfFile().getRootGroup(),
                                   new Attribute("year", this.year));
            grid.addGroupAttribute(grid.getNetcdfFile().getRootGroup(),
                                   new Attribute("doy", this.doy));

            grid.addGroupAttribute(grid.getNetcdfFile().getRootGroup(),
                                   new Attribute("Authors",
                                                 "Dr Said Kharbouche, MSSL-UCL(2014)"));

            grid.create();
            grid.close();

            Array array = new ArrayFloat.D2(this.wG, this.hG);
            for (int i = 0; i < array.getSize(); i++)
                array.setFloat(i, Float.NaN);

            grid = NetcdfFileWriter.openExisting(this.outFile);
            for (int b = 0; b < grid.getNetcdfFile().getVariables().size(); b++)
                grid.write(grid.getNetcdfFile().getVariables().get(b),
                           array.copy());

            Array arrayLat = new ArrayFloat.D2(this.wG, this.hG);
            Array arrayLon = new ArrayFloat.D2(this.wG, this.hG);
            for (int x = 0, p = 0; x < wG; x++)
                for (int y = 0; y < hG; y++, p++)

                {
                    arrayLon.setFloat(p, (this.lonMin + (x * this.resDegX)));
                    arrayLat.setFloat(p, (this.latMax - (y * this.resDegY)));
                }

            grid.write(grid.getNetcdfFile().findVariable(this.latBandName),
                       arrayLat);
            grid.write(grid.getNetcdfFile().findVariable(this.lonBandName),
                       arrayLon);

            grid.close();

            System.out.println("created :)");
        } catch (Exception e) {
            System.out.println("failed!! " + e.getMessage());

            e.printStackTrace();
        }
        System.out.println("end create empty grid.");
    }

    /**
     * extract h-index of the tile from the file name splitted name by "."
     *
     * @param name file name
     * @param idx  position of tile name in file name
     * @return h-index of the til
     */
    private byte getTileH(String name, int idx) {
        return Byte.parseByte(name.split("\\.")[idx].substring(1, 3));
    }

    /**
     * extract v-index of the tile from file name splitted name by "."
     *
     * @param name file name
     * @param idx  position of tile name in file name
     * @return v-index of the tile
     */
    private byte getTileV(String name, int idx) {
        return Byte.parseByte(name.split("\\.")[idx].substring(4, 6));
    }

    public static void main(String[] args) {
        /*
		float resDegX = 0.05f;
		float resDegY = 0.05f;
		String resName = "005";
		String year = "2005";
		String doy = "217";
		String mod = "merge";

		String inTilesDIR = "/unsafe/said/tmp/testCodes/brdf/" ;
		String outfolder = "/unsafe/said/tmp/testCodes/mosaic/brdf";
		String pathbands = "/unsafe/said/tmp/testCodes/params/bands.txt";
		int idxTile = 4;
		int ncVersion = 4;

		float nodata = 0;
		int div = 4;

		int wTile = 1200;
		int hTile = 1200;

		String matchFolder = "/unsafe/said/tmp/testCodes/match/"+resName+"/";
		float alpha = 0.6f;
		float lamda = 2f;
		float highLat = 65;
		float obs = 5;
		float beta = 0.1f;

		boolean transpose = false;

		float latMin = -90f;
		float latMax = 90f;
		float lonMin = -180;
		float lonMax = 180;

		*/
        float resDegX = Float.parseFloat(args[0]);
        float resDegY = Float.parseFloat(args[1]);
        String resName = args[2];
        String year = args[3];
        String doy = args[4];
        String mod = args[5];

        String inTilesDIR = args[6];
        String outfolder = args[7];
        String pathbands = args[8];
        int idxTile = Integer.parseInt(args[9]);
        int ncVersion = Integer.parseInt(args[10]);

        if (args[11].contains("n") || args[11].contains("N"))
            args[11] = "NaN";
        float nodata = Float.parseFloat(args[11]);
        int div = Integer.parseInt(args[12]);

        int wTile = Integer.parseInt(args[13]);
        int hTile = Integer.parseInt(args[14]);

        String matchFolder = args[15];
        float alpha = Float.parseFloat(args[16]);
        float lamda = Float.parseFloat(args[17]);
        float highLat = Float.parseFloat(args[18]);
        float obs = Float.parseFloat(args[19]);
        float beta = Float.parseFloat(args[20]);


        boolean transpose = (Integer.parseInt(args[21]) == 1) ? true : false;

        float latMin = Float.parseFloat(args[22]);
        float latMax = Float.parseFloat(args[23]);
        float lonMin = Float.parseFloat(args[24]);
        float lonMax = Float.parseFloat(args[25]);


        Mosaicking col = new Mosaicking();
        col.resDegX = resDegX;
        col.resDegY = resDegY;
        col.resName = resName;
        col.year = year;
        col.doy = doy;
        col.mod = mod;

        col.inTilesDIR = inTilesDIR;
        col.outfolder = outfolder;
        col.pathbands = pathbands;
        col.idxTile = idxTile;
        col.ncVersion = ncVersion;

        col.nodata = nodata;
        col.wTile = wTile;
        col.hTile = hTile;
        col.div = div;
        col.matchFolder = matchFolder;

        col.alpha = alpha;
        col.lamda = lamda;
        col.highLat = highLat;
        col.obs = obs;
        col.beta = beta;
        col.transpose = transpose;

        col.latMin = latMin;
        col.latMax = latMax;
        col.lonMin = lonMin;
        col.lonMax = lonMax;

        System.out
                .println("\n\n----------------------------------------------");
        System.out.println("resDegX: " + col.resDegX);
        System.out.println("resDegY: " + col.resDegY);
        System.out.println("resName: " + col.resName);
        System.out.println("year: " + col.year);
        System.out.println("doy: " + col.doy);
        System.out.println("mod: " + col.mod);

        System.out.println("inTilesDir: " + col.inTilesDIR);
        System.out.println("outFolder: " + col.outfolder);
        System.out.println("pathbands: " + col.pathbands);
        System.out.println("idxTile: " + col.idxTile);
        System.out.println("version: " + col.ncVersion);

        System.out.println("nodata: " + col.nodata);
        System.out.println("wTile: " + col.wTile);
        System.out.println("hTile: " + col.hTile);
        System.out.println("div: " + col.div);
        System.out.println("matchFolder: " + col.matchFolder);

        System.out.println("threshold on common area: " + col.alpha);
        System.out.println("slope of threshold: " + col.lamda);
        System.out.println("highLat: " + col.highLat);
        System.out.println("maxObsHighLat: " + col.obs);
        System.out.println("slopeObsHighLat: " + col.beta);
        System.out.println("transpose: " + transpose);
        System.out.println("latMin, latMax: " + col.latMin + ", " + col.latMax);
        System.out.println("lonMin, lonMax: " + col.lonMin + ", " + col.lonMax);
        System.out
                .println("--------------------------------------------------------\n\n");

        col.compute();

    }
}

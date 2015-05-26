package org.esa.beam.globalbedo.staging;

import Jama.Matrix;
import ucar.ma2.Array;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayFloat;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.NetcdfFileWriter.Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * This program is about creating albedo product from brdf product (merge, snow, nosnow)
 *
 * @author Said Kharbouche MSSL.UCL (2014)
 */
public class Brdf2Albedo {
    //##################################################################################################################
    /**
     * input brdf file (merge, inversion.snow or inversion.nosnow)
     */
    private String brdfFile = null;
    /**
     * if(1) transpose 2D array of input file
     */
    private int transpose = -1;
    /**
     * output directory
     */
    private String outputFolder = null;
    /**
     * albedo layers to generate
     */
    private String newAttriIn[];
    /**
     * position of date in input brdf file (splitted by ".")
     */
    private int idxDate = -1;
    /**
     * tile name or "null" if mosaic
     */
    private String tile = null;
    /**
     * spatial resolution: 1km, 5km, 25km, 005, 05,
     */
    private String resName;
    /**
     * to be added in the name of resulted albedo file
     */
    private String productName;
    /**
     * 3:netcdf3, 4:netcdf4
     */
    private int ncVersion;
    //##################################################################################################################


    private Boolean existSFband = false;
    private String mod = "";
    // String albedoFolder = null;
    private boolean isMerge;
    private boolean isSnow;
    private boolean isNoSnow;
    private boolean addSnowFraction = true;


    private String newAttr[] = {"DHR_VIS", "DHR_NIR", "DHR_SW", "BHR_VIS", "BHR_NIR",
            "BHR_SW", "DHR_sigmaVIS", "DHR_sigmaNIR", "DHR_sigmaSW",
            "BHR_sigmaVIS", "BHR_sigmaNIR", "BHR_sigmaSW", "DHR_alpha_VIS_NIR",
            "DHR_alpha_VIS_SW", "DHR_alpha_NIR_SW", "BHR_alpha_VIS_NIR",
            "BHR_alpha_VIS_SW", "BHR_alpha_NIR_SW"};

    private String attrMask = "Data_Mask";
    private String attrSZA = "Solar_Zenith_Angle";
    private String attrRelEntropy = "Relative_Entropy";
    private String attrSnowFraction = "Snow_Fraction";
    private String attrSnowFracBrdf = "Proportion_NSamples";

    private String attrOthers[] = {"Weighted_Number_of_Samples", "Goodness_of_Fit",
            "Time_to_the_Closest_Sample", "lat", "lon"};

    private int doy = -1;
    private int year = -1;
    private int b = 9;

    private int dimX = -1;
    private int dimY = -1;

    private String[] meanBands = {"mean_VIS_f0", "mean_VIS_f1", "mean_VIS_f2",
            "mean_NIR_f0", "mean_NIR_f1", "mean_NIR_f2", "mean_SW_f0",
            "mean_SW_f1", "mean_SW_f2"};

    private String[] varBands = {"VAR_VIS_f0_VIS_f0", "VAR_VIS_f0_VIS_f1",
            "VAR_VIS_f0_VIS_f2", "VAR_VIS_f0_NIR_f0", "VAR_VIS_f0_NIR_f1",
            "VAR_VIS_f0_NIR_f2", "VAR_VIS_f0_SW_f0", "VAR_VIS_f0_SW_f1",
            "VAR_VIS_f0_SW_f2", "VAR_VIS_f1_VIS_f1", "VAR_VIS_f1_VIS_f2",
            "VAR_VIS_f1_NIR_f0", "VAR_VIS_f1_NIR_f1", "VAR_VIS_f1_NIR_f2",
            "VAR_VIS_f1_SW_f0", "VAR_VIS_f1_SW_f1", "VAR_VIS_f1_SW_f2",
            "VAR_VIS_f2_VIS_f2", "VAR_VIS_f2_NIR_f0", "VAR_VIS_f2_NIR_f1",
            "VAR_VIS_f2_NIR_f2", "VAR_VIS_f2_SW_f0", "VAR_VIS_f2_SW_f1",
            "VAR_VIS_f2_SW_f2", "VAR_NIR_f0_NIR_f0", "VAR_NIR_f0_NIR_f1",
            "VAR_NIR_f0_NIR_f2", "VAR_NIR_f0_SW_f0", "VAR_NIR_f0_SW_f1",
            "VAR_NIR_f0_SW_f2", "VAR_NIR_f1_NIR_f1", "VAR_NIR_f1_NIR_f2",
            "VAR_NIR_f1_SW_f0", "VAR_NIR_f1_SW_f1", "VAR_NIR_f1_SW_f2",
            "VAR_NIR_f2_NIR_f2", "VAR_NIR_f2_SW_f0", "VAR_NIR_f2_SW_f1",
            "VAR_NIR_f2_SW_f2", "VAR_SW_f0_SW_f0", "VAR_SW_f0_SW_f1",
            "VAR_SW_f0_SW_f2", "VAR_SW_f1_SW_f1", "VAR_SW_f1_SW_f2",
            "VAR_SW_f2_SW_f2"};


    private Version netcdfVersin = Version.netcdf3c;


    /**
     * launch the precess of brdf-->albedo
     */
    public void process() {

        // ###########################
        if (this.ncVersion == 4)
            this.netcdfVersin = Version.netcdf4;
        // ###########################

        for (int j = 0; j < this.newAttr.length; j++) {
            boolean t = true;
            for (int i = 0; i < this.newAttriIn.length; i++)
                if (this.newAttriIn[i].replaceAll(" ", "").equalsIgnoreCase(
                        this.newAttr[j].replaceAll(" ", "")))
                    t = false;
            if (t)
                this.newAttr[j] = null;
        }

        for (int j = 0; j < this.attrOthers.length; j++) {
            boolean t = true;
            for (int i = 0; i < this.newAttriIn.length; i++)
                if (this.newAttriIn[i].replaceAll(" ", "").equalsIgnoreCase(
                        attrOthers[j].replaceAll(" ", "")))
                    t = false;
            if (t)
                this.attrOthers[j] = null;
        }

        boolean t1 = true, t2 = true, t3 = true, t4 = true;
        for (int i = 0; i < this.newAttriIn.length; i++)
            if (this.newAttriIn[i].replaceAll(" ", "").equalsIgnoreCase(
                    attrSnowFraction))
                t1 = false;
            else if (this.newAttriIn[i].replaceAll(" ", "").equalsIgnoreCase(
                    attrMask))
                t2 = false;
            else if (this.newAttriIn[i].replaceAll(" ", "").equalsIgnoreCase(
                    attrSZA))
                t3 = false;
            else if (this.newAttriIn[i].replaceAll(" ", "").equalsIgnoreCase(
                    attrRelEntropy))
                t4 = false;

        if (t1)
            this.attrSnowFraction = null;
        if (t2)
            this.attrMask = null;
        if (t3)
            this.attrSZA = null;
        if (t4)
            this.attrRelEntropy = null;

        try {

            this.isMerge = isMerge();

            this.isSnow = isSnow();
            this.isNoSnow = isNoSnow();

            if (this.isMerge)
                mod = "merge";
            else if (this.isSnow)
                mod = "Snow";
            else if (this.isNoSnow())
                mod = "NoSnow";

            if (!this.addSnowFraction)
                this.attrSnowFraction = null;

            doy = extractDoY(brdfFile);
            year = extractYear(brdfFile);

            String outputfile = outputName();

            System.out.println("start process..");

            NetcdfFile ncfileBrdf = null;

            NetcdfFileWriter outFile = null;
            // NetcdfFile ncfileAlbedo=null;
            boolean exist = false;

            ncfileBrdf = NetcdfFile.open(brdfFile);

            if (ncfileBrdf.findVariable(this.attrSnowFracBrdf) != null)
                this.existSFband = true;

            System.out.println("inputfile: " + ncfileBrdf.getLocation());
            outFile = NetcdfFileWriter.createNew(this.netcdfVersin, outputfile);

            outFile.getNetcdfFile().addAttribute(
                    outFile.getNetcdfFile().getRootGroup(),
                    new Attribute("authors", "Dr. Said Kharbouche"));
            outFile.getNetcdfFile().addAttribute(
                    outFile.getNetcdfFile().getRootGroup(),
                    new Attribute("project_manager", "Prof. Jan-Peter Muller"));
            outFile.getNetcdfFile()
                    .addAttribute(
                            outFile.getNetcdfFile().getRootGroup(),
                            new Attribute(
                                    "institution",
                                    "Mullard Space Science Laboratory, Department of Space and Climate Physics, University College London"));
            outFile.getNetcdfFile().addAttribute(
                    outFile.getNetcdfFile().getRootGroup(),
                    new Attribute("title", "Albedo " + mod));
            outFile.getNetcdfFile()
                    .addAttribute(
                            outFile.getNetcdfFile().getRootGroup(),
                            new Attribute("reference",
                                          "http://globalbedo.org/docs/GlobAlbedo_Albedo_ATBD_V4.12.pdf"));
            outFile.getNetcdfFile().addAttribute(
                    outFile.getNetcdfFile().getRootGroup(),
                    new Attribute("production_date", "2014"));
            outFile.getNetcdfFile().addAttribute(
                    outFile.getNetcdfFile().getRootGroup(),
                    new Attribute("extracted_from", new File(this.brdfFile)
                            .getName()));
            if (this.tile != null)
                outFile.getNetcdfFile().addAttribute(
                        outFile.getNetcdfFile().getRootGroup(),
                        new Attribute("tile", this.tile));
            outFile.getNetcdfFile().addAttribute(
                    outFile.getNetcdfFile().getRootGroup(),
                    new Attribute("grid", this.resName));
            outFile.getNetcdfFile().addAttribute(
                    outFile.getNetcdfFile().getRootGroup(),
                    new Attribute("year", year));
            outFile.getNetcdfFile().addAttribute(
                    outFile.getNetcdfFile().getRootGroup(),
                    new Attribute("doy", doy));
            // outFile.addGlobalAttribute("tile", new
            // File(this.brdfFile).getName());

            System.out.println("output file: " + outputfile);
            System.out.println("Tile: " + tile);
            System.out.println("mode: " + this.mod);
            System.out.println("exits " + this.attrSnowFracBrdf + "?: "
                                       + this.existSFband);
            System.out.println("DoY:" + doy);
            System.out.println("Year:" + year);
            exist = true;

            if (exist) {

                Array[] array = new Array[45];
                Array[] arrayF = new Array[9];

                for (int i = 0; i < 9; i++)
                    arrayF[i] = ncfileBrdf.findVariable(meanBands[i]).read();

                for (int i = 0; i < 45; i++)
                    array[i] = ncfileBrdf.findVariable(varBands[i]).read();

                List<Dimension> dimsTmp = ncfileBrdf.getDimensions();
                List<Dimension> dims = new ArrayList<Dimension>();

                if (!dimsTmp.get(0).getFullName().equals("x")) {
                    dims.add(0, dimsTmp.get(1));
                    dims.add(1, dimsTmp.get(0));
                } else {
                    dims.add(0, dimsTmp.get(0));
                    dims.add(1, dimsTmp.get(1));
                }

                this.dimX = dimsTmp.get(0).getLength();
                this.dimY = dimsTmp.get(1).getLength();

                System.out.println("dimX: " + dims.get(0).getLength() + ", "
                                           + dims.get(0).getFullName());
                System.out.println("dimY: " + dims.get(1).getLength() + ", "
                                           + dims.get(1).getFullName());

                Array entropy = null;
                Array varLat = null;
                Array relEntropy = null;

                try {

                    if (ncfileBrdf.findVariable("lat") != null)
                        varLat = ncfileBrdf.findVariable("lat").read();

                    if (ncfileBrdf.findVariable("Entropy") != null)
                        entropy = ncfileBrdf.findVariable("Entropy").read();

                    if (ncfileBrdf.findVariable("Relative_Entropy") != null)
                        relEntropy = ncfileBrdf
                                .findVariable("Relative_Entropy").read();

                } catch (IOException e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                }

                for (int d = 0; d < dims.size(); d++)
                    outFile.addDimension(
                            outFile.getNetcdfFile().getRootGroup(), dims.get(d)
                                    .getFullName(), dims.get(d).getLength());

                for (int i = 0; i < this.newAttr.length; i++)
                    if (newAttr[i] != null)
                        outFile.addVariable(outFile.getNetcdfFile()
                                                    .getRootGroup(), newAttr[i],
                                            ucar.ma2.DataType.FLOAT, dims);

                for (int i = 0; i < this.attrOthers.length; i++)
                    if (this.attrOthers[i] != null)
                        outFile.addVariable(outFile.getNetcdfFile()
                                                    .getRootGroup(), attrOthers[i],
                                            ucar.ma2.DataType.FLOAT, dims);

                if (this.attrMask != null)
                    outFile.addVariable(outFile.getNetcdfFile().getRootGroup(),
                                        this.attrMask, ucar.ma2.DataType.BYTE, dims);

                if (this.attrSnowFraction != null)
                    outFile.addVariable(outFile.getNetcdfFile().getRootGroup(),
                                        this.attrSnowFraction, ucar.ma2.DataType.FLOAT,
                                        dims);

                if (this.attrSZA != null)
                    outFile.addVariable(outFile.getNetcdfFile().getRootGroup(),
                                        this.attrSZA, ucar.ma2.DataType.FLOAT, dims);

                if (this.attrRelEntropy != null)
                    outFile.addVariable(outFile.getNetcdfFile().getRootGroup(),
                                        this.attrRelEntropy, ucar.ma2.DataType.FLOAT, dims);
                outFile.create();

                double k_bhr[] = {1, 0.189184, -1.377622};

                Matrix m_bhr = new Matrix(3, 9, 0.0);

                for (int a = 0; a < 3; a++) {
                    m_bhr.set(a, (a * 3), k_bhr[0]);
                    m_bhr.set(a, (a * 3) + 1, k_bhr[1]);
                    m_bhr.set(a, (a * 3) + 2, k_bhr[2]);
                }

                Array mask = null;
                Array sza = null;
                Array relentropy = null;

                if (this.attrMask != null)
                    mask = new ArrayByte.D2(dimX, dimY);

                if (this.attrSZA != null)
                    sza = new ArrayFloat.D2(dimX, dimY);

                if (this.attrRelEntropy != null)
                    relentropy = new ArrayFloat.D2(dimX, dimY);

                Array BHR[] = new Array[3];
                Array DHR[] = new Array[3];

                Array alphaBHR[] = new Array[3];
                Array alphaDHR[] = new Array[3];

                Array sigmaBHR[] = new Array[3];
                Array sigmaDHR[] = new Array[3];

                for (int a = 0; a < 3; a++) {
                    BHR[a] = new ArrayFloat.D2(dimX, dimY);
                    DHR[a] = new ArrayFloat.D2(dimX, dimY);
                    ;
                    sigmaBHR[a] = new ArrayFloat.D2(dimX, dimY);
                    ;
                    sigmaDHR[a] = new ArrayFloat.D2(dimX, dimY);
                    alphaBHR[a] = new ArrayFloat.D2(dimX, dimY);
                    alphaDHR[a] = new ArrayFloat.D2(dimX, dimY);
                }

                // #############################################################
                // #
                // #############################################################

                int dimXY = dimX * dimY;

                for (int p = 0; p < dimXY; p++) {

                    for (int a = 0; a < 3; a++) {
                        sigmaBHR[a].setFloat(p, Float.NaN);
                        sigmaDHR[a].setFloat(p, Float.NaN);
                        alphaBHR[a].setFloat(p, Float.NaN);
                        alphaDHR[a].setFloat(p, Float.NaN);
                    }

                    float SZA = Float.NaN;

                    if (varLat != null)
                        SZA = computeSza(varLat.getFloat(p), doy);

                    Matrix C = new Matrix(9, 9, Float.NaN);

                    double[] k_dhr = new double[]{
                            1.0,
                            -0.007574 + (-0.070887 * Math.pow(SZA, 2.0))
                                    + (0.307588 * Math.pow(SZA, 3.0)),
                            -1.284909 + (-0.166314 * Math.pow(SZA, 2.0))
                                    + (0.041840 * Math.pow(SZA, 3.0))};

                    Matrix m_dhr = new Matrix(3, 9, 0.0);

                    for (int a = 0; a < 3; a++) {
                        m_dhr.set(a, (a * 3), k_dhr[0]);
                        m_dhr.set(a, (a * 3) + 1, k_dhr[1]);
                        m_dhr.set(a, (a * 3) + 2, k_dhr[2]);
                    }

                    for (int j = 0, c = 0; j < b; j++)
                        for (int i = j; i < b; c++, i++) {
                            double val = Double.NaN;

                            val = array[c].getFloat(p);

                            C.set(i, j, val);
                            C.set(j, i, val);

                        }
                    float f[][] = new float[3][3];
                    for (int a = 0, c = 0; a < 3; a++)
                        for (int b = 0; b < 3; b++, c++)
                            f[a][b] = arrayF[c].getFloat(p);

                    Matrix c_dhr = m_dhr.times(C.transpose()).times(
                            m_dhr.transpose());
                    Matrix c_bhr = m_bhr.times(C.transpose()).times(
                            m_bhr.transpose());

                    sigmaBHR[0].setFloat(p,
                                         (float) Math.min(1.0, Math.sqrt(c_bhr.get(0, 0))));
                    sigmaBHR[1].setFloat(p,
                                         (float) Math.min(1.0, Math.sqrt(c_bhr.get(1, 1))));
                    sigmaBHR[2].setFloat(p,
                                         (float) Math.min(1.0, Math.sqrt(c_bhr.get(2, 2))));

                    sigmaDHR[0].setFloat(p,
                                         (float) Math.min(1.0, Math.sqrt(c_dhr.get(0, 0))));
                    sigmaDHR[1].setFloat(p,
                                         (float) Math.min(1.0, Math.sqrt(c_dhr.get(1, 1))));
                    sigmaDHR[2].setFloat(p,
                                         (float) Math.min(1.0, Math.sqrt(c_dhr.get(2, 2))));

                    for (int a = 0; a < 3; a++) {
                        if (sigmaBHR[a].getFloat(p) == 0.0f)
                            sigmaBHR[a].setFloat(p, Float.NaN);
                        if (sigmaDHR[a].getFloat(p) == 0.0f)
                            sigmaDHR[a].setFloat(p, Float.NaN);
                    }

                    alphaBHR[0].setFloat(
                            p,
                            (float) (c_bhr.get(0, 1) / Math.sqrt(c_bhr
                                                                         .get(0, 0) * c_bhr.get(1, 1))));
                    alphaBHR[1].setFloat(
                            p,
                            (float) (c_bhr.get(0, 2) / Math.sqrt(c_bhr
                                                                         .get(0, 0) * c_bhr.get(2, 2))));
                    alphaBHR[2].setFloat(
                            p,
                            (float) (c_bhr.get(1, 2) / Math.sqrt(c_bhr
                                                                         .get(1, 1) * c_bhr.get(2, 2))));

                    alphaDHR[0].setFloat(
                            p,
                            (float) (c_dhr.get(0, 1) / Math.sqrt(c_dhr
                                                                         .get(0, 0) * c_dhr.get(1, 1))));
                    alphaDHR[1].setFloat(
                            p,
                            (float) (c_dhr.get(0, 2) / Math.sqrt(c_dhr
                                                                         .get(0, 0) * c_dhr.get(2, 2))));
                    alphaDHR[2].setFloat(
                            p,
                            (float) (c_dhr.get(1, 2) / Math.sqrt(c_dhr
                                                                         .get(1, 1) * c_dhr.get(2, 2))));

                    for (int b = 0; b < 3; b++) {
                        float sumBHR = 0f;
                        float sumDHR = 0f;
                        for (int c = 0; c < 3; c++) {
                            sumBHR += f[b][c] * k_bhr[c];
                            sumDHR += f[b][c] * k_dhr[c];
                        }
                        if (sumBHR == 0.0)
                            sumBHR = Float.NaN;
                        if (sumDHR == 0.0)
                            sumDHR = Float.NaN;

                        BHR[b].setFloat(p, sumBHR);
                        DHR[b].setFloat(p, sumDHR);

                    }

                    if (this.attrMask != null && entropy != null)
                        mask.setByte(p,
                                     (byte) ((entropy.getFloat(p) != 0.0) ? 1 : 0));

                    if (this.attrSZA != null && sza != null)
                        sza.setFloat(p, SZA);

                    if (this.attrRelEntropy != null && relentropy != null
                            && relEntropy != null)
                        relentropy
                                .setFloat(p, (float) (Math.exp(relEntropy
                                                                       .getFloat(p) / 9.0)));

                }

                outFile = NetcdfFileWriter.openExisting(outputfile);

                // for (int i = 0; i <DHR[0].getSize(); i++)
                // if(!Float.isNaN(DHR[0].getFloat(i)))
                // System.out.println(">>>"+DHR[0].getFloat(i));

                for (int i = 0; i < newAttr.length; i++) {
                    if (i < 3 && newAttr[i] != null)
                        outFile.write(outFile.findVariable(newAttr[i]), DHR[i]);

                    else if (i < 6 && newAttr[i] != null)
                        outFile.write(outFile.findVariable(newAttr[i]),
                                      BHR[i - 3]);

                    else if (i < 9 && newAttr[i] != null)
                        outFile.write(outFile.findVariable(newAttr[i]),
                                      sigmaDHR[i - 6]);

                    else if (i < 12 && newAttr[i] != null)
                        outFile.write(outFile.findVariable(newAttr[i]),
                                      sigmaBHR[i - 9]);

                    else if (i < 15 && newAttr[i] != null)
                        outFile.write(outFile.findVariable(newAttr[i]),
                                      alphaDHR[i - 12]);

                    else if (i < 18 && newAttr[i] != null)
                        outFile.write(outFile.findVariable(newAttr[i]),
                                      alphaBHR[i - 15]);

                }

                if (this.attrSnowFraction != null) {
                    Array snowFraction = new ArrayFloat.D2(dimX, dimY);
                    if (this.existSFband)
                        snowFraction = ncfileBrdf.findVariable(
                                this.attrSnowFracBrdf).read();
                    else {
                        float val = Float.NaN;
                        if (this.isSnow)
                            val = 1f;
                        else if (this.isNoSnow)
                            val = 0f;

                        for (int p = 0; p < dimXY; p++)
                            snowFraction.setFloat(p, val);

                    }

                    for (int p = 0; p < dimXY; p++) {
                        boolean masked = true;
                        for (int b = 0; b < BHR.length; b++)
                            if ((!Float.isNaN(BHR[b].getFloat(p)))
                                    || (!Float.isNaN(DHR[b].getFloat(p)))) {
                                masked = false;
                                break;
                            }
                        if (masked)
                            snowFraction.setFloat(p, Float.NaN);
                    }
                    outFile.write(outFile.findVariable(this.attrSnowFraction),
                                  snowFraction);
                }

                // for (int i = 0; i <mask.getSize(); i++)
                // if(!Float.isNaN(mask.getByte(i)))
                // System.out.println(">>>"+mask.getByte(i));
                if (this.attrMask != null)
                    outFile.write(outFile.findVariable(this.attrMask), mask);

                if (this.attrSZA != null)
                    outFile.write(outFile.findVariable(this.attrSZA), sza);

                if (this.attrRelEntropy != null)
                    outFile.write(outFile.findVariable(this.attrRelEntropy),
                                  relentropy);

                for (int i = 0; i < this.attrOthers.length; i++)
                    if (this.attrOthers[i] != null)
                        outFile.write(outFile.findVariable(this.attrOthers[i]),
                                      ncfileBrdf.findVariable(this.attrOthers[i])
                                              .read());

                // List<Variable> listVariabOut = outFile.getVariables();

				/*
                 * for (int i = 0; i < listVariabOut.size(); i++)
				 * System.out.println(i + "- " + listVariabOut.get(i).getName()
				 * + ", type:" + listVariabOut.get(i).getDataType().toString());
				 */

                ncfileBrdf.close();
                outFile.close();

                if (this.transpose == 1) {
                    System.out.println("flip...");
                    outFile = NetcdfFileWriter.openExisting(outputfile);
                    for (int v = 0; v < outFile.getNetcdfFile().getVariables()
                            .size(); v++) {
                        // System.out.println("band: "+outFile.getNetcdfFile().getVariables().get(v).getFullName());
                        Array arr = outFile.getNetcdfFile().getVariables()
                                .get(v).read();
                        Array arrT = arr.transpose(1, 0).copy();

                        outFile.write(outFile.getNetcdfFile().getVariables()
                                              .get(v), arrT);

                    }
                    outFile.close();

                }

                System.out.println("end process.\n\n");
            }

        } catch (Exception e1) {

            e1.printStackTrace();
        }

    }


    /**
     * check if input file is brdf.merge
     *
     * @return
     */
    private boolean isMerge() {

        if ((new File(this.brdfFile).getName().toLowerCase()
                .contains(".merge.")))
            return true;
        else
            return false;

    }

    /**
     * check if input file is brdf.Snow
     *
     * @return
     */
    private boolean isSnow() {

        if ((new File(this.brdfFile).getName().toLowerCase().contains(".snow.")))
            return true;
        else
            return false;

    }

    /**
     * check if input file is berdf.NoSnow
     *
     * @return
     */
    private boolean isNoSnow() {

        if ((new File(this.brdfFile).getName().toLowerCase()
                .contains(".nosnow.")))
            return true;
        else
            return false;

    }

    /**
     * determine the name file of resulted albedo product
     *
     * @return
     */
    private String outputName() {
        String name = new File(this.brdfFile).getName();

        name = name.replaceAll(".NoSnow.", ".");
        name = name.replaceAll(".Snow.", ".");
        name = name.replaceAll(".merge.", ".");
        if (this.productName.toLowerCase().contains("null")
                || this.productName.toLowerCase().contains("none"))
            this.productName = "";
        else
            this.productName = "." + this.productName;

        String out = this.outputFolder
                + name.replace(".brdf.", "." + this.mod + this.productName
                + ".");

        if (out.endsWith(".nc.gz"))
            out = out.replace(".nc.gz", ".nc");
        return out;
    }

    /**
     * extract DoY fron input brdf file
     *
     * @param path
     * @return
     */
    private int extractDoY(String path) {

        String tab[] = path.split("\\.");
        return Integer.parseInt(tab[idxDate].substring(4, 7));

    }

    /**
     * extract year from input brdf file
     *
     * @param path
     * @return
     */
    private int extractYear(String path) {

        String tab[] = path.split("\\.");
        return Integer.parseInt(tab[idxDate].substring(0, 4));
    }

    /**
     * Computes solar zenith angle at local noon as function of Geoposition and
     * DoY
     *
     * @param lat - latitude
     * @param doy    - day of year
     * @return sza - in degrees!!
     */
    public static float computeSza(float lat, int doy) {

        final double latitude = lat * Math.PI / 180.0;

        // # To emulate MODIS products, set fixed LST = 12.00
        final double LST = 12.0;
        // # Now we can calculate the Sun Zenith Angle (SZArad):
        final double h = (12.0 - (LST)) / 12.0 * Math.PI;
        final double delta = -23.45 * (Math.PI / 180.0)
                * Math.cos(2 * Math.PI / 365.0 * (doy + 10));
        float SZA = (float) (Math.acos(Math.sin(latitude) * Math.sin(delta)
                                               + Math.cos(latitude) * Math.cos(delta) * Math.cos(h)));

        return SZA;
    }

    public static void main(String[] args) {
		/*
		int transpose = 0;
		String brdfFile = "/unsafe/said/tmp/testCodes/mosaic/brdf/GlobAlbedo.brdf.merge.005.2005217.nc";
		String outFolder = "/unsafe/said/tmp/testCodes/albedo/";

		String attributes = "BHR_VIS,BHR_NIR,BHR_sigmaVIS,BHR_sigmaNIR,BHR_alpha_VIS_NIR,Weighted_Number_of_Samples";
		int idxDate = 4;
		String tile = "null";
		String resName = "deg005";
		String productName = "albedo";
		int ncVersion = 4;
		*/

        int transpose = Integer.parseInt(args[0]);
        String brdfFile = args[1];
        String outFolder = args[2];

        String attributes = args[3];
        int idxDate = Integer.parseInt(args[4]);
        String tile = args[5];
        String resName = args[6];
        String productName = args[7];
        int ncVersion = Integer.parseInt(args[8]);


        Brdf2Albedo alb = new Brdf2Albedo();
        alb.transpose = transpose;
        alb.brdfFile = brdfFile;
        alb.outputFolder = outFolder;
        alb.newAttriIn = attributes.split(",");
        alb.idxDate = idxDate;
        alb.tile = (tile.contains("v") || tile.contains("V") ? tile : null);
        alb.resName = resName;
        alb.productName = productName;
        alb.ncVersion = ncVersion;

        alb.process();

    }
}

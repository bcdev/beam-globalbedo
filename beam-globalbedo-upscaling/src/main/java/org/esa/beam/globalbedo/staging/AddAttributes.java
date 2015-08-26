package org.esa.beam.globalbedo.staging;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.NetcdfFileWriter.Version;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

/**
 * This program is about adding global and variable attributes to the input netcdf file
 * and time as a third dimension (lat, lon, time)
 *
 * @author Said Kharbouche MSSL.UCL (2014)
 */
public class AddAttributes {
    /**
     * infile
     */
    private String inpath;
    /**
     * outfile
     */
    private String outpath;

    /**
     * variables
     */
    private String inTxtBand;
    /**
     * dimensions
     */
    private String inTxtDim;
    /**
     * global attributes
     */
    private String inTxtGlob;

    /**
     * lat variable name
     */
    private String latName;
    /**
     * longitude variable name
     */
    private String lonName;

    /**
     * create lat/lon 1-Dims variables
     */
    private boolean latLon1D;
    /**
     * create lat/lon 2-Dims variables
     */
    private boolean latLon2D;

    /**
     * add time dimension
     */
    private boolean addTime;
    /**
     * position of YearDoY in file name (splitted by ".")
     */
    private int idxDate;

    /**
     * name of time dimension
     */
    private String timeName;

    /**
     * min latitude
     */
    private float latMin;
    /**
     * max latitude
     */
    private float latMax;
    /**
     * min longitude
     */
    private float lonMin;
    /**
     * max longitude
     */
    private float lonMax;
    /**
     * outfile netcdf version
     */
    private int ncVersion = 4;

    private int timsecond;
    private String varName[];
    private String varNewAttName[][];
    private String varNewAttValue[][];
    private String varNewAttType[][];
    private String globNewAttrName[];
    private String globNewAttrVal[];
    private Array latD1;
    private Array lonD1;
    private Version version;

    /**
     * launch the process of attributes adding and 3D dimensions creation (lat,
     * lon, time)
     */
    void compute() {
        readParams();
        NetcdfFile ncfileIN;
        NetcdfFileWriter ncfileOUT = null;

        try {

            if (ncVersion == 4)
                this.version = Version.netcdf4;
            else
                this.version = Version.netcdf3;

            System.out.println("NetCDF Version: " + this.version.toString());
            ncfileIN = NetcdfFile.open(inpath);
            outpath = outpath + "/" + new File(inpath).getName();
            outpath = outpath.replaceAll(" ", "");
            //System.out.println("output file: " + outpath+" >> "+outpath.charAt(outpath.length()-1));
            ncfileOUT = NetcdfFileWriter.createNew(this.version, outpath);

            // ################################################
            int w = -1;
            int h = -1;
            String tab[] = this.inTxtDim.split(",");
            if (tab[0].split(":")[0].equalsIgnoreCase(ncfileIN.getDimensions()
                                                              .get(0).getFullName())
                    && (this.latName.toLowerCase().contains(tab[0].split(":")[1].toLowerCase()))) {
                w = ncfileIN.getDimensions().get(1).getLength();
                h = ncfileIN.getDimensions().get(0).getLength();
            } else {
                w = ncfileIN.getDimensions().get(0).getLength();
                h = ncfileIN.getDimensions().get(1).getLength();
            }
            // ################################################

            List<Dimension> dimLat = new ArrayList<Dimension>();
            dimLat.add(new Dimension(this.latName, h));

            List<Dimension> dimLon = new ArrayList<Dimension>();
            dimLon.add(new Dimension(this.lonName, w));

            List<Dimension> dims = new ArrayList<Dimension>();
            List<Dimension> dimTime = new ArrayList<Dimension>();
            if (this.addTime) {
                dimTime.add(new Dimension(this.timeName, 1));
                ncfileOUT.getNetcdfFile().addDimension(
                        ncfileOUT.getNetcdfFile().getRootGroup(),
                        dimTime.get(0));
                dims.add(0, dimTime.get(0));
            }
            dims.add(1, dimLat.get(0));
            dims.add(2, dimLon.get(0));

            ncfileOUT.getNetcdfFile().addDimension(
                    ncfileOUT.getNetcdfFile().getRootGroup(), dimLat.get(0));
            ncfileOUT.getNetcdfFile().addDimension(
                    ncfileOUT.getNetcdfFile().getRootGroup(), dimLon.get(0));

            // ncfile.getNetcdfFile().getDimensions().get(0).setName("");
            // for(int i=0; i<oldDim.length; i++)
            // ncfile.renameDimension(ncfile.getNetcdfFile().getRootGroup(),
            // oldDim[i], newDim[i]);

            for (int b = 0; b < ncfileIN.getVariables().size(); b++) {
                Variable var = ncfileIN.getVariables().get(b);
                if ((!var.getFullName().equalsIgnoreCase(this.timeName))
                        && (!var.getFullName().equalsIgnoreCase(this.latName))
                        && (!var.getFullName().equalsIgnoreCase(lonName))) {
                    System.out.println("create variable: " + var.getFullName()
                                               + ", data_type: " + var.getDataType());
                    ncfileOUT.addVariable(ncfileOUT.getNetcdfFile()
                                                  .getRootGroup(), var.getFullName(), var
                                                  .getDataType(), dims);
                }
            }

            ncfileOUT.addVariable(ncfileOUT.getNetcdfFile().getRootGroup(),
                                  this.latName, DataType.FLOAT, dimLat);
            ncfileOUT.addVariable(ncfileOUT.getNetcdfFile().getRootGroup(),
                                  this.lonName, DataType.FLOAT, dimLon);

            if (this.addTime)
                ncfileOUT.addVariable(ncfileOUT.getNetcdfFile().getRootGroup(),
                                      this.timeName, DataType.INT, dimTime);

            // ncfileOUT.abort();
            ncfileOUT.create();
            ncfileOUT.close();

            ncfileOUT = NetcdfFileWriter.openExisting(outpath);
            ncfileOUT.setRedefineMode(true);

            for (int i = 0; i < this.globNewAttrName.length; i++)
                ncfileOUT.getNetcdfFile().addAttribute(
                        ncfileOUT.getNetcdfFile().getRootGroup(),
                        new Attribute(this.globNewAttrName[i],
                                      this.globNewAttrVal[i]));

            for (int i = 0; i < varName.length; i++) {
                if (!varName[i].equalsIgnoreCase("all"))
                    if (ncfileOUT.findVariable(varName[i]) != null)
                        for (int j = 0; j < varNewAttName[i].length; j++) {
                            System.out.println("add attribite to : "
                                                       + varName[i] + " >> " + varNewAttName[i][j]
                                                       + "=" + varNewAttValue[i][j]);
                            if (this.varNewAttType[i][j].startsWith("f"))
                                ncfileOUT
                                        .addVariableAttribute(
                                                ncfileOUT
                                                        .findVariable(varName[i]),
                                                new Attribute(
                                                        varNewAttName[i][j],
                                                        Float.parseFloat(varNewAttValue[i][j])));
                            else if (this.varNewAttType[i][j].startsWith("i"))
                                ncfileOUT
                                        .addVariableAttribute(
                                                ncfileOUT
                                                        .findVariable(varName[i]),
                                                new Attribute(
                                                        varNewAttName[i][j],
                                                        Integer.parseInt(varNewAttValue[i][j])));
                            else if (this.varNewAttType[i][j].toLowerCase()
                                    .startsWith("b"))
                                ncfileOUT
                                        .addVariableAttribute(
                                                ncfileOUT
                                                        .findVariable(varName[i]),
                                                new Attribute(
                                                        varNewAttName[i][j],
                                                        Byte.parseByte(varNewAttValue[i][j])));
                            else
                                ncfileOUT.addVariableAttribute(ncfileOUT
                                                                       .findVariable(varName[i]),
                                                               new Attribute(varNewAttName[i][j],
                                                                             varNewAttValue[i][j]));
                        }

                if (varName[i].equalsIgnoreCase("all"))
                    for (int k = 0; k < ncfileOUT.getNetcdfFile()
                            .getVariables().size(); k++)
                        for (int j = 0; j < varNewAttName[i].length; j++) {
                            System.out.println("add attribite to : "
                                                       + ncfileOUT.getNetcdfFile().getVariables()
                                    .get(k).getFullName() + " >> "
                                                       + varNewAttName[i][j] + "="
                                                       + varNewAttValue[i][j]);
                            if (this.varNewAttType[i][j].startsWith("f"))
                                ncfileOUT
                                        .addVariableAttribute(
                                                ncfileOUT.getNetcdfFile()
                                                        .getVariables().get(k),
                                                new Attribute(
                                                        varNewAttName[i][j],
                                                        Float.parseFloat(varNewAttValue[i][j])));
                            else if (this.varNewAttType[i][j].startsWith("i"))
                                ncfileOUT
                                        .addVariableAttribute(
                                                ncfileOUT.getNetcdfFile()
                                                        .getVariables().get(k),
                                                new Attribute(
                                                        varNewAttName[i][j],
                                                        Integer.parseInt(varNewAttValue[i][j])));
                            else
                                ncfileOUT.addVariableAttribute(ncfileOUT
                                                                       .getNetcdfFile().getVariables().get(k),
                                                               new Attribute(varNewAttName[i][j],
                                                                             varNewAttValue[i][j]));
                        }
            }

            if (latLon1D) {
                System.out.println("latlon 2D-->1D");
                // Array latArr=new ArrayFloat.D1(Math.round(180.0f/this.res));
                // Array lonArr=new ArrayFloat.D1(Math.round(360.0f/this.res));

                latD1 = new ArrayFloat.D1(h);
                lonD1 = new ArrayFloat.D1(w);

                float resH = (latMax - latMin) / (float) h;
                float resW = (lonMax - lonMin) / (float) w;

                System.out.println("\nspatial resolution: " + resW + " x "
                                           + resH);
                System.out.println("lat: [" + this.latMin + ", " + this.latMax
                                           + "]");
                System.out.println("lon: [" + this.lonMin + ", " + this.lonMax
                                           + "]\n");
                for (int i = 0; i < h; i++)
                    latD1.setFloat(i, latMax - (i * resH));

                for (int i = 0; i < w; i++)
                    lonD1.setFloat(i, lonMin + (i * resW));

                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.lonName), new Attribute("long_name",
                                                                                                  "longitude"));
                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.lonName), new Attribute(
                        "standard_name", "longitute"));
                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.lonName), new Attribute("units",
                                                                                                  "degrees_east"));
                ncfileOUT.addVariableAttribute(
                        ncfileOUT.findVariable(this.lonName),
                        new Attribute("valid_min", (float) Math.min(
                                lonD1.getFloat(0),
                                lonD1.getFloat((int) lonD1.getSize() - 1))));
                ncfileOUT.addVariableAttribute(
                        ncfileOUT.findVariable(this.lonName),
                        new Attribute("valid_max", (float) Math.max(
                                lonD1.getFloat(0),
                                lonD1.getFloat((int) lonD1.getSize() - 1))));
                ncfileOUT
                        .addVariableAttribute(
                                ncfileOUT.findVariable(this.lonName),
                                new Attribute("axis", "X"));

                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.latName), new Attribute("long_name",
                                                                                                  "latitude"));
                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.latName), new Attribute(
                        "standard_name", "latitude"));
                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.latName), new Attribute("units",
                                                                                                  "degrees_north"));
                ncfileOUT.addVariableAttribute(
                        ncfileOUT.findVariable(this.latName),
                        new Attribute("valid_min", (float) Math.min(
                                latD1.getFloat(0),
                                latD1.getFloat((int) latD1.getSize() - 1))));
                ncfileOUT.addVariableAttribute(
                        ncfileOUT.findVariable(this.latName),
                        new Attribute("valid_max", (float) Math.max(
                                latD1.getFloat(0),
                                latD1.getFloat((int) latD1.getSize() - 1))));
                ncfileOUT
                        .addVariableAttribute(
                                ncfileOUT.findVariable(this.latName),
                                new Attribute("axis", "Y"));

            } else if (latLon2D) {
                System.out.println("latlon 2D-->1D");
                // Array latArr=new ArrayFloat.D1(Math.round(180.0f/this.res));
                // Array lonArr=new ArrayFloat.D1(Math.round(360.0f/this.res));

                latD1 = new ArrayFloat.D2(w, h);
                lonD1 = new ArrayFloat.D2(w, h);

                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.lonName), new Attribute("long_name",
                                                                                                  "longitude"));
                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.lonName), new Attribute(
                        "standard_name", "longitute"));
                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.lonName), new Attribute("units",
                                                                                                  "degrees_east"));
                ncfileOUT
                        .addVariableAttribute(
                                ncfileOUT.findVariable(this.lonName),
                                new Attribute("axis", "X"));

                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.latName), new Attribute("long_name",
                                                                                                  "latitude"));
                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.latName), new Attribute(
                        "standard_name", "latitude"));
                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.latName), new Attribute("units",
                                                                                                  "degrees_north"));
                ncfileOUT
                        .addVariableAttribute(
                                ncfileOUT.findVariable(this.latName),
                                new Attribute("axis", "Y"));
            }

            if (this.addTime) {
                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.timeName), new Attribute(
                        "long_name", "time"));
                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.timeName), new Attribute(
                        "standard_name", "time"));
                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.timeName), new Attribute("units",
                                                                                                   "seconds since 1970-01-01 00:00:00"));
                ncfileOUT.addVariableAttribute(ncfileOUT
                                                       .findVariable(this.timeName), new Attribute("calendar",
                                                                                                   "standard"));
            }

            ncfileOUT.setRedefineMode(false);
            ncfileOUT.close();

            ncfileOUT = NetcdfFileWriter.openExisting(outpath);
            for (int b = 0; b < ncfileOUT.getNetcdfFile().getVariables().size(); b++) {
                String bandName = ncfileOUT.getNetcdfFile().getVariables()
                        .get(b).getFullName();
                if ((!bandName.equalsIgnoreCase(this.timeName))
                        && (!bandName.equalsIgnoreCase(this.latName))
                        && (!bandName.equalsIgnoreCase(lonName))) {

                    System.out.println("write: " + bandName);
                    DataType dt = ncfileIN.findVariable(bandName).getDataType();

                    Array array = ncfileIN.findVariable(bandName).read();
                    Array array3D = to3Darray(array, dt);

                    ncfileOUT.write(ncfileOUT.findVariable(bandName), array3D);
                }
            }

            if (this.latLon1D) {
                ncfileOUT.write(ncfileOUT.findVariable(this.latName),
                                this.latD1);
                ncfileOUT.write(ncfileOUT.findVariable(this.lonName),
                                this.lonD1);
            } else if (this.latLon2D) {
                ncfileOUT.write(ncfileOUT.findVariable(this.latName), ncfileIN
                        .findVariable(this.latName).read());
                ncfileOUT.write(ncfileOUT.findVariable(this.lonName), ncfileIN
                        .findVariable(this.lonName).read());
            }

            if (this.addTime) {
                getTimeInMillis();
                Array arrtime = new ArrayInt.D1(1);
                arrtime.setInt(0, this.timsecond);
                ncfileOUT.write(ncfileOUT.findVariable(this.timeName), arrtime);

            }


            ncfileIN.close();
            ncfileOUT.close();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * convert 2D array to 3D array: time adding
     *
     * @param array
     * @param dt
     * @return
     */
    private Array to3Darray(Array array, DataType dt) {
        Array out = null;
        int h = array.getShape()[0];
        int w = array.getShape()[1];
        if (dt.compareTo(DataType.FLOAT) == 0)
            out = new ArrayFloat.D3(1, w, h);
        else if (dt.compareTo(DataType.INT) == 0)
            out = new ArrayInt.D3(1, w, h);
        else if (dt.compareTo(DataType.BYTE) == 0)
            out = new ArrayByte.D3(1, w, h);

        for (int j = 0, k = 0; j < h; j++)
            for (int i = 0; i < w; i++, k++) {

                int p = (i * h) + j;

                if (dt.compareTo(DataType.FLOAT) == 0)
                    out.setFloat(p, array.getFloat(k));
                else if (dt.compareTo(DataType.INT) == 0)
                    out.setFloat(p, array.getFloat(k));
                else if (dt.compareTo(DataType.BYTE) == 0)
                    out.setByte(p, array.getByte(k));

            }

        return out;
    }

    /**
     * convert yearDoy to timeInMillis/1000 since 01/01/1970
     */
    private void getTimeInMillis() {
        String filename = new File(this.inpath).getName();
        System.out.println(idxDate + ", " + filename);
        String dateStr = filename.split("\\.")[this.idxDate];
        int year = Integer.parseInt(dateStr.substring(0, 4));
        int doy = -1;
        int month = -1;

        if (dateStr.length() == 7)
            doy = Integer.parseInt(dateStr.substring(4, 7));
        else
            month = Integer.parseInt(dateStr.substring(4, 6));

        Calendar cal = Calendar.getInstance();
        cal.getTimeZone();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        if (doy > 0)
            cal.set(Calendar.DAY_OF_YEAR, doy);
        else {
            cal.set(Calendar.MONTH, (month - 1));
            cal.set(Calendar.DAY_OF_MONTH, 15);
        }


        this.timsecond = (int) (cal.getTimeInMillis() / 1000);

    }


    /**
     * read variable aglobal attributes attributes
     */
    private void readParams() {
        String tab1[] = this.inTxtBand.split(";");
        this.varNewAttName = new String[tab1.length][];
        this.varNewAttValue = new String[tab1.length][];
        this.varNewAttType = new String[tab1.length][];

        this.varName = new String[tab1.length];
        for (int i = 0; i < tab1.length; i++) {
            String tab2[] = tab1[i].split(":")[1].split("\\+\\+");
            this.varNewAttName[i] = new String[tab2.length];
            this.varNewAttValue[i] = new String[tab2.length];
            this.varNewAttType[i] = new String[tab2.length];

            this.varName[i] = tab1[i].split(":")[0];
            for (int j = 0; j < tab2.length; j++) {
                // System.out.println("tab2:: "+j+">>"+tab2[j]);
                this.varNewAttName[i][j] = tab2[j].split("=")[0];
                this.varNewAttValue[i][j] = tab2[j].split("=")[1];
                this.varNewAttType[i][j] = tab2[j].split("=")[2];
            }
        }

        String tab3[] = this.inTxtGlob.split("\\+\\+");
        this.globNewAttrName = new String[tab3.length];
        this.globNewAttrVal = new String[tab3.length];

        for (int i = 0; i < tab3.length; i++) {
            this.globNewAttrName[i] = tab3[i].split(":")[0];
            this.globNewAttrVal[i] = tab3[i].split(":")[1];
        }

        for (int i = 0; i < this.globNewAttrName.length; i++)
            System.out.println("Glob Attruibutes: " + this.globNewAttrName[i]
                                       + " >> " + this.globNewAttrVal[i]);

        for (int i = 0; i < this.varName.length; i++) {
            System.out.println(this.varName[i] + ":");
            for (int j = 0; j < this.varNewAttName[i].length; j++)
                System.out.println("\t" + this.varNewAttName[i][j] + " = "
                                           + this.varNewAttValue[i][j] + ", type:"
                                           + this.varNewAttType[i][j]);
        }

    }

    public static void main(String[] args) {

		/*
          String inpath="/unsafe/said/tmp/testCodes/albedo/GlobAlbedo.merge.albedo.005.2005217.nc ";
		  String outpath="/unsafe/said/tmp/testCodes/albedo/standard/";

		  String txtBand="DHR_VIS:long_name=Black Sky Albedo in VIS=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string++validrange=0,1=string;"
		  +
		  "DHR_NIR:long_name=Black Sky Albedo in NIR=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string++validrange=0,1=string;"
		  +
		  "DHR_SW:long_name=Black Sky Albedo in SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string++validrange=0,1=string;"
		  +
		  "BHR_VIS:long_name=White Sky Albedo in VIS=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string++validrange=0,1=string;"
		  +
		  "BHR_NIR:long_name=White sky albedo in NIR=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string++validrange=0,1=string;"
		  +
		  "BHR_SW:long_name=White Sky Albedo in SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string++validrange=0,1=string;"
		  +

		  "DHR_sigmaVIS:long_name=Uncertainity of Black Sky Albedo in VIS=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +
		  "DHR_sigmaNIR:long_name=Uncertainity of Black Sky Albedo in NIR=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +
		  "DHR_sigmaSW:long_name=Uncertainity of Black Sky Albedo in SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +

		  "BHR_sigmaVIS:long_name=Uncertainity of White Sky Albedo in VIS=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +
		  "BHR_sigmaNIR:long_name=Uncertainity of White Sky Albedo in NIR=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +
		  "BHR_sigmaSW:long_name=Uncertainity of White Sky Albedo in SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +

		  "DHR_alpha_VIS_NIR:long_name=Covariance between DHR_VIS and DHR_NIR=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +
		  "DHR_alpha_VIS_SW:long_name=Covariance between DHR_VIS and DHR_SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +
		  "DHR_alpha_NIR_SW:long_name=Covariance between DHR_NIR and DHR_SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +

		  "BHR_alpha_VIS_NIR:long_name=Covariance between BHR_VIS and BHR_NIR=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +
		  "BHR_alpha_VIS_SW:long_name=Covariance between BHR_VIS and BHR_SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +
		  "BHR_alpha_NIR_SW:long_name=Covariance between BHR_NIR and BHR_SW=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +

		  "Relative_Entropy:long_name=Relative Entropy=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +
		  "Goodness_of_Fit:long_name=Goodness_of_Fit=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +
		  "Snow_Fraction:long_name=Snow Fraction=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +
		  "Data_Mask:long_name=Data Mask=string++_FillValue=0=byte++coordinates=lat lon=string++units=-=string;"
		  +
		  "Weighted_Number_of_Samples:long_name=Weighted number of BRDF samples=string++_FillValue=NaN=float++coordinates=lat lon=string++units=-=string;"
		  +
		  "Time_to_the_Closest_Sample:long_name=Number of days to the closest sample=string++_FillValue=NaN=float++coordinates=lat lon=string++units=days=string;"
		  +
		  "Solar_Zenith_Angle:long_name=Solar Zenith Angle=string++_FillValue=NaN=float++coordinates=lat lon=string++units=degrees=string"
		  ;

		  String txtDim="y:lat,x:lon";
		  String txtGlobAttr= "Conventions:CF-1.4++history:GlobAlbedo Processing, 2013-2014++title:GlobAlbedo Albedo Merge Product++institution:Mullard Space Science Laboratory, Department of Space and Climate Physics, University College London++source:Satellite observations, BRDF/Albedo Inversion Model++references:GlobAlbedo ATBD V3.1++comment:none"
		  ;

		  String latlon1D="true";
		  String latlon2D="false";

		  String latName="lat";
		  String lonName="lon";

		  String addTime="true";
		  String timeName="time";
		  int idxDate=4;

		  float latMin = -90;
		  float latMax = 90;
		  float lonMin = -180;
		  float lonMax = 180;

		  int ncVersion=4;

		 */
        for (int i = 0; i < args.length; i++)
            args[i] = args[i].replace("'", "");

        String inpath = args[0];
        String outpath = args[1];

        String txtBand = args[2];
        String txtDim = args[3];
        String txtGlobAttr = args[4];

        String latlon1D = args[5];
        String latlon2D = args[6];

        String latName = args[7];
        String lonName = args[8];

        String addTime = args[9];
        String timeName = args[10];
        int idxDate = Integer.parseInt(args[11]);
        float latMin = Float.parseFloat(args[12]);
        float latMax = Float.parseFloat(args[13]);
        float lonMin = Float.parseFloat(args[14]);
        float lonMax = Float.parseFloat(args[15]);

        int ncVersion = Integer.parseInt(args[16]);


        AddAttributes add = new AddAttributes();
        add.inpath = inpath;
        add.outpath = outpath;
        add.inTxtGlob = txtGlobAttr;
        add.inTxtBand = txtBand;
        add.inTxtDim = txtDim;
        add.latLon1D = Boolean.parseBoolean(latlon1D);
        add.latLon2D = Boolean.parseBoolean(latlon2D);
        add.addTime = Boolean.parseBoolean(addTime);
        add.idxDate = idxDate;
        add.timeName = timeName;
        add.latName = latName;
        add.lonName = lonName;
        add.latMin = latMin;
        add.latMax = latMax;
        add.lonMin = lonMin;
        add.lonMax = lonMax;
        add.ncVersion = ncVersion;

        add.compute();

    }

}

package org.esa.beam.globalbedo.staging;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * This program is about create monthly products from 8daily products.
 * For a given year and month, weights will be associated to the daily products and then combined linearly.
 *
 * @author Said Kharbouche MSSL.UCL (2014)
 */
public class Daily2Monthly {
    //##################################################################################################################
    /**
     * target year
     */
    private int year = -1;
    /**
     * output directory
     */
    private String outdir;
    /**
     * text file that contains the list if daily input files for the traget year
     */
    private String infilesTxt;
    /**
     * the position of date in file name (splitted by ".")
     */
    private int idxDate = -1;
    //##################################################################################################################


    private String infiles[];


    /**
     * launch the process of monthly product creation
     */
    private void compute() {
        try {
            this.infiles = this.readlines(this.infilesTxt);

            String filename0 = infiles[0].split("/")[infiles[0].split("/").length - 1];
            String date = filename0.split("\\.")[this.idxDate];
            NetcdfFile ncfile0 = NetcdfFile.open(infiles[0]);
            List<Variable> vars = ncfile0.getVariables();


            float[][] freq = computeFreq(this.year);
            // System.exit(0);
            for (int m = 0; m < 12; m++) {
                String strM = "" + (m + 1);
                if ((m + 1) < 10)
                    strM = "0" + strM;
                System.out.println("\n\n >>>> month:" + strM);

                String filename = filename0;
                filename = filename0.replace("." + date + ".", "." + year + strM + ".");

                String outfile = outdir + "/" + filename;


                NetcdfFileWriter grid = null;


                for (int b = 0; b < vars.size(); b++) {
                    String band = vars.get(b).getFullName();
                    System.out.println("BAND: " + band);
                    // System.out.println("Open file: "+outfile0);

                    Array avr = vars.get(b).read();
                    Array avr1 = avr.copy();

                    // if(grid.getNetcdfFile().getVariables().get(b).getDataType().equals(DataType.FLOAT));
                    for (int i = 0; i < avr.getSize(); i++)
                        avr.setDouble(i, 0.0);

                    int doy = 1;
                    for (int d = 0; d < 46; d++) {

                        if (freq[m][d] > 0) {
                            // System.out.println(0);
                            doy = 1 + (8 * d);
                            String strD = "" + doy;
                            if (doy < 10)
                                strD = "00" + strD;
                            else if (doy < 100)
                                strD = "0" + strD;
                            // System.out.println("strD:"+"."+year+strD+".");
                            for (int f = 0; f < infiles.length; f++) {
                                // System.out.println("in files: "+infiles[f].getName());
                                String filename1 = infiles[f].split("/")[infiles[f]
                                        .split("/").length - 1];
                                String date1 = filename1.split("\\.")[this.idxDate];

                                if (date1.equalsIgnoreCase(year + strD)) {
                                    if (grid == null) {
                                        Process p = Runtime.getRuntime().exec("cp -f " + infiles[0] + " " + outfile);
                                        p.waitFor();
                                        InputStream is = p.getErrorStream();
                                        int c;
                                        StringBuilder commandResponse = new StringBuilder();

                                        while ((c = is.read()) != -1) { // read until end of stream
                                            commandResponse.append((char) c);
                                        }
                                        System.out.println(commandResponse); // Print command response
                                        is.close();

                                        grid = NetcdfFileWriter.openExisting(outfile);


                                    }
                                    // System.out.println("open:"+infiles[f].getName());
                                    NetcdfFile infile = NetcdfFile
                                            .open(infiles[f]);

                                    System.out.println("in files: "
                                                               + infiles[f] + ", weight: "
                                                               + freq[m][d]);

                                    Array arr = infile.findVariable(band)
                                            .read();
                                    if (band.toLowerCase().startsWith("lat")
                                            || band.toLowerCase().startsWith(
                                            "lon"))
                                        for (int i = 0; i < avr.getSize(); i++)
                                            avr.setDouble(i, arr.getDouble(i));
                                    else
                                        for (int i = 0; i < avr.getSize(); i++) {
                                            double val = avr.getDouble(i)
                                                    + (freq[m][d] * arr
                                                    .getDouble(i));
                                            if (grid.getNetcdfFile()
                                                    .getVariables().get(b)
                                                    .getDataType()
                                                    .equals(DataType.BYTE))
                                                if (val > 0.0 && val < 1.0)
                                                    val = 1.0;
                                            avr.setDouble(i, val);

                                        }
                                    if (band.equalsIgnoreCase("time"))
                                        System.out.println("time: "
                                                                   + arr.getDouble(0));

                                    infile.close();

                                }
                            }

                        }
                    }
                    if (grid != null) {
                        System.out.println("writing" + band + "...");

                        if (grid.getNetcdfFile().getVariables().get(b)
                                .getDataType().equals(DataType.FLOAT)) {
                            for (int i = 0; i < avr.getSize(); i++)
                                avr1.setFloat(i, (float) avr.getDouble(i));
                            System.out.println("type: "
                                                       + grid.getNetcdfFile().getVariables()
                                    .get(b).getDataType());
                        }

                        if (grid.getNetcdfFile().getVariables().get(b)
                                .getDataType().equals(DataType.BYTE)) {
                            for (int i = 0; i < avr.getSize(); i++)
                                avr1.setByte(i,
                                             (byte) ((int) avr.getDouble(i) + 0.1));
                            System.out.println("type: "
                                                       + grid.getNetcdfFile().getVariables()
                                    .get(b).getDataType());
                        }

                        if (grid.getNetcdfFile().getVariables().get(b)
                                .getDataType().equals(DataType.SHORT)) {
                            for (int i = 0; i < avr.getSize(); i++)
                                avr.setShort(i, (short) avr.getDouble(i));
                            System.out.println("type: "
                                                       + grid.getNetcdfFile().getVariables()
                                    .get(b).getDataType());
                        }

                        if (grid.getNetcdfFile().getVariables().get(b)
                                .getDataType().equals(DataType.INT)) {
                            for (int i = 0; i < avr.getSize(); i++)
                                avr.setInt(i, (int) avr.getDouble(i));
                            System.out.println("type: "
                                                       + grid.getNetcdfFile().getVariables()
                                    .get(b).getDataType());
                        }

                        if (grid.getNetcdfFile().getVariables().get(b)
                                .getDataType().equals(DataType.LONG)) {
                            for (int i = 0; i < avr.getSize(); i++)
                                avr.setLong(i, (long) avr.getDouble(i));
                            System.out.println("type: "
                                                       + grid.getNetcdfFile().getVariables()
                                    .get(b).getDataType());
                        }

                        grid.write(grid.findVariable(band), avr1);
                        if (band.equalsIgnoreCase("time"))
                            System.out.println("month time: " + avr.getLong(0));
                    }

                }

                if (grid != null)
                    grid.close();

            }
            ncfile0.close();


        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * compute the weights of DoYs for each month
     *
     * @param year target year
     * @return wights[12][46]
     */
    private float[][] computeFreq(int year) {
        float out[][] = new float[12][46];

        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, year);
        //SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy");

        for (int m = 0; m < 12; m++) {
            int doy = 0;
            float sum = 0f;
            for (int d = 1; d < 365; d += 8) {
                cal.set(Calendar.DAY_OF_YEAR, d);

                Calendar cal0 = (Calendar) cal.clone();
                cal0.add(Calendar.DAY_OF_YEAR, -8);

                Calendar cal1 = (Calendar) cal.clone();
                cal1.add(Calendar.DAY_OF_YEAR, +7);

                int n = 0;
                while (cal0.getTimeInMillis() <= cal1.getTimeInMillis()) {
                    if (cal0.get(Calendar.MONTH) == m
                            && cal0.get(Calendar.YEAR) == cal
                            .get(Calendar.YEAR))
                        n++;
                    cal0.add(Calendar.DAY_OF_YEAR, 1);

                }
                out[m][doy] = n;
                sum += n;

                doy++;
            }

            for (int d = 0; d < out[m].length; d++) {
                out[m][d] /= sum;

            }
        }

        return out;
    }

    /**
     * read absolute paths of 8-daily products
     *
     * @param path text file path
     * @return list of input products
     */
    private String[] readlines(String path) {
        System.out.println("start reading files from path " + path + "  ...");
        String out[] = new String[0];
        BufferedReader br = null;
        ArrayList<String> list = new ArrayList<String>();
        try {
            br = new BufferedReader(new FileReader(path));
            String line = br.readLine();

            while (line != null) {

                if (!line.replaceAll(" ", "").equalsIgnoreCase("")) {
                    list.add(line);
                    line = br.readLine();
                }
            }
            br.close();
            out = new String[list.size()];
            for (int i = 0; i < out.length; i++)
                out[i] = list.get(i);

        } catch (Exception e) {
            System.out.println("error in " + path);
        }
        return out;
    }

    public static void main(String[] args) {
        /*
		int year = 2005;
		String infilesTxt =  "/unsafe/said/tmp/testCodes/params/list05.txt";
		String outdir = "/unsafe/said/tmp/testCodes/albedo/monthly";
		int idxDate = 4;
		*/


        int year = Integer.parseInt(args[0]);
        String infilesTxt = args[1];
        String outdir = args[2];
        int idxDate = Integer.parseInt(args[3]);


        Daily2Monthly avrm = new Daily2Monthly();

        avrm.year = year;
        avrm.infilesTxt = infilesTxt;
        avrm.outdir = outdir;
        avrm.idxDate = idxDate;

        avrm.compute();
    }

}

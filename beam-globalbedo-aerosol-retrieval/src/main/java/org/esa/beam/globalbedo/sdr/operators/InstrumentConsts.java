/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;

/**
 * Instrument specific constants
 * @author akheckel
 *
 * TODO: revise validPixel Expression: properly include range of LUTs
 * TODO: revise validPixel Expression: enable separate treatment of snow pixels
 */
public class InstrumentConsts {

    private static InstrumentConsts instance;

    private final String[] supportedInstruments;

/****************************************
 * MERIS
 ****************************************/

    private final String idepixFlagBandName = "cloud_classif_flags";
    private final String idepixFwardFlagBandName = "cloud_classif_flags_fward";
    private final String elevationBandName = "elevation";
    private final String[] merisReflectanceNames = {
        "reflectance_1",
        "reflectance_2",
        "reflectance_3",
        "reflectance_4",
        "reflectance_5",
        "reflectance_6",
        "reflectance_7",
        "reflectance_8",
        "reflectance_9",
        "reflectance_10",
        "reflectance_11",
        "reflectance_12",
        "reflectance_13",
        "reflectance_14",
        "reflectance_15"
    };
    private final String[] merisGeomNames = {
        EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME,
        EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME,
        EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME,
        EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME
    };
//    private final float[] merisFitWeights = {1.0f, 1.0f, 1.0f, 1.0f, 0.2f, 1.0f, 1.0f, 1.0f,
//                                             0.05f, 0.05f, 0.05f, 0.05f, 0.05f};
    private final double[] merisFitWeights = {1.0, 1.0, 1.0, 1.0, 0.2, 1.0, 1.0, 1.0,
                                             0.5, 0.5, 0.0, 0.5, 0.5, 0.5, 0.0};
    private final String merisValidExpr = "(!l1_flags.INVALID && "+idepixFlagBandName+".F_CLEAR_LAND)";
    private final int merisNLutBands = 15;
    private final String merisSurfPressureName = "surfPressEstimate";
    private final String merisOzoneName = "ozone";
    private final String merisNdviExp = "(reflectance_13 - reflectance_7) / (reflectance_13 + reflectance_7)";


/****************************************
 * VGT
 ****************************************/

    private final String[] vgtReflectanceNames = {"B0", "B2", "B3", "MIR"};
    private final String[] vgtGeomNames = {"SZA", "SAA", "VZA", "VAA"};
    private final double[] vgtFitWeights = {1.0, 1.0, 0.5, 0.1};
    //private final String  vgtCloudExpr = "!(SM.CLOUD_1 ||SM.CLOUD_2 || SM.ICE_SNOW)";
    private final String  vgtCloudExpr = "!("+idepixFlagBandName+".F_CLOUD)";
    private final String  vgtValidExpr = "(SM.B0_GOOD && SM.B2_GOOD && SM.B3_GOOD && SM.LAND && "+idepixFlagBandName+".F_CLEAR_LAND && (SZA<70))";
    //private final String  vgtValidExpr = "(SM.B0_GOOD && SM.B2_GOOD && SM.B3_GOOD && SM.MIR_GOOD && SM.LAND && cloud_classif_flags.F_LAND && " + vgtCloudExpr + ")";
    //private final String  vgtValidExpr = "(SM.B0_GOOD && SM.B2_GOOD && SM.B3_GOOD && SM.MIR_GOOD && SM.LAND && " + vgtCloudExpr + ")";
    private final int vgtNLutBands = 4;
    private final String vgtSurfPressureName = "surfPressEstimate";
    private final String vgtOzoneName = "OG";
    private final String vgtNdviExp = "(B3-B2)/(B3+B2)";


/****************************************
 * AATSR
 ****************************************/

    private final String[] aatsrReflectanceNames = {
        "reflec_nadir_0550",
        "reflec_nadir_0670",
        "reflec_nadir_0870",
        "reflec_nadir_1600",
        "reflec_fward_0550",
        "reflec_fward_0670",
        "reflec_fward_0870",
        "reflec_fward_1600",
    };
    private final String[] aatsrGeomNames = {
        EnvisatConstants.AATSR_SUN_ELEV_NADIR_DS_NAME,
        EnvisatConstants.AATSR_SUN_AZIMUTH_NADIR_DS_NAME,
        EnvisatConstants.AATSR_VIEW_ELEV_NADIR_DS_NAME,
        EnvisatConstants.AATSR_VIEW_AZIMUTH_NADIR_DS_NAME,
        EnvisatConstants.AATSR_SUN_ELEV_FWARD_DS_NAME,
        EnvisatConstants.AATSR_SUN_AZIMUTH_FWARD_DS_NAME,
        EnvisatConstants.AATSR_VIEW_ELEV_FWARD_DS_NAME,
        EnvisatConstants.AATSR_VIEW_AZIMUTH_FWARD_DS_NAME
    };
    private final double[] aatsrFitWeights = {1.5, 1.0, 1.0, 1.55};
    private final String  aatsrCloudExpr = "!(cloud_flags_nadir.CLOUDY) && !(cloud_flags_fward.CLOUDY)";
    private final String  aatsrAotValExpr = idepixFlagBandName+".F_CLEAR_LAND || "+idepixFwardFlagBandName+".F_CLEAR_LAND";
    private final String  aatsrValidExpr = "("+idepixFlagBandName+".F_CLEAR_LAND && "+idepixFwardFlagBandName+".F_CLEAR_LAND && "
        + " (90-sun_elev_nadir) < 70 &&"
        + " (90-sun_elev_fward) < 70 &&"
        + " reflec_nadir_0550 >= 0 &&"
        + " reflec_nadir_0670 >= 0 &&"
        + " reflec_nadir_0870 >= 0 &&"
        + " reflec_nadir_1600 >= 0 &&"
        + " reflec_fward_0550 >= 0 &&"
        + " reflec_fward_0670 >= 0 &&"
        + " reflec_fward_0870 >= 0 &&"
        + " reflec_fward_1600 >= 0 )";
        //+ aatsrCloudExpr + ")";
    private final int     aatsrNLutBands = 4;
    private final String  aatsrSurfPressureName = "surfPressEstimate";
    private final String  aatsrOzoneName = "ozoneConst";
    private final String  aatsrNdviExp = "(reflec_nadir_0870 - reflec_nadir_0670) / (reflec_nadir_0870 + reflec_nadir_0670)";

    private final Map<String, String[]> reflecNames;
    private final Map<String, String[]> geomNames;
    private final Map<String, double[]> fitWeights;
    private final Map<String, String> validExpr;
    private final Map<String, Integer> nLutBands;
    private final Map<String, String> surfPressureName;
    private final Map<String, String> ozoneName;
    private final Map<String, String> ndviExpression;

    private final String lutLocaFile = System.getProperty("user.home")
                    + File.separator + ".beam"
                    + File.separator + "ga-aerosol"
                    + File.separator + "lut.location";
    private final String lutPattern = "%INSTRUMENT%/%INSTRUMENT%_LUT_MOMO_ContinentalI_80_SDR_noG_v2.bin";
    private final String lutPath;
    private final String ndviName;

    private InstrumentConsts() {
        lutPath = getLutPath() + "/" + lutPattern;

        this.supportedInstruments = new String[]{"MERIS", "VGT", "AATSR"};

        this.reflecNames = new HashMap<String, String[]>(supportedInstruments.length);
        reflecNames.put(supportedInstruments[0], merisReflectanceNames);
        reflecNames.put(supportedInstruments[1], vgtReflectanceNames);
        reflecNames.put(supportedInstruments[2], aatsrReflectanceNames);

        this.geomNames = new HashMap<String, String[]>(supportedInstruments.length);
        geomNames.put(supportedInstruments[0], merisGeomNames);
        geomNames.put(supportedInstruments[1], vgtGeomNames);
        geomNames.put(supportedInstruments[2], aatsrGeomNames);

        this.fitWeights = new HashMap<String, double[]>(supportedInstruments.length);
        fitWeights.put(supportedInstruments[0], merisFitWeights);
        fitWeights.put(supportedInstruments[1], vgtFitWeights);
        fitWeights.put(supportedInstruments[2], aatsrFitWeights);

        this.validExpr = new HashMap<String, String>(supportedInstruments.length);
        validExpr.put(supportedInstruments[0], merisValidExpr);
        validExpr.put(supportedInstruments[1], vgtValidExpr);
        validExpr.put(supportedInstruments[2], aatsrValidExpr);

        this.nLutBands = new HashMap<String, Integer>(supportedInstruments.length);
        nLutBands.put(supportedInstruments[0], merisNLutBands);
        nLutBands.put(supportedInstruments[1], vgtNLutBands);
        nLutBands.put(supportedInstruments[2], aatsrNLutBands);

        this.surfPressureName = new HashMap<String, String>(supportedInstruments.length);
        surfPressureName.put(supportedInstruments[0], merisSurfPressureName);
        surfPressureName.put(supportedInstruments[1], vgtSurfPressureName);
        surfPressureName.put(supportedInstruments[2], aatsrSurfPressureName);

        this.ozoneName = new HashMap<String, String>(supportedInstruments.length);
        ozoneName.put(supportedInstruments[0], merisOzoneName);
        ozoneName.put(supportedInstruments[1], vgtOzoneName);
        ozoneName.put(supportedInstruments[2], aatsrOzoneName);

        this.ndviName = "toaNdvi";
        this.ndviExpression = new HashMap<String, String>(supportedInstruments.length);
        ndviExpression.put(supportedInstruments[0], merisNdviExp);
        ndviExpression.put(supportedInstruments[1], vgtNdviExp);
        ndviExpression.put(supportedInstruments[2], aatsrNdviExp);
    }



    public static InstrumentConsts getInstance() {
        if (instance == null) {
            instance = new InstrumentConsts();
        }
        return instance;
    }

    public String getInstrument(Product p) {
        String inst = null;
        //String[] supportedInstruments = InstrumentConsts.getInstance().getSupportedInstruments();
        for (String suppInstr : supportedInstruments) {
            String[] specBands = getSpecBandNames(suppInstr);
            if (specBands.length > 0 && p.containsBand(specBands[0])) {
                inst = suppInstr;
                break;
            }
        }
        if (inst.equals(null)) throw new OperatorException("Product not supported.");
        return inst;
    }

    public double[] getSpectralFitWeights(String instrument) {
        //return fitWeights.get(instrument);
        return normalize(fitWeights.get(instrument));
    }

    public String[] getGeomBandNames(String instrument) {
        return geomNames.get(instrument);
    }

    public String getLutName(String instrument) {

        return lutPath.replace("%INSTRUMENT%", instrument);
    }

    public String[] getSpecBandNames(String instrument) {
        return reflecNames.get(instrument);
    }

    public String[] getSupportedInstruments() {
        return supportedInstruments;
    }

    public String getValidExpression(String instrument) {
        return validExpr.get(instrument);
    }

    public String getAotExpression(String instrument) {
        return (instrument.equals("AATSR"))? aatsrAotValExpr : getValidExpression(instrument);
    }

    public int getnLutBands(String instrument) {
        return nLutBands.get(instrument);
    }

    public String getSurfPressureName(String instrument) {
        return surfPressureName.get(instrument);
    }

    public String getOzoneName(String instrument) {
        return ozoneName.get(instrument);
    }

    public boolean isReflectanceBand(Band band) {
        Product p = band.getProduct();
        String instrument = getInstrument(p);
        boolean isReflecBand = false;
        for (String specBandName : getSpecBandNames(instrument)){
            if (band.getName().equals(specBandName)){
                isReflecBand = true;
                break;
            }
        }
        return isReflecBand;
    }

    public boolean isToaBand(Band band) {
        Product p = band.getProduct();
        String instrument = getInstrument(p);

        boolean isToaBand = false;
        for (String specBandName : getSpecBandNames(instrument)){
            if (band.getName().equals(specBandName)){
                isToaBand = true;
                return isToaBand;
            }
        }
        if (instrument.equals("VGT")){
            for (String specBandName : getGeomBandNames(instrument)){
                if (band.getName().equals(specBandName)){
                    isToaBand = true;
                    return isToaBand;
                }
            }
            if (band.getName().equals("WVG") || band.getName().equals("OG")){
                return true;
            }
        }

        return isToaBand;
    }

    public boolean isElevationBand(Band band){
        return band.getName().equals("elevation");
    }

    public String getNdviName() {
        return ndviName;
    }

    public String getNdviExpression(String instrument){
        return this.ndviExpression.get(instrument);
    }

    public String getIdepixFlagBandName() {
        return idepixFlagBandName;
    }

    public String getIdepixFwardFlagBandName() {
        return idepixFwardFlagBandName;
    }

    public String getElevationBandName() {
        return elevationBandName;
    }

    public String getLutPath() {
        String lutP = null;
        BufferedReader reader = null;
        try{
            reader = new BufferedReader(new FileReader(lutLocaFile));
            String line;
            while ((line = reader.readLine())!= null) {
                if (line.startsWith("ga.lutInstallDir")) {
                    String[] split = line.split("=");
                    if(split.length > 1) {
                        lutP = split[1].trim();
                        break;
                    }
                }
            }
        } catch (IOException ex) {
            throw new OperatorException(ex);
        }
        if (lutP == null) throw new OperatorException("Lut install dir  not found");
        if (lutP.endsWith(File.separator) || lutP.endsWith("/")) lutP = lutP.substring(0, lutP.length()-1);
        return lutP;
    }

    private double[] normalize(double[] fa) {
        double sum = 0;
        for (double f : fa) {
            sum += f;
        }
        for (int i = 0; i < fa.length; i++) {
            fa[i] /= sum;
        }
        return fa;
    }

    public boolean isVgtAuxBand(Band b) {
        String bname = b.getName();
        for (String geomName : getGeomBandNames("VGT")){
            if (bname.equals(geomName)) return true;
        }
        if (bname.equals(getOzoneName("VGT"))) return true;
        if (bname.equals("WVG")) return true;

        return false;
    }

}

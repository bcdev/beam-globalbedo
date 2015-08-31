package org.esa.beam.globalbedo.inversion.util;

import org.esa.beam.framework.datamodel.*;

import java.util.HashMap;
import java.util.Map;

/**
 * todo: class is not yet used, clarify what we finally need as BRDF and albedo metadata in QA4ECV
 *
 * @author olafd
 */
public class MetadataUtils {

    public static void addBrdfMetadata(Product inversionProduct) {
        final MetadataElement globalElement = new MetadataElement("BRDF_Global_Attributes");

        final ProductData historyAttrData = ProductData.createInstance("QA4ECV Processing, 2014-2017");
        final MetadataAttribute historyAttr = new MetadataAttribute("history", historyAttrData, true);
        globalElement.addAttribute(historyAttr);

        final ProductData conventionsAttrData = ProductData.createInstance("CF-1.4");
        final MetadataAttribute conventionsAttr = new MetadataAttribute("conventions", conventionsAttrData, true);
        globalElement.addAttribute(conventionsAttr);

        final ProductData titleAttrData = ProductData.createInstance("QA4ECV BRDF Product");
        final MetadataAttribute titleAttr = new MetadataAttribute("title", titleAttrData, true);
        globalElement.addAttribute(titleAttr);

        final String institutionString =
                "Mullard Space Science Laboratory, Department of Space and Climate Physics, University College London";
        final ProductData institutionAttrData = ProductData.createInstance(institutionString);
        final MetadataAttribute institutionAttr = new MetadataAttribute("institution", institutionAttrData, true);
        globalElement.addAttribute(institutionAttr);

        final ProductData sourceAttrData = ProductData.createInstance("Satellite observations, BRDF/Albedo Inversion Model");
        final MetadataAttribute sourceAttr = new MetadataAttribute("source", sourceAttrData, true);
        globalElement.addAttribute(sourceAttr);

        final ProductData referencesAttrData = ProductData.createInstance("GlobAlbedo ATBD V3.1");
        final MetadataAttribute referencesAttr = new MetadataAttribute("references", referencesAttrData, true);
        globalElement.addAttribute(referencesAttr);

        final ProductData commentAttrData = ProductData.createInstance("none");
        final MetadataAttribute commentAttr = new MetadataAttribute("comment", commentAttrData, true);
        globalElement.addAttribute(commentAttr);

        addVariableAttributes(inversionProduct);
        inversionProduct.getMetadataRoot().addElement(globalElement);
    }

    static final Map<String, String> waveBandsTextsMap = new HashMap<>();

    static {
        waveBandsTextsMap.put("VIS", "visible");
        waveBandsTextsMap.put("NIR", "near infrared");
        waveBandsTextsMap.put("SW", "shortwave");
    }

    private static void addVariableAttributes(Product inversionProduct) {
        final MetadataElement variablesRootElement = new MetadataElement("BRDF_Variable_Attributes");

        for (MetadataElement metadataElement : variablesRootElement.getElements()) {
            if (metadataElement.getName().equals("metadata")) {
                variablesRootElement.removeElement(metadataElement);
            }
        }

        String delims = "[_]";
        for (Band b : inversionProduct.getBands()) {
            final MetadataElement variableElement = new MetadataElement(b.getName());
            String longNameString = "";
            if (b.getName().startsWith("mean_")) {
                String[] tokens = b.getName().split(delims);
                if (tokens.length == 3) {
                    longNameString =
                            "BRDF model parameter " + tokens[2] + " - " + waveBandsTextsMap.get(tokens[1]) + " band";
                }
            } else if (b.getName().startsWith("VAR_")) {
                String[] tokens = b.getName().split(delims);
                if (tokens.length == 5) {
                    longNameString =
                            "Covariance of BRDF model parameters " +
                                    tokens[1] + "-" + tokens[2] + "/" + tokens[1] + "-" + tokens[2];
                }
            } else if (b.getName().equals("Entropy")) {
                longNameString = "Entropy";
            } else if (b.getName().equals("Relative_Entropy")) {
                longNameString = "Relative entropy";
            } else if (b.getName().equals("Weighted_Number_of_Samples")) {
                longNameString = "Weighted number of BRDF samples";
            } else if (b.getName().equals("Goodness_of_Fit")) {
                longNameString = "Goodness of fit";
            } else if (b.getName().equals("Proportion_NSamples")) {
                longNameString = "Proportion of numbers of snow/noSnow samples";
            } else if (b.getName().equals("Time_to_the_Closest_Sample")) {
                longNameString = "Time to the closest sample";
                final String unitString = "day";
                addMDElement(variableElement, "units", unitString);
            }

            addMDElements(b, longNameString, variableElement);
            variablesRootElement.addElement(variableElement);
        }

        // todo: check if we need metadata entry for 'crs' (appears as scalar-variable in ncdump)
//        longNameString = "Coordinate Reference System";
//        final String commentString =
//                "A coordinate reference system (CRS) defines defines how the georeferenced spatial data " +
//                        "relates to real locations on the Earth's surface";
//        addMDElement(variableElement, "comment", commentString);


        inversionProduct.getMetadataRoot().addElement(variablesRootElement);
    }

    private static void addMDElement(MetadataElement variableElement, String key, String value) {
        final ProductData attrData = ProductData.createInstance(value);
        final MetadataAttribute attr = new MetadataAttribute(key, attrData, true);
        variableElement.addAttribute(attr);
    }

    private static void addMDElement(MetadataElement variableElement, String key, float value) {
        final ProductData attrData = ProductData.createInstance(ProductData.TYPE_FLOAT32);
        attrData.setElemFloat(value);
        final MetadataAttribute attr = new MetadataAttribute(key, attrData, true);
        variableElement.addAttribute(attr);
    }

    private static void addMDElements(Band b, String longNameString, MetadataElement variableElement) {
        // long_name
        addMDElement(variableElement, "long_name", longNameString);
        // standard_name
        addMDElement(variableElement, "standard_name", b.getName());
        // fill_value
        addMDElement(variableElement, "fill_value", (float) b.getNoDataValue());
        // scale_factor
        addMDElement(variableElement, "scale_factor", (float) b.getScalingFactor());
        // add_offset
        addMDElement(variableElement, "add_offset", (float) b.getScalingOffset());
        // units
        if (b.getUnit() != null) {
            addMDElement(variableElement, "units", b.getUnit());
        }
    }

}

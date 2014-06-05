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

package org.esa.beam.dataio.netcdf.util;

import ucar.ma2.DataType;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents an extract of all variables that could be converted to bands.
 */
public class Modis35RasterDigest {

    private final Modis35DimKey rasterDim;
    private final Variable[] variables;
    private Modis35ScaledVariable[] scaledVariables;


    public Modis35RasterDigest(Modis35DimKey rasterDim, Variable[] variables) {
        this(rasterDim, variables, new Modis35ScaledVariable[0]);
    }

    public Modis35RasterDigest(Modis35DimKey rasterDim, Variable[] variables, Modis35ScaledVariable[] scaledVariables) {
        this.rasterDim = rasterDim;
        this.variables = variables;
        this.scaledVariables = scaledVariables;
    }

    public Modis35DimKey getRasterDim() {
        return rasterDim;
    }

    public Variable[] getRasterVariables() {
        return variables;
    }

    public Modis35ScaledVariable[] getScaledVariables() {
        return scaledVariables;
    }

    public static Modis35RasterDigest createRasterDigest(final Group... groups) {
        Map<Modis35DimKey, List<Variable>> variableListMap = new HashMap<Modis35DimKey, List<Variable>>();
        for (Group group : groups) {
            collectVariableLists(group, variableListMap);
        }
        if (variableListMap.isEmpty()) {
            return null;
        }
        if (System.getProperty("raster.digest") != null && System.getProperty("raster.digest").equals("EXTENDED")) {
            final Modis35DimKey rasterDim = getBestRasterDimExtended(variableListMap);
            final Variable[] rasterVariables = getRasterVariablesExtended(variableListMap, rasterDim);
            final Modis35ScaledVariable[] scaledVariables = getScaledVariablesExtended(variableListMap, rasterDim);
            return new Modis35RasterDigest(rasterDim, rasterVariables, scaledVariables);
        } else {
            final Modis35DimKey rasterDim = getBestRasterDim(variableListMap);
            final Variable[] rasterVariables = getRasterVariables(variableListMap, rasterDim);
            final Modis35ScaledVariable[] scaledVariables = getScaledVariables(variableListMap, rasterDim);
            return new Modis35RasterDigest(rasterDim, rasterVariables, scaledVariables);
        }
    }

    private static Modis35ScaledVariable[] getScaledVariablesExtended(Map<Modis35DimKey, List<Variable>> variableListMap, Modis35DimKey rasterDim) {
        List<Modis35ScaledVariable> scaledVariableList = new ArrayList<Modis35ScaledVariable>();
        for (Modis35DimKey dimKey : variableListMap.keySet()) {
            if (!dimKey.equals(rasterDim)) {
                double scaleX = getScale(dimKey.getDimensionX(), rasterDim.getDimensionX());
                double scaleY = getScale(dimKey.getDimensionY(), rasterDim.getDimensionY());
//                if (scaleX == Math.round(scaleX) && scaleX == scaleY) {
                if (Math.round(scaleX) == Math.round(scaleY)) {
                    List<Variable> variableList = variableListMap.get(dimKey);
                    for (Variable variable : variableList) {
                        final Modis35ScaledVariable scaledVariable = new Modis35ScaledVariable((float) Math.round(scaleX), variable);
                        scaledVariable.getVariable().setDimension(0, rasterDim.getDimensionX());
                        scaledVariable.getVariable().setDimension(1, rasterDim.getDimensionY());
                        scaledVariableList.add(scaledVariable);
                    }
                }
            }
        }
        return scaledVariableList.toArray(new Modis35ScaledVariable[scaledVariableList.size()]);
    }

    private static Variable[] getRasterVariablesExtended(Map<Modis35DimKey, List<Variable>> variableListMap, Modis35DimKey rasterDim) {
        return getRasterVariables(variableListMap, rasterDim);
    }

    private static Modis35DimKey getBestRasterDimExtended(Map<Modis35DimKey, List<Variable>> variableListMap) {
        // todo: consider other reasonable cases
        final Set<Modis35DimKey> ncRasterDims = variableListMap.keySet();
        if (ncRasterDims.size() == 0) {
            return null;
        }

        String[] rasterDimNames = null;
        final String rasterDimNameString = System.getProperty("raster.dim.names");
        if (rasterDimNameString != null) {
            rasterDimNames = rasterDimNameString.split(",");
        }
        for (Modis35DimKey rasterDim : ncRasterDims) {
            if (rasterDimNames != null && rasterDimNames.length == rasterDim.getRank()) {
                boolean isOptimalRasterDim = false;
                for (int i = 0; i < rasterDim.getRank(); i++) {
                    isOptimalRasterDim = false;
                    for (int j = 0; j < rasterDimNames.length; j++) {
                        if (rasterDimNames[j].equalsIgnoreCase(rasterDim.getDimension(i).getShortName())) {
                            isOptimalRasterDim = true;
                        }
                    }
                }
                if (isOptimalRasterDim) {
                    return rasterDim;
                }
            }
        }

        Modis35DimKey bestRasterDim = null;
        List<Variable> bestVarList = null;
        for (Modis35DimKey rasterDim : ncRasterDims) {
            if (rasterDim.isTypicalRasterDim()) {
                return rasterDim;
            }
            // Otherwise, we assume the best is the one which holds the most variables
            final List<Variable> varList = variableListMap.get(rasterDim);
            if (bestVarList == null || varList.size() > bestVarList.size()) {
                bestRasterDim = rasterDim;
                bestVarList = varList;
            }
        }

        return bestRasterDim;
    }

    private static Modis35ScaledVariable[] getScaledVariables(Map<Modis35DimKey, List<Variable>> variableListMap, Modis35DimKey rasterDim) {
        List<Modis35ScaledVariable> scaledVariableList = new ArrayList<Modis35ScaledVariable>();
        for (Modis35DimKey dimKey : variableListMap.keySet()) {
            if (!dimKey.equals(rasterDim)) {
                double scaleX = getScale(dimKey.getDimensionX(), rasterDim.getDimensionX());
                double scaleY = getScale(dimKey.getDimensionY(), rasterDim.getDimensionY());
                if (scaleX == Math.round(scaleX) && scaleX == scaleY) {
                    List<Variable> variableList = variableListMap.get(dimKey);
                    for (Variable variable : variableList) {
                        scaledVariableList.add(new Modis35ScaledVariable((float) scaleX, variable));
                    }
                }
            }
        }
        return scaledVariableList.toArray(new Modis35ScaledVariable[scaledVariableList.size()]);
    }

    private static double getScale(Dimension scaledDim, Dimension rasterDim) {
        double length = scaledDim.getLength();
        double rasterLength = rasterDim.getLength();
        return rasterLength / length;
    }

    static Variable[] getRasterVariables(Map<Modis35DimKey, List<Variable>> variableLists,
                                         Modis35DimKey rasterDim) {
        final List<Variable> list = variableLists.get(rasterDim);
        return list.toArray(new Variable[list.size()]);
    }

    static Modis35DimKey getBestRasterDim(Map<Modis35DimKey, List<Variable>> variableListMap) {
        final Set<Modis35DimKey> ncRasterDims = variableListMap.keySet();
        if (ncRasterDims.size() == 0) {
            return null;
        }

        Modis35DimKey bestRasterDim = null;
        List<Variable> bestVarList = null;
        for (Modis35DimKey rasterDim : ncRasterDims) {
            if (rasterDim.isTypicalRasterDim()) {
                return rasterDim;
            }
            // Otherwise, we assume the best is the one which holds the most variables
            final List<Variable> varList = variableListMap.get(rasterDim);
            if (bestVarList == null || varList.size() > bestVarList.size()) {
                bestRasterDim = rasterDim;
                bestVarList = varList;
            }
        }

        return bestRasterDim;
    }

    static void collectVariableLists(Group group, Map<Modis35DimKey, List<Variable>> variableLists) {
        final List<Variable> variables = group.getVariables();
        for (final Variable variable : variables) {
            final int rank = variable.getRank();
            if (rank >= 2 && (DataTypeUtils.isValidRasterDataType(variable.getDataType()) || variable.getDataType() == DataType.LONG)) {
                Modis35DimKey rasterDim = new Modis35DimKey(variable.getDimensions().toArray(new Dimension[variable.getDimensions().size()]));
                final Dimension dimX = rasterDim.getDimensionX();
                final Dimension dimY = rasterDim.getDimensionY();
                if (dimX.getLength() > 1 && dimY.getLength() > 1) {
                    List<Variable> list = variableLists.get(rasterDim);
                    if (list == null) {
                        list = new ArrayList<Variable>();
                        variableLists.put(rasterDim, list);
                    }
                    list.add(variable);
                }
            }
        }
        final List<Group> subGroups = group.getGroups();
        for (final Group subGroup : subGroups) {
            collectVariableLists(subGroup, variableLists);
        }
    }
}

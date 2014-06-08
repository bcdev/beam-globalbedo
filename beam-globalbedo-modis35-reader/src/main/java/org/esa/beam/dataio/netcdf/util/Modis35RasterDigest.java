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
import java.util.List;

/**
 * Represents an extract of all variables that could be converted to bands.
 */
public class Modis35RasterDigest {

    private final Modis35DimKey rasterDim;
    private final Variable[] variables;


    public Modis35RasterDigest(Modis35DimKey rasterDim, Variable[] variables) {
        this.rasterDim = rasterDim;
        this.variables = variables;
    }

    public Modis35DimKey getRasterDim() {
        return rasterDim;
    }

    public Variable[] getRasterVariables() {
        return variables;
    }


    public static Modis35RasterDigest createRasterDigest(String rasterDimNames, final Group... groups) {
        Mod35Raster raster = collectVariables(rasterDimNames, groups);
        if (raster.getRasterDim() == null) {
            return null;
        }

        final List<Variable> rasterVariableList = raster.getVariableList();
        final Variable[] rasterVariables = rasterVariableList.toArray(new Variable[rasterVariableList.size()]);
        return new Modis35RasterDigest(raster.getRasterDim(), rasterVariables);
    }

    static Mod35Raster collectVariables(String rasterDimNames, Group... groups) {
        List<Variable> resultVariables = new ArrayList<>();
        Mod35Raster raster = new Mod35Raster();

        for (Group group : groups) {
            for (Group subGroup : group.getGroups()) {
                for (Group subSubGroup : subGroup.getGroups()) {
                    final List<Variable> variables = subSubGroup.getVariables();
                    for (final Variable variable : variables) {
                        final int rank = variable.getRank();
                        if (rank >= 2 && (DataTypeUtils.isValidRasterDataType(variable.getDataType()) ||
                                variable.getDataType() == DataType.LONG)) {
                            Modis35DimKey rasterDim =
                                    new Modis35DimKey(variable.getDimensions().toArray(new Dimension[variable.getDimensions().size()]));
                            if (rasterDimMatches(rasterDim, rasterDimNames)) {
                                raster.setRasterDim(rasterDim);
                                resultVariables.add(variable);
                            }
                        }
                    }
                }
            }
        }
        if (raster.getRasterDim() != null) {
            raster.setVariableList(resultVariables);
        }
        return raster;
    }

    static boolean rasterDimMatches(Modis35DimKey rasterDim, String rasterDimNameString) {
        String[] rasterDimNames = rasterDimNameString.split(",");
        if (rasterDim.getRank() == rasterDimNames.length) {
            boolean dimsMatch = true;
            for (int j = 0; j < rasterDim.getRank(); j++) {
                if (!rasterDim.getDimension(j).getShortName().equals(rasterDimNames[j])) {
                    dimsMatch = false;
                }
            }
            if (dimsMatch) {
                return true;
            }
        }

        return false;
    }

    static class Mod35Raster {
        Modis35DimKey rasterDim;
        List<Variable> variableList;

        public Modis35DimKey getRasterDim() {
            return rasterDim;
        }

        public void setRasterDim(Modis35DimKey rasterDim) {
            this.rasterDim = rasterDim;
        }

        public List<Variable> getVariableList() {
            return variableList;
        }

        public void setVariableList(List<Variable> variableList) {
            this.variableList = variableList;
        }
    }

}

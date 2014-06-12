package org.esa.beam.dataio.netcdf.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 */
public class Modis35RasterDigestTest {

    private static final String HDF_TESTFILE = "MOD35_L2.A2014157.0910.005.2014157192917.hdf";
    private NetcdfFile netcdfFile;

    @Before
    public void setUp() throws Exception {
        final String testFilePath = Modis35RasterDigest.class.getResource(HDF_TESTFILE).getPath();
        netcdfFile = NetcdfFileOpener.open(testFilePath);
    }

    @After
    public void tearDown() throws Exception {
        netcdfFile.close();
    }

    @Test
    public void testRasterDimMatches() throws Exception {
        final String rasterDimNames = "Cell_Along_Swath_1km,Cell_Across_Swath_1km,QA_Dimension";
        Dimension dim1 = new Dimension("Cell_Along_Swath_1km", 10);
        Dimension dim2 = new Dimension("Cell_Across_Swath_1km", 20);
        Dimension dim3 = new Dimension("QA_Dimension", 30);

        Dimension[] dims = new Dimension[]{dim1, dim3, dim2};
        Modis35DimKey rasterDim = new Modis35DimKey(dims);
        boolean result = Modis35RasterDigest.rasterDimMatches(rasterDim, rasterDimNames);
        assertFalse(result);

        dims = new Dimension[]{dim1, dim2};
        rasterDim = new Modis35DimKey(dims);
        result = Modis35RasterDigest.rasterDimMatches(rasterDim, rasterDimNames);
        assertFalse(result);

        dims = new Dimension[]{dim2, dim1, dim3};
        rasterDim = new Modis35DimKey(dims);
        result = Modis35RasterDigest.rasterDimMatches(rasterDim, rasterDimNames);
        assertFalse(result);

        // only this one should pass:
        dims = new Dimension[]{dim1, dim2, dim3};
        rasterDim = new Modis35DimKey(dims);
        result = Modis35RasterDigest.rasterDimMatches(rasterDim, rasterDimNames);
        assertTrue(result);
    }


    @Test
    public void testCollectVariableLists() throws Exception {
        final Group rootGroup = netcdfFile.getRootGroup();

        String rasterDimNames = "bla,blubb";
        Modis35RasterDigest.Mod35Raster raster =
                Modis35RasterDigest.collectVariables(rasterDimNames, rootGroup);
        assertNotNull(raster);
        assertNull(raster.getRasterDim());
        assertNull(raster.getVariableList());

        rasterDimNames = "Cell_Along_Swath_5km,Cell_Across_Swath_5km";
        raster = Modis35RasterDigest.collectVariables(rasterDimNames, rootGroup);
        assertNotNull(raster);
        Modis35DimKey rasterDim = raster.getRasterDim();
        assertNotNull(rasterDim);
        assertEquals(2, rasterDim.getRank());
        assertNotNull(rasterDim.getDimensionX());
        assertEquals("Cell_Across_Swath_5km", rasterDim.getDimensionX().getShortName());
        assertEquals(270, rasterDim.getDimensionX().getLength());
        assertEquals("Cell_Along_Swath_5km", rasterDim.getDimensionY().getShortName());
        assertEquals(406, rasterDim.getDimensionY().getLength());

        List<Variable> variableList = raster.getVariableList();
        assertNotNull(variableList);
        assertEquals(7, variableList.size());
        assertEquals("Latitude", variableList.get(0).getShortName());
        assertEquals("Longitude", variableList.get(1).getShortName());
        assertEquals("Scan_Start_Time", variableList.get(2).getShortName());
        assertEquals("Solar_Zenith", variableList.get(3).getShortName());
        assertEquals("Solar_Azimuth", variableList.get(4).getShortName());
        assertEquals("Sensor_Zenith", variableList.get(5).getShortName());
        assertEquals("Sensor_Azimuth", variableList.get(6).getShortName());
        assertEquals(270 * 406, variableList.get(0).getSize());
        assertEquals(2, variableList.get(0).getRank());
        assertEquals(2, variableList.get(0).getShape().length);
        assertEquals(406, variableList.get(0).getShape()[0]);
        assertEquals(270, variableList.get(0).getShape()[1]);

        rasterDimNames = "Cell_Along_Swath_1km,Cell_Across_Swath_1km,QA_Dimension";
        raster = Modis35RasterDigest.collectVariables(rasterDimNames, rootGroup);
        assertNotNull(raster);
        rasterDim = raster.getRasterDim();
        assertNotNull(rasterDim);
        assertEquals(3, rasterDim.getRank());
        assertNotNull(rasterDim.getDimensionX());
        assertEquals("Cell_Across_Swath_1km", rasterDim.getDimensionX().getShortName());
        assertEquals(1354, rasterDim.getDimensionX().getLength());
        assertEquals("Cell_Along_Swath_1km", rasterDim.getDimensionY().getShortName());
        assertEquals(2030, rasterDim.getDimensionY().getLength());
        assertEquals("QA_Dimension", rasterDim.getDimension(2).getShortName());
        assertEquals(10, rasterDim.getDimension(2).getLength());

        variableList = raster.getVariableList();
        assertNotNull(variableList);
        assertEquals(1, variableList.size());
        assertEquals("Quality_Assurance", variableList.get(0).getShortName());
        assertEquals(1354 * 2030 * 10, variableList.get(0).getSize());
        assertEquals(3, variableList.get(0).getRank());
        assertEquals(3, variableList.get(0).getShape().length);
        assertEquals(2030, variableList.get(0).getShape()[0]);
        assertEquals(1354, variableList.get(0).getShape()[1]);
        assertEquals(10, variableList.get(0).getShape()[2]);

        rasterDimNames = "Byte_Segment,Cell_Along_Swath_1km,Cell_Across_Swath_1km";
        raster = Modis35RasterDigest.collectVariables(rasterDimNames, rootGroup);
        assertNotNull(raster);
        rasterDim = raster.getRasterDim();
        assertNotNull(rasterDim);
        assertEquals(3, rasterDim.getRank());
        assertNotNull(rasterDim.getDimensionX());
        assertEquals("Cell_Across_Swath_1km", rasterDim.getDimensionX().getShortName());
        assertEquals(1354, rasterDim.getDimensionX().getLength());
        assertEquals("Cell_Along_Swath_1km", rasterDim.getDimensionY().getShortName());
        assertEquals(2030, rasterDim.getDimensionY().getLength());
        assertEquals("Byte_Segment", rasterDim.getDimension(0).getShortName());
        assertEquals(6, rasterDim.getDimension(0).getLength());

        variableList = raster.getVariableList();
        assertNotNull(variableList);
        assertEquals(1, variableList.size());
        assertEquals("Cloud_Mask", variableList.get(0).getShortName());
        assertEquals(1354 * 2030 * 6, variableList.get(0).getSize());
        assertEquals(3, variableList.get(0).getRank());
        assertEquals(3, variableList.get(0).getShape().length);
        assertEquals(6, variableList.get(0).getShape()[0]);
        assertEquals(2030, variableList.get(0).getShape()[1]);
        assertEquals(1354, variableList.get(0).getShape()[2]);
    }

}

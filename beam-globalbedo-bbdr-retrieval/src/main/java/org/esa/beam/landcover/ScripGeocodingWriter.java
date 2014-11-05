package org.esa.beam.landcover;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.AbstractProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.ProductData;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index2D;
import ucar.ma2.Index3D;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

import java.io.File;
import java.io.IOException;

/**
 * Generates SCRIP conformant NetCDF file with the pixel geocoding of the product.
 *
 * <pre>
    netcdf grob3s {
    dimensions:
            grid_size = 12120 ;
            grid_xsize = 120 ;
            grid_ysize = 101 ;
            grid_corners = 4 ;
            grid_rank = 2 ;
    variables:
            int grid_dims(grid_rank) ;
            float grid_center_lat(grid_ysize, grid_xsize) ;
                    grid_center_lat:units = ”degrees” ;
                    grid_center_lat:bounds = ”grid_corner_lat” ;
            float grid_center_lon(grid_ysize, grid_xsize) ;
                    grid_center_lon:units = ”degrees” ;
                    grid_center_lon:bounds = ”grid_corner_lon” ;
            int grid_imask(grid_ysize, grid_xsize) ;
                    grid_imask:units = ”unitless” ;
                    grid_imask:coordinates = ”grid_center_lon grid_center_lat” ;
            float grid_corner_lat(grid_ysize, grid_xsize, grid_corners) ;
                    grid_corner_lat:units = ”degrees” ;
            float grid_corner_lon(grid_ysize, grid_xsize, grid_corners) ;
                    grid_corner_lon:units = ”degrees” ;

    // global attributes:
                    :title = ”grob3s” ;
    }
 * </pre>
 *
 * @author Martin Boettcher
 */
public class ScripGeocodingWriter extends AbstractProductWriter {

    NetcdfFileWriter geoFile = null;

    public ScripGeocodingWriter(ProductWriterPlugIn plugin) {
        super(plugin);
    }

    @Override
    protected void writeProductNodesImpl() throws IOException {
        String outputPath;
        if (getOutput() instanceof String) {
            outputPath = (String) getOutput();
        } else if (getOutput() instanceof File) {
            outputPath = ((File) getOutput()).getPath();
        } else {
            throw new IllegalArgumentException("output " + getOutput() + " neither String nor File");
        }
        geoFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, outputPath);

        int height = getSourceProduct().getSceneRasterHeight();
        int width = getSourceProduct().getSceneRasterWidth();
        geoFile.addDimension(null, "grid_size", height * width);
        geoFile.addDimension(null, "grid_ny", height);
        geoFile.addDimension(null, "grid_nx", width);
        geoFile.addDimension(null, "grid_corners", 4);
        geoFile.addDimension(null, "grid_rank", 2);

        final Variable gridDims = geoFile.addVariable(null, "grid_dims", DataType.INT, "grid_rank");
        final Variable gridCenterLat = geoFile.addVariable(null, "grid_center_lat", DataType.FLOAT, "grid_ny grid_nx");
        final Variable gridCenterLon = geoFile.addVariable(null, "grid_center_lon", DataType.FLOAT, "grid_ny grid_nx");
        final Variable gridMask = geoFile.addVariable(null, "grid_imask", DataType.INT, "grid_ny grid_nx");
        final Variable gridCornerLat = geoFile.addVariable(null, "grid_corner_lat", DataType.FLOAT, "grid_ny grid_nx grid_corners");
        final Variable gridCornerLon = geoFile.addVariable(null, "grid_corner_lon", DataType.FLOAT, "grid_ny grid_nx grid_corners");
        gridCenterLat.addAttribute(new Attribute("units", "degrees"));
        gridCenterLon.addAttribute(new Attribute("units", "degrees"));
        geoFile.addGroupAttribute(null, new Attribute("title", "geo-location in SCRIP format"));

        geoFile.create();
        try {
            geoFile.write(gridDims, Array.factory(new int[] {width, height}));

            final int[] targetStart = {0, 0};
            final int[] targetStart2 = {0, 0, 0};
            final int[] targetShape = {height, width};
            final int[] targetShape2 = {height, width, 4};
            final Array maskData = Array.factory(DataType.INT, targetShape);
            final Array centreLat = Array.factory(DataType.FLOAT, targetShape);
            final Array centreLon = Array.factory(DataType.FLOAT, targetShape);
            final Array cornerLat = Array.factory(DataType.FLOAT, targetShape2);
            final Array cornerLon = Array.factory(DataType.FLOAT, targetShape2);

            GeoCoding geoCoding = getSourceProduct().getGeoCoding();
            GeoPos geoPos = new GeoPos();
            Index2D index = (Index2D) centreLat.getIndex();
            Index3D[] cornerIndex = new Index3D[] { (Index3D) cornerLat.getIndex(), (Index3D) cornerLat.getIndex(), (Index3D) cornerLat.getIndex(), (Index3D) cornerLat.getIndex() };
            GeoPos[] cornerPos = new GeoPos[] { new GeoPos(), new GeoPos(), new GeoPos(), new GeoPos() };
            for (int y=0; y<height; ++y) {
                for (int x=0; x<width; ++x) {
                    geoCoding.getGeoPos(new PixelPos(x+0.5f, y+0.5f), geoPos);
                    geoCoding.getGeoPos(new PixelPos((float) x, (float) y), cornerPos[0]);
                    geoCoding.getGeoPos(new PixelPos((float) x+1.0f, (float) y), cornerPos[1]);
                    geoCoding.getGeoPos(new PixelPos((float) x-1.0f, (float) y+1.0f), cornerPos[2]);
                    geoCoding.getGeoPos(new PixelPos((float) x, (float) y+1.0f), cornerPos[3]);
                    index.set(y, x);
                    if (isValid(geoPos)) {
                        centreLat.setFloat(index, geoPos.getLat());
                        centreLon.setFloat(index, geoPos.getLon());
                        maskData.setInt(index, 1);
                    } else {
                        centreLat.setFloat(index, Float.NaN);
                        centreLon.setFloat(index, Float.NaN);
                        maskData.setInt(index, 0);
                    }
                    for (int corner = 0; corner < 4; ++corner) {
                        cornerIndex[corner].set(y, x, corner);
                        if (isValid(cornerPos[corner])) {
                            cornerLat.setFloat(cornerIndex[corner], cornerPos[corner].getLat());
                            cornerLon.setFloat(cornerIndex[corner], cornerPos[corner].getLat());
                        } else {
                            cornerLat.setFloat(cornerIndex[corner], Float.NaN);
                            cornerLon.setFloat(cornerIndex[corner], Float.NaN);
                        }
                    }
                }
            }
            geoFile.write(gridCenterLat, targetStart, centreLat);
            geoFile.write(gridCenterLon, targetStart, centreLon);
            geoFile.write(gridMask, targetStart, maskData);
            geoFile.write(gridCornerLat, targetStart2, cornerLat);
            geoFile.write(gridCornerLon, targetStart2, cornerLon);
        } catch (InvalidRangeException e) {
            throw new IOException(e);
        } finally {
            try {
                geoFile.close();
            } catch (IOException ignored) {
            }
        }


    }

    private boolean isValid(GeoPos geoPos) {
        return geoPos.getLat() >= -90.0 && geoPos.getLat() <= 90.0 &&
            geoPos.getLon() >= -180.0 && geoPos.getLon() <= 180.0;
    }

    @Override
    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm) throws IOException {

    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {
        //geoFile.close();

    }

    @Override
    public void deleteOutput() throws IOException {
        File outputFile;
        if (getOutput() instanceof String) {
            outputFile = new File((String) getOutput());
        } else if (getOutput() instanceof File) {
            outputFile = (File) getOutput();
        } else {
            throw new IllegalArgumentException("output " + getOutput() + " neither String nor File");
        }
        outputFile.delete();
    }
}

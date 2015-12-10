package org.esa.beam.globalbedo.inversion.io.netcdf;

import org.esa.beam.dataio.netcdf.ProfileWriteContext;
import org.esa.beam.dataio.netcdf.metadata.profiles.cf.CfGeocodingPart;
import org.esa.beam.dataio.netcdf.nc.NFileWriteable;
import org.esa.beam.dataio.netcdf.nc.NVariable;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import ucar.ma2.DataType;

import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Geocoding part to allow specific GA/QA4ECV modifications in netcdf attributes
 *
 * @author olafd
 */
public class AlbedoInversionGeocodingPart extends CfGeocodingPart {
    public static final String TIEPOINT_COORDINATES = "tiepoint_coordinates";

    private static final int LON_INDEX = 0;
    private static final int LAT_INDEX = 1;

    @Override
    public void preEncode(ProfileWriteContext ctx, Product p) throws IOException {
        super.preEncode(ctx, p);
        final GeoCoding geoCoding = p.getGeoCoding();
        if (geoCoding instanceof TiePointGeoCoding) {
            final TiePointGeoCoding tpGC = (TiePointGeoCoding) geoCoding;
            final String[] names = new String[2];
            names[LON_INDEX] = tpGC.getLonGrid().getName();
            names[LAT_INDEX] = tpGC.getLatGrid().getName();
            final String value = StringUtils.arrayToString(names, " ");
            ctx.getNetcdfFileWriteable().addGlobalAttribute(TIEPOINT_COORDINATES, value);
        } else {
            if (geoCoding instanceof CrsGeoCoding) {
                addWktAsVariable(ctx.getNetcdfFileWriteable(), geoCoding);
            }
        }
    }

    private void addWktAsVariable(NFileWriteable ncFile, GeoCoding geoCoding) throws IOException {
        final CoordinateReferenceSystem crs = geoCoding.getMapCRS();
        if (crs != null && crs.toWKT() != null) {
            final double[] matrix = new double[6];
            final MathTransform transform = geoCoding.getImageToMapTransform();
            if (transform instanceof AffineTransform) {
                ((AffineTransform) transform).getMatrix(matrix);
            }

            final NVariable crsVariable = ncFile.addScalarVariable("crs", DataType.INT);
            crsVariable.addAttribute("wkt", crs.toWKT());
            crsVariable.addAttribute("i2m", StringUtils.arrayToCsv(matrix));
            crsVariable.addAttribute("long_name", "Coordinate Reference System");
            final String crsCommentString = "A coordinate reference system (CRS) defines defines how the georeferenced " +
                    "spatial data relates to real locations on the Earth\'s surface";
            crsVariable.addAttribute("comment", crsCommentString);
        }
    }

}

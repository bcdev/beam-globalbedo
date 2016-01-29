package org.esa.beam.globalbedo.inversion.util;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.AbstractGeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Scene;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.Ellipsoid;
import org.esa.beam.util.Debug;
import org.geotools.factory.Hints;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.geotools.referencing.operation.AbstractCoordinateOperationFactory;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.resources.CRSUtilities;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.datum.GeodeticDatum;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;

import java.awt.*;
import java.awt.geom.AffineTransform;

/**
 * todo: add comment
 * To change this template use File | Settings | File Templates.
 * Date: 27.01.2016
 * Time: 10:05
 *
 * @author olafd
 */
public class ModisTileGeoCoding extends AbstractGeoCoding {

    private final AffineTransform imageToMap;
    private final MathTransform imageToGeo;
    private final MathTransform geoToImage;
    private final Datum datum;

    public ModisTileGeoCoding(CoordinateReferenceSystem mapCRS,
                        double easting,
                        double northing,
                        double pixelSizeX,
                        double pixelSizeY) throws FactoryException, TransformException {
        this(mapCRS, easting, northing, pixelSizeX, pixelSizeY, 0.5, 0.5);
    }

    public ModisTileGeoCoding(CoordinateReferenceSystem mapCRS,
                        double easting,
                        double northing,
                        double pixelSizeX,
                        double pixelSizeY,
                        double referencePixelX,
                        double referencePixelY) throws FactoryException, TransformException {
        this(mapCRS,
             createImageToMapTransform(easting, northing, pixelSizeX, pixelSizeY, referencePixelX, referencePixelY));
    }

    public ModisTileGeoCoding(CoordinateReferenceSystem mapCRS, AffineTransform imageToMap) throws FactoryException, TransformException {
        this.imageToMap = imageToMap;
        setMapCRS(mapCRS);
        org.opengis.referencing.datum.Ellipsoid gtEllipsoid = CRS.getEllipsoid(mapCRS);
        String ellipsoidName = gtEllipsoid.getName().getCode();
        Ellipsoid ellipsoid = new Ellipsoid(ellipsoidName,
                                            gtEllipsoid.getSemiMinorAxis(),
                                            gtEllipsoid.getSemiMajorAxis());
        org.opengis.referencing.datum.Datum gtDatum = CRSUtilities.getDatum(mapCRS);
        String datumName = gtDatum.getName().getCode();
        this.datum = new Datum(datumName, ellipsoid, 0, 0, 0);

        MathTransform i2m = new AffineTransform2D(imageToMap);

        if (mapCRS instanceof DerivedCRS) {
            DerivedCRS derivedCRS = (DerivedCRS) mapCRS;
            CoordinateReferenceSystem baseCRS = derivedCRS.getBaseCRS();
            setGeoCRS(baseCRS);
        } else if (gtDatum instanceof GeodeticDatum) {
            setGeoCRS(new DefaultGeographicCRS((GeodeticDatum) gtDatum, DefaultEllipsoidalCS.GEODETIC_2D));
        } else {
            // Fallback
            setGeoCRS(DefaultGeographicCRS.WGS84);
        }

        setImageCRS(createImageCRS(mapCRS, i2m.inverse()));

        MathTransform map2Geo = CRS.findMathTransform(mapCRS, getGeoCRS(), true);
        Hints hints = new Hints(Hints.LENIENT_DATUM_SHIFT, Boolean.TRUE);

        final CoordinateOperationFactory factory = ReferencingFactoryFinder.getCoordinateOperationFactory(hints);
        final MathTransformFactory mtFactory;
        if (factory instanceof AbstractCoordinateOperationFactory) {
            mtFactory = ((AbstractCoordinateOperationFactory) factory).getMathTransformFactory();
        } else {
            mtFactory = ReferencingFactoryFinder.getMathTransformFactory(hints);
        }
        imageToGeo = mtFactory.createConcatenatedTransform(i2m, map2Geo);
        geoToImage = imageToGeo.inverse();
    }

    @Override
    public MathTransform getImageToMapTransform() {
        return new AffineTransform2D(imageToMap);
    }

    @Override
    public boolean transferGeoCoding(Scene srcScene, Scene destScene, ProductSubsetDef subsetDef) {
        final AffineTransform destTransform = new AffineTransform(imageToMap);
        Rectangle destBounds = new Rectangle(destScene.getRasterWidth(), destScene.getRasterHeight());

        if (subsetDef != null) {
            final Rectangle region = subsetDef.getRegion();
            double scaleX = subsetDef.getSubSamplingX();
            double scaleY = subsetDef.getSubSamplingY();
            if (region != null) {
                destTransform.translate(region.getX(), region.getY());
                destBounds.setRect(0, 0, region.getWidth() / scaleX, region.getHeight() / scaleY);
            }
            destTransform.scale(scaleX, scaleY);
        }

        try {
            destScene.setGeoCoding(new ModisTileGeoCoding(getMapCRS(), destTransform));
        } catch (Exception e) {
            Debug.trace(e);
            return false;
        }
        return true;
    }


    @Override
    public boolean isCrossingMeridianAt180() {
        // not needed for our purpose. Also, the standard implementation causes problems for tiles at 'off-planet' edge.
        return false;
    }

    @Override
    public String toString() {
        final String s = super.toString();
        return s + "\n\n" +
                "Map CRS:\n" + getMapCRS().toString() + "\n" +
                "Image To Map:\n" + imageToMap.toString();

    }

    @Override
    public boolean canGetPixelPos() {
        return true;
    }

    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    @Override
    public void dispose() {
        // nothing to dispose
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        try {
            DirectPosition directGeoPos = new DirectPosition2D(geoPos.getLon(), geoPos.getLat());
            DirectPosition directPixelPos = geoToImage.transform(directGeoPos, null);
            pixelPos.setLocation((float) directPixelPos.getOrdinate(0), (float) directPixelPos.getOrdinate(1));
        } catch (Exception ignored) {
            pixelPos.setInvalid();
        }
        return pixelPos;
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        try {
            DirectPosition directPixelPos = new DirectPosition2D(pixelPos);
            DirectPosition directGeoPos = imageToGeo.transform(directPixelPos, null);
            geoPos.setLocation((float) directGeoPos.getOrdinate(1), (float) directGeoPos.getOrdinate(0));
        } catch (Exception ignored) {
            geoPos.setInvalid();
        }
        return geoPos;
    }

    @Override
    public Datum getDatum() {
        return datum;
    }

    private static AffineTransform createImageToMapTransform(double easting,
                                                             double northing,
                                                             double pixelSizeX,
                                                             double pixelSizeY,
                                                             double referencePixelX,
                                                             double referencePixelY) {
        final AffineTransform at = new AffineTransform();
        at.translate(easting, northing);
        at.scale(pixelSizeX, -pixelSizeY);
        at.translate(-referencePixelX, -referencePixelY);
        return at;
    }
}

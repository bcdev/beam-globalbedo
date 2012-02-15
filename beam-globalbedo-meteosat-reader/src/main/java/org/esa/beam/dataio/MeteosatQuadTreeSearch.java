package org.esa.beam.dataio;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.framework.datamodel.PixelGeoCoding;

/**
 * Provides a QuadTree search for Meteosat First and Second Generation Albedo product geocoding.
 * In principle, the algorithm is the same as used in {@link PixelGeoCoding}. A region ID
 * allows for specific implementations for regions containing weird areas (e.g. off-planet in Meteosat MSG Euro product).
 *
 * @author olafd
 */
public class MeteosatQuadTreeSearch {

    private final float[] latData;
    private final float[] lonData;
    private final int width;
    private final int height;
    private String regionID;

    public MeteosatQuadTreeSearch(float[] latData, float[] lonData, int width, int height, String regionID) {
        this.latData = latData;
        this.lonData = lonData;
        this.width = width;
        this.height = height;
        this.regionID = regionID;
    }

    public boolean search(final int depth,
                          final float lat,
                          final float lon,
                          final int x, final int y,
                          final int w, final int h,
                          final Result result) {
        if (w < 2 || h < 2) {
            return false;
        }

        final int x1 = x;
        final int x2 = x1 + w - 1;

        final int y1 = y;
        final int y2 = y1 + h - 1;

        // todo - solve 180° longitude problem here
        GeoPos geoPos = new GeoPos();
        getGeoPosInternal(x1, y1, geoPos);
        final float lat0 = geoPos.lat;
        final float lon0 = geoPos.lon;
        getGeoPosInternal(x1, y2, geoPos);
        final float lat1 = geoPos.lat;
        final float lon1 = geoPos.lon;
        getGeoPosInternal(x2, y1, geoPos);
        final float lat2 = geoPos.lat;
        final float lon2 = geoPos.lon;
        getGeoPosInternal(x2, y2, geoPos);
        final float lat3 = geoPos.lat;
        final float lon3 = geoPos.lon;

        float epsL = 0.04f;
        if (regionID.equals("MSG_Euro")) {
            // do this for the Msg Euro product only
            if (increaseEpsForMSGEuro(lat, lon)) {
                // lat-dependent increase of epsL to avoid gaps in case of (lat,lon)-"boxes" which are extremely distorted
                // compared to their corresponding (x,y)-rectangles (e.g. close to the poles in Meteosat
                // MSG Euro product). This has shown to eliminate the projection artifacts due to missing (lat,lon) quite well
                epsL += 0.04 * 1.0 * Math.abs(lat - 35.0);
            }
        }

        final float latMin = min(lat0, min(lat1, min(lat2, lat3))) - epsL;
        final float latMax = max(lat0, max(lat1, max(lat2, lat3))) + epsL;
        final float lonMin = min(lon0, min(lon1, min(lon2, lon3))) - epsL;
        final float lonMax = max(lon0, max(lon1, max(lon2, lon3))) + epsL;

        boolean pixelFound = false;
        final boolean definitelyOutside = lat < latMin || lat > latMax || lon < lonMin || lon > lonMax;
        if (!definitelyOutside) {
            if (w == 2 && h == 2) {
                final float f = (float) Math.cos(lat * MathUtils.DTOR);
                if (result.update(x1, y1, sqr(lat - lat0, f * (lon - lon0)))) {
                    pixelFound = true;
                }
                if (result.update(x1, y2, sqr(lat - lat1, f * (lon - lon1)))) {
                    pixelFound = true;
                }
                if (result.update(x2, y1, sqr(lat - lat2, f * (lon - lon2)))) {
                    pixelFound = true;
                }
                if (result.update(x2, y2, sqr(lat - lat3, f * (lon - lon3)))) {
                    pixelFound = true;
                }
            } else if (w >= 2 && h >= 2) {
                pixelFound = quadTreeRecursion(depth, lat, lon, x1, y1, w, h, result);
            }
        }

        return pixelFound;
    }

    private boolean increaseEpsForMSGEuro(float lat, float lon) {
        // increase eps threshold towards the pole to make sure that geo positions are not lost in gaps
        // found from visual inspection of reprojected lat and lon products:
        final boolean isInAreaToIncrease = lon + lat > 43 && lat > 39;
        // there were some remaining gaps over greenland...
        final boolean greenlandBox1 = lon > -31.775 && lon < -29.775 && lat > 71.8 && lat < 73.37;
        final boolean greenlandBox2 = lon > -29.175 && lon < -27.675 && lat > 70.45 && lat < 70.825;
        return greenlandBox1 || greenlandBox2 || isInAreaToIncrease;
    }

    private void getGeoPosInternal(int pixelX, int pixelY, GeoPos geoPos) {
        int i = width * pixelY + pixelX;
        geoPos.setLocation(latData[i], lonData[i]);
    }

    private boolean quadTreeRecursion(final int depth,
                                      final float lat, final float lon,
                                      final int i, final int j,
                                      final int w, final int h,
                                      final Result result) {
        int w2 = w >> 1;
        int h2 = h >> 1;
        final int i2 = i + w2;
        final int j2 = j + h2;
        final int w2r = w - w2;
        final int h2r = h - h2;

        if (w2 < 2) {
            w2 = 2;
        }

        if (h2 < 2) {
            h2 = 2;
        }

        final boolean b1 = search(depth + 1, lat, lon, i, j, w2, h2, result);
        final boolean b2 = search(depth + 1, lat, lon, i, j2, w2, h2r, result);
        final boolean b3 = search(depth + 1, lat, lon, i2, j, w2r, h2, result);
        final boolean b4 = search(depth + 1, lat, lon, i2, j2, w2r, h2r, result);

        return b1 || b2 || b3 || b4;
    }


    private static float min(final float a, final float b) {
        return (a <= b) ? a : b;
    }

    private static float max(final float a, final float b) {
        return (a >= b) ? a : b;
    }

    private static float sqr(final float dx, final float dy) {
        return dx * dx + dy * dy;
    }


    static class Result {

        public static final float INVALID = Float.MAX_VALUE;

        private int x;
        private int y;
        private float delta;

        Result() {
            delta = INVALID;
        }

        public final boolean update(final int x, final int y, final float delta) {
            final boolean b = delta < this.delta;
            if (b) {
                this.x = x;
                this.y = y;
                this.delta = delta;
            }
            return b;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        @Override
        public String toString() {
            return "Result[" + x + ", " + y + ", " + delta + "]";
        }
    }

}

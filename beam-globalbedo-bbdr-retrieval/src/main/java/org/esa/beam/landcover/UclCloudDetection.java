/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.landcover;

import org.esa.beam.util.geotiff.IIOUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;

/**
 * The cloud detection from UCL.
 *
 * @author MarcoZ
 */
public class UclCloudDetection {

    private static final float[] hueIndices = new float[]{0f, 3f, 6f, 9f, 12f, 15f, 18f, 21f, 24f, 27f, 30f, 33f, 36f, 39f, 42f, 45f, 48f, 51f, 54f, 57f, 60f, 63f, 66f, 69f, 72f, 75f, 78f, 81f, 84f, 87f, 90f, 93f, 96f, 99f, 102f, 105f, 108f, 111f, 114f, 117f, 120f, 123f, 126f, 129f, 132f, 135f, 138f, 141f, 144f, 147f, 150f, 153f, 156f, 159f, 162f, 165f, 168f, 171f, 174f, 177f, 180f, 183f, 186f, 189f, 192f, 195f, 198f, 201f, 204f, 207f, 210f, 213f, 216f, 219f, 222f, 225f, 228f, 231f, 234f, 237f, 240f, 243f, 246f, 249f, 252f, 255f, 258f, 261f, 264f, 267f, 270f, 273f, 276f, 279f, 282f, 285f, 288f, 291f, 294f, 297f, 300f, 303f, 306f, 309f, 312f, 315f, 318f, 321f, 324f, 327f, 330f, 333f, 336f, 339f, 342f, 345f, 348f, 351f, 354f, 357f, 360f};
    private static final float[] satIndices = new float[]{0f, 0.01f, 0.02f, 0.03f, 0.04f, 0.05f, 0.06f, 0.07f, 0.08f, 0.09f, 0.1f, 0.11f, 0.12f, 0.13f, 0.14f, 0.15f, 0.16f, 0.17f, 0.18f, 0.19f, 0.2f, 0.21f, 0.22f, 0.23f, 0.24f, 0.25f, 0.26f, 0.27f, 0.28f, 0.29f, 0.3f, 0.31f, 0.32f, 0.33f, 0.34f, 0.35f, 0.36f, 0.37f, 0.38f, 0.39f, 0.4f, 0.41f, 0.42f, 0.43f, 0.44f, 0.45f, 0.46f, 0.47f, 0.48f, 0.49f, 0.5f, 0.51f, 0.52f, 0.53f, 0.54f, 0.55f, 0.56f, 0.57f, 0.58f, 0.59f, 0.6f, 0.61f, 0.62f, 0.63f, 0.64f, 0.65f, 0.66f, 0.67f, 0.68f, 0.69f, 0.7f, 0.71f, 0.72f, 0.73f, 0.74f, 0.75f, 0.76f, 0.77f, 0.78f, 0.79f, 0.8f, 0.81f, 0.82f, 0.83f, 0.839999f, 0.849999f, 0.859999f, 0.869999f, 0.879999f, 0.889999f, 0.899999f, 0.909999f, 0.919999f, 0.929999f, 0.939999f, 0.949999f, 0.959999f, 0.969999f, 0.979999f, 0.989999f, 1f};
    private static final float[] valIndices = new float[]{0f, 0.01f, 0.02f, 0.03f, 0.04f, 0.05f, 0.06f, 0.07f, 0.08f, 0.09f, 0.1f, 0.11f, 0.12f, 0.13f, 0.14f, 0.15f, 0.16f, 0.17f, 0.18f, 0.19f, 0.2f, 0.21f, 0.22f, 0.23f, 0.24f, 0.25f, 0.26f, 0.27f, 0.28f, 0.29f, 0.3f, 0.31f, 0.32f, 0.33f, 0.34f, 0.35f, 0.36f, 0.37f, 0.38f, 0.39f, 0.4f, 0.41f, 0.42f, 0.43f, 0.44f, 0.45f, 0.46f, 0.47f, 0.48f, 0.49f, 0.5f, 0.51f, 0.52f, 0.53f, 0.54f, 0.55f, 0.56f, 0.57f, 0.58f, 0.59f, 0.6f, 0.61f, 0.62f, 0.63f, 0.64f, 0.65f, 0.66f, 0.67f, 0.68f, 0.69f, 0.7f, 0.71f, 0.72f, 0.73f, 0.74f, 0.75f, 0.76f, 0.77f, 0.78f, 0.79f, 0.8f, 0.81f, 0.82f, 0.83f, 0.839999f, 0.849999f, 0.859999f, 0.869999f, 0.879999f, 0.889999f, 0.899999f, 0.909999f, 0.919999f, 0.929999f, 0.939999f, 0.949999f, 0.959999f, 0.969999f, 0.979999f, 0.989999f, 1f};

    private static final float CLOUD_UNCERTAINTY_THRESHOLD = -0.1f;

    private static final String CLOUD_SCATTER_FILE = "MER_FSG_SDR.HSV_CLOUD.scatter_percentil.tif";
    private static final String LAND_SCATTER_FILE = "MER_FSG_SDR.HSV_LAND.scatter_percentil.tif";
    private static final String TOWN_SCATTER_FILE = "MER_FSG_SDR.HSV_TOWN.scatter_percentil.tif";

    final ScatterData cloudScatterData;
    final ScatterData landScatterData;
    final ScatterData townScatterData;

    UclCloudDetection(ScatterData cloud, ScatterData land, ScatterData town) {
        this.cloudScatterData = cloud;
        this.landScatterData = land;
        this.townScatterData = town;
    }

    public static UclCloudDetection create() throws IOException {
        ScatterData cloud = new ScatterData(hueIndices, satIndices, valIndices, readScatterRaster(CLOUD_SCATTER_FILE));
        ScatterData land = new ScatterData(hueIndices, satIndices, valIndices, readScatterRaster(LAND_SCATTER_FILE));
        ScatterData town = new ScatterData(hueIndices, satIndices, valIndices, readScatterRaster(TOWN_SCATTER_FILE));
        return new UclCloudDetection(cloud, land, town);
    }

    private static Raster readScatterRaster(String scatterFile) throws IOException {
        InputStream inputStream = UclCloudDetection.class.getResourceAsStream(scatterFile);
        ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream);
        IIOImage iioImage = IIOUtils.readImage(imageInputStream);
        return iioImage.getRenderedImage().getData();
    }

    public boolean isCloud(float sdrRed, float sdrGreen, float sdrBlue) {
        float[] hsv = rgb2hsv(sdrRed, sdrGreen, sdrBlue);
        float cloudD = cloudScatterData.getDensity(hsv);
        float landD = landScatterData.getDensity(hsv);
        float townD = townScatterData.getDensity(hsv);
        return iCloudImpl(cloudD, landD, townD);
    }

    static boolean iCloudImpl(float cloudD, float landD, float townD) {
        boolean isCloud = true;
        float highestValue = Float.NaN;
        if (!Float.isNaN(cloudD)) {
            highestValue = cloudD;
        }
        if (!Float.isNaN(landD)) {
            if (Float.isNaN(highestValue) || landD > highestValue) {
                highestValue = landD;
                isCloud = false;
            }
        }
        if (!Float.isNaN(townD)) {
            if (Float.isNaN(highestValue) || townD > highestValue) {
                highestValue = townD;
                isCloud = false;
            }
        }
        return isCloud;
    }

    public static float[] rgb2hsv(float red, float green, float blue) {
        float hue = Float.NaN;
        float sat = Float.NaN;
        float value = Float.NaN;
        if (!Float.isNaN(red) && !Float.isNaN(green) && !Float.isNaN(blue)) {
            float maxc = Math.max(red, Math.max(green, blue));
            float minc = Math.min(red, Math.min(green, blue));
            float difc = maxc - minc;
            value = maxc;
            if (red == 0f && green == 0f && blue == 0f) {
                sat = 0f;
            } else {
                sat = difc / maxc;
            }
            if ((minc != maxc) && (difc != 0.0)) {
                if ((red == maxc) && (green >= blue)) {
                    hue = (60.0f * ((green - blue) / difc)) + 0.0f;
                } else if ((red == maxc) && (green < blue)) {
                    hue = (60.0f * ((green - blue) / difc)) + 360.0f;
                } else if (green == maxc) {
                    hue = (60.0f * ((blue - red) / difc)) + 120.0f;
                } else if (blue == maxc) {
                    hue = (60.0f * ((red - green) / difc)) + 240.0f;
                } else {
                    hue = Float.NaN;
                }
            }
        }
        return new float[]{hue, sat, value};
    }

    static class ScatterData {
        private final float[] scatterIndexX;
        private final float[] scatterIndexY;
        private final float[] scatterIndexZ;
        private final Raster scatterData;

        private ScatterData(float[] scatterIndexX, float[] scatterIndexY, float[] scatterIndexZ, Raster scatterData) {
            this.scatterIndexX = scatterIndexX;
            this.scatterIndexY = scatterIndexY;
            this.scatterIndexZ = scatterIndexZ;
            this.scatterData = scatterData;
        }

        public float getDensity(float[] hsv) {
            final int indexX = findIndex(scatterIndexX, hsv[0]);
            final int indexY = findIndex(scatterIndexY, hsv[1]);
            final int indexZ = findIndex(scatterIndexZ, hsv[2]);
            if (indexX == -1 || indexY == -1 || indexZ == -1) {
                return Float.NaN;
            }
            final int scatterX = indexX;
            final int scatterY = indexZ * (scatterIndexZ.length - 1) + indexY;
            return scatterData.getSampleFloat(scatterX, scatterY, 0);
        }

        private static int findIndex(float[] scatterIndex, float value) {
            for (int index = 0; index < (scatterIndex.length - 1); index++) {
                if (value >= scatterIndex[index] && value <= scatterIndex[index + 1]) {
                    return index;
                }
            }
            return -1;
        }
    }
}

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

package org.esa.beam.dataio.tiff;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.VirtualDir;
import org.esa.beam.dataio.geotiff.GeoTiffProductReaderPlugIn;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;

import javax.media.jai.Interpolation;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This reader is capable of reading a collection of TIF products
 * located in one directory (which can be a zip archive as well) into a single product.
 * Each of the single TIFs will be represented by one band in the target product.
 *
 * @author Olaf Danne
 */
public class TiffDirectoryReader extends AbstractProductReader {

    private static final String UNITS = "W/(m^2*sr*µm)";      // todo clarify

    private List<Product> bandProducts;
    private VirtualDir input;

    public TiffDirectoryReader(TiffDirectoryReaderPlugin readerPlugin) {
        super(readerPlugin);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        input = TiffDirectoryReaderPlugin.getInput(getInput());
        String[] list = input.list("");
        File metaExtractionFile = null;
        for (String fileName : list) {
            final File file = input.getFile(fileName);
            if (TiffDirectoryReaderPlugin.isMetadataFile(file)) {
                metaExtractionFile = file;
                break;
            }
        }

        Product product;
        Dimension productDim;
        if (metaExtractionFile == null || !metaExtractionFile.canRead()) {
            // default solution:
            // read data without any meta information: we only need the product dimensions, take from first product
            final Product firstTiffProduct = getFirstTiffProduct();
            if (firstTiffProduct == null) {
                throw new ProductIOException("No TIFF products found.");
            }
            product = new Product(firstTiffProduct.getFileLocation().getParentFile().getName(),
                                  firstTiffProduct.getProductType(),
                                  firstTiffProduct.getSceneRasterWidth(),
                                  firstTiffProduct.getSceneRasterHeight());
            ProductUtils.copyMetadata(firstTiffProduct, product);
            ProductUtils.copyGeoCoding(firstTiffProduct, product);
        } else {
            //  if metadata is available:
            // todo: this is a possible future option. define a proper format. current implementation
            // of TiffDirectoryMetadata is preliminary and only applies for distinct format as used
            // for MSSL Hyperspectral Camera products
            TiffDirectoryMetadata tiffMetadata = new TiffDirectoryMetadata(new FileReader(metaExtractionFile));
            if (!(tiffMetadata.isHyperspectralCamera())) {
                throw new ProductIOException("Product is not a 'Hyperspectral Camera' product.");
            }
            productDim = tiffMetadata.getProductDim();

            MetadataElement metadataElement = tiffMetadata.getMetaDataElementRoot();
            product = new Product(getProductName(metaExtractionFile), tiffMetadata.getProductType(), productDim.width, productDim.height);
            product.setFileLocation(metaExtractionFile);

            product.getMetadataRoot().addElement(metadataElement);

            ProductData.UTC utcCenter = tiffMetadata.getCreationTime();
            product.setStartTime(utcCenter);
            product.setEndTime(utcCenter);

        }
        addBands(product, input);

        return product;
    }

    private Product getFirstTiffProduct() throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();
        for (String fileName : input.list("")) {
            final File tiffFile = input.getFile(fileName);
            final String extension = FileUtils.getExtension(tiffFile);
            if (extension.toLowerCase().equals(".tif") || extension.toLowerCase().equals(".tiff")) {
                ProductReader productReader = plugIn.createReaderInstance();
                return productReader.readProductNodes(tiffFile, null);
            }
        }
        return null;
    }

    private static String getProductName(File metafile) {
        String filename = metafile.getName();
        int extensionIndex = filename.toLowerCase().indexOf("_meta.txt");
        return filename.substring(0, extensionIndex);
    }

    private void addBands(Product product, VirtualDir folder) throws IOException {
        final GeoTiffProductReaderPlugIn plugIn = new GeoTiffProductReaderPlugIn();

        bandProducts = new ArrayList<Product>();
        int bandIndex = 0;
        for (String fileName : folder.list("")) {
            final File bandFile = folder.getFile(fileName);
            final String extension = FileUtils.getExtension(bandFile);
            if (extension.toLowerCase().equals(".tif") || extension.toLowerCase().equals(".tiff")) {
                ProductReader productReader = plugIn.createReaderInstance();
                Product bandProduct = productReader.readProductNodes(bandFile, null);
                if (bandProduct != null) {
                    bandProducts.add(bandProduct);
                    Band srcBand = bandProduct.getBandAt(0);
                    String bandName = FileUtils.getFilenameWithoutExtension(fileName);

                    if (isMsslHcProduct(fileName)) {
                        // check special case for MSSL Hyperspectral Camera products:
                        // if filename starts with wavelength, truncate the band name properly
                        // e.g. '1234nm_'...
                        bandName = fileName.substring(0, fileName.indexOf("ms") + 2); // e.g. 0760nm_1443.911ms
                    }

                    Band band = product.addBand(bandName, srcBand.getDataType());
                    band.setNoDataValueUsed(false);
                    if (isMsslHcProduct(fileName)) {
                        // again the special case
                        band.setSpectralBandIndex(bandIndex++);
                        band.setSpectralWavelength((float) Integer.parseInt(bandName.substring(0, 4)));
                        band.setSpectralBandwidth(0.0f);
                        band.setDescription("HC value at " + band.getSpectralWavelength() + " nm.");
                        band.setUnit(UNITS);
                    }
                }
            }
        }

        for (int i = 0; i < bandProducts.size(); i++) {
            Product bandProduct = bandProducts.get(i);
            Band band = product.getBandAt(i);
            if (product.getSceneRasterWidth() == bandProduct.getSceneRasterWidth() &&
                    product.getSceneRasterHeight() == bandProduct.getSceneRasterHeight()) {
                band.setSourceImage(bandProduct.getBandAt(0).getSourceImage());
            } else {
                PlanarImage image = createScaledImage(product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                                                      bandProduct.getSceneRasterWidth(), bandProduct.getSceneRasterHeight(),
                                                      bandProduct.getBandAt(0).getSourceImage());
                band.setSourceImage(image);
            }
        }
    }

    private static RenderedOp createScaledImage(int targetWidth, int targetHeight, int sourceWidth, int sourceHeight, RenderedImage srcImg) {
        float xScale = (float) targetWidth / (float) sourceWidth;
        float yScale = (float) targetHeight / (float) sourceHeight;
        RenderedOp tempImg = ScaleDescriptor.create(srcImg, xScale, yScale, 0.5f, 0.5f,
                                                    Interpolation.getInstance(Interpolation.INTERP_NEAREST), null);
        return CropDescriptor.create(tempImg, 0f, 0f, (float) targetWidth, (float) targetHeight, null);
    }

    private boolean isMsslHcProduct(String fileName) {
        final Pattern pattern = Pattern.compile("[0-9]{4}nm_");
        Matcher matcher = pattern.matcher(fileName.substring(0, 7));
        return matcher.matches();
    }

    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        // all bands use source images as source for its data
        throw new IllegalStateException();
    }

    @Override
    public void close() throws IOException {
        for (Product bandProduct : bandProducts) {
            bandProduct.closeIO();
        }
        bandProducts.clear();
        input.close();
        input = null;
        super.close();
    }
}
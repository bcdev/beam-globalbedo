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

package org.esa.beam.dataio.avhrr;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.object.*;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.ImageUtils;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Product reader responsible for reading MODIS MOD35 cloud mask HDF product.
 *
 * @author Olaf Danne
 */
public class AvhrrLtdrProductReader extends AbstractProductReader {

    private String inputFilePath;
    private String filename;

    private static final int productWidth = 7200;
    private static final int productHeight = 3600;

    public AvhrrLtdrProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inFile = getInputFile();
        inputFilePath = inFile.getPath();
        filename = inFile.getName();

        Product ltdrProduct = null;

        FileFormat h4FileFormat = FileFormat.getFileFormat(FileFormat.FILE_TYPE_HDF4);
        if (h4FileFormat == null) {
            System.err.println("Cannot find HDF4 FileFormat.");
            return null;
        }

        final FileFormat h4File;
        try {
            h4File = h4FileFormat.createInstance(inputFilePath, FileFormat.READ);
            h4File.open();

            final TreeNode rootNode = h4File.getRootNode();

            ltdrProduct = createTargetProduct(inFile, rootNode);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 7. add geocoding to final product (always global, 7200x3600)     todo
//        GeoCoding gc = new CrsGeoCoding()

        // 8. extract properly the cloud and quality info from the single bits of the     todo if needed
        // Cloud_Mask and Quality_Assurance bytes. Also, create corresponding bit masks for Visat
//        Mod35BitMaskUtils.attachPixelClassificationFlagBand(finalProduct);
//        Mod35BitMaskUtils.attachQualityAssuranceFlagBand(finalProduct);

        // return the final product
        return ltdrProduct;
    }

    private Product createTargetProduct(File inputFile, TreeNode inputFileRootNode) throws Exception {

        Product product = null;
        if (inputFileRootNode != null) {
            product = new Product(inputFile.getName(), "AVHRR LTDR", productWidth, productHeight);

            final TreeNode gridNode = inputFileRootNode.getChildAt(0);        // 'Grid'

            final Group rootGroup = (Group) ((DefaultMutableTreeNode) inputFileRootNode).getUserObject();
            final java.util.List rootMetadata = rootGroup.getMetadata();
            addLtdrMetadataElement(rootMetadata, product, "MPH");
            product.setDescription("AVHRR LTDR land surface product");
            product.setFileLocation(inputFile);

            final TreeNode dataFieldsNode = gridNode.getChildAt(0);        // 'Data Fields'

            for (int i = 0; i < dataFieldsNode.getChildCount(); i++) {
                // we have: 'TOA_REFL_CH1', 'TOA_REFL_CH2', 'SZEN', 'VZEN', 'RELAZ', 'TIME', 'QA'
                final TreeNode dataFieldsChildNode = dataFieldsNode.getChildAt(i);
                final String dataFieldsChildNodeName = dataFieldsChildNode.toString();

                final Group dataFieldsChildGroup = (Group) ((DefaultMutableTreeNode) dataFieldsNode).getUserObject();
                if (dataFieldsChildNodeName.startsWith("TOA_REFL")) {
                    //  'TOA_REFL_CH1', 'TOA_REFL_CH2'
                    final HObject dataFieldsChildGroupMember = dataFieldsChildGroup.getMemberList().get(i);
                    final List childMetadata = dataFieldsChildGroupMember.getMetadata();
                    final Band toaBand = createTargetBand(product,
                                                          childMetadata,
                                                          dataFieldsChildNodeName,
                                                          ProductData.TYPE_INT16);


                    Dataset dataset = (Dataset) dataFieldsChildGroupMember;
                    final ProductData productData = ProductData.createInstance(new short[productWidth * productHeight]);
                    productData.setElems(dataset.read());

                    final RenderedImage radiometryImage = ImageUtils.createRenderedImage(productWidth,
                                                                                         productHeight,
                                                                                         productData);
                    toaBand.setSourceImage(radiometryImage);
                } else if (dataFieldsChildNodeName.equals("QA")) {
                    // quality flag
                    // todo: check if needed
                }
            }
        }
        return product;
    }

    private Band createTargetBand(Product product, List metadata, String bandName, int dataType) throws Exception {
        final float scaleFactorAttr = AvhrrLtdrUtils.getFloatAttributeValue(metadata, "scale_factor");
        final float scaleFactor = Float.isNaN(scaleFactorAttr) ? 1.0f : scaleFactorAttr;
        final float scaleOffsetAttr = AvhrrLtdrUtils.getFloatAttributeValue(metadata, "add_offset");
        final float scaleOffset = Float.isNaN(scaleOffsetAttr) ? 0.0f : scaleOffsetAttr;
        final Band band = product.addBand(bandName, dataType);
        band.setScalingFactor(scaleFactor);
        band.setScalingOffset(-1.0 * scaleOffset / scaleFactor);
        band.setNoDataValue(-1.0*scaleFactor);         // todo!
        band.setNoDataValueUsed(true);

        return band;
    }


    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, int sourceStepX, int sourceStepY, Band destBand, int destOffsetX, int destOffsetY, int destWidth, int destHeight, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        throw new IllegalStateException(String.format("No source to read from for band '%s'.", destBand.getName()));
    }

    private File getInputFile() {
        File inFile;
        if (getInput() instanceof String) {
            inFile = new File((String) getInput());
        } else if (getInput() instanceof File) {
            inFile = (File) getInput();
        } else {
            throw new IllegalArgumentException("unsupported input source: " + getInput());
        }
        return inFile;
    }

    private void addLtdrMetadataElement(List<Attribute> rootMetadata,
                                             final Product product,
                                             String metadataElementName) {
        final MetadataElement metadataElement = new MetadataElement(metadataElementName);

        for (Attribute attribute : rootMetadata) {
            metadataElement.addAttribute(new MetadataAttribute(attribute.getName(),
                                                               ProductData.createInstance(AvhrrLtdrUtils.getAttributeValue(attribute)), true));
        }
        product.getMetadataRoot().addElement(metadataElement);
    }

}


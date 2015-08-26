package org.esa.beam.dataio.avhrr;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.object.*;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.util.ImageUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * Product reader responsible for reading AVHRR LTDR HDF product.
 *
 * @author Olaf Danne
 */
public class AvhrrLtdrProductReader extends AbstractProductReader {

    private String inputFilePath;

    private static final int productWidth = 7200;
    private static final int productHeight = 3600;
    private GeoCoding geoCoding;

    public AvhrrLtdrProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    @Override
    protected Product readProductNodesImpl() throws IOException {
        final File inFile = getInputFile();
        inputFilePath = inFile.getPath();

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

        return ltdrProduct;
    }

    private Product createTargetProduct(File inputFile, TreeNode inputFileRootNode) throws Exception {

        Product product = null;
        if (inputFileRootNode != null) {
            product = new Product(inputFile.getName(), "AVHRR LTDR", productWidth, productHeight);
            product.setDescription("AVHRR LTDR land surface product");
            product.setFileLocation(inputFile);

            // add geocoding to product (always global lat/lon, 7200x3600 pixel)
            final CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
            final double easting = -180.0;
            final double northing = 90.0;
            final double pixelSizeX = 0.05;
            final double pixelSizeY = 0.05;
            geoCoding = new CrsGeoCoding(crs, productWidth, productHeight, easting, northing, pixelSizeX, pixelSizeY);
            product.setGeoCoding(geoCoding);

            product.setAutoGrouping("TOA_REFL");

            final TreeNode gridNode = inputFileRootNode.getChildAt(0);        // 'Grid'

            final Group rootGroup = (Group) ((DefaultMutableTreeNode) inputFileRootNode).getUserObject();
            final java.util.List rootMetadata = rootGroup.getMetadata();
            addLtdrMetadataElement(rootMetadata, product, "MPH");

            // add start/end time to product:
            addStartStopTimes(product, rootMetadata);

            final TreeNode dataFieldsNode = gridNode.getChildAt(0);        // 'Data Fields'

            for (int i = 0; i < dataFieldsNode.getChildCount(); i++) {
                // we have: 'TOA_REFL_CH1', 'TOA_REFL_CH2', 'SZEN', 'VZEN', 'RELAZ', 'TIME', 'QA'
                // but so far we only need  'TOA_REFL_CH1', 'TOA_REFL_CH2', 'QA'
                // --> only read those for performance reasons
                // 20150611: we will also need 'SZEN', 'VZEN', 'RELAZ' !!
                final TreeNode dataFieldsChildNode = dataFieldsNode.getChildAt(i);
                final String dataFieldsChildNodeName = dataFieldsChildNode.toString();

                final Group dataFieldsChildGroup = (Group) ((DefaultMutableTreeNode) dataFieldsNode).getUserObject();
                final HObject dataFieldsChildGroupMember = dataFieldsChildGroup.getMemberList().get(i);
                final List childMetadata = dataFieldsChildGroupMember.getMetadata();
                Dataset dataset = (Dataset) dataFieldsChildGroupMember;
                if (dataFieldsChildNodeName.contains("REFL_CH") || dataFieldsChildNodeName.equals("QA") ||
                        dataFieldIsAngle(dataFieldsChildNodeName)) {
                    final Band ltdrBand = createTargetBand(product,
                                                          childMetadata,
                                                          dataFieldsChildNodeName,
                                                          ProductData.TYPE_INT16);

                    final ProductData productData = ProductData.createInstance(new short[productWidth * productHeight]);
                    productData.setElems(dataset.read());

                    final RenderedImage ltdrImage = ImageUtils.createRenderedImage(productWidth,
                                                                                         productHeight,
                                                                                         productData);
                    ltdrBand.setSourceImage(ltdrImage);

                    if (dataFieldsChildNodeName.contains("REFL_CH")) {
                        if (dataFieldsChildNodeName.startsWith("TOA_REFL_CH")) {
                            ltdrBand.setDescription("AVHRR TOA reflectance at " +
                                                            AvhrrLtdrConstants.WAVELENGTHS[i] + " nm");
                        } else {
                            ltdrBand.setDescription("AVHRR SURFACE reflectance at " +
                                                            AvhrrLtdrConstants.WAVELENGTHS[i] + " nm");
                        }
                        ltdrBand.setUnit("dl");
                        ltdrBand.setSpectralBandIndex(i);
                        ltdrBand.setSpectralWavelength(AvhrrLtdrConstants.WAVELENGTHS[i]);
                        ltdrBand.setSpectralBandwidth(AvhrrLtdrConstants.BANDWIDTHS[i]);
                    } else if (dataFieldIsAngle(dataFieldsChildNodeName)) {
                        ltdrBand.setDescription(AvhrrLtdrUtils.getStringAttributeValue(childMetadata, "long name"));
                        ltdrBand.setUnit("deg");
                    } else {
                        ltdrBand.setName(AvhrrLtdrConstants.QA_FLAG_BAND_NAME);
                        ltdrBand.setDescription("AVHRR LTDR quality flag band");
                        ltdrBand.setUnit("dl");
                        FlagCoding ltdrQaFlagCoding = new FlagCoding(AvhrrLtdrConstants.QA_FLAG_BAND_NAME);
                        AvhrrLtdrUtils.addQualityFlags(ltdrQaFlagCoding);
                        AvhrrLtdrUtils.addQualityMasks(product);
                        product.getFlagCodingGroup().add(ltdrQaFlagCoding);
                        ltdrBand.setSampleCoding(ltdrQaFlagCoding);
                    }
                }
            }
        }
        return product;
    }

    private boolean dataFieldIsAngle(String dataFieldsChildNodeName) {
        return dataFieldsChildNodeName.equals("SZEN") || dataFieldsChildNodeName.equals("VZEN") ||
                dataFieldsChildNodeName.equals("RELAZ");
    }

    private Band createTargetBand(Product product, List metadata, String bandName, int dataType) throws Exception {
        final float scaleFactorAttr = AvhrrLtdrUtils.getFloatAttributeValue(metadata, "scale_factor");
        final float scaleFactor = Float.isNaN(scaleFactorAttr) ? 1.0f : scaleFactorAttr;
        final float scaleOffsetAttr = AvhrrLtdrUtils.getFloatAttributeValue(metadata, "add_offset");
        final float scaleOffset = Float.isNaN(scaleOffsetAttr) ? 0.0f : scaleOffsetAttr;
        final Band band = product.addBand(bandName, dataType);
        band.setScalingFactor(scaleFactor);
        band.setScalingOffset(-1.0 * scaleOffset / scaleFactor);

        return band;
    }

    private void addStartStopTimes(Product product, List timeMetadata) throws ParseException {
        product.setStartTime(ProductData.UTC.parse(AvhrrLtdrUtils.getStartEndTimeFromAttributes(timeMetadata)[0],
                                                   AvhrrLtdrConstants.AVHRR_LTDR_DATE_FORMAT_PATTERN));
        product.setEndTime(ProductData.UTC.parse(AvhrrLtdrUtils.getStartEndTimeFromAttributes(timeMetadata)[1],
                                                 AvhrrLtdrConstants.AVHRR_LTDR_DATE_FORMAT_PATTERN));
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


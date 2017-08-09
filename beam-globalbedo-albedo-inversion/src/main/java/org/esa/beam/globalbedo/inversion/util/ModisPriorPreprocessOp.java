package org.esa.beam.globalbedo.inversion.util;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.globalbedo.inversion.AlbedoInversionConstants;
import org.esa.beam.gpf.operators.standard.SubsetOp;
import org.esa.beam.util.logging.BeamLogManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.esa.beam.globalbedo.inversion.AlbedoInversionConstants.NUM_ALBEDO_PARAMETERS;

/**
 * Operator for preprocessing of MODIS priors:
 * - Add tile geocoding
 * - Reproject from source to target resolution (e.g. 1200x1200 --> 200x200 for AVHRR/GEO processing)
 * - Band subsetting (e.g. get rid of spectral bands for AVHRR/GEO processing)
 * <p/>
 * The purpose is to reduce I/O as far as possible when using the Priors in BRDF/Albedo processing
 * NOTE: We assume the bands and format of the Prior version in
 *       /group_workspaces/cems2/qa4ecv/vol1/prior.c6/stage2/1km (SK 20170531)
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.modis.prior.preprocess")
public class ModisPriorPreprocessOp extends Operator {

    @Parameter(defaultValue = "BRDF_Albedo_Parameters_",
            description = "Prefix of prior mean band (default fits to the latest prior version)")
    private String priorMeanBandNamePrefix;

    @Parameter(defaultValue = "BRDF_Albedo_Parameters_",
            description = "Prefix of prior SD band (default fits to the latest prior version)")
    private String priorSdBandNamePrefix;

    @Parameter(description = "MODIS tile")
    private String tile;

    @Parameter(description = "DoY", interval = "[1,366]")
    private int doy;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Parameter(defaultValue = "6.0",
            valueSet = {"0.5", "1.0", "4.0", "6.0"},
            description = "Scale factor with regard to MODIS default 1200x1200. Values > 1.0 reduce product size." +
                    "Should usually be set to 6.0 for AVHRR/GEO (tiles of 200x200).")
    protected double scaleFactor;

    @Parameter(description = "The index of the spectral bands to be added to band subset", interval = "[1,7]")
    private int spectralBandSubsetIndex;

    @Parameter(defaultValue = "true", description = "Add broadband related bands to band subset")
    private boolean addBroadbandsToSubset;


    @SourceProduct(description = "The Prior product to preprocess")
    private Product priorProduct;

    @Override
    public void initialize() throws OperatorException {
        Logger logger = BeamLogManager.getSystemLogger();

        Product rescaledPriorProduct = null;
        priorProduct.setGeoCoding(IOUtils.getSinusoidalTileGeocoding(tile));
        if (scaleFactor != 1.0) {
            rescaledPriorProduct = AlbedoInversionUtils.reprojectToModisTile(priorProduct,
                                                                             tile,
                                                                             "Nearest",
                                                                             scaleFactor);
        } else {
            rescaledPriorProduct = priorProduct;
        }

        if (rescaledPriorProduct != null) {
            // create band subset
            final int subsetWidth = rescaledPriorProduct.getSceneRasterWidth();
            final int subsetHeight = rescaledPriorProduct.getSceneRasterHeight();
            final Rectangle subsetRegion = new Rectangle(0, 0, subsetWidth, subsetHeight);

            Map<String, Object> rescaledPriorSubsetParams = new HashMap<>();
            rescaledPriorSubsetParams.put("region", subsetRegion);
            rescaledPriorSubsetParams.put("copyMetadata", true);
            rescaledPriorSubsetParams.put("bandNames", getSubsetBandnames());
            Product rescaledPriorSubsetProduct = GPF.createProduct(OperatorSpi.getOperatorAlias(SubsetOp.class),
                                                                   rescaledPriorSubsetParams, rescaledPriorProduct);

            setTargetProduct(rescaledPriorSubsetProduct);
        }

        logger.log(Level.INFO, "Finished Prior preprocessing for tile: " + tile + ", DoY: " +
                IOUtils.getDoyString(doy) + " , Snow = " + computeSnow);
    }

    private String[] getSubsetBandnames() {
        List<String> subsetbandNamesList = new ArrayList<>();

        subsetbandNamesList.add("snowFraction");
        subsetbandNamesList.add("landWaterType");

        if (addBroadbandsToSubset) {
            for (int i = 0; i < AlbedoInversionConstants.PRIOR_6_BB_BANDS.length; i++) {
                subsetbandNamesList.add(priorMeanBandNamePrefix + AlbedoInversionConstants.PRIOR_6_BB_BANDS[i] + "_wns");
                for (int j = 0; j < NUM_ALBEDO_PARAMETERS; j++) {
                    final String meanBandName = priorMeanBandNamePrefix +
                            AlbedoInversionConstants.PRIOR_6_BB_BANDS[i] + "_f" + j + "_avr";
                    final String sdBandName = priorMeanBandNamePrefix +
                            AlbedoInversionConstants.PRIOR_6_BB_BANDS[i] + "_f" + j + "_sd";
                    subsetbandNamesList.add(meanBandName);
                    subsetbandNamesList.add(sdBandName);
                }
            }
        }

        if (spectralBandSubsetIndex > 0) {
            subsetbandNamesList.add(priorMeanBandNamePrefix + "Band" + spectralBandSubsetIndex + "_wns");
            for (int j = 0; j < NUM_ALBEDO_PARAMETERS; j++) {
                final String meanBandName = priorMeanBandNamePrefix + "Band" + spectralBandSubsetIndex
                        + "_f" + j + "_avr";
                final String sdBandName = priorMeanBandNamePrefix + "Band" + spectralBandSubsetIndex
                        + "_f" + j + "_sd";
                subsetbandNamesList.add(meanBandName);
                subsetbandNamesList.add(sdBandName);
            }
        }

        return subsetbandNamesList.toArray(new String[subsetbandNamesList.size()]);
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ModisPriorPreprocessOp.class);
        }
    }
}

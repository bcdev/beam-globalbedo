/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.operators;

import com.bc.ceres.core.ProgressMonitor;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.BorderExtender;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.globalbedo.sdr.lutUtils.Aardvarc4DLut;
import org.esa.beam.globalbedo.sdr.lutUtils.MomoLut;
import org.esa.beam.gpf.operators.standard.BandMathsOp;
import org.esa.beam.util.BitSetter;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.RsMathUtils;

/**
 * Generates currently 2 bands: aot and aot_err on lower spatial resolution
 * rasterSize of target is 1/scale of source
 *
 *
 * TODO: implement IDEPIX flags (done for clear land)
 * TODO: implement treatment of wvCol from VGT
 * TODO: should add flag band with QA infos about aot retrieval
 * TODO: getSourceTiles, readAllValues and readAve/DarkestPixel shouldn't depend on order of band names defined in init
 *       !!! Potential error source !!!!
 * TODO: (MINOR) ozone should always be atm.cm not DU
 * TODO: snow pixels should be treated separatly
 * TODO: add new surf specs for spectral Approach over desert and snow
 * TODO: AERONET output
 *
 * @author akheckel
 *
 */
@OperatorMetadata(alias = "ga.AerosolOp2",
                  description = "Computes aerosol optical thickness",
                  authors = "Andreas Heckel",
                  version = "1.1",
                  copyright = "(C) 2010 by University Swansea (a.heckel@swansea.ac.uk)")
public class AerosolOp2 extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;
/*
    //@Parameter(defaultValue="false")
    private boolean saveSdrBands = false;
    //@Parameter(defaultValue="false")
    private boolean saveModelBands = false;
    //@Parameter(defaultValue="surface_reflectance_spec.asc")
 */
    private String SurfaceSpecName = "surface_reflectance_spec.asc";
    @Parameter(defaultValue="2")
    private int vegSpecId;

    private String productName;
    private String productType;

    private InstrumentConsts instrC;
    private String instrument;

    private String[] specBandNames;
    private String[] geomBandNames;
    private String[] auxBandNames;
    private String surfPresName;
    private String ozoneName;
    private String validName;
    private String ndviName;

    private float wvCol = 2.5f;

    @Parameter(defaultValue="9")
    private int scale;
    private int offset;
    private int srcRasterWidth;
    private int srcRasterHeight;
    private int tarRasterWidth;
    private int tarRasterHeight;
    private int nSpecWvl;
    private float[][] specWvl;
    private double[] soilSurfSpec;
    private double[] vegSurfSpec;
    private double[] specWeights;
    private MomoLut momo;
    private Band validBand;
    private Aardvarc4DLut aardvarcLut;
    private boolean useAardvarcLut = false;
    private BorderExtender borderExt;
    private Rectangle pixelWindow;
    private float ndviTheshold;

    private boolean addFitBands = false;

    @Override
    public void initialize() throws OperatorException {
        offset = 4;
        productName = sourceProduct.getName() + "_AOT";
        productType = sourceProduct.getProductType() + "_AOT";
        initRasterDimensions(sourceProduct, scale);
        instrC = InstrumentConsts.getInstance();
        instrument = instrC.getInstrument(sourceProduct);

        geomBandNames = instrC.getGeomBandNames(instrument);
        specBandNames = instrC.getSpecBandNames(instrument);
        ozoneName = instrC.getOzoneName(instrument);
        surfPresName = instrC.getSurfPressureName(instrument);
        ndviName = instrC.getNdviName();
        
        final String validExpression = instrC.getValidExpression(instrument);
        final BandMathsOp validBandOp = BandMathsOp.createBooleanExpressionBand(validExpression, sourceProduct);
        validBand = validBandOp.getTargetProduct().getBandAt(0);
        validName = validBand.getName();

        auxBandNames = new String[]{surfPresName, ozoneName, validName, ndviName};

        specWeights = instrC.getSpectralFitWeights(instrument);
        specWvl = getSpectralWvl(specBandNames);
        nSpecWvl = specWvl[0].length;
        readSurfaceSpectra(SurfaceSpecName);

        if (!sourceProduct.containsRasterDataNode(ozoneName)){
            createConstOzoneBand(0.35f);
        }
        if (!sourceProduct.containsBand(ndviName)){
            createNdviBand();
        }
        ndviTheshold = 0.1f;

        readLookupTable();

        borderExt = BorderExtender.createInstance(BorderExtender.BORDER_COPY);
        pixelWindow = new Rectangle(0, 0, scale, scale);

        createTargetProduct();
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Rectangle srcRec = getSourceRectangle(targetRectangle, pixelWindow);

        if (! containsTileValidData(srcRec)){
            setInvalidTargetSamples(targetTiles);
            return;
        }

        Map<String,Tile> sourceTiles = getSourceTiles(geomBandNames, srcRec, borderExt, ProgressMonitor.NULL);
        sourceTiles.putAll(getSourceTiles(specBandNames, srcRec, borderExt, ProgressMonitor.NULL));
        sourceTiles.putAll(getSourceTiles(auxBandNames, srcRec, borderExt, ProgressMonitor.NULL));

        Map<String,Double> sourceNoDataValues = getSourceNoDataValues(geomBandNames);
        sourceNoDataValues.putAll(getSourceNoDataValues(specBandNames));
        sourceNoDataValues.putAll(getSourceNoDataValues(auxBandNames));

        int x0 = (int) targetRectangle.getX();
        int y0 = (int) targetRectangle.getY();
        int width = (int) targetRectangle.getWidth() + x0 - 1;
        int height = (int) targetRectangle.getHeight() + y0 - 1;

        for (int iY=y0; iY <= height; iY++) {
            for (int iX=x0; iX <= width; iX++) {
                doPixel(sourceTiles, sourceNoDataValues, iX, iY, targetTiles);
                if (pm.isCanceled()) return;
            }
            pm.worked(1);
        }
        pm.done();
    }



    
    private void doPixel(Map<String, Tile> sourceTiles, Map<String, Double> sourceNoDataValues, int iX, int iY, Map<Band, Tile> targetTiles) {
        // read pixel data and init brent fit
        InputPixelData[] inPixField = null;
        BrentFitFunction brentFitFunction = null;
        if (instrument.equals("AATSR")) {
            inPixField = readAveragePixel(sourceTiles, sourceNoDataValues, iX, iY, pixelWindow);
            if (inPixField != null) {
                brentFitFunction = new BrentFitFunction(BrentFitFunction.ANGULAR_MODEL, inPixField, momo, specWeights);
            }
        } else {
            inPixField = readDarkestNPixels(sourceTiles, sourceNoDataValues, iX, iY, pixelWindow);
            if (inPixField != null) {
                brentFitFunction = new BrentFitFunction(BrentFitFunction.SPECTRAL_MODEL, inPixField, momo, specWeights, soilSurfSpec, vegSurfSpec);
            }
        }
        executeRetrieval(inPixField, brentFitFunction, targetTiles, iX, iY);
    }

    private void executeRetrieval(InputPixelData[] inPixField, BrentFitFunction brentFitFunction, Map<Band, Tile> targetTiles, int iX, int iY) {
        // run retrieval and set target samples
        if (inPixField != null) {
            RetrievalResults result = blubb1(brentFitFunction);
            if (!result.isRetrievalFailed()) {
                setTargetSamples(targetTiles, iX, iY, inPixField[0], result);
            } else {
                setInvalidTargetSamples(targetTiles, iX, iY, inPixField[0]);
            }
        } else {
            setInvalidTargetSamples(targetTiles, iX, iY);
        }
    }

    private RetrievalResults blubb1(BrentFitFunction brentFitFunction) {
        final double maxAOT = brentFitFunction.getMaxAOT();
        final PointRetrieval pR = new PointRetrieval(brentFitFunction);
        RetrievalResults result = pR.runRetrieval(maxAOT);
        return result;
    }





    private InputPixelData createInPixelData(double[] tileValues) {
        PixelGeometry geomNadir = null;
        PixelGeometry geomFward = null;
        double[][] toaRefl = new double[2][nSpecWvl];
        int skip = 0;
        if (instrument.equals("AATSR")) {
            geomNadir = new PixelGeometry(90.0-tileValues[0], tileValues[1], 90.0-tileValues[2], tileValues[3]);
            skip += 4;
            geomFward = new PixelGeometry(90.0-tileValues[skip + 0], tileValues[skip + 1], 90.0-tileValues[skip + 2], tileValues[skip + 3]);
            skip += 4;
        }
        else {
            geomNadir = new PixelGeometry(tileValues[0], tileValues[1], tileValues[2], tileValues[3]);
            geomFward = geomNadir;
            skip += 4;
        }
        for (int i = 0; i < nSpecWvl; i++) {
            toaRefl[0][i] = tileValues[skip + i];
            toaRefl[1][i] = tileValues[skip + i];
        }
        skip += nSpecWvl;
        if (instrument.equals("AATSR")) {
            for (int i = 0; i < nSpecWvl; i++) {
                toaRefl[0][i] = RsMathUtils.radianceToReflectance((float) toaRefl[0][i], geomFward.sza, (float) Math.PI) * 0.01f;
                toaRefl[1][i] = tileValues[skip + i];
                toaRefl[1][i] = RsMathUtils.radianceToReflectance((float) toaRefl[1][i], geomFward.sza, (float) Math.PI) * 0.01f;
            }
            skip += nSpecWvl;
        }
        double surfP = Math.min(tileValues[skip], 1013.25);
        double o3DU = ensureO3DobsonUnits(tileValues[skip + 1]);
        InputPixelData ipd = new InputPixelData(geomNadir, geomFward, surfP, o3DU, this.wvCol, specWvl[0], toaRefl[0], toaRefl[1]);
        return ipd;
    }

    private void createTargetProduct() {
        targetProduct = new Product(productName, productType, tarRasterWidth, tarRasterHeight);
        createTargetProductBands();
        setTargetProduct(targetProduct);
    }

    private void createTargetProductBands() {
        Band targetBand = GaHelper.getInstance().createTargetBand(AotConsts.aot, tarRasterWidth, tarRasterHeight);
        //targetBand.setValidPixelExpression(instrC.getValidExpression(instrument));
        targetProduct.addBand(targetBand);

        targetBand = GaHelper.getInstance().createTargetBand(AotConsts.aotErr, tarRasterWidth, tarRasterHeight);
        targetBand.setValidPixelExpression(instrC.getValidExpression(instrument));
        targetProduct.addBand(targetBand);

        if (addFitBands){
            targetBand = new Band("fit_err", ProductData.TYPE_FLOAT32, tarRasterWidth, tarRasterHeight);
            targetBand.setDescription("aot uncertainty");
            targetBand.setNoDataValue(-1);
            targetBand.setNoDataValueUsed(true);
            targetBand.setValidPixelExpression("");
            targetBand.setUnit("dl");
            targetProduct.addBand(targetBand);

            targetBand = new Band("fit_curv", ProductData.TYPE_FLOAT32, tarRasterWidth, tarRasterHeight);
            targetBand.setDescription("aot uncertainty");
            targetBand.setNoDataValue(-1);
            targetBand.setNoDataValueUsed(true);
            targetBand.setValidPixelExpression("");
            targetBand.setUnit("dl");
            targetProduct.addBand(targetBand);
        }
    }

    private Rectangle getSourceRectangle(Rectangle targetRectangle, Rectangle pixelWindow) {
        return new Rectangle(targetRectangle.x * pixelWindow.width + pixelWindow.x,
                             targetRectangle.y * pixelWindow.height + pixelWindow.y,
                             targetRectangle.width * pixelWindow.width,
                             targetRectangle.height * pixelWindow.height);
    }

    private void initRasterDimensions(Product sourceProduct, int scale) {
        srcRasterHeight = sourceProduct.getSceneRasterHeight();
        srcRasterWidth  = sourceProduct.getSceneRasterWidth();
        tarRasterHeight = srcRasterHeight / scale;
        tarRasterWidth  = srcRasterWidth  / scale;
    }

    private Map<String, Tile> getSourceTiles(String[] bandNames, Rectangle srcRec, BorderExtender borderExt, ProgressMonitor pm) {
        Map<String, Tile> tileMap = new HashMap<String, Tile>(bandNames.length);
        for (String name : bandNames) {
            RasterDataNode b = (name.equals(validName)) ? validBand : sourceProduct.getRasterDataNode(name);
            tileMap.put(name, getSourceTile(b, srcRec, borderExt, ProgressMonitor.NULL));
        }
        return tileMap;
    }

    private Map<String, Double> getSourceNoDataValues(String[] bandNames) {
        Map<String, Double> noDataMap = new HashMap<String, Double>(bandNames.length);
        for (String name : bandNames) {
            RasterDataNode b = (name.equals(validName)) ? validBand : sourceProduct.getRasterDataNode(name);
            noDataMap.put(name, b.getGeophysicalNoDataValue());
        }
        return noDataMap;
    }

    private InputPixelData[] readAveragePixel(Map<String, Tile> sourceTiles, Map<String,Double> sourceNoData, int iX, int iY, Rectangle pixelWindow) {
        InputPixelData[] inPixField = null;
        boolean valid = false;
        double[] tileValues = new double[sourceTiles.size()];
        double[] sumVal = new double[sourceTiles.size()];
        for (int k=0; k<sumVal.length; k++) sumVal[k] = 0;
        int nAve = 0;
        int xOffset = iX*pixelWindow.width + pixelWindow.x;
        int yOffset = iY*pixelWindow.height + pixelWindow.y;
        for (int y = yOffset; y < yOffset+pixelWindow.height; y++){
            for (int x = xOffset; x < xOffset+pixelWindow.width; x++){
                valid = sourceTiles.get(validName).getSampleBoolean(x, y);
                if (valid) {
                    valid = valid && readAllValues(x, y, sourceTiles, sourceNoData, tileValues);
                    if (valid){
                        addToSumArray(tileValues, sumVal);
                        nAve++;
                    }
                }
            }
        }
        if (nAve > 0.95*pixelWindow.width*pixelWindow.height){
            for (int i=0; i<sumVal.length; i++) sumVal[i] /= nAve;
            InputPixelData ipd = createInPixelData(sumVal);
            if (momo.isInsideLut(ipd)) {
                inPixField = new InputPixelData[]{ipd};
            }
        }
        return inPixField;
    }

    private InputPixelData[] readDarkestNPixels(Map<String, Tile> sourceTiles, Map<String,Double> sourceNoData, int iX, int iY, Rectangle pixelWindow) {
        int NPixel = 10;
        ArrayList<InputPixelData> inPixelList = new ArrayList<InputPixelData>(pixelWindow.height*pixelWindow.width);
        InputPixelData[] inPixField = null;
        boolean valid = false;
        float ndvi = 0;
        float[] ndviArr = new float[pixelWindow.height*pixelWindow.width];

        double[] tileValues = new double[sourceTiles.size()];
        double[] sumVal = new double[sourceTiles.size()];
        for (int k=0; k<sumVal.length; k++) sumVal[k] = 0;

        int xOffset = iX*pixelWindow.width + pixelWindow.x;
        int yOffset = iY*pixelWindow.height + pixelWindow.y;
        for (int y = yOffset; y < yOffset+pixelWindow.height; y++){
            for (int x = xOffset; x < xOffset+pixelWindow.width; x++){
                valid = sourceTiles.get(validName).getSampleBoolean(x, y);
                ndviArr[(y-yOffset)*pixelWindow.width+(x-xOffset)]  = (valid) ? sourceTiles.get(ndviName).getSampleFloat(x, y) : -1;
            }
        }
        Arrays.sort(ndviArr);
        if (ndviArr[ndviArr.length-1-NPixel] > ndviTheshold){
            for (int y = yOffset; y < yOffset+pixelWindow.height; y++){
                for (int x = xOffset; x < xOffset+pixelWindow.width; x++){
                    valid = sourceTiles.get(validName).getSampleBoolean(x, y);
                    ndvi  = sourceTiles.get(ndviName).getSampleFloat(x, y);
                    if (valid && (ndvi >= ndviArr[ndviArr.length-1-NPixel])) {
                        valid = valid && readAllValues(x, y, sourceTiles, sourceNoData, tileValues);
                        InputPixelData ipd = createInPixelData(tileValues);
                        if (valid && momo.isInsideLut(ipd)){
                            inPixelList.add(ipd);
                        }
                    }
                }
            }
            if (inPixelList.size() > 0) {
                inPixField = new InputPixelData[inPixelList.size()];
                inPixelList.toArray(inPixField);
            }
        }
        return inPixField;
    }

    private void setTargetSamples(Map<Band, Tile> targetTiles, int iX, int iY, InputPixelData ipd, RetrievalResults result) {
        targetTiles.get(targetProduct.getBand("aot")).setSample(iX, iY, result.getOptAOT());
        targetTiles.get(targetProduct.getBand("aot_err")).setSample(iX, iY, result.getRetrievalErr());
        if (addFitBands){
            targetTiles.get(targetProduct.getBand("fit_err")).setSample(iX, iY, result.getOptErr());
            targetTiles.get(targetProduct.getBand("fit_curv")).setSample(iX, iY, result.getCurvature());
        }
    }

    private void setInvalidTargetSamples(Map<Band, Tile> targetTiles, int iX, int iY, InputPixelData ipd) {
        targetTiles.get(targetProduct.getBand("aot")).setSample(iX, iY, targetProduct.getBand("aot").getGeophysicalNoDataValue());
        targetTiles.get(targetProduct.getBand("aot_err")).setSample(iX, iY, targetProduct.getBand("aot_err").getGeophysicalNoDataValue());
        if (addFitBands){
            targetTiles.get(targetProduct.getBand("fit_err")).setSample(iX, iY, targetProduct.getBand("fit_err").getGeophysicalNoDataValue());
            targetTiles.get(targetProduct.getBand("fit_curv")).setSample(iX, iY, targetProduct.getBand("fit_curv").getGeophysicalNoDataValue());
        }
    }

    private void setInvalidTargetSamples(Map<Band, Tile> targetTiles, int iX, int iY) {
        for (Tile t : targetTiles.values()){
            t.setSample(iX, iY, t.getRasterDataNode().getNoDataValue());
        }
    }

    private void setInvalidTargetSamples(Map<Band, Tile> targetTiles) {
        for (Tile.Pos pos : targetTiles.get(targetProduct.getBandAt(0))){
            setInvalidTargetSamples(targetTiles, pos.x, pos.y);
        }
    }

    private void createConstOzoneBand(float ozonAtmCm) {
        String ozoneExpr = String.format("%5.3f", ozonAtmCm);
        VirtualBand ozoneBand = new VirtualBand(ozoneName, ProductData.TYPE_FLOAT32, srcRasterWidth, srcRasterHeight, ozoneExpr);
        ozoneBand.setDescription("constant ozone band");
        ozoneBand.setNoDataValue(0);
        ozoneBand.setNoDataValueUsed(true);
        ozoneBand.setUnit("atm.cm");
        sourceProduct.addBand(ozoneBand);

    }

    private void createNdviBand() {
        VirtualBand ndviBand = new VirtualBand(ndviName, ProductData.TYPE_FLOAT32,
                                   srcRasterWidth, srcRasterHeight,
                                   instrC.getNdviExpression(instrument));
        sourceProduct.addBand(ndviBand);
    }

    private void readSurfaceSpectra(String fname) {
        Guardian.assertNotNull("specWvl", specWvl);
        final InputStream inputStream = AerosolOp2.class.getResourceAsStream(fname);
        Guardian.assertNotNull("surface spectra InputStream", inputStream);
        BufferedReader reader = null;
        reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        float[] fullWvl  = new float[10000];
        float[] fullSoil = new float[10000];
        float[] fullVeg  = new float[10000];
        int nWvl = 0;
        try {
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!(line.isEmpty() || line.startsWith("#") || line.startsWith("*"))) {
                    String[] stmp = line.split("[ \t]+");
                    fullWvl[nWvl] = Float.valueOf(stmp[0]);
                    if (fullWvl[nWvl] < 100) fullWvl[nWvl] *= 1000; // conversion from um to nm
                    fullSoil[nWvl] = Float.valueOf(stmp[1]);
                    fullVeg[nWvl] = Float.valueOf(stmp[this.vegSpecId]);
                    nWvl++;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(AerosolOp2.class.getName()).log(Level.SEVERE, null, ex);
            throw new OperatorException(ex.getMessage(), ex.getCause());
        }

        soilSurfSpec = new double[nSpecWvl];
        vegSurfSpec  = new double[nSpecWvl];
        int j = 0;
        for (int i=0; i<nSpecWvl; i++) {
            float wvl = specWvl[0][i];
            float width = specWvl[1][i];
            int count = 0;
            while (j < nWvl && fullWvl[j] < wvl-width/2) j++;
            if (j == nWvl) throw new OperatorException("wavelength not found reading surface spectra");
            while (fullWvl[j] < wvl+width/2) {
                soilSurfSpec[i] += fullSoil[j];
                vegSurfSpec[i]  += fullVeg[j];
                count++; j++;
            }
            if (j == nWvl) throw new OperatorException("wavelength window exceeds surface spectra range");
            if (count > 0) {
                soilSurfSpec[i] /= count;
                vegSurfSpec[i]  /= count;
            }
        }
    }

    private void readLookupTable() {
        String lutName = InstrumentConsts.getInstance().getLutName(instrument);
        int nLutBands = InstrumentConsts.getInstance().getnLutBands(instrument);
        momo = new MomoLut(lutName, nLutBands);
        if (useAardvarcLut){
            Guardian.assertEquals("instrument", instrument, "AATSR");
            lutName = "e:/projects/Synergy/wgrey/AATSR/src/aardvarc/aardvarc_v1/LUT6S/LUT6s_1_4D";
            aardvarcLut = new Aardvarc4DLut(lutName);
        }
    }

    private float[][] getSpectralWvl(String[] bandNames) {
        int nBands = (instrument.equals("AATSR")) ? 4 : bandNames.length;
        float[][] wvl = new float[2][nBands];
        for (int i=0; i<nBands; i++) {
            wvl[0][i] = sourceProduct.getBand(bandNames[i]).getSpectralWavelength();
            wvl[1][i] = sourceProduct.getBand(bandNames[i]).getSpectralBandwidth();
        }
        return wvl;
    }

    private boolean readAllValues(int x, int y, Map<String, Tile> sourceTiles, Map<String, Double> sourceNoData, double[] tileValues) {
        boolean valid = true;
        for (int i=0; i<geomBandNames.length; i++){
            tileValues[i] = sourceTiles.get(geomBandNames[i]).getSampleDouble(x, y);
            valid = valid && (sourceNoData.get(geomBandNames[i]).compareTo(tileValues[i]) != 0);

        }
        int skip = geomBandNames.length;
        for (int i=0; i<specBandNames.length; i++){
            tileValues[i+skip] = sourceTiles.get(specBandNames[i]).getSampleDouble(x, y);
            valid = valid && (sourceNoData.get(specBandNames[i]).compareTo(tileValues[i+skip]) != 0);

        }
        skip += specBandNames.length;
/*
        // NADIR geometry
        for (int i=0; i < 4; i++){
            tileValues[i] = sourceTiles.get(geomBandNames[i]).getSampleDouble(x, y);
            valid = valid && (sourceNoData.get(geomBandNames[i]).compareTo(tileValues[i]) != 0);
        }
        int skip=4;
        //FWARD geometry
        if (instrument.equals("AATSR")){
            for (int i=4; i < 8; i++){
                tileValues[i] = sourceTiles.get(geomBandNames[i]).getSampleDouble(x, y);
                valid = valid && (sourceNoData.get(geomBandNames[i]).compareTo(tileValues[i]) != 0);
            }
            skip = 8;
        }

        // NADIR spec channels
        for (int i=0; i < nSpecWvl; i++){
            tileValues[i+skip] = sourceTiles.get(specBandNames[i]).getSampleDouble(x, y);
            valid = valid && (sourceNoData.get(specBandNames[i]).compareTo(tileValues[i+skip]) != 0);
        }
        skip += nSpecWvl;
        //FWARD spec channels
        if (instrument.equals("AATSR")){
            for (int i = 0; i < nSpecWvl; i++){
                tileValues[i+skip] = sourceTiles.get(specBandNames[i+nSpecWvl]).getSampleDouble(x, y);
                valid = valid && (sourceNoData.get(specBandNames[i+nSpecWvl]).compareTo(tileValues[i+skip]) != 0);
            }
            skip += nSpecWvl;
        }
 *
 */
        // surface pressure data
        tileValues[skip] = sourceTiles.get(surfPresName).getSampleDouble(x, y);
        valid = valid && (sourceNoData.get(surfPresName).compareTo(tileValues[skip]) != 0);

        // ozone data
        tileValues[skip+1] = sourceTiles.get(ozoneName).getSampleDouble(x, y);
        valid = valid && (sourceNoData.get(ozoneName).compareTo(tileValues[skip+1]) != 0);

        return valid;
    }

    private void addToSumArray(double[] tileValues, double[] sumVal) {
        for (int i=0; i<sumVal.length; i++){
            sumVal[i] += tileValues[i];
        }
    }

    /**
     * verify that ozone column is in DU
     * function assumes 2 common possibilities:
     *    1. Ozone column being in DU (generally 100 < ozDU < 1000)
     *    2. Ozone column being in atm.cm (generally < 1 )
     * conversion factor is 1000
     * @param ozoneColumn either [DU] or [atm.cm]
     * @return ozone column [DU]
     */
    private double ensureO3DobsonUnits(double ozoneColumn) {
        return (ozoneColumn<1) ? ozoneColumn*1000 : ozoneColumn;
    }

    private boolean containsTileValidData(Rectangle srcRec) {
        Tile validTile = getSourceTile(validBand, srcRec, ProgressMonitor.NULL);
        boolean valid = false;
        for (Tile.Pos pos: validTile){
            valid = valid || validTile.getSampleBoolean(pos.x, pos.y);
            if (valid) break;
        }
        return valid;
    }

    private void printInData(InputPixelData ipd) {
        for (int i=0;i<15;i++) System.err.printf("%ff, ", ipd.getSpecWvl()[i]);
        System.err.println();
        for (int i=0;i<15;i++) System.err.printf("%ff, ",ipd.getToaReflec()[i]);
        System.err.println();
        System.err.printf("%ff %ff %ff",ipd.getGeom().sza, ipd.getGeom().vza, ipd.getGeom().razi);
        System.err.println();
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(AerosolOp2.class);
        }
    }
}

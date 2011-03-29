package org.esa.beam.globalbedo.inversion;

/**
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Operator for the inversion and albedo retrieval part in Albedo Inversion
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
@OperatorMetadata(alias = "ga.inversion.albedo")
public class GlobalbedoLevel3Albedo extends Operator {

    @Parameter(defaultValue = "", description = "Globalbedo root directory") // e.g., /data/Globalbedo
    private String gaRootDir;

    @Parameter(defaultValue = "h18v04", description = "MODIS tile")
    private String tile;

    @Parameter(defaultValue = "2005", description = "Year")
    private int year;

    @Parameter(defaultValue = "540", description = "Wings")   // 540 # One year plus 3 months wings
    private int wings;

    @Parameter(defaultValue = "false", description = "Compute only snow pixels")
    private boolean computeSnow;

    @Override
    public void initialize() throws OperatorException {
        JAI.getDefaultInstance().getTileScheduler().setParallelism(1); // for debugging purpose

        // STEP 1: get Prior input files...

        // STEP 2: get Daily Accumulator input files...

        // STEP 3: compute pixelwise results (perform inversion) and write output
        // --> InversionOp (pixel operator), implement breadboard method 'Inversion'


    }

    private Product[] getPriorProducts() throws IOException {

        String priorDir = gaRootDir + File.separator + "Priors" + File.separator + tile +
                          "background/processed.p1.0.618034.p2.1.00000_java";

        String[] priorFiles = (new File(priorDir)).list();

        List<String> snowFilteredPriorList = new ArrayList<String>();
        for (String s:priorFiles) {
            if (computeSnow) {
                if (s.endsWith("_Snow")) {
                    snowFilteredPriorList.add(s);
                }
            } else {
                if (s.endsWith("_NoSnow")) {
                    snowFilteredPriorList.add(s);
                }
            }
        }

        Product[] priorProducts = new Product[snowFilteredPriorList.size()];

        int productIndex = 0;
        for (Iterator<String> i = snowFilteredPriorList.iterator(); i.hasNext();) {
            String sourceProductFileName = priorDir + File.separator + i.next();
            Product product = ProductIO.readProduct(sourceProductFileName);
            priorProducts[productIndex] = product;
            productIndex++;
        }

        return priorProducts;
    }

//    private Product[] getInputProducts(int doy) throws IOException {
//        String daystring = AlbedoInversionUtils.getDateFromDoy(year, doy);
//
//        String merisBbdrDir = bbdrRootDir + File.separator + "MERIS" + File.separator + year + File.separator + tile;
//        String[] merisBbdrFiles = (new File(merisBbdrDir)).list();
//        List<String> merisBbdrFileList = AlbedoInversionUtils.getDailyBBDRFilenames(merisBbdrFiles, daystring);
//
//        final int numberOfInputProducts = merisBbdrFileList.size() + aatsrBbdrFileList.size() + vgtBbdrFileList.size();
////        final int numberOfInputProducts = merisBbdrFileList.size() + aatsrBbdrFileList.size();
//        Product[] bbdrProducts = new Product[numberOfInputProducts];
//
//        int productIndex = 0;
//        for (Iterator<String> i = merisBbdrFileList.iterator(); i.hasNext();) {
//            String sourceProductFileName = merisBbdrDir + File.separator + i.next();
//            Product product = ProductIO.readProduct(sourceProductFileName);
//            bbdrProducts[productIndex] = product;
//            productIndex++;
//        }
//
//        if (productIndex == 0) {
//            throw new OperatorException("No source products found - check contents of BBDR directory!");
//        }
//
//        return bbdrProducts;
//    }

//
//    def GetObsFiles(doy, year, tile, location, wings, Snow):
//
//    import sys
//    import glob
//
//    allFiles = []
//    allDoYs = []
//    count = 0
//    # Actually, the 'MODIS' doy is 8 days behind the period centre
//    # so add 8 onto doy, wehere we understand DoY to mean MODIS form DoY
//    doy += 8
//
//    if Snow == 1:
//        files_path = location + '/200?/' + tile + '/Snow/M_200?*.npy'
//    else:
//        files_path = location + '/200?/' + tile + '/NoSnow/M_200?*.npy'
//
//    #Get all BBDR directories - the symlink to the .data directories
//    filelist = glob.glob(files_path)
//
//    #It is neccesary to sort BBDR files by YYYY/YYYYMMDD
//    from operator import itemgetter, attrgetter
//
//    TmpList = []
//    for i in range(0,len(filelist)):
//        TmpList.append(tuple(filelist[i].split('/'))) # create a list of tupples
//
//    # First, sort by year, In this case year is in field 5
//    # e.g. /unsafe/GlobAlbedo/BBDR/AccumulatorFiles/2005/h18v04/NoSnow/M_2005363.npy
//    s = sorted(TmpList, key=itemgetter(5))
//    # Now sort the sorted-by-year list by filename
//    s = sorted(s, key=itemgetter(8))
//
//    # Tuple to list
//    filelist = []
//    file = ''
//    for i in range(0,len(s)):
//        for j in range(1,len(s[0])):
//            file = file + '/' + s[i][j]
//        filelist.append(file)
//        file = ''
//
//    DoYs, Year = GetDaysOfYear(filelist)
//
//    # Left wing
//    if ( 365+(doy-wings)<=366 ):
//        DoY_index = numpy.where((DoYs>=366+(doy-wings)) & (Year<year))[0]
//        for i in DoY_index:
//            allFiles.append(filelist[i])
//            allDoYs.append((DoYs[i]-doy-366))
//
//    # Center
//    DoY_index = numpy.where((DoYs<doy+wings) & (DoYs>=doy-wings) & (Year==year))[0]
//    for i in DoY_index:
//        allFiles.append(filelist[i])
//        allDoYs.append(DoYs[i]-doy)
//
//    # Right wing
//    if ( (doy+wings)-365>0 ):
//        DoY_index = numpy.where((DoYs<=(doy+wings)-365) & (Year>year))[0]
//        for i in DoY_index:
//            allFiles.append(filelist[i])
//            allDoYs.append(DoYs[i]-doy+365)
//
//    if len(allFiles) == 0:
//        Err = 1
//    else:
//        Err, count = 0, len(allFiles)
//
//    return ReturnValueGetObsFiles(Err, count, allFiles, allDoYs)
}

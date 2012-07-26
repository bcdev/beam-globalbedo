package org.esa.beam.globalbedo.inversion;

/**
 * Container object holding the lists of input products and wing-dependent productDoys for albedo retrieval
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AlbedoInput {

    private String[] productBinaryFilenames;
    private int referenceYear;
    private int referenceDoy;

    public String[] getProductBinaryFilenames() {
        return productBinaryFilenames;
    }

    public void setProductBinaryFilenames(String[] productBinaryFilenames) {
        this.productBinaryFilenames = productBinaryFilenames;
    }

    public int getReferenceDoy() {
        return referenceDoy;
    }

    public void setReferenceDoy(int referenceDoy) {
        this.referenceDoy = referenceDoy;
    }

    public int getReferenceYear() {
        return referenceYear;
    }

    public void setReferenceYear(int referenceYear) {
        this.referenceYear = referenceYear;
    }
}

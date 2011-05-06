package org.esa.beam.globalbedo.inversion;

/**
 * Container object holding the lists of input products and wing-dependent productDoys for albedo retrieval
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AlbedoInput {

    private String[] productFilenames;
    private String[] productBinaryFilenames;
    private int[] productDoys;
    private int[] productYears;

    public String[] getProductFilenames() {
        return productFilenames;
    }

    public void setProductFilenames(String[] productFilenames) {
        this.productFilenames = productFilenames;
    }

    public String[] getProductBinaryFilenames() {
        return productBinaryFilenames;
    }

    public void setProductBinaryFilenames(String[] productBinaryFilenames) {
        this.productBinaryFilenames = productBinaryFilenames;
    }

    public int[] getProductDoys() {
        return productDoys;
    }

    public void setProductDoys(int[] productDoys) {
        this.productDoys = productDoys;
    }

    public int[] getProductYears() {
        return productYears;
    }

    public void setProductYears(int[] productYears) {
        this.productYears = productYears;
    }
}

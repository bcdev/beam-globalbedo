package org.esa.beam.globalbedo.inversion;

/**
 * Container object holding the lists of input products and wing-dependent productDoys for albedo retrieval
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AlbedoInput {

    private String[] productFilenames;
    private int[] productDoys;
    private int[] productYears;

    public String[] getProductFilenames() {
        return productFilenames;
    }

    public void setProductFilenames(String[] productFilenames) {
        this.productFilenames = productFilenames;
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

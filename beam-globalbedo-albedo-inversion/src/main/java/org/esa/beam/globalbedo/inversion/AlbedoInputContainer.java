package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;

/**
 * Container object holding the lists of input products and wing-dependent inputProductDoys for albedo retrieval
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AlbedoInputContainer {

    private Product[] inputProducts;
    private int[] inputProductDoys;

    private int[] inputProductYears;

    public Product[] getInputProducts() {
        return inputProducts;
    }

    public void setInputProducts(Product[] inputProducts) {
        this.inputProducts = inputProducts;
    }

    public int[] getInputProductDoys() {
        return inputProductDoys;
    }

    public void setInputProductDoys(int[] inputProductDoys) {
        this.inputProductDoys = inputProductDoys;
    }

    public int[] getInputProductYears() {
        return inputProductYears;
    }

    public void setInputProductYears(int[] inputProductYears) {
        this.inputProductYears = inputProductYears;
    }
}

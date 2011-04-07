package org.esa.beam.globalbedo.inversion;

import org.esa.beam.framework.datamodel.Product;

/**
 * Container object holding the lists of input products and wing-dependent productDoys for albedo retrieval
 *
 * @author Olaf Danne
 * @version $Revision: $ $Date:  $
 */
public class AlbedoInput {

    private Product[] products;
    private int[] productDoys;
    private int[] productYears;

    public Product[] getProducts() {
        return products;
    }

    public void setProducts(Product[] products) {
        this.products = products;
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.esa.beam.globalbedo.sdr.lutUtils;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.esa.beam.util.math.LookupTable;

/**
 * Serializable Class used to store values and dimensions of a lookuptable
 * in an object which can be written to and read from disc
 *
 * @author akheckel
 */
public class LookuptableStorage implements Serializable {
    private final static long serialVersionUID = 25041973L;
    private final float[] values;
    private final float[][] dimensions;

    /**
     * empty Standart consturctor
     */
    public LookuptableStorage() {
        this.values = null;
        this.dimensions = null;
    }
    
    /**
     * Constructor for the lookup table
     * @param values - Values of the lookuptable
     * @param dimensions - sequence of 1d arrays providing the dimensions of the lookup table
     */
    public LookuptableStorage(float[] values, float[]... dimensions) {
        this.values = values;
        this.dimensions = dimensions;
    }

    /**
     * Constructor for the lookup table reading the data from a file
     * @param fname - file name to read LUT from
     */
    public LookuptableStorage(String fname) {
        float[] vals = null;
        float[][] dims = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream(fname);
            ois = new ObjectInputStream(fis);
            vals = ((LookuptableStorage) ois.readObject()).getValues();
            dims = ((LookuptableStorage) ois.readObject()).getDimensions();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                ois.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        this.dimensions = dims;
        this.values = vals;
    }

    public float[] getValues() {
        return values;
    }

    public float[][] getDimensions() {
        return dimensions;
    }

    public LookupTable getLookupTable() {
        LookupTable lut = new LookupTable(values, dimensions);
        return lut;
    }

    public void writeLut(String fname) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            fos = new FileOutputStream(fname);
            oos = new ObjectOutputStream(fos);
            oos.writeObject(this);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                oos.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}

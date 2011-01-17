package org.esa.beam.globalbedo.bbdr;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
public class SdrOpTest {

    @Test
    public void testGetIndexBefore() {
        double[] values = {1.8, 2.2, 4.5, 5.5};
        try {
            SdrOp.getIndexBefore(1.2, values);
            fail();
        } catch (IllegalArgumentException e) {
        }
        assertEquals(1, SdrOp.getIndexBefore(2.5, values));
        assertEquals(2, SdrOp.getIndexBefore(4.6, values));
        try {
            SdrOp.getIndexBefore(7.7, values);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

}

package org.esa.beam.globalbedo.bbdr;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
public class BbdrUtilsTest {

    @Test
    public void testGetIndexBefore() {
        float[] values = {1.8f, 2.2f, 4.5f, 5.5f};
//        try {
//            BbdrUtils.getIndexBefore(1.2f, values);
//            fail();
//        } catch (IllegalArgumentException e) {
//        }
        assertEquals(0, BbdrUtils.getIndexBefore(1.2f, values));
        assertEquals(1, BbdrUtils.getIndexBefore(2.5f, values));
        assertEquals(2, BbdrUtils.getIndexBefore(4.6f, values));
        try {
            BbdrUtils.getIndexBefore(7.7f, values);
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

}

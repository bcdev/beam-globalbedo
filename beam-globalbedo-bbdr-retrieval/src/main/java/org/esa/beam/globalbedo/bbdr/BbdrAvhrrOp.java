/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.globalbedo.bbdr;

import Jama.Matrix;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.pointop.*;
import org.esa.beam.landcover.StatusPostProcessOp;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.math.FracIndex;
import org.esa.beam.util.math.LookupTable;

import java.io.IOException;

import static java.lang.Math.*;
import static java.lang.StrictMath.toRadians;

/**
 * Computes BBDRs and kernel parameters for AVHRR (i.e. AVHRR-LTDR)
 * todo: implement when specific algorithm is available
 *
 * @author Olaf Danne
 * @author Marco Zuehlke
 */
@OperatorMetadata(alias = "ga.bbdr.avhrr",
                  description = "Computes BBDRs and kernel parameters for AVHRR",
                  authors = "Marco Zuehlke, Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2011 by Brockmann Consult")
public class BbdrAvhrrOp extends BbdrMasterOp {

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        // todo: implement when specific algorithm is available
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(BbdrAvhrrOp.class);
        }
    }
}

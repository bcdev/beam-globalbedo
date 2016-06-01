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

import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.pointop.Sample;
import org.esa.beam.framework.gpf.pointop.WritableSample;

/**
 * Computes BBDRs for METEOSAT MVIRI
 * todo: clarify if and what to implement
 *
 * @author Olaf Danne
 */
@OperatorMetadata(alias = "ga.bbdr.meteosat",
                  description = "Computes BBDRs for Meteosat MVIRI",
                  authors = "Olaf Danne",
                  version = "1.0",
                  copyright = "(C) 2011 by Brockmann Consult")
public class MeteosatBbdrFromBrfOp extends BbdrMasterOp {

    @Override
    protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
        // todo: clarify if and what to implement

        // 1. add angles: implement Eumetsat C code
        // 2. provide BRF: broadband BRF is directly 'BB VIS'. No BB NIR, SW.
        // 3. provide kernels: same as for AVHRR, neglect Nsky and coupling
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(MeteosatBbdrFromBrfOp.class);
        }
    }
}

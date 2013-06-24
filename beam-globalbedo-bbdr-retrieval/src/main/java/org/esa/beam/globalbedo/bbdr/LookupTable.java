/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.util.math.Array;
import org.esa.beam.util.math.FracIndex;
import org.esa.beam.util.math.IntervalPartition;

import java.text.MessageFormat;

/**
 * TODO remove when compatibility is not required anymore.
 * This class is a copy from (package org.esa.beam.util.math) in beam-core for compatibility with BEAM 4.8
 *
 * The class {@code LookupTable} performs the function of multilinear
 * interpolation for lookup tables with an arbitrary number of dimensions.
 * <p/>
 * todo - method for degrading a table (see C++ code below)
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class LookupTable {

    /**
     * The lookup values.
     */
    private final Array values;
    /**
     * The dimensions associated with the lookup table.
     */
    private final IntervalPartition[] dimensions;
    /**
     * The strides defining the layout of the lookup value array.
     */
    private final int[] strides;
    /**
     * The relative array offsets of the lookup values for the vertices of a coordinate grid cell.
     */
    private final int[] o;

    /**
     * Constructs a lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     *
     * @throws IllegalArgumentException if the length of the {@code values} array is not equal to
     *                                  the number of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public LookupTable(final double[] values, final IntervalPartition... dimensions) {
        this(new Array.Double(values), dimensions);
    }

    /**
     * Constructs a lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     *
     * @throws IllegalArgumentException if the length of the {@code values} array is not equal to
     *                                  the number of coordinate grid vertices.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public LookupTable(final float[] values, final IntervalPartition... dimensions) {
        this(new Array.Float(values), dimensions);
    }

    /**
     * Constructs a lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     *
     * @throws IllegalArgumentException if the length of the {@code values} array is is not equal to
     *                                  the number of coordinate grid vertices or any dimension is
     *                                  not an interval partion.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public LookupTable(final double[] values, final double[]... dimensions) {
        this(values, IntervalPartition.createArray(dimensions));
    }

    /**
     * Constructs a lookup table for the lookup values and dimensions supplied as arguments.
     *
     * @param values     the lookup values. The {@code values} array must be laid out in row-major
     *                   order, so that the dimension associated with the last axis varies fastest.
     * @param dimensions the interval partitions defining the dimensions associated with the lookup
     *                   table. An interval partition is a strictly increasing sequence of at least
     *                   two real numbers, see {@link IntervalPartition}.
     *
     * @throws IllegalArgumentException if the length of the {@code values} array is is not equal to
     *                                  the number of coordinate grid vertices or any dimension is
     *                                  not an interval partion.
     * @throws NullPointerException     if the {@code values} array or the {@code dimensions} array
     *                                  is {@code null} or any dimension is {@code null}.
     */
    public LookupTable(final float[] values, final float[]... dimensions) {
        this(values, IntervalPartition.createArray(dimensions));
    }

    private LookupTable(final Array values, final IntervalPartition... dimensions) {
        ensureLegalArray(dimensions);
        ensureLegalArray(values, getVertexCount(dimensions));

        this.values = values;
        this.dimensions = dimensions;

        final int n = dimensions.length;

        strides = new int[n];
        // Compute strides
        for (int i = n, stride = 1; i-- > 0; stride *= dimensions[i].getCardinal()) {
            strides[i] = stride;
        }

        o = new int[1 << n];
        computeVertexOffsets(strides, o);
    }

    /**
     * Returns the number of dimensions associated with the lookup table.
     *
     * @return the number of dimensions.
     */
    public final int getDimensionCount() {
        return dimensions.length;
    }

    /**
     * Returns the dimensions associated with the lookup table.
     *
     * @return the dimensions.
     */
    public final IntervalPartition[] getDimensions() {
        return dimensions;
    }

    /**
     * Returns the the ith dimension associated with the lookup table.
     *
     * @param i the index number of the dimension of interest
     *
     * @return the ith dimension.
     */
    public final IntervalPartition getDimension(final int i) {
        return dimensions[i];
    }

    /**
     * Returns an interpolated value for the given coordinates.
     *
     * @param coordinates the coordinates of the lookup point.
     *
     * @return the interpolated value.
     *
     * @throws IllegalArgumentException if the length of the {@code coordinates} array is
     *                                  not equal to the number of dimensions associated
     *                                  with the lookup table.
     * @throws NullPointerException     if the {@code coordinates} array is {@code null}.
     */
    public final double getValue(final double... coordinates) throws IllegalArgumentException, NullPointerException {
        return getValue(coordinates, FracIndex.createArray(coordinates.length), new double[1 << coordinates.length]);
    }

    /**
     * Returns an interpolated value for the given coordinates.
     *
     * @param coordinates the coordinates of the lookup point.
     * @param fracIndexes workspace array of (at least) the same length as {@code coordinates}.
     * @param v           workspace array of (at least) length {@code 1 << coordinates.length}.
     *
     * @return the interpolated value.
     *
     * @throws ArrayIndexOutOfBoundsException if the {@code fracIndexes} and {@code v} arrays
     *                                        do not have proper length.
     * @throws IllegalArgumentException       if the length of the {@code coordinates} array is
     *                                        not equal to the number of dimensions associated
     *                                        with the lookup table.
     * @throws NullPointerException           if any parameter is {@code null} or exhibits any
     *                                        element, which is {@code null}.
     */
    public final double getValue(final double[] coordinates, final FracIndex[] fracIndexes, final double[] v)
            throws IllegalArgumentException, IndexOutOfBoundsException, NullPointerException {
        ensureLegalArray(coordinates, dimensions.length);

        for (int i = 0; i < dimensions.length; ++i) {
            computeFracIndex(dimensions[i], coordinates[i], fracIndexes[i]);
        }

        return getValue(fracIndexes, v);
    }

    /**
     * Returns an interpolated value for the given fractional indices.
     *
     * @param fracIndexes workspace array of (at least) the same length as {@code coordinates}.
     * @param v           workspace array of (at least) length {@code 1 << coordinates.length}.
     *
     * @return the interpolated value.
     *
     * @throws ArrayIndexOutOfBoundsException if the {@code fracIndexes} and {@code v} arrays
     *                                        do not have proper length.
     * @throws IllegalArgumentException       if the length of the {@code coordinates} array is
     *                                        not equal to the number of dimensions associated
     *                                        with the lookup table.
     * @throws NullPointerException           if any parameter is {@code null} or exhibits any
     *                                        element, which is {@code null}.
     */
    public final double getValue(final FracIndex[] fracIndexes, final double[] v) {
        int origin = 0;
        for (int i = 0; i < dimensions.length; ++i) {
            origin += fracIndexes[i].i * strides[i];
        }
        for (int i = 0; i < v.length; ++i) {
            v[i] = values.getValue(origin + o[i]);
        }
        for (int i = dimensions.length; i-- > 0;) {
            final int m = 1 << i;
            final double f = fracIndexes[i].f;

            for (int j = 0; j < m; ++j) {
                v[j] += f * (v[m + j] - v[j]);
            }
        }

        return v[0];
    }

    /**
     * Computes the {@link FracIndex} of a coordinate value with respect to a given
     * interval partition. The integral component of the returned {@link FracIndex}
     * corresponds to the index of the maximum partition member which is less than
     * or equal to the coordinate value. The [0, 1) fractional component describes
     * the position of the coordinate value within its bracketing subinterval.
     * <p/>
     * Exception: If the given coordinate value is equal to the partition maximum,
     * the fractional component of the returned {@link FracIndex} is equal to 1.0,
     * and the integral component is set to the index of the next to last partition
     * member.
     *
     * @param partition  the interval partition.
     * @param coordinate the coordinate value. If the coordinate value is less (greater)
     *                   than the minimum (maximum) of the given interval partition,
     *                   the returned {@link FracIndex} is the same as if the coordinate.
     *                   value was equal to the partition minimum (maximum).
     * @param fracIndex  the {@link FracIndex}.
     */
    public final static void computeFracIndex(final IntervalPartition partition, final double coordinate,
                                 final FracIndex fracIndex) {
        int lo = 0;
        int hi = partition.getCardinal() - 1;

        while (hi > lo + 1) {
            final int m = (lo + hi) >> 1;

            if (coordinate < partition.get(m)) {
                hi = m;
            } else {
                lo = m;
            }
        }

        fracIndex.i = lo;
        fracIndex.f = (coordinate - partition.get(lo)) / (partition.get(hi) - partition.get(lo));
        fracIndex.truncate();
    }

    /**
     * Computes the relative array offsets of the lookup values for the vertices
     * of a coordinate grid cell.
     *
     * @param strides the strides defining the layout of the lookup value array.
     * @param offsets the offsets.
     */
    static void computeVertexOffsets(final int[] strides, final int[] offsets) {
        for (int i = 0; i < strides.length; ++i) {
            final int k = 1 << i;

            for (int j = 0; j < k; ++j) {
                offsets[k + j] = offsets[j] + strides[i];
            }
        }
    }

    /**
     * Returns the number of vertices in the coordinate grid defined by the given dimensions.
     *
     * @param dimensions the dimensions defining the coordinate grid.
     *
     * @return the number of vertices.
     */
    static int getVertexCount(final IntervalPartition[] dimensions) {
        int count = 1;

        for (final IntervalPartition dimension : dimensions) {
            count *= dimension.getCardinal();
        }

        return count;
    }

    static <T> void ensureLegalArray(final T[] array) throws IllegalArgumentException, NullPointerException {
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        if (array.length == 0) {
            throw new IllegalArgumentException("array.length == 0");
        }
        for (final T element : array) {
            if (element == null) {
                throw new NullPointerException("element == null");
            }
        }
    }

    static void ensureLegalArray(final float[] array, final int length) throws
                                                                        IllegalArgumentException,
                                                                        NullPointerException {
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        if (array.length != length) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "array.length = {0} does not correspond to the expected length {1}", array.length, length));
        }
    }

    static void ensureLegalArray(final double[] array, final int length) throws
                                                                         IllegalArgumentException,
                                                                         NullPointerException {
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        if (array.length != length) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "array.length = {0} does not correspond to the expected length {1}", array.length, length));
        }
    }

    static void ensureLegalArray(Array array, final int length) throws
                                                                IllegalArgumentException,
                                                                NullPointerException {
        if (array == null) {
            throw new NullPointerException("array == null");
        }
        if (array.getLength() != length) {
            throw new IllegalArgumentException(MessageFormat.format(
                    "array.length = {0} does not correspond to the expected length {1}", array.getLength(), length));
        }
    }
}

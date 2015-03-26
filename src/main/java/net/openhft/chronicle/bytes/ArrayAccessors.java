/*
 * Copyright 2014 Higher Frequency Trading http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

import static net.openhft.chronicle.bytes.NativeAccess.U;

final class ArrayAccessors {

    static final long BOOLEAN_BASE;
    static final long BYTE_BASE;
    static final long CHAR_BASE;
    static final long SHORT_BASE;
    static final long INT_BASE;
    static final long LONG_BASE;
    static final long FLOAT_BASE;
    static final long DOUBLE_BASE;

    static {
        try {
            BOOLEAN_BASE = U.arrayBaseOffset(boolean[].class);
            BYTE_BASE = U.arrayBaseOffset(byte[].class);
            CHAR_BASE = U.arrayBaseOffset(char[].class);
            SHORT_BASE = U.arrayBaseOffset(short[].class);
            INT_BASE = U.arrayBaseOffset(int[].class);
            LONG_BASE = U.arrayBaseOffset(long[].class);
            FLOAT_BASE = U.arrayBaseOffset(float[].class);
            DOUBLE_BASE = U.arrayBaseOffset(double[].class);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    enum Boolean implements Accessor.Full<boolean[], boolean[]> {
        INSTANCE;

        @Override
        public Access<boolean[]> access(boolean[] source) {
            return NativeAccess.instance();
        }

        @Override
        public boolean[] handle(boolean[] source) {
            return source;
        }

        @Override
        public long offset(boolean[] source, long index) {
            return BOOLEAN_BASE + index;
        }
    }

    enum Byte implements Accessor.Full<byte[], byte[]> {
        INSTANCE;

        @Override
        public Access<byte[]> access(byte[] source) {
            return NativeAccess.instance();
        }

        @Override
        public byte[] handle(byte[] source) {
            return source;
        }

        @Override
        public long offset(byte[] source, long index) {
            return BYTE_BASE + index;
        }
    }

    enum Char implements Accessor.Full<char[], char[]> {
        INSTANCE;

        @Override
        public Access<char[]> access(char[] source) {
            return NativeAccess.instance();
        }

        @Override
        public char[] handle(char[] source) {
            return source;
        }

        @Override
        public long offset(char[] source, long index) {
            return CHAR_BASE + (index * 2L);
        }

        @Override
        public long size(long size) {
            return size * 2L;
        }
    }

    enum Short implements Accessor.Full<short[], short[]> {
        INSTANCE;

        @Override
        public Access<short[]> access(short[] source) {
            return NativeAccess.instance();
        }

        @Override
        public short[] handle(short[] source) {
            return source;
        }

        @Override
        public long offset(short[] source, long index) {
            return SHORT_BASE + (index * 2L);
        }

        @Override
        public long size(long size) {
            return size * 2L;
        }
    }

    enum Int implements Accessor.Full<int[], int[]> {
        INSTANCE;

        @Override
        public Access<int[]> access(int[] source) {
            return NativeAccess.instance();
        }

        @Override
        public int[] handle(int[] source) {
            return source;
        }

        @Override
        public long offset(int[] source, long index) {
            return INT_BASE + (index * 4L);
        }

        @Override
        public long size(long size) {
            return size * 4L;
        }
    }

    enum Long implements Accessor.Full<long[], long[]> {
        INSTANCE;

        @Override
        public Access<long[]> access(long[] source) {
            return NativeAccess.instance();
        }

        @Override
        public long[] handle(long[] source) {
            return source;
        }

        @Override
        public long offset(long[] source, long index) {
            return LONG_BASE + (index * 8L);
        }

        @Override
        public long size(long size) {
            return size * 8L;
        }
    }

    enum Float implements Accessor.Full<float[], float[]> {
        INSTANCE;

        @Override
        public Access<float[]> access(float[] source) {
            return NativeAccess.instance();
        }

        @Override
        public float[] handle(float[] source) {
            return source;
        }

        @Override
        public long offset(float[] source, long index) {
            return FLOAT_BASE + (index * 4L);
        }

        @Override
        public long size(long size) {
            return size * 4L;
        }
    }

    enum Double implements Accessor.Full<double[], double[]> {
        INSTANCE;

        @Override
        public Access<double[]> access(double[] source) {
            return NativeAccess.instance();
        }

        @Override
        public double[] handle(double[] source) {
            return source;
        }

        @Override
        public long offset(double[] source, long index) {
            return DOUBLE_BASE + (index * 8L);
        }

        @Override
        public long size(long size) {
            return size * 8L;
        }
    }

    private ArrayAccessors() {}
}

/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.util.zip.*;

/**
 * Provides different compression algorithms for bytes.
 */
@SuppressWarnings("rawtypes")
public enum Compressions implements Compression {

    /**
     * Represents a no-operation compression. This does not actually compress data.
     */
    Binary {
        /**
         * No-operation compression. Returns the input bytes without changes.
         *
         * @param bytes the input bytes
         * @return the same input bytes
         */
        @Override
        public byte[] compress(byte[] bytes) {
            return bytes;
        }

        /**
         * No-operation decompression. Returns the input bytes without changes.
         *
         * @param bytes the input bytes
         * @return the same input bytes
         * @throws IORuntimeException if an I/O error occurs
         */
        @Override
        public byte[] uncompress(byte[] bytes)
                throws IORuntimeException {
            return bytes;
        }

        /**
         * Copies data from the input BytesIn to the output BytesOut without any compression.
         *
         * @param from the input BytesIn
         * @param to   the output BytesOut
         * @throws IllegalStateException     if the state is not legal for the operation
         * @throws BufferOverflowException if the buffer overflows
         */
        @Override
        public void compress(@NotNull BytesIn<?> from, @NotNull BytesOut<?> to) throws IllegalStateException, BufferOverflowException {
            copy(from, to);
        }

        /**
         * Copies data from the input BytesIn to the output BytesOut without any decompression.
         *
         * @param from the input BytesIn
         * @param to   the output BytesOut
         * @throws IllegalStateException     if the state is not legal for the operation
         * @throws BufferOverflowException if the buffer overflows
         */
        @Override
        public void uncompress(@NotNull BytesIn<?> from, @NotNull BytesOut<?> to) throws IllegalStateException, BufferOverflowException {
            copy(from, to);
        }

        /**
         * Copies data from the input BytesIn to the output BytesOut without any changes.
         *
         * @param from the input BytesIn
         * @param to   the output BytesOut
         * @throws IllegalStateException     if the state is not legal for the operation
         * @throws BufferOverflowException if the buffer overflows
         */
        private void copy(@NotNull BytesIn<?> from, @NotNull BytesOut<?> to) throws IllegalStateException, BufferOverflowException {
            long copied = from.copyTo((BytesStore) to);
            to.writeSkip(copied);
        }

        /**
         * Returns the input stream without any changes.
         *
         * @param input the input stream
         * @return the same input stream
         */
        @Override
        public InputStream decompressingStream(InputStream input) {
            return input;
        }

        /**
         * Returns the output stream without any changes.
         *
         * @param output the output stream
         * @return the same output stream
         */
        @Override
        public OutputStream compressingStream(OutputStream output) {
            return output;
        }
    },

    /**
     * Represents LZW (Lempel–Ziv–Welch) compression.
     */
    LZW {
        /**
         * Returns an input stream that decompresses LZW-compressed data.
         *
         * @param input the LZW-compressed input stream
         * @return the decompressing input stream
         */
        @NotNull
        @Override
        public InputStream decompressingStream(@NotNull InputStream input) {
            return new InflaterInputStream(input);
        }

        /**
         * Returns an output stream that compresses data using the LZW algorithm.
         *
         * @param output the output stream
         * @return the compressing output stream
         */
        @NotNull
        @Override
        public OutputStream compressingStream(@NotNull OutputStream output) {
            return new DeflaterOutputStream(output, new Deflater(Deflater.DEFAULT_COMPRESSION));
        }
    },

    /**
     * Represents GZIP compression.
     */
    GZIP {
        /**
         * Returns an input stream that decompresses GZIP-compressed data.
         *
         * @param input the GZIP-compressed input stream
         * @return the decompressing input stream
         * @throws IORuntimeException if an I/O error occurs
         */
        @NotNull
        @Override
        public InputStream decompressingStream(@NotNull InputStream input)
                throws IORuntimeException {
            try {
                return new GZIPInputStream(input);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }

        /**
         * Returns an output stream that compresses data using the GZIP algorithm.
         *
         * @param output the output stream
         * @return the compressing output stream
         */
        @NotNull
        @Override
        public OutputStream compressingStream(@NotNull OutputStream output) {
            try {
                return new GZIPOutputStream(output);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }
    }
}

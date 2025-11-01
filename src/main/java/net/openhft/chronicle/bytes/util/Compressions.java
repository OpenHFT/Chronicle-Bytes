/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.util.zip.*;
/**
 * Compression algorithms supported by Chronicle Bytes. Each enum constant
 * provides {@link Compression} implementations for stream-based compression and
 * decompression.
 */
@SuppressWarnings("rawtypes")
public enum Compressions implements Compression {

    /**
     * Represents a no-operation compression strategy. The data is passed through
     * unchanged and no CPU time is spent compressing or expanding it.
     *
     * @see Compression
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
         * @throws IORuntimeException If an I/O error occurs
         */
        @Override
        public byte[] uncompress(byte[] bytes) throws IORuntimeException {
            return bytes;
        }

        /**
         * Copies data from the input BytesIn to the output BytesOut without any compression.
         *
         * @param from the input BytesIn
         * @param to   the output BytesOut
         * @throws BufferOverflowException If the buffer overflows
         * @throws ClosedIllegalStateException    If the resource has been released or closed.
         * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
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
         * @throws BufferOverflowException If the buffer overflows
         * @throws ClosedIllegalStateException    If the resource has been released or closed.
         * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
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
         * @throws BufferOverflowException If the buffer overflows
         * @throws ClosedIllegalStateException    If the resource has been released or closed.
         * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
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
     * Uses {@link java.util.zip.InflaterInputStream} and
     * {@link java.util.zip.DeflaterOutputStream} in a DEFLATE variant of LZW.
     * Provides modest compression ratios at a low CPU cost.
     *
     * @see Compression
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
     * Uses {@link java.util.zip.GZIPInputStream} and
     * {@link java.util.zip.GZIPOutputStream}. GZIP typically yields higher
     * compression at increased CPU usage and is defined in RFC 1952.
     *
     * @see Compression
     */
    GZIP {
        /**
         * Returns an input stream that decompresses GZIP-compressed data.
         *
         * @param input the GZIP-compressed input stream
         * @return the decompressing input stream
         * @throws IORuntimeException if creating the stream fails
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

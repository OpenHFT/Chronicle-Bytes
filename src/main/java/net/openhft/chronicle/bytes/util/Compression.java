/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.core.util.ThrowingFunction;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.BufferOverflowException;

/**
 * Interface for providing compression and decompression functionality
 * to various types of input data.
 */
public interface Compression {

    /**
     * Compresses {@code uncompressed} into {@code compressed} using the named algorithm.
     * Unrecognised names cause a fall back to {@link Compressions#Binary} which performs no compression.
     * Supported aliases are {@code "lzw"} and {@code "gzip"}.
     *
     * @param cs           the algorithm name, must not be {@code null}
     * @param uncompressed the input data to compress
     * @param compressed   the destination buffer for compressed data
     * @throws BufferOverflowException        If there is not enough space in the output buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    static void compress(@NotNull CharSequence cs, @NotNull Bytes<?> uncompressed, @NotNull Bytes<?> compressed)
            throws IllegalStateException, BufferOverflowException {
        switch (cs.charAt(0)) {
            case 'l':
                if (StringUtils.isEqual("lzw", cs)) {
                    Compressions.LZW.compress(uncompressed, compressed);
                    return;
                }
                break;
            case 'g':
                if (StringUtils.isEqual("gzip", cs)) {
                    Compressions.GZIP.compress(uncompressed, compressed);
                    return;
                }
                break;
            default:
                break;
        }
        Compressions.Binary.compress(uncompressed, compressed);
    }

    /**
     * Decompresses data using the named algorithm. Alias {@code "!binary"} behaves
     * the same as {@code "binary"}. Any other unrecognised name results in an
     * {@link IllegalArgumentException}.
     *
     * @param cs   the algorithm name, must not be {@code null}
     * @param from the input compressed data
     * @param to   the destination for decompressed bytes
     * @throws IORuntimeException             If an I/O error occurs.
     * @throws IllegalArgumentException       If the algorithm is unsupported.
     * @throws BufferOverflowException        If there is not enough space in the output buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    static void uncompress(@NotNull CharSequence cs, @NotNull BytesIn<?> from, @NotNull BytesOut<?> to)
            throws IORuntimeException, IllegalArgumentException, IllegalStateException, BufferOverflowException {
        switch (cs.charAt(0)) {
            case 'b':
            case '!':
                if (StringUtils.isEqual("binary", cs) || StringUtils.isEqual("!binary", cs)) {
                    Compressions.Binary.uncompress(from, to);
                }
                break;
            case 'l':
                if (StringUtils.isEqual("lzw", cs)) {
                    Compressions.LZW.uncompress(from, to);
                }
                break;
            case 'g':
                if (StringUtils.isEqual("gzip", cs)) {
                    Compressions.GZIP.uncompress(from, to);
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported compression " + cs);
        }
    }

    /**
     * Uncompresses data using the specified algorithm and a custom function to read bytes.
     *
     * @param cs    The compression algorithm to be used (e.g. "lzw", "gzip").
     * @param t     The input data.
     * @param bytes A function to read bytes from the input data.
     * @return The uncompressed data as byte array.
     * @throws IORuntimeException If an I/O error occurs.
     */
    static <T> byte[] uncompress(@NotNull CharSequence cs, T t, @NotNull ThrowingFunction<T, byte[], IORuntimeException> bytes)
            throws IORuntimeException {
        switch (cs.charAt(0)) {
            case 'b':
            case '!':
                if (StringUtils.isEqual("binary", cs) || StringUtils.isEqual("!binary", cs))
                    return Compressions.Binary.uncompress(bytes.apply(t));
                break;
            case 'l':
                if (StringUtils.isEqual("lzw", cs))
                    return Compressions.LZW.uncompress(bytes.apply(t));
                break;
            case 'g':
                if (StringUtils.isEqual("gzip", cs))
                    return Compressions.GZIP.uncompress(bytes.apply(t));
                break;
            default:
                return null;
        }
        return null;
    }

    /**
     * Compresses a byte array using the streams returned by
     * {@link #compressingStream(OutputStream)}.
     *
     * @param bytes the input bytes to compress
     * @return the compressed data
     * @throws AssertionError if an unexpected {@link IOException} occurs
     */
    default byte[] compress(byte[] bytes) {
        @NotNull ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStream output = compressingStream(baos)) {
            output.write(bytes);

        } catch (IOException e) {
            throw new AssertionError(e); // compressing in memory
        }
        return baos.toByteArray();
    }

    /**
     * Compresses data from {@code from} to {@code to} using this implementation's
     * algorithm. The stream wrappers originate from
     * {@link #compressingStream(OutputStream)}. Unexpected I/O issues are
     * wrapped in an {@link AssertionError} as the streams are memory based.
     *
     * @param from The input data to be compressed.
     * @param to   The output to write the compressed data.
     * @throws BufferOverflowException        If there is not enough space in the output buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    default void compress(@NotNull BytesIn<?> from, @NotNull BytesOut<?> to) throws IllegalStateException, BufferOverflowException {
        try (OutputStream output = compressingStream(to.outputStream())) {
            from.copyTo(output);

        } catch (IOException e) {
            throw new AssertionError(e); // compressing in memory
        }
    }

    /**
     * Uncompresses a byte array using the stream returned by
     * {@link #decompressingStream(InputStream)}.
     *
     * @param bytes the compressed data
     * @return the resulting uncompressed bytes
     * @throws IORuntimeException if an I/O error occurs while reading the data
     */
    default byte[] uncompress(byte[] bytes)
            throws IORuntimeException {
        @NotNull ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream input = decompressingStream(new ByteArrayInputStream(bytes))) {
            byte[] buf = new byte[512];
            for (int len; (len = input.read(buf)) > 0; )
                baos.write(buf, 0, len);

        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return baos.toByteArray();
    }

    /**
     * Uncompresses data from the input to the output using the implementing uncompression algorithm.
     *
     * @param from The input compressed data.
     * @param to   The output to write the uncompressed data.
     * @throws IORuntimeException             If an I/O error occurs.
     * @throws BufferOverflowException        If there is not enough space in the output buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    default void uncompress(@NotNull BytesIn<?> from, @NotNull BytesOut<?> to)
            throws IORuntimeException, IllegalStateException, BufferOverflowException {
        try (InputStream input = decompressingStream(from.inputStream())) {
            to.copyFrom(input);

        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    /**
     * Returns a new {@link InputStream} that wraps {@code input} and will
     * decompress bytes as they are read. The caller is responsible for closing
     * the returned stream. Closing the wrapper does not close the underlying
     * {@code input} stream. Future implementations may buffer the input and
     * callers should not depend on the concrete type of the returned stream.
     *
     * @param input the underlying stream supplying compressed data
     * @return a stream producing uncompressed bytes
     * @throws IORuntimeException if an I/O error occurs
     */
    InputStream decompressingStream(InputStream input)
            throws IORuntimeException;

    /**
     * Returns a new {@link OutputStream} that compresses any bytes written to it
     * using this algorithm's strategy. The caller must close the returned stream
     * to flush all data. Closing it does not close {@code output}. The returned
     * stream may be wrapped in additional buffering and its exact class is not
     * guaranteed.
     *
     * @param output the underlying stream to receive compressed data
     * @return a stream accepting uncompressed bytes
     */
    OutputStream compressingStream(OutputStream output);

    /**
     * Indicates whether compression/decompression is available.
     *
     * @return true if available, false otherwise.
     */
    default boolean available() {
        return true;
    }
}

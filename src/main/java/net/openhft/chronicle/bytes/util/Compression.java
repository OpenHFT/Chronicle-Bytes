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

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.StringUtils;
import net.openhft.chronicle.core.util.ThrowingFunction;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.BufferOverflowException;

/**
 * Provides an interface for compressing and uncompressing data using various algorithms.
 */
public interface Compression {

    /**
     * Compresses the input uncompressed data into the output compressed data using the specified algorithm.
     *
     * @param cs           The compression algorithm to be used (e.g. "lzw", "gzip").
     * @param uncompressed The input data to be compressed.
     * @param compressed   The output to write the compressed data.
     * @throws IllegalStateException     if the compression algorithm fails.
     * @throws BufferOverflowException   if there is not enough space in the output buffer.
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
     * Uncompresses the input compressed data into the output uncompressed data using the specified algorithm.
     *
     * @param cs   The compression algorithm to be used (e.g. "lzw", "gzip").
     * @param from The input compressed data.
     * @param to   The output to write the uncompressed data.
     * @throws IORuntimeException        if an I/O error occurs.
     * @throws IllegalArgumentException   if the algorithm is unsupported.
     * @throws IllegalStateException      if the uncompression algorithm fails.
     * @throws BufferOverflowException   if there is not enough space in the output buffer.
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
     * @throws IORuntimeException if an I/O error occurs.
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
     * Compresses a byte array.
     *
     * @param bytes The input byte array to compress.
     * @return The compressed data as byte array.
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
     * Compresses data from the input to the output using the implementing compression algorithm.
     *
     * @param from The input data to be compressed.
     * @param to   The output to write the compressed data.
     * @throws IllegalStateException     if the compression algorithm fails.
     * @throws BufferOverflowException   if there is not enough space in the output buffer.
     */
    default void compress(@NotNull BytesIn<?> from, @NotNull BytesOut<?> to) throws IllegalStateException, BufferOverflowException {
        try (OutputStream output = compressingStream(to.outputStream())) {
            from.copyTo(output);

        } catch (IOException e) {
            throw new AssertionError(e); // compressing in memory
        }
    }

    /**
     * Uncompresses a byte array.
     *
     * @param bytes The input compressed data as byte array.
     * @return The uncompressed data as byte array.
     * @throws IORuntimeException if an I/O error occurs.
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
     * @throws IORuntimeException       if an I/O error occurs.
     * @throws IllegalStateException    if the uncompression algorithm fails.
     * @throws BufferOverflowException if there is not enough space in the output buffer.
     */
    default void uncompress(@NotNull BytesIn<?> from, @NotNull BytesOut<?> to)
            throws IORuntimeException, IllegalStateException, BufferOverflowException {
        try (InputStream input = decompressingStream(from.inputStream())) {
            to.copyFrom(input);

        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    InputStream decompressingStream(InputStream input)
            throws IORuntimeException;

    OutputStream compressingStream(OutputStream output);

    default boolean available() {
        return true;
    }
}

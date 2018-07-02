/*
 * Copyright 2016 higherfrequencytrading.com
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
import org.jetbrains.annotations.Nullable;

import java.io.*;

/*
 * Created by peter.lawrey on 09/12/2015.
 */
public interface Compression {

    static <T> void compress(@NotNull CharSequence cs, @NotNull Bytes uncompressed, @NotNull Bytes compressed) {
        switch (cs.charAt(0)) {
            case 'l':
                if (StringUtils.isEqual("lzw", cs)) {
                    Compressions.LZW.compress(uncompressed, compressed);
                    return;
                }
                break;
            case 's':
                if (StringUtils.isEqual("snappy", cs)) {
                    Compressions.Snappy.compress(uncompressed, compressed);
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

    static void uncompress(@NotNull CharSequence cs, @NotNull BytesIn from, @NotNull BytesOut to) throws IORuntimeException {
        switch (cs.charAt(0)) {
            case 'b':
            case '!':
                if (StringUtils.isEqual("binary", cs) || StringUtils.isEqual("!binary", cs)) {
                    Compressions.Binary.uncompress(from, to);
                    return;
                }

                break;
            case 'l':
                if (StringUtils.isEqual("lzw", cs)) {
                    Compressions.LZW.uncompress(from, to);
                    return;
                }
                break;
            case 's':
                if (StringUtils.isEqual("snappy", cs)) {
                    Compressions.Snappy.uncompress(from, to);
                    return;
                }
                break;
            case 'g':
                if (StringUtils.isEqual("gzip", cs)) {
                    Compressions.GZIP.uncompress(from, to);
                    return;
                }
                break;
            default:
                throw new IllegalArgumentException("Unsupported compression " + cs);
        }

    }

    @Nullable
    static <T> byte[] uncompress(@NotNull CharSequence cs, T t, @NotNull ThrowingFunction<T, byte[], IORuntimeException> bytes) throws IORuntimeException {
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
            case 's':
                if (StringUtils.isEqual("snappy", cs))
                    return Compressions.Snappy.uncompress(bytes.apply(t));
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

    default byte[] compress(@NotNull byte[] bytes) {
        @NotNull ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStream output = compressingStream(baos)) {
            output.write(bytes);

        } catch (IOException e) {
            throw new AssertionError(e); // compressing in memory
        }
        return baos.toByteArray();
    }

    default void compress(@NotNull BytesIn from, @NotNull BytesOut to) {
        try (OutputStream output = compressingStream(to.outputStream())) {
            from.copyTo(output);

        } catch (IOException e) {
            throw new AssertionError(e); // compressing in memory
        }
    }

    default byte[] uncompress(@NotNull byte[] bytes) throws IORuntimeException {
        @NotNull ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream input = decompressingStream(new ByteArrayInputStream(bytes))) {
            @NotNull byte[] buf = new byte[512];
            for (int len; (len = input.read(buf)) > 0; )
                baos.write(buf, 0, len);

        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return baos.toByteArray();
    }

    default void uncompress(@NotNull BytesIn from, @NotNull BytesOut to) throws IORuntimeException {
        try (InputStream input = decompressingStream(from.inputStream())) {
            to.copyFrom(input);

        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    InputStream decompressingStream(InputStream input) throws IORuntimeException;

    OutputStream compressingStream(OutputStream output);

    default boolean available() {
        return true;
    }
}

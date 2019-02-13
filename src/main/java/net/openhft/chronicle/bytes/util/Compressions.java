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

import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.*;

/*
 * Created by peter.lawrey on 09/12/2015.
 */
public enum Compressions implements Compression {
    Binary {
        @NotNull
        @Override
        public byte[] compress(byte[] bytes) {
            return bytes;
        }

        @Override
        public byte[] uncompress(byte[] bytes) throws IORuntimeException {
            return bytes;
        }

        @Override
        public void compress(@NotNull BytesIn from, @NotNull BytesOut to) {
            copy(from, to);
        }

        @Override
        public void uncompress(@NotNull BytesIn from, @NotNull BytesOut to) {
            copy(from, to);
        }

        private void copy(@NotNull BytesIn from, @NotNull BytesOut to) {
            long copied = from.copyTo((BytesStore) to);
            to.writeSkip(copied);
        }

        @Override
        public InputStream decompressingStream(InputStream input) {
            return input;
        }

        @Override
        public OutputStream compressingStream(OutputStream output) {
            return output;
        }
    },
    LZW {
        @NotNull
        @Override
        public InputStream decompressingStream(@NotNull InputStream input) {
            return new InflaterInputStream(input);
        }

        @NotNull
        @Override
        public OutputStream compressingStream(@NotNull OutputStream output) {
            return new DeflaterOutputStream(output, new Deflater(Deflater.DEFAULT_COMPRESSION));
        }
    },
    GZIP {
        @NotNull
        @Override
        public InputStream decompressingStream(@NotNull InputStream input) throws IORuntimeException {
            try {
                return new GZIPInputStream(input);

            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }

        @NotNull
        @Override
        public OutputStream compressingStream(@NotNull OutputStream output) {
            try {
                return new GZIPOutputStream(output);

            } catch (IOException e) {
                throw new AssertionError(e); // in memory.
            }
        }
    }
}

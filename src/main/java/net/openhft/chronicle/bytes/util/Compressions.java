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
import net.openhft.chronicle.core.io.IORuntimeException;
import org.xerial.snappy.SnappyInputStream;
import org.xerial.snappy.SnappyOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.*;

/**
 * Created by peter.lawrey on 09/12/2015.
 */
public enum Compressions implements Compression {
    Binary {
        @Override
        public byte[] compress(byte[] bytes) throws IORuntimeException {
            return bytes;
        }

        @Override
        public byte[] uncompress(byte[] bytes) throws IORuntimeException {
            return bytes;
        }

        @Override
        public void compress(Bytes from, Bytes to) throws IORuntimeException {
            long copied = from.copyTo(to);
            to.writeSkip(copied);
        }

        @Override
        public void uncompress(Bytes from, Bytes to) {
            from.copyTo(to);
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
        @Override
        public InputStream decompressingStream(InputStream input) {
            return new InflaterInputStream(input);
        }

        @Override
        public OutputStream compressingStream(OutputStream output) {
            return new DeflaterOutputStream(output, new Deflater(Deflater.DEFAULT_COMPRESSION));
        }
    },
    GZIP {
        @Override
        public InputStream decompressingStream(InputStream input) {
            try {
                return new GZIPInputStream(input);
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        }

        @Override
        public OutputStream compressingStream(OutputStream output) {
            try {
                return new GZIPOutputStream(output);
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        }
    },
    Snappy {
        @Override
        public byte[] compress(byte[] bytes) throws IORuntimeException {
            try {
                return org.xerial.snappy.Snappy.compress(bytes);
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        }

        @Override
        public byte[] uncompress(byte[] bytes) throws IORuntimeException {
            try {
                return org.xerial.snappy.Snappy.uncompress(bytes);
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        }

        @Override
        public InputStream decompressingStream(InputStream input) {
            try {
                return new SnappyInputStream(input);
            } catch (IOException e) {
                throw new IORuntimeException(e);
            }
        }

        @Override
        public OutputStream compressingStream(OutputStream output) {
            return new SnappyOutputStream(output);
        }
    }
}

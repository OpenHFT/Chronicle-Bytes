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
    BINARY {
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
    SNAPPY {
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

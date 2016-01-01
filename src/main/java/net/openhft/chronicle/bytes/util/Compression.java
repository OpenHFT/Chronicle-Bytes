package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.function.Function;

/**
 * Created by peter.lawrey on 09/12/2015.
 */
public interface Compression {

    static <T> void compress(CharSequence cs, Bytes uncompressed, Bytes compressed) {
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

    @Nullable
    static <T> byte[] uncompress(CharSequence cs, T t, Function<T, byte[]> bytes) {
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
        }

        return null;
    }

    default byte[] compress(byte[] bytes) throws IORuntimeException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (OutputStream output = compressingStream(baos)) {
            output.write(bytes);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return baos.toByteArray();
    }

    default void compress(Bytes from, Bytes to) throws IORuntimeException {
        try (OutputStream output = compressingStream(to.outputStream())) {
            from.copyTo(output);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    default byte[] uncompress(byte[] bytes) throws IORuntimeException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream input = decompressingStream(new ByteArrayInputStream(bytes))) {
            byte[] buf = new byte[512];
            for (int len; (len = input.read(buf)) > 0; )
                baos.write(buf, 0, len);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
        return baos.toByteArray();
    }

    default void uncompress(Bytes from, Bytes to) {
        try (InputStream input = decompressingStream(from.inputStream())) {
            to.copyFrom(input);
        } catch (IOException e) {
            throw new IORuntimeException(e);
        }
    }

    InputStream decompressingStream(InputStream input);

    OutputStream compressingStream(OutputStream output);
}

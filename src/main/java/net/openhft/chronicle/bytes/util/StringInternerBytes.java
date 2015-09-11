package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import static net.openhft.chronicle.bytes.BytesUtil.toCharArray;

/**
 * @author Rob Austin.
 */
public class StringInternerBytes extends StringInterner {

    public StringInternerBytes(int capacity) {
        super(capacity);
    }

    /**
     * converts the bytes to a ISO-8859-1 String, the end of the string is either the bytes .limit
     * () or a byte containing the stopByte ( which ever comes first ). If the string can be
     * obtained from the pool, this string is used instead. otherwise, the string is added to the
     * pool.
     *
     * @param bytes    the bytes to convert to a string
     * @param length  parse the string up to the length
     * @return the string made from bytes only ( rather than chars )
     */
    public String bytesToSting(@NotNull final Bytes bytes, int length) {
        try {
            int h = BytesStoreHash.hash32(bytes, length) & mask;
            final String s = interner[h];

            long position = bytes.readPosition();
            if (BytesUtil.bytesEqual(s, bytes, position, length))
                return s;

            char[] chars = toCharArray(bytes, position, length);
            return interner[h] =
                    StringUtils.newString(chars);
        } finally {
            bytes.readSkip(length);
        }
    }
}

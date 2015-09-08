package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Maths;
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
     * @param stopByte parse the string up to the stopByte
     * @return the string made from bytes only ( rather than chars )
     */
    public String bytesToSting(@NotNull final Bytes bytes, final byte stopByte) {

        final long limit = bytes.readLimit();

        try {
            final int h = hash(bytes, bytes.readPosition(), bytes.readLimit(), stopByte) & mask;
            final String s = interner[h];

            if (StringUtils.isEqual(s, bytes))
                return s;

            return interner[h] = StringUtils.newString(toCharArray(bytes));
        } finally {
            bytes.readPosition(bytes.readLimit());
            bytes.readLimit(limit);
        }
    }


    private static int hash(Bytes cs, long position, long limit, final byte delimitor) {
        long h = longHash(cs, position, limit, delimitor);
        return (int) (h ^ (h >> 32));
    }

    private static long longHash(Bytes cs, long position, long limit, byte delimitor) {
        long hash = 0;
        for (long i = position; i < limit; i++) {
            final byte b = cs.readByte(i);
            if (b == delimitor) {
                cs.readLimit(i);
                break;
            }
            hash = Long.rotateLeft(hash, 7) + b;
        }
        return Maths.longHash(hash);
    }


}

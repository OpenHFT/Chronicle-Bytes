package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.pool.StringInterner;
import net.openhft.chronicle.core.util.StringUtils;

/**
 * @author Rob Austin.
 */
public class StringInternerBytes extends StringInterner {

    private static final StringBuilderPool SBP = new StringBuilderPool();

    public StringInternerBytes(int capacity) {
        super(capacity);
    }

    /**
     * converts the bytes an ISO-8859-1 String, the end of the string is either then bytes.limit ()
     * or a byte containing the stopByte ( which ever comes first ) The string is interned, and
     * added to a pool. If the string can be obtained from the pool, this string is used instead.
     *
     * @param bytes    the bytes to convert to a string
     * @param stopByte parse the string up to the stopByte
     * @return the string made from bytes only ( rather than chars )
     */
    public String bytesToSting(Bytes bytes, final byte stopByte) {

        final long limit = bytes.readLimit();

        try {
            final int h = hash(bytes, bytes.readPosition(), bytes.readLimit(), stopByte) & mask;
            final String s = interner[h];

            if (StringUtils.isEqual(s, bytes))
                return s;

            final char[] chars = new char[(int) bytes.readRemaining()];

            for (int i = 0; i < bytes.readRemaining(); i++) {
                chars[i] = (char) bytes.readUnsignedByte(i + bytes.readPosition());
            }

            return interner[h] = StringUtils.newString(chars);
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

/*
 *
 *  *     Copyright (C) ${YEAR}  higherfrequencytrading.com
 *  *
 *  *     This program is free software: you can redistribute it and/or modify
 *  *     it under the terms of the GNU Lesser General Public License as published by
 *  *     the Free Software Foundation, either version 3 of the License.
 *  *
 *  *     This program is distributed in the hope that it will be useful,
 *  *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  *     GNU Lesser General Public License for more details.
 *  *
 *  *     You should have received a copy of the GNU Lesser General Public License
 *  *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

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
     * @param bytes  the bytes to convert to a string
     * @param length parse the string up to the length
     * @return the string made from bytes only ( rather than chars )
     */
    public String intern(@NotNull final Bytes bytes, int length) {
        try {
            int hash32 = BytesStoreHash.hash32(bytes, length);
            int h = hash32 & mask;
            String s = interner[h];
            long position = bytes.readPosition();
            if (BytesUtil.bytesEqual(s, bytes, position, length))
                return s;
            int h2 = (hash32 >> shift) & mask;
            String s2 = interner[h2];
            if (BytesUtil.bytesEqual(s2, bytes, position, length))
                return s2;

            char[] chars = toCharArray(bytes, position, length);
            return interner[s == null || (s2 != null && toggle()) ? h : h2] = StringUtils.newString(chars);
        } finally {
            bytes.readSkip(length);
        }
    }
}

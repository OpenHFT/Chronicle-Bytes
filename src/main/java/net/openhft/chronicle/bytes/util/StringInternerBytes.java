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
import net.openhft.chronicle.bytes.BytesUtil;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
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

    public String intern(@NotNull final Bytes bytes) {
        return intern(bytes, Maths.toUInt31(bytes.readRemaining()));
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

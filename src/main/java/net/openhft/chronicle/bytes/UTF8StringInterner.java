/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.util.StringUtils;

/**
 * @author peter.lawrey
 */
public class UTF8StringInterner {
    private static final StringBuilderPool SBP = new StringBuilderPool();

    private static final long K0 = 0xc3a5c85c97cb3127L;
    private static final long K_MUL = 0x9ddfea08eb382d69L;

    private final String[] interner;
    private final int mask;

    public UTF8StringInterner(int capacity) {
        int n = Maths.nextPower2(capacity, 128);
        interner = new String[n];
        mask = n - 1;
    }

    public String intern(Bytes cs) {
        int h = (int) ((hash(cs) >> 16) & mask);
        String s = interner[h];
        if (StringUtils.isEqual(s, cs))
            return s;
        StringBuilder sb = SBP.acquireStringBuilder();
        BytesUtil.parseUTF(cs, sb, StopCharTesters.ALL);
        return interner[h] = sb.toString();
    }

    public static long hash(Bytes bs) {
        long h = 0;
        int i = 0, len = Maths.toInt32(bs.readRemaining());
        long start = bs.readPosition();
        for (; i < len - 7; i += 8)
            h = h * K0 + bs.readLong(start + i);
        long l = 0;
        for (; i < len; i++)
            l = (l << 8) + bs.readUnsignedByte(start + i);
        h = h * K0 + l;
        return h * K_MUL;
    }
}

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

package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.UTFDataFormatRuntimeException;
import net.openhft.chronicle.bytes.algo.BytesStoreHash;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;
import java.util.stream.Stream;

/**
 * @author peter.lawrey
 */
public class UTF8StringInterner {
    private static final StringBuilderPool SBP = new StringBuilderPool();

    @NotNull
    protected final String[] interner;
    protected final int mask, shift;
    protected boolean toggle = false;

    public UTF8StringInterner(int capacity) throws IllegalArgumentException {
        int n = Maths.nextPower2(capacity, 128);
        shift = Maths.intLog2(n);
        interner = new String[n];
        mask = n - 1;
    }

    public String intern(@NotNull Bytes cs)
            throws IllegalArgumentException, UTFDataFormatRuntimeException, BufferUnderflowException {
        if (cs.readRemaining() > interner.length)
            return getString(cs);
        int hash = BytesStoreHash.hash32(cs);
        int h = hash & mask;
        String s = interner[h];
        if (cs.isEqual(s))
            return s;
        int h2 = (hash >> shift) & mask;
        String s2 = interner[h2];
        if (cs.isEqual(s))
            return s2;
        String str = getString(cs);
        return interner[s == null || (s2 != null && toggle()) ? h : h2] = str;
    }

    @NotNull
    private String getString(@NotNull Bytes cs) {
        StringBuilder sb = SBP.acquireStringBuilder();
        long pos = cs.readPosition();
        cs.parseUtf8(sb, Maths.toInt32(cs.readRemaining()));
        cs.readPosition(pos);
        return sb.toString();
    }

    protected boolean toggle() {
        return toggle = !toggle;
    }

    public int valueCount() {
        return (int) Stream.of(interner).filter(s -> s != null).count();
    }
}

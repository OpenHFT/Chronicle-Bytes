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

/**
 * @author peter.lawrey
 */
public class UTF8StringInterner {
    private static final StringBuilderPool SBP = new StringBuilderPool();

    @NotNull
    private final String[] interner;
    private final int mask;

    public UTF8StringInterner(int capacity) throws IllegalArgumentException {
        int n = Maths.nextPower2(capacity, 128);
        interner = new String[n];
        mask = n - 1;
    }

    public String intern(@NotNull Bytes cs)
            throws IllegalArgumentException, UTFDataFormatRuntimeException, BufferUnderflowException {
        int h = BytesStoreHash.hash32(cs) & mask;
        String s = interner[h];
        if (cs.isEqual(s))
            return s;
        StringBuilder sb = SBP.acquireStringBuilder();
        long pos = cs.readPosition();
        cs.parseUTF(sb, Maths.toInt32(cs.readRemaining()));
        cs.readPosition(pos);
        return interner[h] = sb.toString();
    }
}

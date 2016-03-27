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

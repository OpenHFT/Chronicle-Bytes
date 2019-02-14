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
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.annotation.NotNull;

import java.nio.BufferUnderflowException;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author peter.lawrey
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractInterner<T> {
    @NotNull
    protected final InternerEntry<T>[] entries;
    protected final int mask, shift;
    protected boolean toggle = false;

    public AbstractInterner(int capacity) throws IllegalArgumentException {
        int n = Maths.nextPower2(capacity, 128);
        shift = Maths.intLog2(n);
        entries = new InternerEntry[n];
        mask = n - 1;
    }

    private static int hash32(@NotNull BytesStore bs, int length) {
        return bs.fastHash(bs.readPosition(), length);
    }

    public T intern(@NotNull Bytes cs)
            throws IllegalArgumentException, IORuntimeException, BufferUnderflowException {
        return intern((BytesStore) cs, (int) cs.readRemaining());
    }

    public T intern(@NotNull BytesStore cs)
            throws IllegalArgumentException, IORuntimeException, BufferUnderflowException {
        return intern(cs, (int) cs.readRemaining());
    }

    public T intern(@NotNull Bytes cs, int length)
            throws IllegalArgumentException, IORuntimeException, BufferUnderflowException {
        return intern((BytesStore) cs, length);
    }

    public T intern(@NotNull BytesStore cs, int length)
            throws IllegalArgumentException, IORuntimeException, BufferUnderflowException {
        if (length > entries.length)
            return getValue(cs, length);
        int hash = hash32(cs, length);
        int h = hash & mask;
        InternerEntry<T> s = entries[h];
        if (s != null && s.bytes.length() == length && s.bytes.equalBytes(cs, length))
            return s.t;
        int h2 = (hash >> shift) & mask;
        InternerEntry<T> s2 = entries[h2];
        if (s2 != null && s2.bytes.length() == length && s2.bytes.equalBytes(cs, length))
            return s2.t;
        @NotNull T t = getValue(cs, length);
        @NotNull final byte[] bytes = new byte[length];
        @NotNull BytesStore bs = BytesStore.wrap(bytes);
        cs.read(cs.readPosition(), bytes, 0, length);
        entries[s == null || (s2 != null && toggle()) ? h : h2] = new InternerEntry<>(bs, t);
        return t;
    }

    @NotNull
    protected abstract T getValue(BytesStore bs, int length) throws IORuntimeException;

    protected boolean toggle() {
        return toggle = !toggle;
    }

    public int valueCount() {
        return (int) Stream.of(entries).filter(Objects::nonNull).count();
    }

    static class InternerEntry<T> {
        final BytesStore bytes;
        final T t;

        InternerEntry(BytesStore bytes, T t) {
            this.bytes = bytes;
            this.t = t;
        }
    }
}

/*
 * Copyright 2016-2025 chronicle.software
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
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;
import java.util.Objects;
import java.util.stream.Stream;

import static net.openhft.chronicle.core.Jvm.uncheckedCast;

/**
 * Abstract base class for an interning mechanism designed to reduce memory allocation
 * by reusing immutable objects that are content equal. When a byte sequence is presented,
 * the interner attempts to return a previously cached instance. If no such instance exists
 * a new one is created via {@link #getValue(BytesStore, int)}, cached and returned.
 *
 * <p>The cache is capacity bounded and entries are replaced in a pseudo-LRU fashion
 * using the {@link #toggle()} heuristic. Hashing relies on
 * {@link net.openhft.chronicle.bytes.algo.BytesStoreHash#hash32(BytesStore, long)}
 * and collisions are resolved by probing two slots.</p>
 *
 * <p>The cache guarantees equality of the returned objects' contents but does not guarantee
 * object identity across invocations or threads. This class itself does not ensure
 * thread safety. Concurrent access may result in benign races where later inserts overwrite
 * earlier ones.</p>
 *
 * @param <T> the type of the object being interned
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractInterner<T> {
    /**
     * The array storing {@link InternerEntry} objects. Concurrent modifications
     * should be externally synchronised as no locking is performed here.
     */
    protected final InternerEntry<T>[] entries;
    /**
     * Mask used to hash into {@link #entries}, typically {@code capacity - 1}.
     */
    protected final int mask;
    /**
     * Shift value used when computing the secondary hash index.
     */
    protected final int shift;
    /**
     * Flag toggled when choosing between the two hash slots for new entries. It
     * approximates a round-robin placement to avoid hot spots.
     */
    protected boolean toggle = false;

    /**
     * Constructor for creating an intern cache with the given capacity. The capacity will be adjusted to the next
     * power of 2 if it is not already a power of 2, to a limit of {@code 1 << 30}.
     * The resulting structure is not inherently thread safe.
     *
     * @param capacity the desired capacity for the intern cache
     * @throws IllegalArgumentException if {@code capacity} is negative
     */
    protected AbstractInterner(@NonNegative int capacity){
        int n = Maths.nextPower2(capacity, 128);
        shift = Maths.intLog2(n);
        entries = uncheckedCast(new InternerEntry[n]);
        mask = n - 1;
    }

    /**
     * Returns the 32-bit hash code of the given bytes store and length.
     *
     * @param bs     the bytes store
     * @param length the length
     * @return the 32-bit hash code
     * @throws BufferUnderflowException If there is not enough data in the buffer
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    private static int hash32(@NotNull BytesStore<?, ?> bs, @NonNegative int length) throws IllegalStateException, BufferUnderflowException {
        return bs.fastHash(bs.readPosition(), length);
    }

    /**
     * Interns the given {@link Bytes} instance using all remaining readable bytes
     * starting from its {@code readPosition()}.
     *
     * @param cs the Bytes object to intern
     * @return the cached object instance
     * @throws IORuntimeException       If an I/O error occurs
     * @throws NullPointerException     if {@code cs} is {@code null}
     * @throws BufferUnderflowException If there is not enough data in the buffer
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public T intern(@NotNull Bytes<?> cs)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        return intern((BytesStore) cs, (int) cs.readRemaining());
    }

    /**
     * Interns the given {@link BytesStore} instance using all readable bytes starting
     * from its {@code readPosition()}.
     *
     * @param cs the BytesStore object to intern
     * @return the cached object instance
     * @throws NullPointerException     if {@code cs} is {@code null}
     * @throws IORuntimeException       If an I/O error occurs
     * @throws BufferUnderflowException If there is not enough data in the buffer
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public T intern(@NotNull BytesStore<?, ?> cs)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        return intern(cs, (int) cs.readRemaining());
    }

    /**
     * Interns the specified {@link Bytes} instance reading exactly {@code length}
     * bytes from {@code cs.readPosition()}.
     *
     * @param cs     the Bytes object to intern
     * @param length the length of the Bytes object to intern
     * @return the cached object instance
     * @throws NullPointerException     if {@code cs} is {@code null}
     * @throws IORuntimeException       If an I/O error occurs
     * @throws BufferUnderflowException If there is not enough data in the buffer
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public T intern(@NotNull Bytes<?> cs, @NonNegative int length)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        return intern((BytesStore) cs, length);
    }

    /**
     * Interns the specified {@link BytesStore}. Two possible cache slots are
     * examined for a match using primary and secondary hashes. If neither slot
     * contains a matching entry a new value is created via {@link #getValue(BytesStore, int)}
     * and cached.
     *
     * @param cs     the Bytes to intern
     * @param length number of bytes to read from {@code cs}
     * @return the cached object instance
     * @throws NullPointerException     if {@code cs} is {@code null}
     * @throws IORuntimeException       If an I/O error occurs
     * @throws BufferUnderflowException If there is not enough data in the buffer
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public T intern(@NotNull BytesStore<?, ?> cs, @NonNegative int length)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException {
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
        final byte[] bytes = new byte[length];
        @NotNull BytesStore<?, ?> bs = BytesStore.wrap(bytes);
        IOTools.unmonitor(bs);
        cs.read(cs.readPosition(), bytes, 0, length);
        entries[s == null || (s2 != null && toggle()) ? h : h2] = new InternerEntry<>(bs, t);
        // Store fence ensures the entry is visible before returning
        return t;
    }

    /**
     * Converts the bytes from {@code bs} into an instance of {@code T}. The
     * implementation must read exactly {@code length} bytes starting from
     * {@code bs.readPosition()} without modifying that position.
     *
     * @param bs     the bytes store supplying the data
     * @param length the number of bytes to read
     * @return the value derived from the byte sequence
     * @throws IORuntimeException       if an I/O error occurs
     * @throws BufferUnderflowException if there is insufficient data available
     * @throws ClosedIllegalStateException    if the resource has been released or closed
     * @throws ThreadingIllegalStateException if accessed by multiple threads unsafely
     */
    @NotNull
    protected abstract T getValue(BytesStore<?, ?> bs, @NonNegative int length)
            throws IORuntimeException, IllegalStateException, BufferUnderflowException;

    /**
     * Toggles the internal toggle state and returns its new value.
     *
     * @return the new state of the toggle
     */
    protected boolean toggle() {
        toggle = !toggle;
        return toggle;
    }

    /**
     * Returns the number of non-null values in the interner entries.
     *
     * @return the count of non-null values
     */
    public int valueCount() {
        return (int) Stream.of(entries).filter(Objects::nonNull).count();
    }

    /**
     * Represents an entry in the interner.
     *
     * @param <T> the type of the object being interned
     */
    private static final class InternerEntry<T> {
        /**
         * A heap-based copy of the original byte sequence used for equality checks.
         */
        final BytesStore<?, ?> bytes;
        /**
         * The cached object instance.
         */
        final T t;

        /**
         * Constructs an {@code InternerEntry}.
         *
         * @param bytes a heap-based copy of the byte sequence
         * @param t     the cached value
         */
        InternerEntry(BytesStore<?, ?> bytes, T t) {
            this.bytes = bytes;
            this.t = t;
        }
    }
}

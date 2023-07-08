/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * This class provides a caching mechanism that returns a value which matches the decoded bytes. It does not
 * guarantee the return of the same object across different invocations or from different threads, but it
 * guarantees that the contents will be the same. Although not strictly thread-safe, it behaves correctly
 * under concurrent access.
 * <p>
 * The main usage is to reduce the amount of memory used by creating new objects when the same byte sequence is
 * repeatedly decoded into an object.
 *
 * This cache only guarantees it will provide a String which matches the decoded bytes.
 * <p>
 * It doesn't guarantee it will always return the same object,
 * nor that different threads will return the same object,
 * though the contents should always be the same.
 * <p>
 * While not technically thread safe, it should still behave correctly.
 *
 * Abstract base class for implementing an interning mechanism, which helps
 * in reusing instances of immutable objects. This class is designed to store objects
 * and return previously stored instances that are equal to the required instance.
 * <p>
 * Note: The interning cache may not always return the same object instance, but
 * the contents of the instances will be equal.
 * *
 * @param <T> the type of the object being interned
 * @author peter.lawrey
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AbstractInterner<T> {
    protected final InternerEntry<T>[] entries;
    protected final int mask;
    protected final int shift;
    protected boolean toggle = false;

    /**
     * Constructor for creating an intern cache with the given capacity. The capacity will be adjusted to the next
     * power of 2 if it is not already a power of 2.
     *
     * @param capacity the desired capacity for the intern cache
     * @throws IllegalArgumentException if the calculated capacity exceeds the maximum possible array size
     */
    protected AbstractInterner(@NonNegative int capacity)
            throws IllegalArgumentException {
        int n = Maths.nextPower2(capacity, 128);
        shift = Maths.intLog2(n);
        entries = new InternerEntry[n];
        mask = n - 1;
    }

    /**
     * Returns the 32-bit hash code of the given bytes store and length.
     *
     * @param bs the bytes store
     * @param length the length
     * @return the 32-bit hash code
     * @throws IllegalStateException if the bytes cannot be accessed
     * @throws BufferUnderflowException if there is not enough data in the buffer
     */
    private static int hash32(@NotNull BytesStore bs, @NonNegative int length) throws IllegalStateException, BufferUnderflowException {
        return bs.fastHash(bs.readPosition(), length);
    }
    /**
     * Interns the specified Bytes object. If the Bytes object is already in the cache,
     * this method returns the cached instance; otherwise, it adds the Bytes object to the cache
     * and returns the newly cached instance. The length of Bytes object for interning is determined
     * by the remaining readable bytes.
     *
     * @param cs the Bytes object to intern
     * @return the interned instance
     * @throws IORuntimeException if an I/O error occurs
     * @throws BufferUnderflowException if there is not enough data in the buffer
     * @throws IllegalStateException if the buffer is in an unusable state
     */
    public T intern(@NotNull Bytes<?> cs)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        return intern((BytesStore) cs, (int) cs.readRemaining());
    }

    /**
     * Interns the specified BytesStore object. If the BytesStore object is already in the cache,
     * this method returns the cached instance; otherwise, it adds the BytesStore object to the cache
     * and returns the newly cached instance. The length of BytesStore object for interning is determined
     * by the remaining readable bytes.
     *
     * @param cs the BytesStore object to intern
     * @return the interned instance
     * @throws IORuntimeException if an I/O error occurs
     * @throws BufferUnderflowException if there is not enough data in the buffer
     * @throws IllegalStateException if the buffer is in an unusable state
     */
    public T intern(@NotNull BytesStore cs)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        return intern(cs, (int) cs.readRemaining());
    }

    /**
     * Interns the specified Bytes object of a given length. If the Bytes object is already in the cache,
     * this method returns the cached instance; otherwise, it adds the Bytes object to the cache
     * and returns the newly cached instance.
     *
     * @param cs the Bytes object to intern
     * @param length the length of the Bytes object to intern
     * @return the interned instance
     * @throws IORuntimeException if an I/O error occurs
     * @throws BufferUnderflowException if there is not enough data in the buffer
     * @throws IllegalStateException if the buffer is in an unusable state
     */
    public T intern(@NotNull Bytes<?> cs, @NonNegative int length)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        return intern((BytesStore) cs, length);
    }

    /**
     * Interns the specified Bytes. If the Bytes are already in the cache, this method returns the cached instance;
     * otherwise, it adds the Bytes to the cache and returns the newly cached instance.
     *
     * @param cs the Bytes to intern
     * @param length of bytes to read
     * @return the interned instance
     * @throws IORuntimeException if an I/O error occurs
     * @throws BufferUnderflowException if there is not enough data in the buffer
     * @throws IllegalStateException if the buffer is in an unusable state
     */
    public T intern(@NotNull BytesStore cs, @NonNegative int length)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException {
        if (length > entries.length)
            return getValue(cs, length);
        // Todo: This needs to be reviewed: UnsafeMemory UNSAFE loadFence
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
        @NotNull BytesStore bs = BytesStore.wrap(bytes);
        IOTools.unmonitor(bs);
        cs.read(cs.readPosition(), bytes, 0, length);
        entries[s == null || (s2 != null && toggle()) ? h : h2] = new InternerEntry<>(bs, t);
        // UnsafeMemory UNSAFE storeFence
        return t;
    }

    /**
     * Retrieves the value corresponding to the bytes store and length.
     * This method must be implemented by subclasses.
     *
     * @param bs the bytes store
     * @param length the length of the data in the bytes store
     * @return the value corresponding to the given bytes store and length
     * @throws IORuntimeException if an IO error occurs
     * @throws IllegalStateException if the bytes cannot be accessed
     * @throws BufferUnderflowException if there is not enough data in the buffer
     */
    @NotNull
    protected abstract T getValue(BytesStore bs, @NonNegative int length)
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
        final BytesStore bytes;
        final T t;

        /**
         * Constructs an InternerEntry with the given bytes store and value.
         *
         * @param bytes the bytes store
         * @param t the value
         */
        InternerEntry(BytesStore bytes, T t) {
            this.bytes = bytes;
            this.t = t;
        }
    }
}

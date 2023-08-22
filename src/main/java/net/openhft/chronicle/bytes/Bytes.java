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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.bytes.internal.EmbeddedBytes;
import net.openhft.chronicle.bytes.util.DecoratedBufferOverflowException;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.annotation.SingleThreaded;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.io.*;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;

import static net.openhft.chronicle.bytes.internal.ReferenceCountedUtil.throwExceptionIfReleased;
import static net.openhft.chronicle.core.util.Longs.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * The {@code Bytes} class is a versatile container for raw byte data, providing rich functionality to read and write
 * data in various formats including integers, longs, floating-point values, strings, and more. It also supports
 * advanced operations such as seeking, slicing, and copying.
 * <p>
 * This class serves as an abstraction layer over a ByteBuffer, byte[], POJO, or native memory
 *
 * <p>
 * Besides basic primitive types, {@code Bytes} can serialize and deserialize more complex structures,
 * including custom user-defined objects that implement {@code ReadBytesMarshallable} or {@code WriteBytesMarshallable}.
 *
 * <p>
 * {@code Bytes} can be used for a variety of purposes such as:
 * <ul>
 *     <li>Reading and writing binary data to and from files</li>
 *     <li>Bytes level serialization and deserialization of objects</li>
 *     <li>Performing low-level binary manipulation for networking applications</li>
 *     <li>Temporary storage (in-memory) of binary data</li>
 *     <li>Shared memory in memory mapped files</li>
 * </ul>
 *
 * <p>
 * {@code Bytes} is essentially a view of a region within a {@link BytesStore} and is equipped with separate read and
 * write cursors, boasting a 63-bit addressing capacity (approximately 8 EiB), as opposed to Java's built-in
 * {@link ByteBuffer}, which only supports 31-bit addressing with a single cursor.
 *
 * <p>
 * A {@code Bytes} instance can represent either a fixed region of memory or an elastic buffer that dynamically
 * resizes as needed. It is mutable and designed for single-threaded use as it is not thread-safe.
 *
 * <p>
 * The cursors within Bytes consist of a write-position and a read-position, which must adhere to these constraints:
 * <ul>
 *     <li>{@code start() <= readPosition() <= writePosition() <= writeLimit() <= capacity()}</li>
 *     <li>{@code readLimit() == writePosition() && readPosition() <= safeLimit()}</li>
 * </ul>
 * <p>
 * It is important to note that a Bytes object is a resource that is ReferenceCounted. Operations on a Bytes
 * object that has been released may result in an {@link IllegalStateException}.
 * <p>
 * Note: Some operations on {@code Bytes} may throw {@code IllegalStateException} if the underlying storage has been released.
 * Always ensure proper release of resources when finished with a {@code Bytes} instance, especially if it's backed by system resources
 * like file handles or network sockets.
 *
 * @param <U> The type of the {@link BytesStore} that backs this {@code Bytes} instance.
 * @see BytesStore
 * @see ReadBytesMarshallable
 * @see WriteBytesMarshallable
 */
@SingleThreaded
@SuppressWarnings({"rawtypes", "unchecked"})
public interface Bytes<U> extends
        BytesStore<Bytes<U>, U>,
        BytesIn<U>,
        BytesOut<U>,
        SingleThreadedChecked {

    /**
     * The maximum capacity a Bytes object can have, approximately 8 EiB (Exbibytes) minus 16 bytes.
     */
    long MAX_CAPACITY = Long.MAX_VALUE & ~0xF;

    /**
     * The maximum capacity a Bytes object can have if it is allocated on the heap, which is 2 GiB (Gibibytes) minus 16 bytes.
     */
    int MAX_HEAP_CAPACITY = Integer.MAX_VALUE & ~0xF;

    /**
     * The default initial size of an elastic Bytes object backed by a ByteBuffer, which is 256 bytes.
     */
    int DEFAULT_BYTE_BUFFER_CAPACITY = 256;

    /**
     * Creates and returns a new elastic wrapper for a direct (off-heap) ByteBuffer with a default capacity,
     * which can be resized as needed. The default capacity is determined by the value of {@code DEFAULT_BYTE_BUFFER_CAPACITY}.
     * <p>
     * The memory for the returned Bytes object is allocated off-heap, meaning it's not under the JVM's garbage collector's
     * immediate purview. It's crucial to conscientiously release these resources when they're no longer required to ensure
     * timely memory deallocation. If these resources are not manually released, they will still eventually be reclaimed by
     * the garbage collector, but possibly with a delay.
     *
     * @return a new elastic wrapper for an off-heap ByteBuffer with default capacity
     */
    @NotNull
    static Bytes<ByteBuffer> elasticByteBuffer() {
        return elasticByteBuffer(DEFAULT_BYTE_BUFFER_CAPACITY);
    }

    /**
     * Creates and returns a new elastic wrapper for a direct (off-heap) ByteBuffer with
     * the given {@code initialCapacity} which will be resized as required.
     * <p>
     * The memory for the returned Bytes object is allocated off-heap, meaning it's not under the JVM's garbage collector's
     * immediate purview. It's crucial to conscientiously release these resources when they're no longer required to ensure
     * timely memory deallocation. If these resources are not manually released, they will still eventually be reclaimed by
     * the garbage collector, but possibly with a delay.
     *
     * @param initialCapacity the initial non-negative capacity given in bytes
     * @return a new elastic wrapper for an off-heap ByteBuffer with the specified initial capacity
     * @throws IllegalArgumentException if the provided {@code initialCapacity} is negative.
     */
    @NotNull
    static Bytes<ByteBuffer> elasticByteBuffer(@NonNegative int initialCapacity) {
        return elasticByteBuffer(initialCapacity, MAX_HEAP_CAPACITY);
    }

    /**
     * Creates and returns a new elastic wrapper for a direct (off-heap) ByteBuffer with
     * the given {@code initialCapacity} which will be resized as required up
     * to the given {@code maxCapacity}.
     * <p>
     * The memory for the returned Bytes object is allocated off-heap, meaning it's not under the JVM's garbage collector's
     * immediate purview. It's crucial to conscientiously release these resources when they're no longer required to ensure
     * timely memory deallocation. If these resources are not manually released, they will still eventually be reclaimed by
     * the garbage collector, but possibly with a delay.
     * <p>
     * This method allows for fine-tuned control over the buffer's capacity, enabling performance optimization
     * and resource management.
     *
     * @param initialCapacity the initial non-negative capacity given in bytes
     * @param maxCapacity     the maximum capacity given in bytes; must be non-negative and at least as large as {@code initialCapacity}
     * @return a new elastic wrapper for an off-heap ByteBuffer with the specified initial capacity and maximum capacity
     * @throws IllegalArgumentException if the provided {@code initialCapacity} or {@code maxCapacity} is negative,
     *                                  or if {@code maxCapacity} is less than {@code initialCapacity}.
     */
    @NotNull
    static Bytes<ByteBuffer> elasticByteBuffer(@NonNegative final int initialCapacity,
                                               @NonNegative final int maxCapacity) {
        requireNonNegative(initialCapacity);
        requireNonNegative(maxCapacity);

        @NotNull BytesStore<?, ByteBuffer> bs = BytesStore.elasticByteBuffer(initialCapacity, maxCapacity);
        try {
            return bs.bytesForWrite();
        } finally {
            bs.release(ReferenceOwner.INIT);
        }
    }

    /**
     * Creates and returns a new elastic wrapper for a heap ByteBuffer, with the provided {@code initialCapacity},
     * that can dynamically resize as required.
     * <p>
     * The returned Bytes object utilizes heap memory, which is managed by the JVM's garbage collector.
     * This is suitable for scenarios where the buffer size may change, and the application requires
     * the flexibility of automatic memory management.
     * <p>
     * Please note that, since this is heap memory, the garbage collector will manage the memory deallocation.
     *
     * @param initialCapacity the initial capacity of the buffer in bytes; must be non-negative
     * @return a new elastic wrapper for a heap ByteBuffer
     * @throws IllegalArgumentException if the provided {@code initialCapacity} is negative
     */
    @NotNull
    static Bytes<ByteBuffer> elasticHeapByteBuffer(@NonNegative int initialCapacity) {
        requireNonNegative(initialCapacity);
        @NotNull BytesStore<?, ByteBuffer> bs = BytesStore.wrap(ByteBuffer.allocate(initialCapacity));
        try {
            return NativeBytes.wrapWithNativeBytes(bs, Bytes.MAX_HEAP_CAPACITY);
        } finally {
            bs.release(INIT);
        }
    }

    /**
     * Creates and returns a new elastic wrapper for a heap ByteBuffer with a default {@code initialCapacity}
     * of 128 bytes, that can dynamically resize as required.
     * <p>
     * This is a convenience method that behaves similarly to {@code elasticHeapByteBuffer(int initialCapacity)},
     * but with a preset default initial capacity.
     * <p>
     * The returned Bytes object utilizes heap memory, which is managed by the JVM's garbage collector.
     *
     * @return a new elastic wrapper for a heap ByteBuffer with default initial capacity of 128 bytes
     */
    @NotNull
    static Bytes<ByteBuffer> elasticHeapByteBuffer() {
        return elasticHeapByteBuffer(128);
    }

    /**
     * Creates and returns a new Bytes view that maps to a group of fields within the provided {@code object}.
     * The group of fields is identified by the {@link FieldGroup} annotation with a name specified by {@code groupName}.
     * This method effectively turns the group of fields into a contiguous memory segment that can be accessed and manipulated
     * as a single entity using the returned Bytes object.
     *
     * <p>Example of using {@link FieldGroup} to group fields:
     * <pre>{@code
     *     static class Padding extends Parent {
     *         {@literal @}FieldGroup("p")
     *         // 128 bytes
     *         transient long p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15;
     *     }
     * }</pre>
     *
     * <p>Example of creating a Bytes view for a field group:
     * <pre>{@code
     *     Padding example = new Padding();
     *     Bytes<Padding> bytes = Bytes.forFieldGroup(example, "p");
     * }</pre>
     * <p><b>Note:</b> The data in the resulting Bytes view is preceded by the length of the data encoded as an unsigned byte.
     * This consumes one byte of storage and limits the maximum size of the data in the view to 255 usable bytes.
     *
     * @param <T>       The type of the underlying object
     * @param object    The non-null object containing the fields to be mapped
     * @param groupName The non-null name of the field group as specified in {@link FieldGroup}
     * @return A new Bytes view mapping to the specified field group within the provided object
     * @throws NullPointerException  If the provided {@code object} or the provided {@code groupName} is {@code null}
     */
    static <T> Bytes<T> forFieldGroup(@NotNull final T object,
                                      @NotNull final String groupName) {
        requireNonNull(object);
        requireNonNull(groupName);
        @NotNull BytesStore<?, T> bs = BytesStore.forFields(object, groupName, 1);
        try {
            final EmbeddedBytes<T> bytes = EmbeddedBytes.wrap(bs);
            return bytes.writeLimit(bs.writeLimit());
        } finally {
            bs.release(INIT);
        }
    }

    /**
     * Creates and returns a new Bytes instance that wraps the provided {@code byteBuffer}.
     * The returned Bytes instance is configured for reading from the current position of the
     * {@code byteBuffer} up to its limit.
     *
     * <p>This method is provided for convenience. However, it's important to note that this method
     * may create intermediate objects, which might not be ideal for performance-sensitive applications
     * that require minimizing garbage creation. To avoid creating intermediate objects, consider using
     * a different approach as demonstrated in the example below.
     *
     * @param byteBuffer The ByteBuffer to wrap. Must be non-null and must not be read-only.
     * @return A new Bytes instance wrapping the provided {@code byteBuffer}, ready for reading.
     * @throws NullPointerException     If the provided {@code byteBuffer} is {@code null}.
     * @throws ReadOnlyBufferException  If the provided {@code byteBuffer} is read-only.
     * @throws BufferUnderflowException If there is not enough data in the buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    static Bytes<ByteBuffer> wrapForRead(@NotNull final ByteBuffer byteBuffer)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNull(byteBuffer);
        BytesStore<?, ByteBuffer> bs = BytesStore.wrap(byteBuffer);
        try {
            Bytes<ByteBuffer> bytesForRead = bs.bytesForRead();
            bytesForRead.readLimit(byteBuffer.limit());
            bytesForRead.readPosition(byteBuffer.position());
            return bytesForRead;
        } finally {
            bs.release(INIT);
        }
    }

    /**
     * Creates and returns a new Bytes instance that wraps the provided {@code byteBuffer}.
     * The returned Bytes instance is configured for writing, starting at the current position of the
     * {@code byteBuffer} up to its limit.
     *
     * <p>Important Note: When the returned Bytes instance is closed, any direct {@code byteBuffer} will be deallocated
     * and should no longer be used.
     *
     * @param byteBuffer The ByteBuffer to wrap. Must be non-null and must not be read-only.
     * @return A new Bytes instance wrapping the provided {@code byteBuffer}, ready for writing.
     * @throws NullPointerException    If the provided {@code byteBuffer} is {@code null}.
     * @throws ReadOnlyBufferException If the provided {@code byteBuffer} is read-only.
     * @throws BufferOverflowException If there is not enough space in the buffer.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    static Bytes<ByteBuffer> wrapForWrite(@NotNull final ByteBuffer byteBuffer) throws ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNull(byteBuffer);
        BytesStore<?, ByteBuffer> bs = BytesStore.wrap(byteBuffer);
        try {
            Bytes<ByteBuffer> bytesForWrite = bs.bytesForWrite();
            bytesForWrite.writePosition(byteBuffer.position());
            bytesForWrite.writeLimit(byteBuffer.limit());
            return bytesForWrite;
        } finally {
            bs.release(INIT);
        }
    }

    /**
     * Creates and returns a new Bytes instance that wraps the provided {@code byteArray}.
     * The returned Bytes instance is configured for reading, starting from the beginning of the
     * {@code byteArray} up to its length.
     *
     * <p>Important Note: This method creates intermediate objects and might not be ideal for performance-sensitive applications
     * due to garbage creation.
     *
     * @param byteArray The byte array to wrap. Must be non-null.
     * @return A new Bytes instance wrapping the provided {@code byteArray}, ready for reading.
     * @throws NullPointerException If the provided {@code byteArray} is {@code null}.
     */
    @NotNull
    static Bytes<byte[]> wrapForRead(byte[] byteArray) {
        requireNonNull(byteArray);
        @NotNull BytesStore<?, byte[]> bs = BytesStore.wrap(byteArray);
        try {
            return bs.bytesForRead();
        } finally {
            bs.release(INIT);
        }
    }

    /**
     * Creates and returns a new Bytes instance that wraps the provided {@code byteArray} and
     * is ready for writing operations.
     *
     * <p>Important Note: This method is intended for convenience, but might not be optimal for performance-sensitive scenarios
     * as it creates intermediate objects which might lead to garbage collection overhead.
     *
     * @param byteArray The byte array to wrap for writing operations. Must be non-null.
     * @return A new Bytes instance wrapping the provided {@code byteArray}, ready for writing.
     * @throws NullPointerException If the provided {@code byteArray} is {@code null}.
     */
    @NotNull
    static Bytes<byte[]> wrapForWrite(byte[] byteArray) {
        requireNonNull(byteArray);
        final BytesStore bs = BytesStore.wrap(byteArray);
        try {
            return bs.bytesForWrite();
        } finally {
            bs.release(INIT);
        }
    }

    /**
     * Constructs and returns a new Bytes instance which contains the provided {@code text} encoded in ISO-8859-1.
     * The returned Bytes instance is ready for reading and is allocated on the heap.
     *
     * @param text The non-null text to be converted and wrapped in a Bytes instance.
     * @return A new Bytes instance containing the provided text encoded in ISO-8859-1.
     * @throws NullPointerException If the provided {@code text} is {@code null}.
     */
    @NotNull
    static Bytes<byte[]> from(@NotNull CharSequence text) {
        requireNonNull(text);
        return from(text.toString());
    }

    /**
     * Constructs and returns a new Bytes instance which contains the provided {@code text} encoded in ISO-8859-1.
     * The returned Bytes instance is ready for reading and is allocated using native memory.
     *
     * @param text The non-null text to be converted and wrapped in a Bytes instance.
     * @return A new Bytes instance containing the provided text encoded in ISO-8859-1.
     * @throws NullPointerException If the provided {@code text} is {@code null}.
     */
    static Bytes<Void> fromDirect(@NotNull CharSequence text) {
        requireNonNull(text);
        return NativeBytes.nativeBytes(text.length()).append(text);
    }

    /**
     * Constructs and returns a new Bytes instance which contains the provided {@code text} encoded in ISO-8859-1.
     * The returned Bytes instance is ready for reading and is allocated using native memory.
     *
     * @param text The non-null text to be converted and wrapped in a Bytes instance.
     * @return A new Bytes instance containing the provided text encoded in ISO-8859-1.
     * @throws NullPointerException  If the provided {@code text} is {@code null}.
     */
    @NotNull
    static Bytes<byte[]> directFrom(@NotNull String text) {
        BytesStore<?, byte[]> from = BytesStore.from(text);
        try {
            return from.bytesForRead();
        } finally {
            from.release(INIT);
        }
    }

    /**
     * Creates and returns a new  object containing the given {@code text} encoded in ISO-8859-1 character set.
     * <p>
     * The returned {@code Bytes} object is allocated on the heap and is ready for reading.
     * <p>
     * The ISO-8859-1 encoding is used because it is a single-byte encoding that supports the first 256 Unicode characters,
     * making it well-suited for encoding texts that primarily consist of Western European characters.
     *
     * @param text A non-null String to be converted into bytes using ISO-8859-1 encoding and wrapped in a {@code Bytes} object.
     * @return A new {@code Bytes} object containing the text, encoded using the ISO-8859-1 character set.
     * @throws NullPointerException If the provided {@code text} is {@code null}.
     */
    @NotNull
    static Bytes<byte[]> from(@NotNull String text) {
        return wrapForRead(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    /**
     * Creates and returns an empty, fixed-size, immutable  object.
     *
     * @return An empty {@code Bytes} object with fixed size and immutable contents.
     */
    static Bytes<?> empty() {
        return BytesStore.empty().bytesForRead();
    }

    /**
     * A reflection-friendly alias for the {@link #from(String)} method.
     * <p>
     * This method is functionally equivalent to {@link #from(String)} and is primarily intended for invocation through reflection mechanisms.
     *
     * <p>
     * It creates and returns a new  object containing the given {@code text}, encoded in the ISO-8859-1 character set.
     *
     * @param text A non-null String to be converted into bytes using ISO-8859-1 encoding and wrapped in a {@code Bytes} object.
     * @return A new {@code Bytes} object containing the text, encoded using the ISO-8859-1 character set.
     * @throws NullPointerException If the provided {@code text} is {@code null}.
     */
    @UsedViaReflection
    static Bytes<byte[]> valueOf(String text) {
        return from(text);
    }

    /**
     * Creates and returns a new {@link VanillaBytes} object that is a fixed-size wrapper for native memory
     * (accessible via 64-bit addresses) with the specified {@code capacity}.
     * <p>
     * This method is used to allocate a fixed amount of native memory, which can be useful for performance-sensitive applications.
     *
     * @param capacity The non-negative capacity of the native memory to be allocated, measured in bytes.
     * @return A new {@code VanillaBytes} object that wraps the allocated native memory.
     * @throws IllegalArgumentException If the provided {@code capacity} is negative.
     */
    @NotNull
    static VanillaBytes<Void> allocateDirect(@NonNegative long capacity)
            throws IllegalArgumentException {
        @NotNull BytesStore<?, Void> bs = BytesStore.nativeStoreWithFixedCapacity(requireNonNegative(capacity));
        try {
            return new NativeBytes<>(bs);
        } finally {
            bs.release(INIT);
        }
    }

    /**
     * Creates and returns a new elastic wrapper for native memory (accessible via 64-bit addresses)
     * with an initial capacity of zero bytes. The capacity of the wrapper will be automatically
     * resized as needed.
     * <p>
     * Native memory allocation can be faster and is not subject to Java's garbage collection,
     * making it suitable for performance-sensitive applications.
     *
     * @return A new {@code NativeBytes} object representing an elastic wrapper for native memory.
     */
    @NotNull
    static NativeBytes<Void> allocateElasticDirect() {
        return NativeBytes.nativeBytes();
    }

    /**
     * Creates and returns a new elastic wrapper for native memory (accessible via 64-bit addresses)
     * with the given {@code initialCapacity}. The capacity of the wrapper will be automatically
     * resized as needed.
     * <p>
     * Native memory allocation can be faster and is not subject to Java's garbage collection,
     * making it suitable for performance-sensitive applications.
     *
     * @param initialCapacity The initial non-negative capacity of the wrapper in bytes.
     * @return A new {@code NativeBytes} object representing an elastic wrapper for native memory.
     * @throws IllegalArgumentException If the provided {@code initialCapacity} is negative.
     */
    @NotNull
    static NativeBytes<Void> allocateElasticDirect(@NonNegative long initialCapacity)
            throws IllegalArgumentException {
        return NativeBytes.nativeBytes(requireNonNegative(initialCapacity));
    }

    /**
     * Creates and returns a new elastic wrapper for memory allocated on the heap,
     * with an initial capacity of 32 bytes. The capacity of the wrapper will be
     * automatically resized as needed.
     * <p>
     * Heap memory allocation is managed by the Java Virtual Machine and is subject
     * to garbage collection.
     *
     * @return A new {@code OnHeapBytes} object representing an elastic wrapper for heap memory.
     */
    @NotNull
    static OnHeapBytes allocateElasticOnHeap() {
        return allocateElasticOnHeap(32);
    }

    /**
     * Creates and returns a new elastic wrapper for memory allocated on the heap,
     * with the specified {@code initialCapacity}. The capacity of the wrapper will
     * be automatically resized as needed.
     * <p>
     * Heap memory allocation is managed by the Java Virtual Machine and is subject
     * to garbage collection.
     *
     * @param initialCapacity The initial non-negative capacity of the wrapper in bytes.
     * @return A new {@code OnHeapBytes} object representing an elastic wrapper for heap memory.
     * @throws IllegalArgumentException If the provided {@code initialCapacity} is negative.
     */
    @NotNull
    static OnHeapBytes allocateElasticOnHeap(@NonNegative int initialCapacity) {
        requireNonNegative(initialCapacity);
        BytesStore<?, byte[]> wrap = BytesStore.wrap(new byte[initialCapacity]);
        try {
            return new OnHeapBytes(wrap, true);
        } finally {
            wrap.release(INIT);
        }
    }

    /**
     * Creates and returns a substring from the provided {@code buffer} between its current
     * {@code readPosition} and {@code readLimit}.
     * The buffer's state remains unchanged by this method.
     * <p>
     * If the length of the extracted substring is greater than {@code maxLen},
     * an ellipsis "..." is appended to the resulting string. Note that in this case, the
     * length of the returned string will be {@code maxLen + 3}.
     *
     * @param buffer The buffer to extract the string from. Must not be {@code null}.
     * @return A string extracted from the buffer, possibly truncated and appended with an ellipsis.
     * @throws BufferUnderflowException If there is not enough data in the buffer.
     * @throws IllegalArgumentException If {@code maxLen} is negative.
     * @throws NullPointerException     If the provided {@code buffer} is {@code null}.
     */
    @NotNull
    static String toString(@NotNull final Bytes<?> buffer)
            throws BufferUnderflowException, IllegalArgumentException {
        return toString(buffer, MAX_HEAP_CAPACITY);
    }

    /**
     * Creates and returns a substring from the provided {@code buffer} between its current
     * {@code readPosition} and {@code readLimit}, truncated to the specified maximum length.
     * The buffer's state remains unchanged by this method.
     * <p>
     * If the length of the extracted substring is greater than {@code maxLen},
     * an ellipsis "..." is appended to the resulting string. Note that in this case, the
     * length of the returned string will be {@code maxLen + 3}.
     *
     * @param buffer The buffer to extract the string from. Must not be {@code null}.
     * @param maxLen The maximum length of the string to be extracted.
     * @return A string extracted from the buffer, possibly truncated and appended with an ellipsis.
     * @throws BufferUnderflowException If there is not enough data in the buffer.
     * @throws IllegalArgumentException If {@code maxLen} is negative.
     * @throws NullPointerException     If the provided {@code buffer} is {@code null}.
     */
    @NotNull
    static String toString(@NotNull final Bytes<?> buffer,
                           @NonNegative final long maxLen)
            throws BufferUnderflowException, IllegalArgumentException {
        requireNonNegative(maxLen);
        if (buffer.refCount() < 1)
            // added because something is crashing the JVM
            return "<unknown>";
        try {
            ReferenceOwner toString = ReferenceOwner.temporary("toString");
            buffer.reserve(toString);
            try {

                if (buffer.readRemaining() == 0)
                    return "";

                final long length = Math.min(maxLen + 1, buffer.readRemaining());

                @NotNull final StringBuilder builder = new StringBuilder();

                final long readPosition = buffer.readPosition();
                try {
                    buffer.readWithLength(length, b -> {
                        while (buffer.readRemaining() > 0) {
                            if (builder.length() >= maxLen) {
                                builder.append("...");
                                break;
                            }
                            builder.append((char) buffer.readByte());
                        }
                    });
                } catch (Exception e) {
                    builder.append(' ').append(e);
                } finally {
                    buffer.readPosition(readPosition);
                }
                return builder.toString();
            } finally {
                buffer.release(toString);
            }
        } catch (Exception e) {
            return e.toString();
        }
    }

    /**
     * Extracts a string from the provided {@code buffer} starting at the specified {@code position},
     * and spanning for the specified {@code length} number of characters. The buffer's state
     * remains unchanged by this method.
     * <p>
     * The method reads {@code length} bytes from the {@code buffer}, starting at {@code position},
     * and constructs a string from these bytes.
     *
     * @param buffer   The buffer to extract the string from. Must not be {@code null}.
     * @param position The position in the buffer to start extracting the string from.
     * @param length   The number of characters to include in the extracted string.
     * @return A string extracted from the buffer.
     * @throws IllegalArgumentException If the provided {@code position} or {@code length} is negative.
     * @throws NullPointerException     If the provided {@code buffer} is {@code null}.
     */
    @NotNull
    static String toString(@NotNull final Bytes<?> buffer,
                           @NonNegative final long position,
                           @NonNegative final long length) {
        requireNonNull(buffer);
        requireNonNegative(position);
        requireNonNegative(length);
        try {
            final long pos = buffer.readPosition();
            final long limit = buffer.readLimit();
            buffer.readPositionRemaining(position, length);

            try {
                @NotNull final StringBuilder builder = new StringBuilder();
                while (buffer.readRemaining() > 0) {
                    builder.append((char) buffer.readByte());
                }

                // remove the last comma
                return builder.toString();
            } finally {
                buffer.readLimit(limit);
                buffer.readPosition(pos);
            }
        } catch (Exception e) {
            return e.toString();
        }
    }

    /**
     * Creates and returns a new fix sized wrapper for native (64-bit address)
     * memory with the contents copied from the given {@code bytes} array.
     * <p>
     * Changes in the given {@code bytes} will not be affected by writes in
     * the returned wrapper or vice versa.
     *
     * @param bytes array to copy, the array reference must not be null
     * @return a new fix sized wrapper
     * @throws NullPointerException If the provided {@code bytes} is {@code null}
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    static VanillaBytes allocateDirect(byte[] bytes) throws ClosedIllegalStateException, ThreadingIllegalStateException {
        VanillaBytes<Void> result = allocateDirect(bytes.length);
        result.write(bytes);
        return result;
    }

    /**
     * Converts a hexadecimal string, generated by one of the {@code toHexString} methods, back into a {@code Bytes} object.
     * <p>
     * This method takes a string that represents hexadecimal data along with comments (which describe the meaning of
     * the bytes), and converts it back into a {@code Bytes} object. The input string should have been generated by the
     * {@code toHexString} method. It can contain hexadecimal values, spaces, and comments. The method is able to
     * correctly parse and ignore spaces and comments during conversion.
     * <p>
     * For example, given the string:
     * <pre>
     * "c3 76 6d 68                                     # vmh:
     *  b6 03 56 4d 48                                  # VMH"
     * </pre>
     * This method would convert it to a {@code Bytes} object representing the original bytes before they were converted
     * to the hexadecimal representation.
     *
     * @param s the hexadecimal string to convert, which must have been generated by the {@code toHexString} method.
     *          This parameter must not be {@code null}.
     * @return a {@code Bytes} object representing the original byte values of the provided hexadecimal string.
     * @throws IllegalArgumentException If the provided string is not in the valid format produced by the {@code toHexString}
     *                                  method.
     */
    @NotNull
    static Bytes<?> fromHexString(@NotNull String s) {
        return BytesInternal.fromHexString(s);
    }

    /**
     * Creates and returns a new Bytes which is optionally unchecked as indicated by the provided {@code unchecked}.
     * <p>
     * This allows bounds checks to be turned off.
     * Note: this means that the result is no longer elastic, even if {@code this} is elastic.
     * Note: It is only possible to go from a checked Bytes to an unchecked bytes and not vice versa.
     *
     * @param unchecked if true, minimal bounds checks will be performed
     * @return a new, potentially unchecked, Bytes
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way.
     */
    @NotNull
    default Bytes<U> unchecked(boolean unchecked)
            throws ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfReleased(this);
        if (unchecked) {
            if (isElastic())
                BytesUtil.WarnUncheckedElasticBytes.warn();
            Bytes<U> underlyingBytes = start() == 0 && bytesStore().isDirectMemory()
                    ? new UncheckedNativeBytes<>(this)
                    : new UncheckedBytes<>(this);
            release(INIT);
            return underlyingBytes;
        }
        return this;
    }

    /**
     * Checks whether this Bytes object operates in an unchecked mode.
     * <p>
     * When a Bytes object is unchecked, it performs minimal or no bounds checking on read and write operations.
     * This can improve performance but may result in undefined behavior if attempting to access out-of-bounds elements.
     *
     * @return {@code true} if this Bytes object operates in unchecked mode; {@code false} otherwise.
     */
    default boolean unchecked() {
        return false;
    }

    @Override
    default long safeLimit() {
        return bytesStore().safeLimit();
    }

    @Override
    default boolean isClear() {
        return start() == readPosition() && writeLimit() == capacity();
    }

    /**
     * @inheritDoc <P>
     * If this Bytes {@link #isElastic()} the {@link #realCapacity()} can be
     * lower than the virtual {@link #capacity()}.
     */
    @Override
    default @NonNegative long realCapacity() {
        return BytesStore.super.realCapacity();
    }

    /**
     * Creates and returns a deep copy of this Bytes object, including the data between
     * {@link RandomCommon#readPosition()} and {@link RandomCommon#readLimit()}.
     * <p>
     * The copy will have its own separate storage and state, and modifications to the copy will not affect the original
     * Bytes object, and vice versa.
     *
     * @return A new Bytes object that is a copy of this object.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    BytesStore<Bytes<U>, U> copy()
            throws IllegalStateException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Creates and returns a new String representing the contents of this Bytes object in hexadecimal form,
     * with comments describing the meaning of the bytes. The representation starts at the {@code readPosition()}
     * and encodes at most 1024 bytes.
     * <p>
     * Each line of the output string contains hexadecimal representation of 16 bytes, followed by comments
     * describing the meaning of those bytes.
     * <p>
     * For example, if the Bytes object contains bytes representing the ASCII string "VMH",
     * the output would be similar to this:
     * <pre>
     * c3 76 6d 68                                     # vmh:
     * b6 03 56 4d 48                                  # VMH
     * </pre>
     *
     * @return A new hexadecimal String representing the content with comments describing the bytes.
     */
    @NotNull
    default String toHexString() {
        return toHexString(1024);
    }

    /**
     * Creates and returns a new String representing the contents of this Bytes object in hexadecimal form,
     * with comments describing the meaning of the bytes. The representation starts at the {@code readPosition()}
     * and encodes at most the provided {@code maxLength} bytes.
     * <p>
     * Each line of the output string contains hexadecimal representation of 16 bytes, followed by comments
     * describing the meaning of those bytes.
     * <p>
     * For example, if the Bytes object contains bytes representing the ASCII string "VMH", and maxLength is
     * large enough, the output would be similar to this:
     * <pre>
     * c3 76 6d 68                                     # vmh:
     * b6 03 56 4d 48                                  # VMH
     * </pre>
     *
     * @param maxLength Limit the number of bytes to be encoded.
     * @return A new hexadecimal String representing the content with comments describing the bytes.
     * @throws IllegalArgumentException If the provided {@code maxLength} is negative.
     */
    @NotNull
    default String toHexString(@NonNegative long maxLength) {
        return toHexString(readPosition(), maxLength);
    }

    /**
     * Creates and returns a new String representing the contents of this Bytes object in hexadecimal form,
     * with comments describing the meaning of the bytes. The representation starts at the provided {@code offset}
     * and encodes at most the provided {@code maxLength} bytes.
     * <p>
     * The method reads data starting from the specified offset. Each line of the output string contains
     * hexadecimal representation of 16 bytes, followed by comments describing the meaning of those bytes.
     * If the number of bytes from the offset to the end exceeds {@code maxLength}, the output string
     * will be suffixed with "... truncated".
     * <p>
     * For example, if the Bytes object contains bytes representing the ASCII string "VMH", and offset is 0,
     * and maxLength is large enough, the output would be similar to this:
     * <pre>
     * c3 76 6d 68                                     # vmh:
     * b6 03 56 4d 48                                  # VMH
     * </pre>
     *
     * @param offset    The starting offset within the Bytes object to begin the hexadecimal conversion.
     * @param maxLength Limit the number of bytes to be encoded. If the number of bytes exceeds maxLength,
     *                  the output will be suffixed with "... truncated".
     * @return A new hexadecimal String representing the content with comments describing the bytes.
     * @throws IllegalArgumentException If the provided {@code offset} or {@code maxLength} is negative.
     */
    @NotNull
    default String toHexString(@NonNegative long offset, @NonNegative long maxLength) {
        requireNonNegative(offset);
        requireNonNegative(maxLength);

        final long maxLength2 = Math.min(maxLength, readLimit() - offset);
        final String ret = BytesInternal.toHexString(this, offset, maxLength2);
        return maxLength2 < readLimit() - offset ? ret + "... truncated" : ret;
    }

    /**
     * Checks if this Bytes object is elastic, meaning it can dynamically resize when more data is written
     * than its current {@link #realCapacity()}.
     * <p>
     * Elastic Bytes objects can automatically grow to accommodate additional data, whereas
     * non-elastic ones have a fixed capacity.
     *
     * @return {@code true} if this Bytes object is elastic; {@code false} otherwise.
     */
    boolean isElastic();

    /**
     * Ensures that this Bytes object has the capacity to accommodate the specified amount of data.
     * <p>
     * If this Bytes object is elastic and doesn't have enough capacity, it will be resized. If it is not
     * elastic and doesn't have enough capacity, a {@link DecoratedBufferOverflowException} will be thrown.
     *
     * @param desiredCapacity The minimum capacity, in bytes, that is required.
     * @throws DecoratedBufferOverflowException If the Bytes object is not elastic and there isn't enough space
     *                                          or if the provided {@code desiredCapacity} is negative.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    default void ensureCapacity(@NonNegative long desiredCapacity)
            throws DecoratedBufferOverflowException, IllegalStateException, ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNegative(desiredCapacity);
        if (desiredCapacity > capacity())
            throw new DecoratedBufferOverflowException(isElastic() ? "Resizing required" : "Buffer is not elastic");
    }

    /**
     * Creates and returns a new slice of this Bytes object with its start position set to the current
     * read position and its capacity determined by the current limit.
     * <p>
     * Note that the slice is a subsection of this Bytes object and will not be elastic regardless of
     * the elasticity of the parent Bytes object.
     *
     * @return A new slice of this Bytes object.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    @Override
    default Bytes<U> bytesForRead()
            throws IllegalStateException, ClosedIllegalStateException, ThreadingIllegalStateException {
        throwExceptionIfReleased(this);

        BytesStore bytesStore = bytesStore();
        assert bytesStore != null : "bytesStore is null";
        return isClear()
                ? bytesStore.bytesForRead()
                : new SubBytes<>(bytesStore, readPosition(), readLimit() + start());
    }

    /**
     * Creates and returns a Bytes object that wraps the bytesStore of this Bytes object,
     * ranging from the {@code start} position to the {@code realCapacity}.
     * <p>
     * The returned Bytes object is non-elastic and supports both read and write operations using cursors.
     *
     * @return A Bytes object wrapping the bytesStore of this Bytes object.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    @NotNull
    default Bytes<U> bytesForWrite()
            throws IllegalStateException, ClosedIllegalStateException {
        throwExceptionIfReleased(this);

        BytesStore bytesStore = bytesStore();
        assert bytesStore != null : "bytesStore is null";
        return new VanillaBytes<>(bytesStore, writePosition(), writeLimit());
    }

    /**
     * Returns the backing BytesStore that this Bytes object wraps. If this Bytes object doesn't have
     * a backing BytesStore, this method returns null.
     * <p>
     * The BytesStore represents the underlying storage of bytes that the Bytes object is manipulating.
     *
     * @return The backing BytesStore, or null if there isn't one.
     */
    @Override
    @Nullable
    BytesStore bytesStore();

    /**
     * Compares the contents of this Bytes object with the provided {@code other} string for equality.
     * <p>
     * This method returns {@code true} if the contents of this Bytes object is equal to the contents
     * of the {@code other} string. Otherwise, it returns {@code false}.
     *
     * @param other The string to compare with the contents of this Bytes object, can be null.
     * @return {@code true} if the contents of this Bytes object equals the contents of the provided
     * {@code other} string; {@code false} otherwise.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    default boolean isEqual(@Nullable String other)
            throws IllegalStateException {
        return StringUtils.isEqual(this, other);
    }

    /**
     * Compacts this Bytes object by moving the read position to the start.
     * <p>
     * This operation is useful to free up space for writing by discarding bytes that have already
     * been read.
     *
     *
     * @return This Bytes object, for method chaining.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @NotNull
    Bytes<U> compact()
            throws ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Copies bytes from this Bytes object into the provided {@code targetByteStore}.
     * <p>
     * The number of bytes copied is the minimum of the remaining bytes in this Bytes object and the
     * remaining capacity in the {@code targetByteStore}.
     *
     * @param targetByteStore The target BytesStore to copy bytes into, must not be null.
     * @return The actual number of bytes that were copied.
     * @throws NullPointerException        If the provided {@code targetByteStore} is null.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    default long copyTo(@NotNull final BytesStore targetByteStore)
            throws ClosedIllegalStateException {
        return BytesStore.super.copyTo(targetByteStore);
    }

    /**
     * Copies bytes from this Bytes object to the provided {@code outputStream}.
     * <p>
     * Bytes are written from the current read position up to the limit of this Bytes object.
     *
     * @param outputStream The OutputStream to write the bytes to, must not be null.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws IOException                 If an I/O error occurs while writing to the provided
     *                                     {@code outputStream}.
     * @throws NullPointerException        If the provided {@code outputStream} is null.
     */
    @Override
    default void copyTo(@NotNull OutputStream outputStream)
            throws IOException, ClosedIllegalStateException {
        BytesStore.super.copyTo(outputStream);
    }

    /**
     * Determines whether this Bytes object is backed by memory that can be shared across multiple
     * processes.
     * <p>
     * A Bytes object backed by a memory-mapped file, for example, can be shared among processes,
     * whereas a Bytes object backed by heap memory cannot be shared in this way.
     *
     * @return {@code true} if this Bytes object uses memory that can be shared across processes,
     * {@code false} otherwise.
     */
    @Override
    default boolean sharedMemory() {
        return bytesStore().sharedMemory();
    }

    /**
     * Removes a specified number of bytes starting from a given offset within this Bytes object.
     * <p>
     * This method effectively removes {@code count} bytes beginning at the specified {@code fromOffset},
     * shifting any subsequent bytes to the left (reduces their index).
     * The write position is adjusted accordingly, and the capacity remains unchanged.
     *
     * @param fromOffset The offset from which to start removing bytes.
     * @param count      The number of bytes to remove.
     * @throws BufferUnderflowException    If {@code fromOffset} is negative or greater than the
     *                                     write position.
     * @throws BufferOverflowException     If there isn't enough space to perform the unwrite operation.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    default void unwrite(@NonNegative long fromOffset, @NonNegative int count)
            throws BufferUnderflowException, BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        long wp = writePosition();

        if (wp < fromOffset)
            return;

        write(fromOffset, this, fromOffset + count, wp - fromOffset - count);
        writePosition(wp - count);
    }

    /**
     * Returns the index within this Bytes object of the first occurrence of the specified
     * sub-bytes represented by the {@code source} Bytes object.
     * <p>
     * The method returns the lowest index {@code k} at which the contents of this Bytes object
     * equals the provided {@code source} Bytes object. If no such sequence exists, then {@code -1}
     * is returned.
     *
     * <p>
     * Formally, the method returns the lowest index {@code k} such that:
     *
     * <blockquote><pre>
     * this.startsWith(source, k)
     * </pre></blockquote>
     * <p>
     * If no such value of {@code k} exists, then {@code -1} is returned.
     *
     * @param source The sub-bytes to search for within this Bytes object, must not be null.
     * @return The index of the first occurrence of the specified sub-bytes, or {@code -1} if there
     * is no such occurrence.
     * @throws NullPointerException        If the provided {@code source} is null.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     */
    default long indexOf(@NotNull Bytes source)
            throws ClosedIllegalStateException {
        // TODO use indexOf(Bytes, long);
        throwExceptionIfReleased(this);
        throwExceptionIfReleased(source);
        long sourceOffset = readPosition();
        long otherOffset = source.readPosition();
        long sourceCount = readRemaining();
        long otherCount = source.readRemaining();

        if (sourceCount <= 0) {
            return Math.toIntExact(otherCount == 0 ? sourceCount : -1);
        }
        if (otherCount == 0) {
            return 0;
        }
        byte firstByte = source.readByte(otherOffset);
        long max = sourceOffset + (sourceCount - otherCount);

        for (long i = sourceOffset; i <= max; i++) {
            /* Look for first character. */
            if (readByte(i) != firstByte) {
                while (++i <= max && readByte(i) != firstByte) ;
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                long j = i + 1;
                long end = j + otherCount - 1;
                for (long k = otherOffset + 1; j < end && readByte(j) == source.readByte(k); j++, k++) {
                    // Do nothing
                }

                if (j == end) {
                    /* Found whole string. */
                    return Math.toIntExact(i - sourceOffset);
                }
            }
        }
        return -1;
    }

    /**
     * Returns the lowest index value starting from the provided {@code fromIndex} for which the contents of this
     * Bytes object equals the provided {@code source }, or -1 if no such index value exists.
     * <p>
     * On other words, returns the index within this Bytes object of the first occurrence of the
     * provided {@code source}
     * <p>
     * The returned index is the smallest value <i>k</i> for which:
     * <blockquote><pre>
     * this.startsWith(bytes, <i>k</i>)
     * </pre></blockquote>
     * If no such value of <i>k</i> exists, then {@code -1} is returned.
     *
     * @param source    the non-null sub-bytes to search for.
     * @param fromIndex to start searching from
     * @return index of equal contents or -1
     * @throws NullPointerException        If the provided {@code source } is {@code null}
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     */
    default int indexOf(@NotNull BytesStore source, @NonNegative int fromIndex)
            throws ClosedIllegalStateException {
        // TODO shouldn't fromIndex be absolute instead of relative
        throwExceptionIfReleased(this);
        throwExceptionIfReleased(source);
        long sourceOffset = readPosition();
        long otherOffset = source.readPosition();
        long sourceCount = readRemaining();
        long otherCount = source.readRemaining();

        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (fromIndex >= sourceCount) {
            return Math.toIntExact(otherCount == 0 ? sourceCount : -1);
        }
        if (otherCount == 0) {
            return fromIndex;
        }

        byte firstByte = source.readByte(otherOffset);
        long max = sourceOffset + (sourceCount - otherCount);

        for (long i = sourceOffset + fromIndex; i <= max; i++) {
            /* Look for first character. */
            if (readByte(i) != firstByte) {
                while (++i <= max && readByte(i) != firstByte) ;
            }

            /* Found first character, now look at the rest of v2 */
            if (i <= max) {
                long j = i + 1;
                long end = j + otherCount - 1;
                for (long k = otherOffset + 1; j < end && readByte(j) == source.readByte(k); j++, k++) {
                    // Do nothing
                }

                if (j == end) {
                    /* Found whole string. */
                    return Math.toIntExact(i - sourceOffset);
                }
            }
        }
        return -1;
    }

    /**
     * Clears the content of this Bytes object and resets its state.
     *
     * @return This Bytes object.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    @Override
    @NotNull
    Bytes<U> clear()
            throws IllegalStateException, ClosedIllegalStateException, ThreadingIllegalStateException;

    /**
     * Checks if this Bytes object supports both reading and writing.
     *
     * @return {@code true} if this Bytes object supports both reading and writing, {@code false} otherwise.
     */
    @Override
    default boolean readWrite() {
        return bytesStore().readWrite();
    }

    /**
     * Writes the specified number of bytes from this Bytes object into the provided {@code bytesOut}.
     * <p>
     * This operation does not affect the readLimit or readPosition of this Bytes object.
     *
     * @param length   The number of bytes to write. Must be non-negative.
     * @param bytesOut The target BytesOut object to write to. Must not be null.
     * @throws BufferUnderflowException    If the specified {@code length} is greater than the number of bytes available for reading from this Bytes object.
     * @throws IORuntimeException          If an error occurs while reading from this Bytes object or writing to {@code bytesOut}.
     * @throws BufferOverflowException     If {@code bytesOut} does not have enough capacity to hold the bytes being written.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     * @throws IllegalArgumentException    If the specified {@code length} is negative.
     * @throws NullPointerException        If the provided {@code bytesOut} is null.
     */
    default void readWithLength(@NonNegative long length, @NotNull BytesOut<U> bytesOut)
            throws BufferUnderflowException, IORuntimeException, BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNegative(length);
        if (length > readRemaining())
            throw new BufferUnderflowException();
        long limit0 = readLimit();
        long limit = readPosition() + length;
        boolean lenient = lenient();
        try {
            lenient(true);
            readLimit(limit);
            bytesOut.write(this);
        } finally {
            readLimit(limit0);
            readPosition(limit);
            lenient(lenient);
        }
    }

    /**
     * Reads the content of this Bytes object and converts it into an instance of the specified class.
     * Optionally updates the provided instance if not null. The content is assumed to have a
     * 16-bit length indicator preceding the actual object data.
     * <p>
     * If the {@code using} parameter is not null, this method will update its state with the read content.
     * Otherwise, it will create a new instance of the specified class.
     *
     * @param clazz The class of the object to be read. Must not be null.
     * @param using An optional instance to update with the read content. Can be null.
     * @param <T>   The type of the object being read.
     * @return The object that was read from this Bytes object, or the updated instance if {@code using} was provided.
     * @throws BufferUnderflowException     If there are not enough bytes in this Bytes object to read the content.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws InvalidMarshallableException If the data in this Bytes object cannot be converted into an instance of the specified class.
     * @throws NullPointerException         If the provided {@code clazz} is null.
     * @see #writeMarshallableLength16(WriteBytesMarshallable)
     */
    default <T extends ReadBytesMarshallable> T readMarshallableLength16(@NotNull final Class<T> clazz,
                                                                         @Nullable final T using)
            throws BufferUnderflowException, ClosedIllegalStateException, InvalidMarshallableException, ThreadingIllegalStateException {

        final T object = (using == null)
                ? ObjectUtils.newInstance(clazz)
                : using;
        int length = readUnsignedShort();
        long limit = readLimit();
        long end = readPosition() + length;
        boolean lenient = lenient();
        try {
            lenient(true);
            readLimit(end);
            object.readMarshallable(this);

        } finally {
            readPosition(end);
            readLimit(limit);
            lenient(lenient);
        }
        return object;
    }

    /**
     * Writes the content of the provided {@code marshallable} into this Bytes object, including a 16-bit length indicator
     * preceding the actual object data. The length indicator denotes the number of bytes taken up by the object data.
     * <p>
     * This method is useful for serialization, as it writes not only the content of the {@code marshallable} object
     * but also information about its size.
     *
     * @param marshallable The object to be serialized and written to this Bytes object. Must not be null.
     * @throws BufferOverflowException     If there is not enough capacity in this Bytes object to store the serialized data.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     *                                     cannot be addressed with 16-bits.
     * @throws NullPointerException        If the provided {@code marshallable} is null.
     * @see #readMarshallableLength16(Class, ReadBytesMarshallable)
     */
    default void writeMarshallableLength16(@NotNull final WriteBytesMarshallable marshallable)
            throws BufferOverflowException, ClosedIllegalStateException, BufferUnderflowException, InvalidMarshallableException, ThreadingIllegalStateException {
        requireNonNull(marshallable);
        long position = writePosition();
        ValidatableUtil.validate(marshallable);
        writeUnsignedShort(0);
        marshallable.writeMarshallable(this);
        long length = lengthWritten(position) - 2;
        if (length >= 1 << 16)
            throw new IllegalStateException("Marshallable " + marshallable.getClass() + " too long was " + length);
        writeUnsignedShort(position, (int) length);
    }

    /**
     * Writes the contents of the provided {@code inputStream} into this Bytes object. Continues reading from the
     * {@code inputStream} and writing into this Bytes object until the end of the stream is reached.
     * <p>
     * Note: This method does not close the provided {@code inputStream}.
     *
     * @param inputStream The input stream from which data is to be read. Must not be null.
     * @return This Bytes object.
     * @throws IOException                 If an I/O error occurs when reading from the {@code inputStream}.
     * @throws BufferOverflowException     If there is not enough capacity in this Bytes object to write the data read from the input stream.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     * @throws NullPointerException        If the provided {@code inputStream} is null.
     */
    default Bytes write(@NotNull final InputStream inputStream)
            throws IOException, BufferOverflowException, ClosedIllegalStateException, ThreadingIllegalStateException {
        requireNonNull(inputStream);
        for (; ; ) {
            int read;
            read = inputStream.read();
            if (read == -1)
                break;
            writeByte((byte) read);
        }
        return this;
    }
}
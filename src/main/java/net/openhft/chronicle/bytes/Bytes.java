/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.bytes.internal.EnbeddedBytes;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.UsedViaReflection;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Bytes is a pointer to a region of memory within a BytesStore. It can be for a fixed region of
 * memory or an "elastic" buffer which can be resized, but not for a fixed region. <p></p> This is a
 * BytesStore which is mutable and not thread safe. It has a write position and read position which
 * must follow these constraints <p></p> {@code start() <= readPosition() <= writePosition() <= writeLimit() <= capacity()}
 * <p></p> Also {@code readLimit() == writePosition() && readPosition() <= safeLimit()}
 * <p></p>
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface Bytes<Underlying> extends
        BytesStore<Bytes<Underlying>, Underlying>,
        BytesIn<Underlying>,
        BytesOut<Underlying> {

    long MAX_CAPACITY = Long.MAX_VALUE & ~0xF; // 8 EiB - 16
    int MAX_HEAP_CAPACITY = Integer.MAX_VALUE & ~0xF;  // 2 GiB - 16
    @Deprecated(/* to be removed in x.22 */)
    int MAX_BYTE_BUFFER_CAPACITY = MAX_HEAP_CAPACITY;
    int DEFAULT_BYTE_BUFFER_CAPACITY = 256;

    /**
     * Creates and returns a new elastic wrapper for a direct (off-heap) ByteBuffer with a default capacity
     * which will be resized as required.
     *
     * @return a new elastic wrapper for a direct (off-heap) ByteBuffer with a default capacity
     * which will be resized as required
     */
    @NotNull
    static Bytes<ByteBuffer> elasticByteBuffer() {
        return elasticByteBuffer(DEFAULT_BYTE_BUFFER_CAPACITY);
    }

    /**
     * Creates and returns a new elastic wrapper for a direct (off-heap) ByteBuffer with
     * the given {@code initialCapacity} which will be resized as required.
     *
     * @param initialCapacity the initial non-negative capacity given in bytes
     * @return a new elastic wrapper for a direct (off-heap) ByteBuffer with
     * the given {@code initialCapacity} which will be resized as required
     */
    @NotNull
    static Bytes<ByteBuffer> elasticByteBuffer(int initialCapacity) {
        return elasticByteBuffer(initialCapacity, MAX_HEAP_CAPACITY);
    }

    /**
     * Creates and returns a new elastic wrapper for a direct (off-heap) ByteBuffer with
     * the given {@code initialCapacity} which will be resized as required up
     * to the given {@code maxSize}.
     *
     * @param initialCapacity the initial non-negative capacity given in bytes
     * @param maxCapacity     the max capacity given in bytes equal or greater than initialCapacity
     * @return a new elastic wrapper for a direct (off-heap) ByteBuffer with
     * the given {@code initialCapacity} which will be resized as required up
     * to the given {@code maxCapacity}
     */
    @NotNull
    static Bytes<ByteBuffer> elasticByteBuffer(int initialCapacity, int maxCapacity) {
        @NotNull BytesStore<?, ByteBuffer> bs = BytesStore.elasticByteBuffer(initialCapacity, maxCapacity);
        try {
            try {
                return bs.bytesForWrite();
            } finally {
                bs.release(ReferenceOwner.INIT);
            }
        } catch (IllegalStateException ise) {
            throw new AssertionError(ise);
        }
    }

    /**
     * Creates and returns a new elastic wrapper for a heap ByteBuffer with
     * the given {@code initialCapacity} which will be resized as required.
     *
     * @param initialCapacity the initial non-negative capacity given in bytes
     * @return a new elastic wrapper for a heap ByteBuffer with
     * the given {@code initialCapacity} which will be resized as required
     */
    @NotNull
    static Bytes<ByteBuffer> elasticHeapByteBuffer(int initialCapacity) {
        @NotNull BytesStore<?, ByteBuffer> bs = BytesStore.wrap(ByteBuffer.allocate(initialCapacity));
        try {
            return NativeBytes.wrapWithNativeBytes(bs, Bytes.MAX_HEAP_CAPACITY);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new AssertionError(e);
        } finally {
            try {
                bs.release(INIT);
            } catch (IllegalStateException ise) {
                throw new AssertionError(ise);
            }
        }
    }

    /**
     * Creates and returns a new elastic wrapper for a heap ByteBuffer with
     * the {@code initialCapacity} 128 bytes which will be resized as required.
     *
     * @return a new elastic wrapper for a heap ByteBuffer with the {@code initialCapacity}
     * 128 bytes which will be resized as required
     */
    @NotNull
    static Bytes<ByteBuffer> elasticHeapByteBuffer() {
        return elasticHeapByteBuffer(128);
    }

    static <T> Bytes<T> forFieldGroup(T t, String groupName) {
        @NotNull BytesStore<?, T> bs = BytesStore.forFields(t, groupName, 1);
        try {
            final EnbeddedBytes<T> bytes = EnbeddedBytes.wrap(bs);
            return bytes.writeLimit(bs.writeLimit());
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new AssertionError(e);
        } finally {
            try {
                bs.release(INIT);
            } catch (IllegalStateException ise) {
                throw new AssertionError(ise);
            }
        }
    }

    /**
     * Wrap the ByteBuffer ready for reading
     * Method for convenience only - might not be ideal for performance (creates garbage).
     * To avoid garbage, use something like this example:
     * <pre>{@code
     * import net.openhft.chronicle.bytes.Bytes;
     * import java.nio.ByteBuffer;
     *
     * public class ChronicleBytesWithByteBufferExampleTest {
     *     private static final String HELLO_WORLD = "hello world";
     *
     *     public static void main(String[] args)
     * throws InterruptedException {
     *         //setup Bytes and ByteBuffer to write from
     *         Bytes b = Bytes.elasticByteBuffer();
     *         ByteBuffer toWriteFrom = ByteBuffer.allocate(HELLO_WORLD.length());
     *         toWriteFrom.put(HELLO_WORLD.getBytes(), 0, HELLO_WORLD.length());
     *         toWriteFrom.flip();
     *         byte[] toReadTo = new byte[HELLO_WORLD.length()];
     *
     *         doWrite(b, toWriteFrom);
     *         ByteBuffer byteBuffer = doRead(b);
     *
     *         //check result
     *         final StringBuilder sb = new StringBuilder();
     *         for (int i = 0; i < HELLO_WORLD.length(); i++) {
     *             sb.append((char) byteBuffer.get());
     *         }
     *         assert sb.toString().equals(HELLO_WORLD): "Failed - strings not equal!";
     *     }
     *
     *     private static void doWrite(Bytes b, ByteBuffer toWrite) {
     *         //no garbage when writing to Bytes from ByteBuffer
     *         b.clear();
     *         b.write(b.writePosition(), toWrite, toWrite.position(), toWrite.limit());
     *     }
     *
     *     private static ByteBuffer doRead(Bytes b) {
     *         //no garbage when getting the underlying ByteBuffer
     *         assert b.underlyingObject() instanceof ByteBuffer;
     *         ByteBuffer byteBuffer = (ByteBuffer) b.underlyingObject();
     *         return byteBuffer;
     *     }
     * }
     * }</pre>
     *
     * @param byteBuffer to wrap
     * @return a Bytes which wraps the provided ByteBuffer and is ready for reading.
     */
    @NotNull
    static Bytes<ByteBuffer> wrapForRead(@NotNull ByteBuffer byteBuffer) {
        BytesStore<?, ByteBuffer> bs = BytesStore.wrap(byteBuffer);
        try {
            try {
                Bytes<ByteBuffer> bbb = bs.bytesForRead();
                bbb.readLimit(byteBuffer.limit());
                bbb.readPosition(byteBuffer.position());
                return bbb;
            } finally {
                bs.release(INIT);
            }
        } catch (IllegalStateException | BufferUnderflowException ise) {
            throw new AssertionError(ise);
        }
    }

    /**
     * Wraps the ByteBuffer ready for writing
     * Method for convenience only - might not be ideal for performance (creates garbage).
     * To avoid garbage, use something like this example:
     * <pre>{@code
     * import net.openhft.chronicle.bytes.Bytes;
     * import java.nio.ByteBuffer;
     *
     * public class ChronicleBytesWithByteBufferExampleTest {
     *     private static final String HELLO_WORLD = "hello world";
     *
     *     public static void main(String[] args)
     * throws InterruptedException {
     *         //setup Bytes and ByteBuffer to write from
     *         Bytes b = Bytes.elasticByteBuffer();
     *         ByteBuffer toWriteFrom = ByteBuffer.allocate(HELLO_WORLD.length());
     *         toWriteFrom.put(HELLO_WORLD.getBytes(), 0, HELLO_WORLD.length());
     *         toWriteFrom.flip();
     *         byte[] toReadTo = new byte[HELLO_WORLD.length()];
     *
     *         doWrite(b, toWriteFrom);
     *         ByteBuffer byteBuffer = doRead(b);
     *
     *         //check result
     *         final StringBuilder sb = new StringBuilder();
     *         for (int i = 0; i < HELLO_WORLD.length(); i++) {
     *             sb.append((char) byteBuffer.get());
     *         }
     *         assert sb.toString().equals(HELLO_WORLD): "Failed - strings not equal!";
     *     }
     *
     *     private static void doWrite(Bytes b, ByteBuffer toWrite) {
     *         //no garbage when writing to Bytes from ByteBuffer
     *         b.clear();
     *         b.write(b.writePosition(), toWrite, toWrite.position(), toWrite.limit());
     *     }
     *
     *     private static ByteBuffer doRead(Bytes b) {
     *         //no garbage when getting the underlying ByteBuffer
     *         assert b.underlyingObject() instanceof ByteBuffer;
     *         ByteBuffer byteBuffer = (ByteBuffer) b.underlyingObject();
     *         return byteBuffer;
     *     }
     * }
     * }</pre>
     *
     * @param byteBuffer to wrap
     * @return a Bytes which wraps the provided ByteBuffer and is ready for writing.
     */
    @NotNull
    static Bytes<ByteBuffer> wrapForWrite(@NotNull ByteBuffer byteBuffer) {
        BytesStore<?, ByteBuffer> bs = BytesStore.wrap(byteBuffer);
        try {
            try {
                Bytes<ByteBuffer> bbb = bs.bytesForWrite();
                bbb.writePosition(byteBuffer.position());
                bbb.writeLimit(byteBuffer.limit());
                return bbb;
            } finally {
                bs.release(INIT);
            }
        } catch (IllegalStateException | BufferOverflowException ise) {
            throw new AssertionError(ise);
        }
    }

    /**
     * Wraps the byte[] ready for reading
     * Method for convenience only - might not be ideal for performance (creates garbage).
     * To avoid garbage, use something like this example:
     * <pre>{@code
     * import net.openhft.chronicle.bytes.Bytes;
     * import java.nio.charset.Charset;
     *
     * public class ChronicleBytesWithPrimByteArrayExampleTest {
     *     private static final Charset ISO_8859 = Charset.forName("ISO-8859-1");
     *     private static final String HELLO_WORLD = "hello world";
     *
     *     public static void main(String[] args) {
     *         //setup Bytes and byte[]s to write from and read to
     *         Bytes b = Bytes.elasticByteBuffer();
     *         byte[] toWriteFrom = HELLO_WORLD.getBytes(ISO_8859);
     *         byte[] toReadTo = new byte[HELLO_WORLD.length()];
     *
     *         doWrite(b, toWriteFrom);
     *         doRead(b, toReadTo);
     *
     *         //check result
     *         final StringBuilder sb = new StringBuilder();
     *         for (int i = 0; i < HELLO_WORLD.length(); i++) {
     *             sb.append((char) toReadTo[i]);
     *         }
     *         assert sb.toString().equals(HELLO_WORLD): "Failed - strings not equal!";
     *     }
     *
     *     private static void doWrite(Bytes b, byte[] toWrite) {
     *         //no garbage when writing to Bytes from byte[]
     *         b.clear();
     *         b.write(toWrite);
     *     }
     *
     *     private static void doRead(Bytes b, byte[] toReadTo) {
     *         //no garbage when reading from Bytes into byte[]
     *         b.read( toReadTo, 0, HELLO_WORLD.length());
     *     }
     * }
     * }</pre>
     *
     * @param byteArray to wrap
     * @return the Bytes ready for reading.
     */
    @NotNull
    static Bytes<byte[]> wrapForRead(@NotNull byte[] byteArray) {
        @NotNull BytesStore<?, byte[]> bs = BytesStore.wrap(byteArray);
        try {
            try {
                return bs.bytesForRead();
            } finally {
                bs.release(INIT);
            }
        } catch (IllegalStateException ise) {
            throw new AssertionError(ise);
        }
    }

    /**
     * Wraps the byte[] ready for writing
     * Method for convenience only - might not be ideal for performance (creates garbage).
     * To avoid garbage, use something like this example:
     * <pre>{@code
     * import net.openhft.chronicle.bytes.Bytes;
     * import java.nio.charset.Charset;
     *
     * public class ChronicleBytesWithPrimByteArrayExampleTest {
     *     private static final Charset ISO_8859 = Charset.forName("ISO-8859-1");
     *     private static final String HELLO_WORLD = "hello world";
     *
     *     public static void main(String[] args) {
     *         //setup Bytes and byte[]s to write from and read to
     *         Bytes b = Bytes.elasticByteBuffer();
     *         byte[] toWriteFrom = HELLO_WORLD.getBytes(ISO_8859);
     *         byte[] toReadTo = new byte[HELLO_WORLD.length()];
     *
     *         doWrite(b, toWriteFrom);
     *         doRead(b, toReadTo);
     *
     *         //check result
     *         final StringBuilder sb = new StringBuilder();
     *         for (int i = 0; i < HELLO_WORLD.length(); i++) {
     *             sb.append((char) toReadTo[i]);
     *         }
     *         assert sb.toString().equals(HELLO_WORLD): "Failed - strings not equal!";
     *     }
     *
     *     private static void doWrite(Bytes b, byte[] toWrite) {
     *         //no garbage when writing to Bytes from byte[]
     *         b.clear();
     *         b.write(toWrite);
     *     }
     *
     *     private static void doRead(Bytes b, byte[] toReadTo) {
     *         //no garbage when reading from Bytes into byte[]
     *         b.read( toReadTo, 0, HELLO_WORLD.length());
     *     }
     * }
     * }</pre>
     *
     * @param byteArray to wrap
     * @return the Bytes ready for writing.
     */
    @NotNull
    static Bytes<byte[]> wrapForWrite(@NotNull byte[] byteArray) {
        @NotNull BytesStore bs = BytesStore.wrap(byteArray);
        try {
            try {
                return bs.bytesForWrite();
            } finally {
                bs.release(INIT);
            }
        } catch (IllegalStateException ise) {
            throw new AssertionError(ise);
        }
    }

    /**
     * Converts text to bytes using ISO-8859-1 encoding and returns a Bytes ready for reading.
     * <p>
     * Note: this returns a direct Bytes now
     *
     * @param text to convert
     * @return Bytes ready for reading.
     */
    @NotNull
    static Bytes<?> from(@NotNull CharSequence text) {
        return from(text.toString());
    }

    static Bytes<?> fromDirect(@NotNull CharSequence text) {
        return NativeBytes.nativeBytes(text.length()).append(text);
    }

    @Deprecated(/* to be removed in x.22 */)
    static Bytes<?> fromString(String text) {
        return from(text);
    }

    /**
     * Converts text to bytes using ISO-8859-1 encoding and return a Bytes ready for reading.
     * <p>
     * Note: this returns a direct Bytes now
     *
     * @param text to convert
     * @return Bytes ready for reading.
     */
    @NotNull
    static Bytes<?> directFrom(@NotNull String text) {
        BytesStore from = BytesStore.from(text);
        try {
            try {
                return from.bytesForRead();
            } finally {
                from.release(INIT);
            }
        } catch (IllegalStateException ise) {
            throw new AssertionError(ise);
        }
    }

    /**
     * Converts text to bytes using ISO-8859-1 encoding and returns a Bytes ready for reading.
     * <p>
     * The returned Bytes is allocated on the heap.
     *
     * @param text to convert
     * @return Bytes ready for reading.
     */
    @NotNull
    static Bytes<byte[]> from(@NotNull String text) {
        return wrapForRead(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    @UsedViaReflection
    static Bytes<byte[]> valueOf(String text) {
        return from(text);
    }

    /**
     * Creates and returns a new fix sized wrapper for native (64-bit address)
     * memory with the given {@code capacity}.
     *
     * @param capacity the non-negative capacity given in bytes
     * @return a new fix sized wrapper for native (64-bit address)
     * memory with the given {@code capacity}
     */
    @NotNull
    static VanillaBytes<Void> allocateDirect(long capacity)
            throws IllegalArgumentException {
        @NotNull BytesStore<?, Void> bs = BytesStore.nativeStoreWithFixedCapacity(capacity);
        try {
            try {
                return new NativeBytes<>(bs);
            } finally {
                bs.release(INIT);
            }
        } catch (IllegalStateException ise) {
            throw new AssertionError(ise);
        }
    }

    /**
     * Creates and returns a new elastic wrapper for native (64-bit address)
     * memory with zero initial capacity which will be resized as required.
     *
     * @return a new elastic wrapper for native (64-bit address)
     * memory with zero initial capacity which will be resized as required
     */
    @NotNull
    static NativeBytes<Void> allocateElasticDirect() {
        return NativeBytes.nativeBytes();
    }

    /**
     * Creates and returns a new elastic wrapper for native (64-bit address)
     * memory with the given {@code initialCapacity} which will be resized as required.
     *
     * @param initialCapacity the initial non-negative capacity given in bytes
     * @return a new elastic wrapper for native (64-bit address)
     * memory with the given {@code initialCapacity} which will be resized as required
     */
    @NotNull
    static NativeBytes<Void> allocateElasticDirect(long initialCapacity)
            throws IllegalArgumentException {
        return NativeBytes.nativeBytes(initialCapacity);
    }

    /**
     * Creates and returns a new elastic wrapper for on heap memory with the
     * {@code initialCapacity} 32 bytes which will be resized as required.
     *
     * @return a new elastic wrapper for on heap memory
     */
    @NotNull
    static OnHeapBytes allocateElasticOnHeap() {
        return allocateElasticOnHeap(32);
    }

    /**
     * Creates and returns a new elastic wrapper for on heap memory with the specified
     * {@code initialCapacity} which will be resized as required.
     *
     * @param initialCapacity the initial capacity of the wrapper in bytes
     * @return                a new elastic wrapper for on-heap memory with an initial capacity of initialCapacity
     */
    @NotNull
    static OnHeapBytes allocateElasticOnHeap(int initialCapacity) {
        BytesStore<?, byte[]> wrap = BytesStore.wrap(new byte[initialCapacity]);
        try {
            try {
                return new OnHeapBytes(wrap, true);
            } finally {
                wrap.release(INIT);
            }
        } catch (IllegalStateException | IllegalArgumentException ise) {
            throw new AssertionError(ise);
        }
    }

    /**
     * Creates and returns a string from the {@code readPosition} to the {@code readLimit}, The buffer is not modified
     * by this call
     *
     * @param buffer the buffer to use
     * @return a string contain the text from the {@code readPosition}  to the  {@code readLimit}
     */
    @NotNull
    static String toString(@NotNull final Bytes<?> buffer)
            throws BufferUnderflowException, IllegalStateException, IllegalArgumentException {
        return toString(buffer, MAX_HEAP_CAPACITY);
    }

    /**
     * Creates and returns a string from the {@code readPosition} to the {@code readLimit} with a specified maximum length, The buffer is not modified
     * by this call.
     * <p>
     * If the length of the string between {@code readPosition} to {@code readLimit} is greater than the specified maximum length,
     * the "..." will be appended to the resulting string; in this case the length of the resulting string will be the specified maximum length+3.
     *
     * @param buffer the buffer to use
     * @param maxLen the maximum length from the buffer, used to create a new string
     * @return       a string contain the text from the {@code readPosition} to the {@code readLimit} of the buffer
     */
    @NotNull
    static String toString(@NotNull final Bytes<?> buffer, long maxLen) throws
            BufferUnderflowException, IllegalStateException, IllegalArgumentException {

        if (buffer.refCount() < 1)
            // added because something is crashing the JVM
            return "<unknown>";

        ReferenceOwner toString = ReferenceOwner.temporary("toString");
        buffer.reserve(toString);
        try {

            if (buffer.readRemaining() == 0)
                return "";

            final long length = Math.min(maxLen + 1, buffer.readRemaining());

            @NotNull final StringBuilder builder = new StringBuilder();
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
            }
            return builder.toString();
        } finally {
            buffer.release(toString);
        }
    }

    /**
     * Creates and returns a string from the bytes of a given buffer with a specified length and from an specified offset.
     * The buffer is not modified by this call.
     *
     * @param buffer   the buffer to use
     * @param position the offset position to create the string from
     * @param len      the number of characters to include in the string
     * @return         a string with length len contain the text from offset position
     */
    @NotNull
    static String toString(@NotNull final Bytes buffer, long position, long len) {
        try {
            final long pos = buffer.readPosition();
            final long limit = buffer.readLimit();
            buffer.readPositionRemaining(position, len);

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
     * @param bytes array to copy
     * @return a new fix sized wrapper for native (64-bit address)
     * memory with the contents copied from the given {@code bytes} array
     */
    @NotNull
    static VanillaBytes allocateDirect(@NotNull byte[] bytes)
            throws IllegalArgumentException {
        VanillaBytes<Void> result = allocateDirect(bytes.length);
        try {
            result.write(bytes);
        } catch (BufferOverflowException | IllegalStateException e) {
            throw new AssertionError(e);
        }
        return result;
    }

    @NotNull
    static Bytes fromHexString(@NotNull String s) {
        return BytesInternal.fromHexString(s);
    }

    /**
     * Code shared by String and StringBuffer to do searches. The
     * source is the character array being searched, and the target
     * is the string being searched for.
     *
     * @param source    the read bytes being searched.
     * @param target    the read bytes being searched for.
     * @param fromIndex the index to begin searching from,
     * @return the index of where the text was found.
     */
    static int indexOf(@NotNull BytesStore source, @NotNull BytesStore target, int fromIndex)
            throws IllegalStateException {

        long sourceOffset = source.readPosition();
        long targetOffset = target.readPosition();
        long sourceCount = source.readRemaining();
        long targetCount = target.readRemaining();

        if (fromIndex >= sourceCount) {
            return Math.toIntExact(targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }
        try {
            byte firstByte = target.readByte(targetOffset);
            long max = sourceOffset + (sourceCount - targetCount);

            for (long i = sourceOffset + fromIndex; i <= max; i++) {
                /* Look for first character. */
                if (source.readByte(i) != firstByte) {
                    while (++i <= max && source.readByte(i) != firstByte) ;
                }

                /* Found first character, now look at the rest of v2 */
                if (i <= max) {
                    long j = i + 1;
                    long end = j + targetCount - 1;
                    for (long k = targetOffset + 1; j < end && source.readByte(j) == target.readByte(k); j++, k++) {
                    }

                    if (j == end) {
                        /* Found whole string. */
                        return Math.toIntExact(i - sourceOffset);
                    }
                }
            }
            return -1;
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns a Bytes which is optionally unchecked. This allows bounds checks to be turned off.
     * Note: this means that the result is no longer elastic, even if <code>this</code> is elastic.
     *
     * @param unchecked if true, minimal bounds checks will be performed
     * @return Bytes without bounds checking
     * @throws IllegalStateException if the underlying BytesStore has been released
     */
    @NotNull
    default Bytes<Underlying> unchecked(boolean unchecked)
            throws IllegalStateException {
        if (unchecked) {
            if (isElastic())
                BytesUtil.WarnUncheckedElasticBytes.warn();
            Bytes<Underlying> underlyingBytes = start() == 0 && bytesStore().isDirectMemory() ?
                    new UncheckedNativeBytes<>(this) :
                    new UncheckedBytes<>(this);
            release(INIT);
            return underlyingBytes;
        }
        return this;
    }

    default boolean unchecked() {
        return false;
    }

    /**
     * @inheritDoc <P>
     * If this Bytes {@link #isElastic()} the {@link #safeLimit()} can be
     * lower than the point it can safely write.
     */
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
    default long realCapacity() {
        return BytesStore.super.realCapacity();
    }

    /**
     * @return a copy of this Bytes from position() to limit().
     */
    @Override
    BytesStore<Bytes<Underlying>, Underlying> copy()
            throws IllegalStateException;

    @NotNull
    default String toHexString() {
        return toHexString(1024);
    }

    /**
     * Displays the hex data of {@link Bytes} from the {@code readPosition} up to the specified length.
     *
     * @param maxLength limit the number of bytes to be dumped
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    @NotNull
    default String toHexString(long maxLength) {
        return toHexString(readPosition(), maxLength);
    }

    /**
     * Displays the hex data of {@link Bytes} with the specified maximum length from the specified offset.
     *
     * @param offset the specified offset to start from
     * @param maxLength limit the number of bytes to be dumped
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    @NotNull
    default String toHexString(long offset, long maxLength) {
//        if (Jvm.isDebug() && Jvm.stackTraceEndsWith("Bytes", 3))
//            return "Not Available";

        long maxLength2 = Math.min(maxLength, readLimit() - offset);
        try {
            @NotNull String ret = BytesInternal.toHexString(this, offset, maxLength2);
            return maxLength2 < readLimit() - offset ? ret + "... truncated" : ret;
        } catch (BufferUnderflowException | IllegalStateException e) {
            return e.toString();
        }
    }

    /**
     * Returns if this Bytes is elastic. I.e. it can resize when more data is written
     * than it's {@link #realCapacity()}.
     *
     * @return if this Bytes is elastic
     */
    boolean isElastic();

    /**
     * Grows the buffer if the buffer is elastic, if the buffer is not elastic and there is not
     * enough capacity then this method will
     * throws {@link java.nio.BufferOverflowException}
     *
     * @param desiredCapacity the capacity that you required
     * @throws IllegalArgumentException if the buffer is not elastic and there is not enough space
     */
    default void ensureCapacity(long desiredCapacity)
            throws IllegalArgumentException, IllegalStateException {
        if (desiredCapacity > capacity())
            throw new IllegalArgumentException(isElastic() ? "todo" : "not elastic");
    }

    /**
     * Creates a slice of the current Bytes based on its readPosition and limit. As a sub-section
     * of a Bytes it cannot be elastic.
     *
     * @return a slice of the existing Bytes where the start is moved to the readPosition and the
     * current limit determines the capacity.
     * @throws IllegalStateException if the underlying BytesStore has been released
     */
    @NotNull
    @Override
    default Bytes<Underlying> bytesForRead()
            throws IllegalStateException {
        try {
            return isClear() ? BytesStore.super.bytesForRead() : new SubBytes<>(this, readPosition(), readLimit() + start());
        } catch (IllegalArgumentException | BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * @return the ByteStore this Bytes wraps.
     */
    @Override
    @Nullable
    BytesStore bytesStore();

    default boolean isEqual(String s)
            throws IllegalStateException {
        return StringUtils.isEqual(this, s);
    }

    /**
     * Compact these Bytes by moving the readPosition to the start.
     *
     * @return this
     */
    @NotNull
    Bytes<Underlying> compact()
            throws IllegalStateException;

    /**
     * Copies bytes from this Bytes to another ByteStore.
     *
     * @param store the BytesStore to copy to
     * @return      the number of bytes copied
     */
    @Override
    default long copyTo(@NotNull BytesStore store)
            throws IllegalStateException {
        return BytesStore.super.copyTo(store);
    }

    /**
     * Copies bytes from this Bytes to a specified OutputStream.
     *
     * @param out  the specified OutputStream that this BytesStore is copied to
     * @throws IllegalStateException if this Bytes has been released
     * @throws IOException if an I/O error occurs
     */
    @Override
    default void copyTo(@NotNull OutputStream out)
            throws IOException, IllegalStateException {
        BytesStore.super.copyTo(out);
    }

    @Override
    default boolean sharedMemory() {
        return bytesStore().sharedMemory();
    }

    /**
     * Will un-write a specified number of bytes from an offset from this Bytes.
     * <p>
     * Calling this method will update the cursors of this Bytes.
     *
     * @param fromOffset the offset from the target bytes
     * @param count      the number of bytes to un-write
     */
    default void unwrite(long fromOffset, int count)
            throws BufferUnderflowException, BufferOverflowException, IllegalStateException {
        long wp = writePosition();

        if (wp < fromOffset)
            return;

        write(fromOffset, this, fromOffset + count, wp - fromOffset - count);
        writePosition(wp - count);
    }

    @NotNull
    default BigDecimal readBigDecimal()
            throws ArithmeticException, BufferUnderflowException, IllegalStateException {
        return new BigDecimal(readBigInteger(), Maths.toUInt31(readStopBit()));
    }

    @NotNull
    default BigInteger readBigInteger()
            throws ArithmeticException, BufferUnderflowException, IllegalStateException {
        int length = Maths.toUInt31(readStopBit());
        if (length == 0)
            if (lenient())
                return BigInteger.ZERO;
            else
                throw new BufferUnderflowException();
        @NotNull byte[] bytes = new byte[length];
        read(bytes);
        return new BigInteger(bytes);
    }

    /**
     * Returns the index within this Bytes of the first occurrence of the
     * specified sub-bytes.
     * <p>
     * The returned index is the smallest value <i>k</i> for which:
     * <blockquote><pre>
     * this.startsWith(bytes, <i>k</i>)
     * </pre></blockquote>
     * If no such value of <i>k</i> exists, then {@code -1} is returned.
     *
     * @param source the sub-bytes to search for.
     * @return       the index of the first occurrence of the specified sub-bytes,
     *               or {@code -1} if there is no such occurrence.
     */
    default long indexOf(@NotNull Bytes source)
            throws IllegalStateException {
        return indexOf(source, 0);
    }

    /**
     * Returns the index within this Bytes of the first occurrence of the
     * specified sub-bytes.
     * <p>
     * The returned index is the smallest value <i>k</i> for which:
     * <blockquote><pre>
     * this.startsWith(bytes, <i>k</i>)
     * </pre></blockquote>
     * If no such value of <i>k</i> exists, then {@code -1} is returned.
     *
     * @param source    the sub-bytes to search for.
     * @param fromIndex start the search from this offset
     * @return          the index of the first occurrence of the specified sub-bytes,
     *                  or {@code -1} if there is no such occurrence.
     */
    default int indexOf(@NotNull BytesStore source, int fromIndex)
            throws IllegalStateException {
        return indexOf(this, source, fromIndex);
    }

    @Deprecated(/* to be removed in x.22 */)
    default long indexOf(@NotNull Bytes source, int fromIndex)
            throws IllegalStateException {
        return indexOf(this, source, fromIndex);
    }

    @Override
    @NotNull
    Bytes<Underlying> clear()
            throws IllegalStateException;

    @Override
    default boolean readWrite() {
        return bytesStore().readWrite();
    }

    default void readWithLength(long length, @NotNull BytesOut<Underlying> bytesOut)
            throws BufferUnderflowException, IORuntimeException, BufferOverflowException, IllegalStateException {
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

    default <T extends ReadBytesMarshallable> T readMarshallableLength16(Class<T> tClass, T object)
            throws BufferUnderflowException, IllegalStateException {
        if (object == null) object = ObjectUtils.newInstance(tClass);
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

    default void writeMarshallableLength16(WriteBytesMarshallable marshallable)
            throws IllegalArgumentException, BufferOverflowException, IllegalStateException, BufferUnderflowException {
        long position = writePosition();
        try {
            writeUnsignedShort(0);
            marshallable.writeMarshallable(this);
            long length = writePosition() - position - 2;
            if (length >= 1 << 16)
                throw new IllegalStateException("Marshallable " + marshallable.getClass() + " too long was " + length);
            writeUnsignedShort(position, (int) length);
        } catch (ArithmeticException e) {
            throw new AssertionError(e);
        }
    }

    default Bytes write(final InputStream inputStream)
            throws IOException, BufferOverflowException, IllegalStateException {
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
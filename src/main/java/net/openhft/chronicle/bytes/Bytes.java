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
import net.openhft.chronicle.bytes.internal.EmbeddedBytes;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.annotation.NonNegative;
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
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;

import static net.openhft.chronicle.core.util.Longs.requireNonNegative;
import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

/**
 * Bytes is a view of a region of memory within a {@link BytesStore }
 * with 63-bit addressing capability (~8 EiB) and separate read and write cursors.
 * <p>
 * Java's built-in {@link ByteBuffer} can only handle 31-bits addresses and has only a single cursor.
 * <p>
 * A Bytes object can be for a fixed region of memory or an "elastic" buffer which can be resized on demand.
 * <p>
 * A Bytes is mutable and not thread-safe.
 * <p>
 * The cursors consist of a write-position and read-position which must follow these constraints
 * <p> {@code start() <= readPosition() <= writePosition() <= writeLimit() <= capacity()}
 * <p>Also {@code readLimit() == writePosition() && readPosition() <= safeLimit()}
 * <p>
 * Generally, a Bytes object is a resource that is ReferenceCounted and certain operations invoked on a Bytes
 * object that has been released, might result in an {@link IllegalStateException} being thrown.
 *
 * @see BytesStore
 * @param <Underlying> Underlying type
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface Bytes<Underlying> extends
        BytesStore<Bytes<Underlying>, Underlying>,
        BytesIn<Underlying>,
        BytesOut<Underlying> {

    /**
     * The max capacity a Bytes can ever have.
     */
    long MAX_CAPACITY = Long.MAX_VALUE & ~0xF; // 8 EiB - 16

    /**
     * The max capacity a Bytes can ever have if it is allocated on heap.
     */
    int MAX_HEAP_CAPACITY = Integer.MAX_VALUE & ~0xF;  // 2 GiB - 16

    /**
     * The default initial size of an elastic Bytes backed by a ByteBuffer
     */
    int DEFAULT_BYTE_BUFFER_CAPACITY = 256;

    /**
     * Creates and returns a new elastic wrapper for a direct (off-heap) ByteBuffer with a default capacity
     * which will be resized as required.
     *
     * @return a new elastic wrapper
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
     * @return a new elastic wrapper
     * @throws IllegalArgumentException if the provided {@code initialCapacity} negative.
     */
    @NotNull
    static Bytes<ByteBuffer> elasticByteBuffer(@NonNegative int initialCapacity) {
        return elasticByteBuffer(initialCapacity, MAX_HEAP_CAPACITY);
    }

    /**
     * Creates and returns a new elastic wrapper for a direct (off-heap) ByteBuffer with
     * the given {@code initialCapacity} which will be resized as required up
     * to the given {@code maxSize}.
     *
     * @param initialCapacity the initial non-negative capacity given in bytes
     * @param maxCapacity     the max capacity given in bytes equal or greater than initialCapacity
     * @return a new elastic wrapper
     * @throws IllegalArgumentException if the provided {@code initialCapacity} or provided {@code maxCapacity} is negative.
     */
    @NotNull
    static Bytes<ByteBuffer> elasticByteBuffer(@NonNegative final int initialCapacity,
                                               @NonNegative final int maxCapacity) {
        requireNonNegative(initialCapacity);
        requireNonNegative(maxCapacity);

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
     * @return a new elastic wrapper
     * @throws IllegalArgumentException if the provided {@code initialCapacity} is negative.
     */
    @NotNull
    static Bytes<ByteBuffer> elasticHeapByteBuffer(@NonNegative int initialCapacity) {
        requireNonNegative(initialCapacity);
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
     * @return a new elastic wrapper
     */
    @NotNull
    static Bytes<ByteBuffer> elasticHeapByteBuffer() {
        return elasticHeapByteBuffer(128);
    }

    /**
     * Creates and returns a new Bytes view of fields in the provided {@code object} in a {@link FieldGroup} named as
     * the provided {@code groupName} effectively turning the groups of fields into a memory segment.
     * <p>
     * Here is an example of field groups:
     * <pre>{@code
     *     static class Padding extends Parent {
     *         @FieldGroup("p")
     *         // 128 bytes
     *         transient long p0, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15;
     *     }
     * }</pre>
     *
     * @param object    non-null object whose fields are to be reflected
     * @param groupName of the field group, non-null
     * @return a new Bytes view of fields
     * @throws NullPointerException if the provided {@code object} or the provided {@code groupName} is {@code null}
     */
    static <T> Bytes<T> forFieldGroup(@NotNull final T object,
                                      @NotNull final String groupName) {
        requireNonNull(object);
        requireNonNull(groupName);
        @NotNull BytesStore<?, T> bs = BytesStore.forFields(object, groupName, 1);
        try {
            final EmbeddedBytes<T> bytes = EmbeddedBytes.wrap(bs);
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
     * Creates and returns a new Bytes wrapping the provided {@code byteBuffer}.
     * <p>
     * The returned Bytes is ready for reading.
     * <p>
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
     * @param byteBuffer to wrap, non-null
     * @return a new Bytes wrapping the provided {@code byteBuffer}
     * @throws NullPointerException    if the provided {@code byteBuffer} is {@code null}
     * @throws ReadOnlyBufferException if the provided {@code byteBuffer} is read-only
     */
    @NotNull
    static Bytes<ByteBuffer> wrapForRead(@NotNull final ByteBuffer byteBuffer) {
        requireNonNull(byteBuffer);
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
     * Creates and returns a new Bytes wrapping the provided {@code byteBuffer}.
     * <p>
     * The returned Bytes is ready for writing.
     * <p>
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
     * @param byteBuffer to wrap, non-null
     * @return a new Bytes wrapping the provided {@code byteBuffer}
     * @throws NullPointerException    if the provided {@code byteBuffer} is {@code null}
     * @throws ReadOnlyBufferException if the provided {@code byteBuffer} is read-only
     */
    @NotNull
    static Bytes<ByteBuffer> wrapForWrite(@NotNull final ByteBuffer byteBuffer) {
        requireNonNull(byteBuffer);
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
     * Creates and returns a new Bytes wrapping the provided {@code byteArray}.
     * <p>
     * The returned Bytes is ready for reading.
     * <p>
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
     * @param byteArray to wrap, non-null
     * @return a new Bytes wrapping the provided {@code byteArray}
     * @throws IllegalStateException if the provided {@code byteArray} is {@code null}
     */
    @NotNull
    static Bytes<byte[]> wrapForRead(@NotNull byte[] byteArray) {
        requireNonNull(byteArray);
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
     * Creates and returns a new Bytes wrapping the provided {@code byteArray}.
     * <p>
     * The returned Bytes is ready for writing.
     * <p>
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
     * @param byteArray to wrap, non-null
     * @return Creates and returns a new Bytes wrapping the provided {@code byteArray}
     * @throws NullPointerException if the provided {@code byteArray} is {@code null}
     */
    @NotNull
    static Bytes<byte[]> wrapForWrite(@NotNull byte[] byteArray) {
        requireNonNull(byteArray);
        final BytesStore bs = BytesStore.wrap(byteArray);
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
     * Creates and returns a new ISO-8859-1 coded Bytes object from the {@code text}.
     * <p>
     * The returned Bytes is ready for reading.
     * <p>
     * The returned Bytes is allocated on the heap.
     *
     * @param text non-null text to convert
     * @return a new Bytes containing text
     * @throws NullPointerException if the provided {@code text} is {@code null}
     */
    @NotNull
    static Bytes<byte[]> from(@NotNull CharSequence text) {
        requireNonNull(text);
        return from(text.toString());
    }

    /**
     * Creates and returns a new ISO-8859-1 coded Bytes object from the {@code text}.
     * <p>
     * The returned Bytes is ready for reading.
     * <p>
     * The returned Bytes is allocated using native memory.
     *
     * @param text non-null text to convert
     * @return a new Bytes containing text
     * @throws NullPointerException if the provided {@code text} is {@code null}
     */
    static Bytes<Void> fromDirect(@NotNull CharSequence text) {
        requireNonNull(text);
        return NativeBytes.nativeBytes(text.length()).append(text);
    }

    /**
     * Creates and returns a new ISO-8859-1 coded Bytes object from the {@code text}.
     * <p>
     * The returned Bytes is ready for reading.
     * <p>
     * The returned Bytes is allocated using native memory.
     *
     * @param text non-null text to convert
     * @return a new Bytes containing text
     * @throws NullPointerException if the provided {@code text} is {@code null}
     */
    @NotNull
    static Bytes<byte[]> directFrom(@NotNull String text) {
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
     * Creates and returns a new ISO-8859-1 coded Bytes object from the {@code text}.
     * <p>
     * The returned Bytes is ready for reading.
     * <p>
     * The returned Bytes is allocated using native memory.
     *
     * @param text non-null text to convert
     * @return a new Bytes containing text
     * @throws NullPointerException if the provided {@code text} is {@code null}
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
     * memory with the provided {@code capacity}.
     * <p>
     *
     * @param capacity the non-negative capacity given in bytes
     * @return a new fix sized wrapper
     * @throws IllegalArgumentException if the provided {@code capacity} is negative.
     */
    @NotNull
    static VanillaBytes<Void> allocateDirect(@NonNegative long capacity)
            throws IllegalArgumentException {
        @NotNull BytesStore<?, Void> bs = BytesStore.nativeStoreWithFixedCapacity(requireNonNegative(capacity));
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
     * @return a new elastic wrapper
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
     * @return a new elastic wrapper
     * @throws IllegalArgumentException if the provided {@code initialCapacity} is negative
     */
    @NotNull
    static NativeBytes<Void> allocateElasticDirect(@NonNegative long initialCapacity)
            throws IllegalArgumentException {
        return NativeBytes.nativeBytes(requireNonNegative(initialCapacity));
    }

    /**
     * Creates and returns a new elastic wrapper for on heap memory with the
     * {@code initialCapacity} 32 bytes which will be resized as required.
     *
     * @return a new elastic wrapper
     */
    @NotNull
    static OnHeapBytes allocateElasticOnHeap() {
        return allocateElasticOnHeap(32);
    }

    /**
     * Creates and returns a new elastic wrapper for on heap memory with the provided
     * {@code initialCapacity} which will be resized as required.
     *
     * @param initialCapacity the initial capacity of the wrapper in bytes
     * @return a new elastic wrapper
     * @throws IllegalArgumentException if the provided {@code initialCapacity} is negative
     */
    @NotNull
    static OnHeapBytes allocateElasticOnHeap(@NonNegative int initialCapacity) {
        requireNonNegative(initialCapacity);
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
     * Creates and returns a String extracted from the provided {@code buffer }
     * staring from the provided {@code readPosition} to the
     * provided {@code readLimit}. The buffer is not modified by this call.
     *
     * @param buffer the non-null buffer to use
     * @return a String extracted from the buffer
     * provided {@code readLimit}
     * @throws NullPointerException if the provided {@code buffer} is {@code null}
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
     * @param buffer the non-null buffer to use
     * @param maxLen the maximum length from the buffer, used to create a new string
     * @return a string extracted from the buffer
     * @throws NullPointerException if the provided {@code buffer} is {@code null} or if the provided {@code maxLen} is negative
     */
    @NotNull
    static String toString(@NotNull final Bytes<?> buffer,
                           final long maxLen) throws
            BufferUnderflowException, IllegalStateException, IllegalArgumentException {
        requireNonNegative(maxLen);
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
     * Creates and returns a String from the bytes of the provided {@code buffer} with the provided {@code length }
     * staring from the provided {@code offset}.
     * <p>
     * The buffer is not modified by this call.
     *
     * @param buffer   the non-null buffer to use
     * @param position the offset position to create the string from
     * @param length   the number of characters to include in the string
     * @return a String extracted from the buffer
     * @throws NullPointerException if the provided {@code buffer} is {@code null} or
     * @throws IllegalArgumentException if the provided {@code position} or provided {@code length} is negative
     */
    @NotNull
    static String toString(@NotNull final Bytes buffer,
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
     * @throws NullPointerException if the provided {@code bytes} is {@code null}
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
     * Returns the lowest value such that the contents of the provided {@code source } equals the contents of the
     * provided {@code other } starting at the provided {@code fromSourceOffset}, or -1 is returned if no such value exists.
     * <p>
     * If the provided {@code fromSourceOffset} is negative, it will be treated as zero.
     * <p>
     * Code shared by String and StringBuffer to do searching. The
     * source is the character array being searched, and the other
     * is the string being searched for.
     *
     * @param source           the non-null read bytes being searched.
     * @param other            the non-null read bytes being searched for.
     * @param fromSourceOffset the index to begin searching from,
     * @return index where contents equals or -1
     * @throws NullPointerException if the provided {@code source} or the provided {@code source} is {@code null}
     * @deprecated for removal in x.23
     */
    @Deprecated(/* suggest for removal in x.23 as this is supposed to be used only by other methods in this interface and can be internalised */)
    static int indexOf(final @NotNull BytesStore source,
                       final @NotNull BytesStore other,
                       int fromSourceOffset) throws IllegalStateException {

        long sourceOffset = source.readPosition();
        long otherOffset = other.readPosition();
        long sourceCount = source.readRemaining();
        long otherCount = other.readRemaining();

        if (fromSourceOffset >= sourceCount) {
            return Math.toIntExact(otherCount == 0 ? sourceCount : -1);
        }
        if (fromSourceOffset < 0) {
            fromSourceOffset = 0;
        }
        if (otherCount == 0) {
            return fromSourceOffset;
        }
        try {
            byte firstByte = other.readByte(otherOffset);
            long max = sourceOffset + (sourceCount - otherCount);

            for (long i = sourceOffset + fromSourceOffset; i <= max; i++) {
                /* Look for first character. */
                if (source.readByte(i) != firstByte) {
                    while (++i <= max && source.readByte(i) != firstByte) ;
                }

                /* Found first character, now look at the rest of v2 */
                if (i <= max) {
                    long j = i + 1;
                    long end = j + otherCount - 1;
                    for (long k = otherOffset + 1; j < end && source.readByte(j) == other.readByte(k); j++, k++) {
                        // Do nothing
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
     * Creates and returns a new Bytes which is optionally unchecked as indicated by the provided {@code unchecked}.
     * <p>
     * This allows bounds checks to be turned off.
     * Note: this means that the result is no longer elastic, even if <code>this</code> is elastic.
     *
     * @param unchecked if true, minimal bounds checks will be performed
     * @return a new, potentially unchecked, Bytes
     * @throws IllegalStateException if the underlying BytesStore has been released
     */
    @NotNull
    default Bytes<Underlying> unchecked(boolean unchecked)
            throws IllegalStateException {
        if (unchecked) {
            if (isElastic())
                BytesUtil.WarnUncheckedElasticBytes.warn();
            Bytes<Underlying> underlyingBytes = start() == 0 && bytesStore().isDirectMemory()
                    ? new UncheckedNativeBytes<>(this)
                    : new UncheckedBytes<>(this);
            release(INIT);
            return underlyingBytes;
        }
        return this;
    }

    /**
     * Returns if this Bytes object is unchecked.
     * <p>
     * An unchecked Bytes object performs little or no bounds checking
     *
     * @return if this Bytes object is unchecked
     */
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
     * Creates and returns a new copy of this Bytes object from position() to limit().
     *
     * @return a copy of this Bytes object
     */
    @Override
    BytesStore<Bytes<Underlying>, Underlying> copy()
            throws IllegalStateException;

    /**
     * Creates and returns a new String representing the contents of this Bytes object in hexadecimal form encoding
     * at most 1024 bytes.
     *
     * @return a new hex String
     */
    @NotNull
    default String toHexString() {
        return toHexString(1024);
    }

    /**
     * Creates and returns a new String representing the contents of this Bytes object in hexadecimal form encoding
     * at most the provided {@code maxLength } bytes.
     * <p>
     * Displays the hex data of {@link Bytes} from the {@code readPosition} up to the specified length.
     *
     * @param maxLength limit the number of bytes to be dumped
     * @return a new hex String
     * @throws IllegalArgumentException if the provided {@code maxLength} is negative.
     */
    @NotNull
    default String toHexString(long maxLength) {
        return toHexString(readPosition(), maxLength);
    }

    /**
     * Creates and returns a new String representing the contents of this Bytes object in hexadecimal form
     * starting at the provided {@code offset} encoding at most the provided {@code maxLength } bytes.
     *
     * @param offset    the specified offset to start from
     * @param maxLength limit the number of bytes to be encoded
     * @return a new hex String
     * @throws IllegalArgumentException if the provided {@code maxLength}  or provided {@code maxLength} is negative.
     */
    @NotNull
    default String toHexString(long offset, long maxLength) {
        requireNonNegative(offset);
        requireNonNegative(maxLength);

        long maxLength2 = Math.min(maxLength, readLimit() - offset);
        try {
            @NotNull String ret = BytesInternal.toHexString(this, offset, maxLength2);
            return maxLength2 < readLimit() - offset ? ret + "... truncated" : ret;
        } catch (BufferUnderflowException | IllegalStateException e) {
            return e.toString();
        }
    }

    /**
     * Returns if this Bytes object is elastic. I.e. it can resize when more data is written
     * than it's {@link #realCapacity()}.
     *
     * @return if this Bytes object is elastic
     */
    boolean isElastic();

    /**
     * Grows the buffer if the buffer is elastic, if the buffer is not elastic and there is not
     * enough capacity then this method will throw an {@link IllegalArgumentException}
     *
     * @param desiredCapacity the capacity that you required
     * @throws IllegalArgumentException if the buffer is not elastic and there is not enough space or if the
     *                                  provided {@code desiredCapacity} is negative;
     */
    default void ensureCapacity(@NonNegative long desiredCapacity)
            throws IllegalArgumentException, IllegalStateException {
        requireNonNegative(desiredCapacity);
        if (desiredCapacity > capacity())
            throw new IllegalArgumentException(isElastic() ? "todo" : "not elastic");
    }

    /**
     * Creates and returns a new slice of this Bytes object whereby the start is moved to the readPosition and the
     * current limit determines the capacity.
     * <p>
     * As a sub-section of a Bytes, it cannot be elastic.
     *
     * @return a new slice of this current Bytes object.
     * @throws IllegalStateException if the underlying BytesStore has been released
     */
    @NotNull
    @Override
    default Bytes<Underlying> bytesForRead()
            throws IllegalStateException {
        try {
            return isClear()
                    ? BytesStore.super.bytesForRead()
                    : new SubBytes<>(this, readPosition(), readLimit() + start());
        } catch (IllegalArgumentException | BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns the backing ByteStore this Bytes object wraps, or null.
     *
     * @return the backing ByteStore
     */
    @Override
    @Nullable
    BytesStore bytesStore();

    /**
     * Returns if this Bytes object contents equals the provided {@code other}.
     *
     * @param other to test for equality, nullable
     * @return if this Bytes object contents equals the provided {@code other}
     * @throws IllegalStateException if this Bytes object has been previously released
     */
    default boolean isEqual(@Nullable String other)
            throws IllegalStateException {
        return StringUtils.isEqual(this, other);
    }

    /**
     * Compact these Bytes by moving the readPosition to the start.
     *
     * @return this Bytes object
     * @throws IllegalStateException if this Bytes object has been previously released
     */
    @NotNull
    Bytes<Underlying> compact()
            throws IllegalStateException;

    /**
     * Copies bytes from this Bytes object to another provided {@code targetByteStore}.
     *
     * @param targetByteStore the non-null BytesStore to copy to
     * @return the actual number of bytes copied
     * @throws NullPointerException if the provided {@code targetByteStore} is {@code null}
     */
    @Override
    default long copyTo(@NotNull final BytesStore targetByteStore)
            throws IllegalStateException {
        return BytesStore.super.copyTo(targetByteStore);
    }

    /**
     * Copies bytes from this Bytes object to the provided {@code outputStream}.
     *
     * @param outputStream the specified non-null OutputStream that this Bytes object is copied to
     * @throws IllegalStateException if this Bytes object has been previously released
     * @throws IOException           if an I/O error occurs writing to the provided {@code outputStream}
     * @throws NullPointerException  if the provided {@code outputStream } is {@code null}
     */
    @Override
    default void copyTo(@NotNull OutputStream outputStream)
            throws IOException, IllegalStateException {
        BytesStore.super.copyTo(outputStream);
    }

    /**
     * Returns if this Bytes object is using memory that can be shared across processes.
     * <p>
     * A Bytes object that is backed by a memory mapped file can be shared whereas a Bytes object that
     * is backed by heap memory cannot be shared across processes.
     *
     * @return if this Bytes object is using memory that can be shared across processes
     */
    @Override
    default boolean sharedMemory() {
        return bytesStore().sharedMemory();
    }

    /**
     * Will un-write a specified number of bytes from an offset from this Bytes object.
     * <p>
     * Calling this method will update the cursors of this Bytes object.
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

    /**
     * Creates and returns a new BigDecimal representing the contents of this Bytes object.
     * <p>
     * If this Byte object is empty, an object equal to {@link BigDecimal#ZERO} is returned.
     *
     * @return a new BigDecimal
     * @throws ArithmeticException      if the content of this Bytes object could not be successfully converted
     * @throws BufferUnderflowException if the content of this Bytes object is insufficient to be successfully converted
     * @throws IllegalStateException    if this Bytes object was previously released
     */
    @NotNull
    default BigDecimal readBigDecimal()
            throws ArithmeticException, BufferUnderflowException, IllegalStateException {
        return new BigDecimal(readBigInteger(), Maths.toUInt31(readStopBit()));
    }

    /**
     * Creates and returns a new BigInteger representing the contents of this Bytes object or {@link BigInteger#ZERO}
     * if this Bytes object is empty.
     *
     * @return a new BigInteger
     * @throws ArithmeticException      if the content of this Bytes object could not be successfully converted
     * @throws BufferUnderflowException if the content of this Bytes object is insufficient to be successfully converted
     * @throws IllegalStateException    if this Bytes object was previously released
     */
    @NotNull
    default BigInteger readBigInteger()
            throws ArithmeticException, BufferUnderflowException, IllegalStateException {
        int length = Maths.toUInt31(readStopBit());
        if (length == 0) {
            if (lenient()) {
                return BigInteger.ZERO;
            } else {
                throw new BufferUnderflowException();
            }
        }
        byte[] bytes = new byte[length];
        read(bytes);
        return new BigInteger(bytes);
    }

    /**
     * Returns the lowest index value for which the contents of this Bytes object equals the provided {@code source },
     * or -1 if no such index value exists.
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
     * @param source the non-null sub-bytes to search for.
     * @return index of equal contents or -1
     * @throws NullPointerException if the provided {@code source} is {@code null}
     */
    default long indexOf(@NotNull Bytes source)
            throws IllegalStateException {
        return indexOf(this, source, 0);
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
     * @throws NullPointerException if the provided {@code source } is {@code null}
     */
    default int indexOf(@NotNull BytesStore source, int fromIndex)
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

    /**
     * Writes the content of this Bytes object into the provided {@code bytesOut} writing the provided
     * {@code length } bytes.
     * <p>
     * The readLimit and readPosition is not affected by this operation.
     *
     * @param length   indicating the number of bytes to write
     * @param bytesOut non-null target to write the content of this Bytes object to
     * @throws BufferUnderflowException if the provided {@code length} is greater than the remaining bytes readable.
     * @throws IORuntimeException       if there is an error reading or writing data
     * @throws BufferOverflowException  if the provided {@code bytesOut} cannot hold the bytes to be written
     * @throws IllegalStateException    if this Bytes object or the provided {@code bytesOut} has been previously released.
     * @throws IllegalArgumentException if the provided {@code length} is negative.
     * @throws NullPointerException     if the provided {@code bytesOut} is {@code null}.
     */
    default void readWithLength(long length, @NotNull BytesOut<Underlying> bytesOut)
            throws BufferUnderflowException, IORuntimeException, BufferOverflowException, IllegalStateException {
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
     * Reads the content of this Bytes object converting it to an object of the provided {@code clazz} using the
     * provided {@code using}, or if {@code null} is provided for {@code using}, creates a new object, eventually
     * returning whichever object was updated.
     * <p>
     * The content of this Bytes object is assumed to have a 16-bit length indicator preceding the actual object.
     *
     * @param clazz non-null type to read
     * @param using to update, nullable
     * @param <T>   type of the provided using
     * @return An object that was read from this Bytes object
     * @throws BufferUnderflowException if the content of this Bytes object is insufficient.
     * @throws IllegalStateException    if this Bytes object or the provided {@code bytesOut} has been previously released.
     * @throws NullPointerException     if the provided {@code clazz} is {@code null}
     * @see #writeMarshallableLength16(WriteBytesMarshallable)
     */
    default <T extends ReadBytesMarshallable> T readMarshallableLength16(@NotNull final Class<T> clazz,
                                                                         @Nullable final T using)
            throws BufferUnderflowException, IllegalStateException {

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
     * Writes the content of the provided {@code marshallable} to this Bytes object converting from an object
     * to a number of bytes.
     * <p>
     * The content of this Bytes object will have a 16-bit length indicator preceding the actual object.
     *
     * @param marshallable non-null object to write to this Bytes object
     * @throws BufferOverflowException  if the capacity of this Bytes object is insufficient.
     * @throws IllegalStateException    if this Bytes object or the provided {@code bytesOut} has been previously released
     *                                  or if the contents of this Bytes object cannot fit under 16-bit addressing
     * @throws NullPointerException     if the provided {@code marshallable} is {@code null}
     * @see #readMarshallableLength16(Class, ReadBytesMarshallable)
     */
    default void writeMarshallableLength16(@NotNull final WriteBytesMarshallable marshallable)
            throws IllegalArgumentException, BufferOverflowException, IllegalStateException, BufferUnderflowException {
        requireNonNull(marshallable);
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

    /**
     * Writes the contents of the provided `inputStream` to the contents of this Bytes object.
     *
     * @param inputStream non-null to read from
     * @return this Bytes object
     * @throws IOException              if an I/O error occur on the inputStream
     * @throws BufferOverflowException  if this Bytes object lacks capacity to write the entire provided {@code inputStream }
     * @throws IllegalStateException    if this Bytes object or the provided {@code bytesOut} has been previously released
     * @throws NullPointerException     if the provided {@code InputStream} is {@code null}.
     */
    default Bytes write(@NotNull final InputStream inputStream)
            throws IOException, BufferOverflowException, IllegalStateException {
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
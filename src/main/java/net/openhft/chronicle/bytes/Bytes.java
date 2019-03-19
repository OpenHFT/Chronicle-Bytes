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

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;
import net.openhft.chronicle.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
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
 * must follow these constraints <p></p> start() &lt;= readPosition() &lt;= writePosition() &lt;=
 * writeLimit() &lt;= capacity() <p></p> Also readLimit() == writePosition() and readPosition()
 * &lt;= safeLimit(); <p></p>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public interface Bytes<Underlying> extends
        BytesStore<Bytes<Underlying>, Underlying>,
        BytesIn<Underlying>,
        BytesOut<Underlying> {

    long MAX_CAPACITY = Long.MAX_VALUE; // 8 EiB - 1
    int MAX_BYTE_BUFFER_CAPACITY = Integer.MAX_VALUE & ~(OS.pageSize() - 1);
    int DEFAULT_BYTE_BUFFER_CAPACITY = 256;

    /**
     * @return an elastic wrapper for a direct ByteBuffer which will be resized as required.
     */
    static Bytes<ByteBuffer> elasticByteBuffer() {
        return elasticByteBuffer(DEFAULT_BYTE_BUFFER_CAPACITY);
    }

    static Bytes<ByteBuffer> elasticByteBuffer(int initialCapacity, int maxSize) {
        @NotNull NativeBytesStore<ByteBuffer> bs = NativeBytesStore.elasticByteBuffer(initialCapacity, maxSize);
        try {
            return bs.bytesForWrite();
        } finally {
            bs.release();
        }
    }

    /**
     * Returns an elastic wrapper for a direct ByteBuffer which will be resized as required, with
     * the given initial capacity.
     */
    static Bytes<ByteBuffer> elasticByteBuffer(int initialCapacity) {
        return elasticByteBuffer(initialCapacity, MAX_BYTE_BUFFER_CAPACITY);
    }

    /**
     * Returns an elastic wrapper for a heap ByteBuffer which will be resized as required, with
     * the given initial capacity.
     */
    @NotNull
    static Bytes<ByteBuffer> elasticHeapByteBuffer(int initialCapacity) {
        @NotNull HeapBytesStore<ByteBuffer> bs = HeapBytesStore.wrap(ByteBuffer.allocate(initialCapacity));
        try {
            return new NativeBytes<>(bs);
        } finally {
            bs.release();
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
     *     public static void main(String[] args) throws InterruptedException {
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
    static Bytes<ByteBuffer> wrapForRead(@NotNull ByteBuffer byteBuffer) {
        BytesStore<?, ByteBuffer> bs = BytesStore.wrap(byteBuffer);
        try {
            Bytes<ByteBuffer> bbb = bs.bytesForRead();
            bbb.readLimit(byteBuffer.limit());
            bbb.readPosition(byteBuffer.position());
            return bbb;
        } finally {
            bs.release();
        }
    }

    /**
     * Wrap the ByteBuffer ready for writing
     * Method for convenience only - might not be ideal for performance (creates garbage).
     * To avoid garbage, use something like this example:
     * <pre>{@code
     * import net.openhft.chronicle.bytes.Bytes;
     * import java.nio.ByteBuffer;
     *
     * public class ChronicleBytesWithByteBufferExampleTest {
     *     private static final String HELLO_WORLD = "hello world";
     *
     *     public static void main(String[] args) throws InterruptedException {
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
    static Bytes<ByteBuffer> wrapForWrite(@NotNull ByteBuffer byteBuffer) {
        BytesStore<?, ByteBuffer> bs = BytesStore.wrap(byteBuffer);
        try {
            Bytes<ByteBuffer> bbb = bs.bytesForWrite();
            bbb.writePosition(byteBuffer.position());
            bbb.writeLimit(byteBuffer.limit());
            return bbb;
        } finally {
            bs.release();
        }
    }

    /**
     * Wrap the byte[] ready for reading
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
    static Bytes<byte[]> wrapForRead(@NotNull byte[] byteArray) {
        @NotNull HeapBytesStore<byte[]> bs = BytesStore.wrap(byteArray);
        try {
            return bs.bytesForRead();
        } finally {
            bs.release();
        }
    }

    /**
     * Wrap the byte[] ready for writing
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
    static Bytes<byte[]> wrapForWrite(@NotNull byte[] byteArray) {
        @NotNull BytesStore bs = BytesStore.wrap(byteArray);
        try {
            return bs.bytesForWrite();
        } finally {
            bs.release();
        }
    }

    /**
     * Convert text to bytes using ISO-8859-1 encoding and return a Bytes ready for reading.
     *
     * @param text to convert
     * @return Bytes ready for reading.
     */
    static Bytes<byte[]> from(@NotNull CharSequence text) {
        if (text instanceof BytesStore)
            return ((BytesStore) text).copy().bytesForRead();
        return wrapForRead(text.toString().getBytes(StandardCharsets.ISO_8859_1));
    }

    static Bytes<byte[]> fromString(@NotNull String text) throws IllegalArgumentException, IllegalStateException {
        return wrapForRead(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    /**
     * Allocate a fixed size buffer read for writing.
     *
     * @param capacity minimum to allocate
     * @return a new Bytes ready for writing.
     */
    static VanillaBytes<Void> allocateDirect(long capacity) throws IllegalArgumentException {
        @NotNull NativeBytesStore<Void> bs = NativeBytesStore.nativeStoreWithFixedCapacity(capacity);
        try {
            return bs.bytesForWrite();
        } finally {
            bs.release();
        }
    }

    /**
     * Allocate an elastic buffer with initially no size.
     *
     * @return Bytes for writing.
     */
    static NativeBytes<Void> allocateElasticDirect() {
        return NativeBytes.nativeBytes();
    }

    /**
     * Allocate an elastic buffer with {@code initialCapacity} size.
     *
     * @return Bytes for writing.
     */
    static NativeBytes<Void> allocateElasticDirect(long initialCapacity) throws IllegalArgumentException {
        return NativeBytes.nativeBytes(initialCapacity);
    }

    /**
     * Creates a string from the {@code position} to the {@code limit}, The buffer is not modified
     * by this call
     *
     * @param buffer the buffer to use
     * @return a string contain the text from the {@code position}  to the  {@code limit}
     */
    static String toString(@NotNull final Bytes<?> buffer) throws BufferUnderflowException {
        return toString(buffer, Integer.MAX_VALUE - 4);
    }

    /**
     * Creates a string from the {@code position} to the {@code limit}, The buffer is not modified
     * by this call
     *
     * @param buffer the buffer to use
     * @param maxLen of the result returned
     * @return a string contain the text from the {@code position}  to the  {@code limit}
     */
    static String toString(@NotNull final Bytes<?> buffer, long maxLen) throws
            BufferUnderflowException {

        if (buffer.refCount() < 1)
            // added because something is crashing the JVM
            return "<unknown>";

        buffer.reserve();
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
            buffer.release();
        }
    }

    /**
     * The buffer is not modified by this call
     *
     * @param buffer   the buffer to use
     * @param position the position to create the string from
     * @param len      the number of characters to show in the string
     * @return a string contain the text from offset {@code position}
     */

    static String toString(@NotNull final Bytes buffer, long position, long len)
            throws BufferUnderflowException {
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
    }

    /**
     * copies the contents of bytes into a direct byte buffer
     *
     * @param bytes the bytes to wrap
     * @return a direct byte buffer contain the {@code bytes}
     */
    @NotNull
    static Bytes allocateDirect(@NotNull byte[] bytes) throws IllegalArgumentException {
        Bytes<Void> result = allocateDirect(bytes.length);
        try {
            result.write(bytes);
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
        return result;
    }

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
     */
    static int indexOf(@NotNull BytesStore source, @NotNull BytesStore target, int fromIndex) {

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
    }

    /**
     * Return a Bytes which is optionally unchecked.  This allows bounds checks to be turned off.
     * Note: this means that the result is no longer elastic, even if <code>this</code> is elastic.
     *
     * @param unchecked if true, minimal bounds checks will be performed.
     * @return Bytes without bounds checking.
     * @throws IllegalStateException if the underlying BytesStore has been released
     */
    @NotNull
    default Bytes<Underlying> unchecked(boolean unchecked) throws IllegalStateException {
        if (unchecked) {
            if (isElastic())
                Jvm.debug().on(getClass(), "Wrapping elastic bytes with unchecked() will require calling ensureCapacity() as needed!");
            return start() == 0 && bytesStore().isDirectMemory() ?
                    new UncheckedNativeBytes<>(this) :
                    new UncheckedBytes<>(this);
        }
        return this;
    }

    default boolean unchecked() {
        return false;
    }

    /**
     * @return the size which can be safely read.  If this isElastic() it can be lower than the
     * point it can safely write.
     */
    @Override
    default long safeLimit() {
        return bytesStore().safeLimit();
    }

    /**
     * @return is the readPosition at the start and the writeLimit at the end.
     */
    @Override
    default boolean isClear() {
        return start() == readPosition() && writeLimit() == capacity();
    }

    /**
     * @return if isElastic, this can be much lower than the virtual capacity().
     */
    @Override
    default long realCapacity() {
        return BytesStore.super.realCapacity();
    }

    /**
     * @return a copy of this Bytes from position() to limit().
     */
    @Override
    BytesStore<Bytes<Underlying>, Underlying> copy();

    /**
     * display the hex data of {@link Bytes} from the position() to the limit()
     *
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    @NotNull
    default String toHexString() {
        return toHexString(1024);
    }

    /**
     * display the hex data of {@link Bytes} from the position() to the limit()
     *
     * @param maxLength limit the number of bytes to be dumped.
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    @NotNull
    default String toHexString(long maxLength) {
        return toHexString(readPosition(), maxLength);
    }

    /**
     * display the hex data of {@link Bytes} from the position() to the limit()
     *
     * @param maxLength limit the number of bytes to be dumped.
     * @return hex representation of the buffer, from example [0D ,OA, FF]
     */
    @NotNull
    default String toHexString(long offset, long maxLength) {
//        if (Jvm.isDebug() && Jvm.stackTraceEndsWith("Bytes", 3))
//            return "Not Available";

        long maxLength2 = Math.min(maxLength, readLimit() - offset);
        @NotNull String ret = BytesInternal.toHexString(this, offset, maxLength2);
        return maxLength2 < readLimit() - offset ? ret + "... truncated" : ret;
    }

    /**
     * @return can the Bytes resize when more data is written than it's realCapacity()
     */
    boolean isElastic();

    /**
     * grow the buffer if the buffer is elastic, if the buffer is not elastic and there is not
     * enough capacity then this method will throws {@link java.nio.BufferOverflowException}
     *
     * @param size the capacity that you required
     * @throws IllegalArgumentException if the buffer is not elastic and there is not enough space
     */
    default void ensureCapacity(long size) throws IllegalArgumentException {
        if (size > capacity())
            throw new IllegalArgumentException(isElastic() ? "todo" : "not elastic");
    }

    /**
     * Creates a slice of the current Bytes based on its position() and limit().  As a sub-section
     * of a Bytes it cannot be elastic.
     *
     * @return a slice of the existing Bytes where the start is moved to the position and the
     * current limit determines the capacity.
     * @throws IllegalStateException if the underlying BytesStore has been released
     */
    @NotNull
    @Override
    default Bytes<Underlying> bytesForRead() throws IllegalStateException {
        return isClear() ? BytesStore.super.bytesForRead() : new SubBytes<>(this, readPosition(), readLimit() + start());
    }

    /**
     * @return the ByteStore this Bytes wraps.
     */
    @Override
    @Nullable
    BytesStore bytesStore();

    default boolean isEqual(String s) {
        return StringUtils.isEqual(this, s);
    }

    /**
     * Compact these Bytes by moving the readPosition to the start.
     *
     * @return this
     */
    @NotNull
    Bytes<Underlying> compact();

    /**
     * copy bytes from one ByteStore to another
     *
     * @param store to copy to
     * @return the number of bytes copied.
     */
    @Override
    default long copyTo(@NotNull BytesStore store) {
        return BytesStore.super.copyTo(store);
    }

    @Override
    default void copyTo(OutputStream out) throws IOException {
        BytesStore.super.copyTo(out);
    }

    @Override
    default boolean sharedMemory() {
        return bytesStore().sharedMemory();
    }

    /**
     * will unwrite from the offset upto the current write position of the destination bytes
     *
     * @param fromOffset the offset from the target byytes
     * @param count      the number of bytes to un-write
     */
    default void unwrite(long fromOffset, int count) {
        long wp = writePosition();

        if (wp < fromOffset)
            return;

        write(fromOffset, this, fromOffset + count, wp - fromOffset);
        writeSkip(-count);
    }

    @NotNull
    default BigDecimal readBigDecimal() {
        return new BigDecimal(readBigInteger(), Maths.toUInt31(readStopBit()));
    }

    @NotNull
    default BigInteger readBigInteger() {
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
     * Returns the index within this bytes of the first occurrence of the
     * specified sub-bytes.
     * <p>
     * <p>The returned index is the smallest value <i>k</i> for which:
     * <blockquote><pre>
     * this.startsWith(bytes, <i>k</i>)
     * </pre></blockquote>
     * If no such value of <i>k</i> exists, then {@code -1} is returned.
     *
     * @param source the sub-bytes to search for.
     * @return the index of the first occurrence of the specified sub-bytes,
     * or {@code -1} if there is no such occurrence.
     */
    default long indexOf(@NotNull Bytes source) {
        return indexOf(source, 0);
    }

    /**
     * Returns the index within this bytes of the first occurrence of the
     * specified subbytes.
     * <p>
     * <p>The returned index is the smallest value <i>k</i> for which:
     * <blockquote><pre>
     * this.startsWith(bytes, <i>k</i>)
     * </pre></blockquote>
     * If no such value of <i>k</i> exists, then {@code -1} is returned.
     *
     * @param source    the sub-bytes to search for.
     * @param fromIndex start the seach from this offset
     * @return the index of the first occurrence of the specified sub-bytes,
     * or {@code -1} if there is no such occurrence.
     */
    default int indexOf(@NotNull BytesStore source, int fromIndex) {
        return indexOf(this, source, fromIndex);
    }

    @Deprecated
    default long indexOf(@NotNull Bytes source, int fromIndex) {
        return indexOf(this, source, fromIndex);
    }

    @Override
    @NotNull
    Bytes<Underlying> clear();

    @Override
    default boolean readWrite() {
        return bytesStore().readWrite();
    }

    default void readWithLength(long length, @NotNull BytesOut<Underlying> bytesOut)
            throws BufferUnderflowException, IORuntimeException {
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

    default <T extends ReadBytesMarshallable> T readMarshallableLength16(Class<T> tClass, T object) {
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

    default void writeMarshallableLength16(WriteBytesMarshallable marshallable) {
        long position = writePosition();
        writeUnsignedShort(0);
        marshallable.writeMarshallable(this);
        long length = writePosition() - position - 2;
        if (length >= 1 << 16)
            throw new IllegalStateException("Marshallable " + marshallable.getClass() + " too long was " + length);
        writeUnsignedShort(position, (int) length);
    }

}
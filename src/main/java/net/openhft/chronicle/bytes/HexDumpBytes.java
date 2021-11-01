/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import net.openhft.chronicle.core.util.Histogram;
import net.openhft.chronicle.core.util.ThrowingConsumer;
import net.openhft.chronicle.core.util.ThrowingConsumerNonCapturing;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.regex.Pattern;

import static net.openhft.chronicle.core.util.ObjectUtils.requireNonNull;

@SuppressWarnings({"rawtypes", "unchecked"})
public class HexDumpBytes
        implements Bytes<Void> {

    private static final char[] HEXADECIMAL = "0123456789abcdef".toCharArray();
    private static final Pattern HEX_PATTERN = Pattern.compile("[0-9a-fA-F]{1,2}");

    private final NativeBytes<Void> base;
    private final Bytes<byte[]> text = Bytes.allocateElasticOnHeap(1024);
    private final Bytes<byte[]> comment = Bytes.allocateElasticOnHeap(64);
    private OffsetFormat offsetFormat = null;
    private long startOfLine = 0;
    private int indent = 0;
    private int numberWrap = 16;

    public HexDumpBytes() {
        try {
            base = Bytes.allocateElasticDirect(256);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    HexDumpBytes(@NotNull BytesStore base, Bytes text) {
        try {
            this.base = Bytes.allocateElasticDirect(256);
            this.base.write(base);
            this.text.write(text);
        } catch (BufferOverflowException | IllegalStateException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    public static HexDumpBytes fromText(@NotNull Reader reader)
            throws NumberFormatException {
        HexDumpBytes tb = new HexDumpBytes();
        Reader reader2 = new TextBytesReader(reader, tb.text);
        try (Scanner sc = new Scanner(reader2)) {
            while (sc.hasNext()) {
                if (sc.hasNext(HEX_PATTERN))
                    tb.base.rawWriteByte((byte) Integer.parseInt(sc.next(), 16));
                else
                    sc.nextLine(); // assume it's a comment
            }
        } catch (BufferOverflowException | IllegalStateException e) {
            throw new AssertionError(e);
        }
        return tb;
    }

    public static HexDumpBytes fromText(@NotNull CharSequence text)
            throws NumberFormatException {
        return fromText(new StringReader(text.toString()));
    }

    private static boolean startsWith(@NotNull CharSequence comment, char first) {
        return comment.length() > 0 && comment.charAt(0) == first;
    }

    public HexDumpBytes offsetFormat(OffsetFormat offsetFormat) {
        this.offsetFormat = offsetFormat;
        return this;
    }

    public int numberWrap() {
        return numberWrap;
    }

    public HexDumpBytes numberWrap(int numberWrap) {
        this.numberWrap = numberWrap;
        return this;
    }

    @Override
    public long readRemaining() {
        return base.readRemaining();
    }

    @Override
    public long writeRemaining() {
        return base.writeRemaining();
    }

    @Override
    public long readLimit() {
        return base.readLimit();
    }

    @Override
    public long writeLimit() {
        return base.writeLimit();
    }

    @NotNull
    @Override
    public String toHexString() {
        try {
            if (lineLength() > 0)
                newLine();
            return text.toString();
        } catch (Throwable e) {
            return e.toString();
        }
    }

    @Override
    public int hashCode() {
        return base.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return base.equals(obj);
    }

    @Override
    @NotNull
    public String toString() {
        return base.toString();
    }

    @Override
    public boolean retainsComments() {
        return true;
    }

    @Override
    public Bytes<Void> comment(CharSequence comment)
            throws IllegalStateException {
        if (this.comment.readRemaining() > 0)
            newLine();
        if (startsWith(comment, '#')) {
            indent = 0;
            this.text.append('#').append(comment).append('\n');
            startOfLine = this.text.writePosition();
        } else {
            this.comment.clear().append(comment);
        }
        return this;
    }

    @Override
    public BytesOut indent(int n)
            throws IllegalStateException {
        indent += n;
        if (lineLength() > 0) {
            newLine();
        }
        return this;
    }

    private long lineLength() {
        return this.text.writePosition() - startOfLine;
    }

    private void newLine()
            throws IllegalStateException {
        if (this.comment.readRemaining() > 0) {
            while (lineLength() < numberWrap * 3L - 3)
                this.text.append("   ");
            while (lineLength() < numberWrap * 3L)
                this.text.append(' ');
            this.text.append("# ");
            this.text.append(comment);
            comment.clear();
        }
        this.text.append('\n');
        startOfLine = this.text.writePosition();
    }

    private void appendOffset(long offset)
            throws IllegalStateException, BufferUnderflowException {
        if (offsetFormat == null) return;
        offsetFormat.append(offset, this.text);
        long wp = text.writePosition();
        if (wp > 0 && text.peekUnsignedByte(wp - 1) > ' ')
            text.append(' ');
        startOfLine = text.writePosition();
    }

    @Override
    public BytesStore copy() {
        return new HexDumpBytes(base, text);
    }

    @Override
    public boolean isElastic() {
        return base.isElastic();
    }

    @Override
    public void ensureCapacity(long desiredCapacity)
            throws IllegalArgumentException, IllegalStateException {
        base.ensureCapacity(desiredCapacity);
    }

    @Override
    @NotNull
    public BytesStore bytesStore() {
        return base;
    }

    @Override
    @NotNull
    public Bytes compact() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes clear()
            throws IllegalStateException {
        base.clear();
        text.clear();
        return this;
    }

    @Override
    public boolean isDirectMemory() {
        return false;
    }

    @Override
    public long capacity() {
        return base.capacity();
    }

    @Override
    public long addressForRead(long offset)
            throws UnsupportedOperationException, IllegalStateException, BufferUnderflowException {
        return base.addressForRead(offset);
    }

    @Override
    public long addressForWrite(long offset)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long addressForWritePosition()
            throws UnsupportedOperationException, BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value)
            throws BufferOverflowException, IllegalStateException {
        if (base.compareAndSwapInt(offset & 0xFFFFFFFFL, expected, value)) {
            copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 4);
            return true;
        }
        return false;
    }

    @Override
    public void testAndSetInt(long offset, int expected, int value)
            throws IllegalStateException, BufferOverflowException {
        long off = offset & 0xFFFFFFFFL;
        base.testAndSetInt(off, expected, value);
        copyToText(off, offset >>> 32, 4);
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value)
            throws BufferOverflowException, IllegalStateException {
        if (base.compareAndSwapLong(offset & 0xFFFFFFFFL, expected, value)) {
            copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 8);
            return true;
        }
        return false;
    }

    @Override
    @Nullable
    public Void underlyingObject() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(long from, long to, long length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reserve(ReferenceOwner owner)
            throws IllegalStateException {
        base.reserve(owner);
    }

    @Override
    public void release(ReferenceOwner owner)
            throws IllegalStateException {
        base.release(owner);
        if (base.refCount() == 0) {
            text.releaseLast();
            comment.releaseLast();
        }
    }

    @Override
    public void releaseLast(ReferenceOwner owner)
            throws IllegalStateException {
        base.releaseLast(owner);
        if (base.refCount() == 0) {
            text.releaseLast();
            comment.releaseLast();
        }
    }

    @Override
    public int refCount() {
        return base.refCount();
    }

    @Override
    public boolean tryReserve(ReferenceOwner owner)
            throws IllegalStateException, IllegalArgumentException {
        return base.tryReserve(owner);
    }

    @Override
    public boolean reservedBy(ReferenceOwner owner)
            throws IllegalStateException {
        return base.reservedBy(owner);
    }

    @Override
    @NotNull
    public Bytes<Void> writeByte(long offset, byte i8)
            throws BufferOverflowException, IllegalStateException {
        base.writeByte(offset & 0xFFFFFFFFL, i8);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 1);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeShort(long offset, short i)
            throws BufferOverflowException, IllegalStateException {
        base.writeShort(offset & 0xFFFFFFFFL, i);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 2);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeInt24(long offset, int i)
            throws BufferOverflowException, IllegalStateException {
        base.writeInt24(offset & 0xFFFFFFFFL, i);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 3);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeInt(long offset, int i)
            throws BufferOverflowException, IllegalStateException {
        return writeOrderedInt(offset, i);
    }

    @Override
    @NotNull
    public Bytes<Void> writeOrderedInt(long offset, int i)
            throws BufferOverflowException, IllegalStateException {
        base.writeOrderedInt(offset & 0xFFFFFFFFL, i);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 4);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeLong(long offset, long i)
            throws BufferOverflowException, IllegalStateException {
        return writeOrderedLong(offset, i);
    }

    @Override
    @NotNull
    public Bytes<Void> writeOrderedLong(long offset, long i)
            throws BufferOverflowException, IllegalStateException {
        base.writeOrderedLong(offset & 0xFFFFFFFFL, i);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 8);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeFloat(long offset, float d)
            throws BufferOverflowException, IllegalStateException {
        base.writeFloat(offset & 0xFFFFFFFFL, d);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 4);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeDouble(long offset, double d)
            throws BufferOverflowException, IllegalStateException {
        base.writeDouble(offset & 0xFFFFFFFFL, d);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 8);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeVolatileByte(long offset, byte i8)
            throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> writeVolatileShort(long offset, short i16)
            throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> writeVolatileInt(long offset, int i32)
            throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> writeVolatileLong(long offset, long i64)
            throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> write(long offsetInRDO, @NotNull byte[] bytes, int offset, int length) {
        requireNonNull(bytes);
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(long offsetInRDO, @NotNull ByteBuffer bytes, int offset, int length) {
        requireNonNull(bytes);
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> write(long writeOffset, @NotNull RandomDataInput bytes, long readOffset, long length) {
        requireNonNull(bytes);
        throw new UnsupportedOperationException();
    }

    @Override
    public long write8bit(long position, @NotNull BytesStore bs) {
        requireNonNull(bs);
        throw new UnsupportedOperationException();
    }

    @Override
    public long write8bit(long position, @NotNull String s, int start, int length) {
        requireNonNull(s);
        throw new UnsupportedOperationException();
    }

    @Override
    public void nativeWrite(long address, long position, long size) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> readPosition(long position)
            throws BufferUnderflowException, IllegalStateException {
        base.readPosition(position);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> readLimit(long limit)
            throws BufferUnderflowException {
        base.readLimit(limit);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> readSkip(long bytesToSkip)
            throws BufferUnderflowException, IllegalStateException {
        base.readSkip(bytesToSkip);
        return this;
    }

    @Override
    public void uncheckedReadSkipOne() {
        base.uncheckedReadSkipOne();
    }

    @Override
    public void uncheckedReadSkipBackOne() {
        base.uncheckedReadSkipBackOne();
    }

    @Override
    public long readStopBit()
            throws IORuntimeException, IllegalStateException, BufferUnderflowException {
        return base.readStopBit();
    }

    @Override
    public char readStopBitChar()
            throws IORuntimeException, IllegalStateException, BufferUnderflowException {
        return base.readStopBitChar();
    }

    @Override
    public double readStopBitDouble()
            throws IllegalStateException {
        return base.readStopBitDouble();
    }

    @Override
    public double readStopBitDecimal()
            throws IllegalStateException, BufferUnderflowException {
        return base.readStopBitDecimal();
    }

    @Override
    public byte readByte()
            throws IllegalStateException {
        return base.readByte();
    }

    @Override
    public int readUnsignedByte()
            throws IllegalStateException {
        return base.readUnsignedByte();
    }

    @Override
    public int uncheckedReadUnsignedByte() {
        return base.uncheckedReadUnsignedByte();
    }

    @Override
    public short readShort()
            throws BufferUnderflowException, IllegalStateException {
        return base.readShort();
    }

    @Override
    public int readInt()
            throws BufferUnderflowException, IllegalStateException {
        return base.readInt();
    }

    @Override
    public long readLong()
            throws BufferUnderflowException, IllegalStateException {
        return base.readLong();
    }

    @Override
    public float readFloat()
            throws BufferUnderflowException, IllegalStateException {
        return base.readFloat();
    }

    @Override
    public double readDouble()
            throws BufferUnderflowException, IllegalStateException {
        return base.readDouble();
    }

    @Override
    public int readVolatileInt()
            throws BufferUnderflowException, IllegalStateException {
        return base.readVolatileInt();
    }

    @Override
    public long readVolatileLong()
            throws BufferUnderflowException, IllegalStateException {
        return base.readVolatileLong();
    }

    @Override
    public int peekUnsignedByte()
            throws IllegalStateException {
        return base.peekUnsignedByte();
    }

    @Override
    public int lastDecimalPlaces() {
        return base.lastDecimalPlaces();
    }

    @Override
    public void lastDecimalPlaces(int lastDecimalPlaces) {
        base.lastDecimalPlaces(lastDecimalPlaces);
    }

    @Override
    public boolean lastNumberHadDigits() {
        return base.lastNumberHadDigits();
    }

    @Override
    public void lastNumberHadDigits(boolean lastNumberHadDigits) {
base.lastNumberHadDigits(lastNumberHadDigits);
    }

    @NotNull
    @Override
    public BigDecimal readBigDecimal()
            throws BufferUnderflowException, ArithmeticException, IllegalStateException {
        return base.readBigDecimal();
    }

    @NotNull
    @Override
    public BigInteger readBigInteger()
            throws BufferUnderflowException, ArithmeticException, IllegalStateException {
        return base.readBigInteger();
    }

    @Override
    public void readWithLength(long length, @NotNull BytesOut<Void> bytesOut)
            throws BufferUnderflowException, IORuntimeException, IllegalStateException, BufferOverflowException {
        base.readWithLength(length, bytesOut);
    }

    @Override
    public <T extends ReadBytesMarshallable> T readMarshallableLength16(@NotNull Class<T> clazz, @Nullable T using)
            throws BufferUnderflowException, IllegalStateException {
        return base.readMarshallableLength16(clazz, using);
    }

    @NotNull
    @Override
    public Bytes<Void> readPositionUnlimited(long position)
            throws BufferUnderflowException, IllegalStateException {
        return base.readPositionUnlimited(position);

    }

    @NotNull
    @Override
    public Bytes<Void> readPositionRemaining(long position, long remaining)
            throws BufferUnderflowException, IllegalStateException {
        return base.readPositionRemaining(position, remaining);

    }

    @Override
    public void readWithLength0(long length, @NotNull ThrowingConsumerNonCapturing<Bytes<Void>, IORuntimeException, BytesOut> bytesConsumer, StringBuilder sb, BytesOut toBytes)
            throws BufferUnderflowException, IORuntimeException, IllegalStateException {
        base.readWithLength0(length, bytesConsumer, sb, toBytes);

    }

    @Override
    public void readWithLength(long length, @NotNull ThrowingConsumer<Bytes<Void>, IORuntimeException> bytesConsumer)
            throws BufferUnderflowException, IORuntimeException, IllegalStateException {
        base.readWithLength(length, bytesConsumer);

    }

    @Override
    public boolean readBoolean()
            throws IllegalStateException {
        return base.readBoolean();

    }

    @Override
    public int readUnsignedShort()
            throws BufferUnderflowException, IllegalStateException {
        return base.readUnsignedShort();

    }

    @Override
    public int readInt24()
            throws BufferUnderflowException, IllegalStateException {
        return base.readInt24();

    }

    @Override
    public int readUnsignedInt24()
            throws BufferUnderflowException, IllegalStateException {
        return base.readUnsignedInt24();

    }

    @Override
    public long readUnsignedInt()
            throws BufferUnderflowException, IllegalStateException {
        return base.readUnsignedInt();

    }

    @Nullable
    @Override
    public String readUtf8()
            throws BufferUnderflowException, IORuntimeException, IllegalStateException, ArithmeticException {
        return base.readUtf8();

    }

    @Nullable
    public String readUTFΔ()
            throws IORuntimeException, BufferUnderflowException, IllegalStateException, ArithmeticException {
        return BytesInternal.readUtf8(base);

    }

    @Nullable
    @Override
    public String read8bit()
            throws IORuntimeException, BufferUnderflowException, IllegalStateException, ArithmeticException {
        return base.read8bit();

    }

    @Override
    public <C extends Appendable & CharSequence> boolean readUtf8(@NotNull C sb)
            throws IORuntimeException, IllegalArgumentException, BufferUnderflowException, IllegalStateException, ArithmeticException {
        return base.readUtf8(sb);
    }

    @Override
    public <C extends Appendable & CharSequence> long readUtf8(long offset, @NotNull C sb)
            throws IORuntimeException, IllegalArgumentException, BufferUnderflowException, ArithmeticException, IllegalStateException {
        return base.readUtf8(offset, sb);
    }

    @Override
    public <C extends Appendable & CharSequence> long readUtf8Limited(long offset, @NotNull C sb, int maxUtf8Len)
            throws IORuntimeException, IllegalArgumentException, BufferUnderflowException, IllegalStateException {
        return base.readUtf8Limited(offset, sb, maxUtf8Len);
    }

    @Override
    public @Nullable String readUtf8Limited(long offset, int maxUtf8Len)
            throws BufferUnderflowException, IORuntimeException, IllegalArgumentException, IllegalStateException {
        return base.readUtf8Limited(offset, maxUtf8Len);
    }

    public <C extends Appendable & CharSequence> boolean readUTFΔ(@NotNull C sb)
            throws IORuntimeException, IllegalArgumentException, BufferUnderflowException, IllegalStateException, ArithmeticException {
        return base.readUtf8(sb);

    }

    @Override
    public boolean read8bit(@NotNull Bytes b)
            throws BufferUnderflowException, IllegalStateException, BufferOverflowException, ArithmeticException {
        return base.read8bit(b);

    }

    @Override
    public <C extends Appendable & CharSequence> boolean read8bit(@NotNull C sb)
            throws IORuntimeException, IllegalArgumentException, BufferUnderflowException, IllegalStateException, ArithmeticException {
        return base.read8bit(sb);

    }

    @Override
    public boolean read8bit(@NotNull StringBuilder sb)
            throws IORuntimeException, BufferUnderflowException, ArithmeticException, IllegalStateException {
        return base.read8bit(sb);

    }

    @Override
    public int read(@NotNull byte[] bytes)
            throws IllegalStateException, BufferUnderflowException {
        return base.read(bytes);

    }

    @Override
    public int read(@NotNull byte[] bytes, int off, int len)
            throws IllegalStateException, BufferUnderflowException {
        return base.read(bytes, off, len);

    }

    @Override
    public int read(@NotNull char[] bytes, int off, int len)
            throws IllegalStateException {
        return base.read(bytes, off, len);

    }

    @Override
    public void read(@NotNull ByteBuffer buffer)
            throws IllegalStateException {
        base.read(buffer);

    }

    @Override
    public void read(@NotNull Bytes bytes, int length)
            throws BufferUnderflowException, IllegalStateException, BufferOverflowException {
        base.read(bytes, length);

    }

    @NotNull
    @Override
    public <E extends Enum<E>> E readEnum(@NotNull Class<E> eClass)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException, ArithmeticException, BufferOverflowException {
        return base.readEnum(eClass);

    }

    @Override
    public void readHistogram(@NotNull Histogram histogram)
            throws BufferUnderflowException, IllegalStateException, ArithmeticException {
        base.readHistogram(histogram);

    }

    @Override
    public void readWithLength(Bytes bytes)
            throws ArithmeticException, BufferOverflowException, IllegalStateException, BufferUnderflowException {
        base.readWithLength(bytes);

    }

    @Override
    @NotNull
    public Bytes<Void> writePosition(long position)
            throws BufferOverflowException {
        base.writePosition(position & 0xFFFFFFFFL);
        text.writePosition(position >>> 32);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeLimit(long limit)
            throws BufferOverflowException {
        base.writeLimit(limit);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeSkip(long bytesToSkip)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeSkip(bytesToSkip);
            return this;
        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> writeByte(byte i8)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeByte(i8);
            return this;
        } finally {
            copyToText(pos);
        }
    }

    /**
     * For HexDumpBytes it needs to remember the writePosition for the underlying bytes as well as the text hex dump, so it encodes both in one number so you can call writePosition later.
     *
     * @return the base and text writePositions.
     */
    @Override
    public long writePosition() {
        return base.writePosition() | (text.writePosition() << 32);
    }

    private void copyToText(long pos)
            throws IllegalStateException {
        try {
            if (lineLength() == 0 && offsetFormat != null) {
                appendOffset(pos);
                startOfLine = text.writePosition();
            }

            long end = base.writePosition();
            if (pos < end) {
                doIndent();
                do {
                    int value = base.readUnsignedByte(pos);
                    long ll = lineLength();
                    if (ll >= numberWrap * 3L - 1) {
                        newLine();
                        appendOffset(pos);
                        doIndent();
                        startOfLine = text.writePosition();
                    }
                    pos++;
                    long wp = text.writePosition();
                    if (wp > 0 && text.peekUnsignedByte(wp - 1) > ' ')
                        text.append(' ');
                    try {
                        text.appendBase16(value, 2);
                    } catch (IllegalArgumentException | BufferOverflowException e) {
                        throw new AssertionError(e);
                    }
                } while (pos < end);
            }
        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        }
    }

    private void copyToText(long pos, long tpos, int length)
            throws IllegalStateException {
        try {
            if (tpos > 0 && text.readUnsignedByte(tpos) <= ' ')
                tpos++;
            while (length-- > 0) {
                int value = base.readUnsignedByte(pos++);
                text.writeUnsignedByte(tpos++, HEXADECIMAL[value >> 4]);
                text.writeUnsignedByte(tpos++, HEXADECIMAL[value & 0xF]);
                if (length > 0)
                    text.writeUnsignedByte(tpos++, ' ');
            }
        } catch (BufferUnderflowException | BufferOverflowException | IllegalArgumentException | ArithmeticException e) {
            throw new AssertionError(e);
        }
    }

    private void doIndent() {
        if (lineLength() == 0 && indent > 0) {
            for (int i = 0; i < indent; i++)
                text.append("   ");
            startOfLine = text.writePosition();
        }
    }

    @Override
    @NotNull
    public Bytes<Void> writeShort(short i16)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeShort(i16);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> writeInt(int i)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeInt(i);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> writeIntAdv(int i, int advance)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeIntAdv(i, advance);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> writeLong(long i64)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeLong(i64);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> writeLongAdv(long i64, int advance)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeLongAdv(i64, advance);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> writeFloat(float f)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeFloat(f);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> writeDouble(double d)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeDouble(d);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> writeDoubleAndInt(double d, int i)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeDouble(d);
            base.writeInt(i);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> write(@NotNull byte[] bytes, int offset, int length)
            throws BufferOverflowException, IllegalArgumentException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.write(bytes, offset, length);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> writeSome(@NotNull ByteBuffer buffer)
            throws BufferOverflowException, IllegalStateException, BufferUnderflowException {
        long pos = base.writePosition();
        try {
            base.writeSome(buffer);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> writeOrderedInt(int i)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeOrderedInt(i);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> writeOrderedLong(long i)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeOrderedLong(i);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> clearAndPad(long length)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.clearAndPad(length);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> prewrite(byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> prewrite(BytesStore bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> prewriteByte(byte b) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> prewriteShort(short i) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> prewriteInt(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> prewriteLong(long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte readByte(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return base.readByte(offset);
    }

    @Override
    public int peekUnsignedByte(long offset)
            throws IllegalStateException, BufferUnderflowException {
        return base.peekUnsignedByte(offset);
    }

    @Override
    public short readShort(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return base.readShort(offset);
    }

    @Override
    public int readInt(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return base.readInt(offset);
    }

    @Override
    public long readLong(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return base.readLong(offset);
    }

    @Override
    public float readFloat(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return base.readFloat(offset);
    }

    @Override
    public double readDouble(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return base.readDouble(offset);
    }

    @Override
    public byte readVolatileByte(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return base.readVolatileByte(offset);
    }

    @Override
    public short readVolatileShort(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return base.readVolatileShort(offset);
    }

    @Override
    public int readVolatileInt(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return base.readVolatileInt(offset);
    }

    @Override
    public long readVolatileLong(long offset)
            throws BufferUnderflowException, IllegalStateException {
        return base.readVolatileLong(offset);
    }

    @Override
    public void nativeRead(long position, long address, long size)
            throws BufferUnderflowException, IllegalStateException {
        base.nativeRead(position, address, size);
    }

    @Override
    public long readPosition() {
        return base.readPosition() | (text.readPosition() << 32);
    }

    @Override
    public void lenient(boolean lenient) {
        base.lenient(lenient);
    }

    @Override
    public boolean lenient() {
        return base.lenient();
    }

    @Override
    public void writeMarshallableLength16(@NotNull WriteBytesMarshallable marshallable)
            throws IllegalArgumentException, BufferOverflowException, BufferUnderflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeMarshallableLength16(marshallable);
        } finally {
            copyToText(pos);
        }
    }

    @Override
    public Bytes write(@NotNull InputStream inputStream)
            throws IOException, IllegalStateException, BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.write(inputStream);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeStopBit(long x)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeStopBit(x);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeStopBit(char x)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeStopBit(x);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeStopBit(double d)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeStopBit(d);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeStopBitDecimal(double d)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeStopBitDecimal(d);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeUtf8(@NotNull CharSequence text)
            throws BufferOverflowException, IllegalStateException, IllegalArgumentException {
        long pos = base.writePosition();
        try {
            base.writeUtf8(text);
            return this;

        } catch (BufferUnderflowException e) {
            throw new AssertionError(e);
        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeUtf8(@Nullable String text)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeUtf8(text);
            return this;

        } catch (BufferUnderflowException | IllegalArgumentException e) {
            throw new AssertionError(e);
        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Deprecated(/* for removal in x.23, appears to be removed from other Bytes implementations */)
    public Bytes<Void> writeUTFΔ(CharSequence cs)
            throws BufferOverflowException, IllegalStateException, IllegalArgumentException, BufferUnderflowException {
        long pos = base.writePosition();
        try {
            base.writeUtf8(cs);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write8bit(@NotNull CharSequence text)
            throws BufferOverflowException, IllegalStateException, BufferUnderflowException, ArithmeticException {
        long pos = base.writePosition();
        try {
            base.write8bit(text);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write8bit(@NotNull CharSequence text, int start, int length)
            throws BufferOverflowException, IndexOutOfBoundsException, IllegalStateException, BufferUnderflowException, ArithmeticException {
        long pos = base.writePosition();
        try {
            base.write8bit(text, start, length);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write8bit(@NotNull String text, int start, int length)
            throws BufferOverflowException, IndexOutOfBoundsException, IllegalStateException, BufferUnderflowException, ArithmeticException {
        long pos = base.writePosition();
        try {
            base.write8bit(text, start, length);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write(@NotNull CharSequence text)
            throws BufferOverflowException, IllegalStateException, IndexOutOfBoundsException {
        long pos = base.writePosition();
        try {
            base.write(text);
            return this;
        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write(@NotNull CharSequence text, int startText, int length)
            throws BufferOverflowException, IndexOutOfBoundsException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.write(text, startText, length);
            return this;
        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write8bit(@NotNull String s)
            throws BufferOverflowException, IllegalStateException, ArithmeticException {
        long pos = base.writePosition();
        try {
            base.write8bit(s);
            return this;
        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    public Bytes<Void> write8bit(@NotNull BytesStore bs)
            throws BufferOverflowException, IllegalStateException, BufferUnderflowException {
        long pos = base.writePosition();
        try {
            if (bs == null) {
                base.writeStopBit(-1);
            } else {
                long offset = bs.readPosition();
                long readRemaining = Math.min(base.writeRemaining(), bs.readLimit() - offset);
                base.writeStopBit(readRemaining);
                try {
                    base.write(bs, offset, readRemaining);
                } catch (BufferUnderflowException | IllegalArgumentException e) {
                    throw new AssertionError(e);
                }
            }
            return this;
        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeUnsignedByte(int i)
            throws BufferOverflowException, IllegalStateException, ArithmeticException {
        long pos = base.writePosition();
        try {
            base.writeUnsignedByte(i);
            return this;
        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeUnsignedShort(int u16)
            throws BufferOverflowException, IllegalStateException, ArithmeticException {
        long pos = base.writePosition();
        try {
            base.writeUnsignedShort(u16);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeInt24(int i)
            throws BufferOverflowException, IllegalStateException, ArithmeticException {
        long pos = base.writePosition();
        try {
            base.writeInt24(i);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeUnsignedInt24(int i)
            throws BufferOverflowException, IllegalStateException, ArithmeticException {
        long pos = base.writePosition();
        try {
            base.writeUnsignedInt24(i);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeUnsignedInt(long i)
            throws BufferOverflowException, ArithmeticException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeUnsignedInt(i);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write(@NotNull RandomDataInput bytes)
            throws IllegalStateException, BufferOverflowException {
        long pos = base.writePosition();
        try {

            base.write(bytes);
            return this;
        } finally {
            copyToText(pos);
        }
    }

    @Override
    public Bytes<Void> write(@NotNull BytesStore bytes)
            throws IllegalStateException, BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.write(bytes);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeSome(@NotNull Bytes bytes)
            throws IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeSome(bytes);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write(@NotNull RandomDataInput bytes, long offset, long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException, IllegalArgumentException {
        long pos = base.writePosition();
        try {
            base.write(bytes, offset, length);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write(@NotNull BytesStore bytes, long offset, long length)
            throws BufferOverflowException, BufferUnderflowException, IllegalStateException, IllegalArgumentException {
        long pos = base.writePosition();
        try {
            base.write(bytes, offset, length);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write(@NotNull byte[] byteArray)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.write(byteArray);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeBoolean(boolean flag)
            throws BufferOverflowException, IllegalStateException {
        long pos = base.writePosition();
        try {
            base.writeBoolean(flag);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    public <E extends Enum<E>> Bytes<Void> writeEnum(@NotNull E e)
            throws BufferOverflowException, IllegalStateException, ArithmeticException {
        long pos = base.writePosition();
        try {
            base.writeEnum(e);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    public void writePositionRemaining(long position, long length)
            throws BufferOverflowException {
        writePosition(position);
        writeLimit(base.writePosition() + length);
    }

    @Override
    public void writeHistogram(@NotNull Histogram histogram)
            throws IllegalStateException, BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.writeHistogram(histogram);
        } finally {
            copyToText(pos);
        }
    }

    @Override
    public void writeBigDecimal(@NotNull BigDecimal bd)
            throws IllegalArgumentException, IllegalStateException, BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.writeBigDecimal(bd);
        } finally {
            copyToText(pos);
        }
    }

    @Override
    public void writeBigInteger(@NotNull BigInteger bi)
            throws IllegalArgumentException, IllegalStateException, BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.writeBigInteger(bi);
        } finally {
            copyToText(pos);
        }
    }

    @Override
    public void writeWithLength(@NotNull RandomDataInput bytes)
            throws IllegalStateException, BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.writeWithLength(bytes);
        } finally {
            copyToText(pos);
        }
    }

    private static class TextBytesReader extends Reader {
        private final Reader reader;
        private final Bytes base;

        public TextBytesReader(Reader reader, Bytes base) {
            this.reader = reader;
            this.base = base;
        }

        @Override
        public int read(@NotNull char[] cbuf, int off, int len)
                throws IOException {
            int len2 = reader.read(cbuf, off, len);
            base.append(new String(cbuf, off, len)); // TODO Optimise
            return len2;
        }

        @Override
        public void close()
                throws IOException {
            reader.close();
        }
    }
}

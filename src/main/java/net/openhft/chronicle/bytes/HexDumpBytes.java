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

@SuppressWarnings({"rawtypes", "unchecked"})
public class HexDumpBytes
        implements Bytes<Void> {

    private static final char[] HEXADECIMAL = "0123456789abcdef".toCharArray();
    private static final Pattern HEX_PATTERN = Pattern.compile("[0-9a-fA-F]{1,2}");

    private final NativeBytes<Void> base = Bytes.allocateElasticDirect(256);
    private final Bytes<byte[]> text = Bytes.allocateElasticOnHeap(1024);
    private final Bytes<byte[]> comment = Bytes.allocateElasticOnHeap(64);
    private OffsetFormat offsetFormat = null;
    private long startOfLine = 0;
    private int indent = 0;
    private int numberWrap = 16;

    public HexDumpBytes() {
    }

    HexDumpBytes(BytesStore base, Bytes text) {
        try {
            this.base.write(base);
            this.text.write(text);
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }

    public static HexDumpBytes fromText(Reader reader) {
        HexDumpBytes tb = new HexDumpBytes();
        Reader reader2 = new TextBytesReader(reader, tb.text);
        try (Scanner sc = new Scanner(reader2)) {
            while (sc.hasNext()) {
                if (sc.hasNext(HEX_PATTERN))
                    tb.base.rawWriteByte((byte) Integer.parseInt(sc.next(), 16));
                else
                    sc.nextLine(); // assume it's a comment
            }
        } catch (BufferOverflowException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
        return tb;
    }

    public static HexDumpBytes fromText(CharSequence text) {
        return fromText(new StringReader(text.toString()));
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
        if (lineLength() > 0)
            newLine();
        return text.toString();
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
    public Bytes<Void> comment(CharSequence comment) {
        if (this.comment.readRemaining() > 0)
            newLine();
        if (comment.length() > 0 && comment.charAt(0) == '#') {
            indent = 0;
            this.text.append('#').append(comment).append('\n');
            startOfLine = this.text.writePosition();
        } else {
            this.comment.clear().append(comment);
        }
        return this;
    }

    @Override
    public BytesOut indent(int n) {
        indent += n;
        if (lineLength() > 0) {
            newLine();
        }
        return this;
    }

    private long lineLength() {
        return this.text.writePosition() - startOfLine;
    }

    private void newLine() {
        if (this.comment.readRemaining() > 0) {
            while (lineLength() < numberWrap * 3 - 3)
                this.text.append("   ");
            while (lineLength() < numberWrap * 3)
                this.text.append(' ');
            this.text.append("# ");
            this.text.append(comment);
            comment.clear();
        }
        this.text.append('\n');
        startOfLine = this.text.writePosition();
    }

    private void appendOffset(long offset) {
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
    public void ensureCapacity(long size) throws IllegalArgumentException {
        base.ensureCapacity(size);
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
    public Bytes clear() {
        return base.clear();
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
    public long addressForRead(long offset) throws UnsupportedOperationException {
        return base.addressForRead(offset);
    }

    @Override
    public long addressForWrite(long offset) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public long addressForWritePosition() throws UnsupportedOperationException, BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value) throws BufferOverflowException {
        if (base.compareAndSwapInt(offset & 0xFFFFFFFFL, expected, value)) {
            copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 4);
            return true;
        }
        return false;
    }

    @Override
    public void testAndSetInt(long offset, int expected, int value) {
        long off = offset & 0xFFFFFFFFL;
        base.testAndSetInt(off, expected, value);
        copyToText(off, offset >>> 32, 4);
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) throws BufferOverflowException {
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
    public void reserve(ReferenceOwner owner) throws IllegalStateException {
        base.reserve(owner);
    }

    @Override
    public void release(ReferenceOwner owner) throws IllegalStateException {
        base.release(owner);
        if (base.refCount() == 0) {
            text.releaseLast();
            comment.releaseLast();
        }
    }

    @Override
    public void releaseLast(ReferenceOwner owner) throws IllegalStateException {
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
    public boolean tryReserve(ReferenceOwner owner) {
        return base.tryReserve(owner);
    }

    @Override
    public boolean reservedBy(ReferenceOwner owner) {
        return base.reservedBy(owner);
    }

    @Override
    @NotNull
    public Bytes<Void> writeByte(long offset, byte i8) throws BufferOverflowException {
        base.writeByte(offset & 0xFFFFFFFFL, i8);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 1);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeShort(long offset, short i) throws BufferOverflowException {
        base.writeShort(offset & 0xFFFFFFFFL, i);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 2);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeInt24(long offset, int i) throws BufferOverflowException {
        base.writeInt24(offset & 0xFFFFFFFFL, i);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 3);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeInt(long offset, int i) throws BufferOverflowException {
        return writeOrderedInt(offset, i);
    }

    @Override
    @NotNull
    public Bytes<Void> writeOrderedInt(long offset, int i) throws BufferOverflowException {
        base.writeOrderedInt(offset & 0xFFFFFFFFL, i);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 4);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeLong(long offset, long i) throws BufferOverflowException {
        return writeOrderedLong(offset, i);
    }

    @Override
    @NotNull
    public Bytes<Void> writeOrderedLong(long offset, long i) throws BufferOverflowException {
        base.writeOrderedLong(offset & 0xFFFFFFFFL, i);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 8);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeFloat(long offset, float d) throws BufferOverflowException {
        base.writeFloat(offset & 0xFFFFFFFFL, d);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 4);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeDouble(long offset, double d) throws BufferOverflowException {
        base.writeDouble(offset & 0xFFFFFFFFL, d);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 8);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeVolatileByte(long offset, byte i8) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> writeVolatileShort(long offset, short i16) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> writeVolatileInt(long offset, int i32) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> writeVolatileLong(long offset, long i64) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> write(long offsetInRDO, byte[] bytes, int offset, int length) throws BufferOverflowException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(long offsetInRDO, ByteBuffer bytes, int offset, int length) throws BufferOverflowException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> write(long writeOffset, RandomDataInput bytes, long readOffset, long length) throws BufferOverflowException, IllegalArgumentException, BufferUnderflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void nativeWrite(long address, long position, long size) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<Void> readPosition(long position) throws BufferUnderflowException {
        base.readPosition(position);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> readLimit(long limit) throws BufferUnderflowException {
        base.readLimit(limit);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> readSkip(long bytesToSkip) throws BufferUnderflowException {
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
    public long readStopBit() throws IORuntimeException {
        return base.readStopBit();
    }

    @Override
    public char readStopBitChar() throws IORuntimeException {
        return base.readStopBitChar();
    }

    @Override
    public double readStopBitDouble() {
        return base.readStopBitDouble();
    }

    @Override
    public double readStopBitDecimal() throws BufferOverflowException {
        return base.readStopBitDecimal();
    }

    @Override
    public byte readByte() {
        return base.readByte();
    }

    @Override
    public int readUnsignedByte() {
        return base.readUnsignedByte();
    }

    @Override
    public int uncheckedReadUnsignedByte() {
        return base.uncheckedReadUnsignedByte();
    }

    @Override
    public short readShort() throws BufferUnderflowException {
        return base.readShort();
    }

    @Override
    public int readInt() throws BufferUnderflowException {
        return base.readInt();
    }

    @Override
    public long readLong() throws BufferUnderflowException {
        return base.readLong();
    }

    @Override
    public float readFloat() throws BufferUnderflowException {
        return base.readFloat();
    }

    @Override
    public double readDouble() throws BufferUnderflowException {
        return base.readDouble();
    }

    @Override
    public int readVolatileInt() throws BufferUnderflowException {
        return base.readVolatileInt();
    }

    @Override
    public long readVolatileLong() throws BufferUnderflowException {
        return base.readVolatileLong();
    }

    @Override
    public int peekUnsignedByte() {
        return base.peekUnsignedByte();
    }

    @Override
    public void nativeRead(long address, long size) throws BufferUnderflowException {
        base.nativeRead(address, size);
    }

    @Override
    public int lastDecimalPlaces() {
        return base.lastDecimalPlaces();
    }

    @Override
    public void lastDecimalPlaces(int lastDecimalPlaces) {
        base.lastDecimalPlaces(lastDecimalPlaces);
    }

    @NotNull
    @Override
    public BigDecimal readBigDecimal() {
        return base.readBigDecimal();
    }

    @NotNull
    @Override
    public BigInteger readBigInteger() {
        return base.readBigInteger();
    }

    @Override
    public void readWithLength(long length, @NotNull BytesOut<Void> bytesOut) throws BufferUnderflowException, IORuntimeException {
        base.readWithLength(length, bytesOut);
    }

    @Override
    public <T extends ReadBytesMarshallable> T readMarshallableLength16(Class<T> tClass, T object) {
        return base.readMarshallableLength16(tClass, object);
    }

    @NotNull
    @Override
    public Bytes<Void> readPositionUnlimited(long position) throws BufferUnderflowException {
        return base.readPositionUnlimited(position);

    }

    @NotNull
    @Override
    public Bytes<Void> readPositionRemaining(long position, long remaining) throws BufferUnderflowException {
        return base.readPositionRemaining(position, remaining);

    }

    @Override
    public void readWithLength0(long length, @NotNull ThrowingConsumerNonCapturing<Bytes<Void>, IORuntimeException, BytesOut> bytesConsumer, StringBuilder sb, BytesOut toBytes) throws BufferUnderflowException, IORuntimeException {
        base.readWithLength0(length, bytesConsumer, sb, toBytes);

    }

    @Override
    public void readWithLength(long length, @NotNull ThrowingConsumer<Bytes<Void>, IORuntimeException> bytesConsumer) throws BufferUnderflowException, IORuntimeException {
        base.readWithLength(length, bytesConsumer);

    }

    @Override
    public boolean readBoolean() {
        return base.readBoolean();

    }

    @Override
    public int readUnsignedShort() throws BufferUnderflowException {
        return base.readUnsignedShort();

    }

    @Override
    public int readInt24() throws BufferUnderflowException {
        return base.readInt24();

    }

    @Override
    public int readUnsignedInt24() throws BufferUnderflowException {
        return base.readUnsignedInt24();

    }

    @Override
    public long readUnsignedInt() throws BufferUnderflowException {
        return base.readUnsignedInt();

    }

    @Nullable
    @Override
    public String readUtf8() throws BufferUnderflowException, IORuntimeException, IllegalArgumentException {
        return base.readUtf8();

    }

    @Nullable
    @Override
    public String readUTFΔ() throws IORuntimeException, BufferUnderflowException, IllegalArgumentException {
        return base.readUTFΔ();

    }

    @Nullable
    @Override
    public String read8bit() throws IORuntimeException, BufferUnderflowException {
        return base.read8bit();

    }

    @Override
    public <ACS extends Appendable & CharSequence> boolean readUtf8(@NotNull ACS sb) throws IORuntimeException, IllegalArgumentException, BufferUnderflowException {
        return base.readUtf8(sb);

    }

    @Override
    public <ACS extends Appendable & CharSequence> boolean readUTFΔ(@NotNull ACS sb) throws IORuntimeException, IllegalArgumentException, BufferUnderflowException {
        return base.readUTFΔ(sb);

    }

    @Override
    public boolean read8bit(@NotNull Bytes b) throws BufferUnderflowException, IllegalStateException, BufferOverflowException {
        return base.read8bit(b);

    }

    @Override
    public <ACS extends Appendable & CharSequence> boolean read8bit(@NotNull ACS sb) throws IORuntimeException, IllegalArgumentException, BufferUnderflowException {
        return base.read8bit(sb);

    }

    @Override
    public boolean read8bit(@NotNull StringBuilder sb) throws IORuntimeException, BufferUnderflowException {
        return base.read8bit(sb);

    }

    @Override
    public int read(@NotNull byte[] bytes) {
        return base.read(bytes);

    }

    @Override
    public int read(@NotNull byte[] bytes, int off, int len) {
        return base.read(bytes, off, len);

    }

    @Override
    public int read(@NotNull char[] bytes, int off, int len) {
        return base.read(bytes, off, len);

    }

    @Override
    public void read(@NotNull ByteBuffer buffer) {
        base.read(buffer);

    }

    @Override
    public void read(@NotNull Bytes bytes, int length) {
        base.read(bytes, length);

    }

    @NotNull
    @Override
    public <E extends Enum<E>> E readEnum(@NotNull Class<E> eClass) throws IORuntimeException, BufferUnderflowException {
        return base.readEnum(eClass);

    }

    @Override
    public void readHistogram(@NotNull Histogram histogram) {
        base.readHistogram(histogram);

    }

    @Override
    public void readWithLength(Bytes bytes) {
        base.readWithLength(bytes);

    }

    @Override
    @NotNull
    public Bytes<Void> writePosition(long position) throws BufferOverflowException {
        base.writePosition(position & 0xFFFFFFFFL);
        text.writePosition(position >>> 32);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeLimit(long limit) throws BufferOverflowException {
        base.writeLimit(limit);
        return this;
    }

    @Override
    @NotNull
    public Bytes<Void> writeSkip(long bytesToSkip) throws BufferOverflowException {
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
    public Bytes<Void> writeByte(byte i8) throws BufferOverflowException {
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

    private void copyToText(long pos) {
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
                if (ll >= numberWrap * 3 - 1) {
                    newLine();
                    appendOffset(pos);
                    doIndent();
                    startOfLine = text.writePosition();
                }
                pos++;
                long wp = text.writePosition();
                if (wp > 0 && text.peekUnsignedByte(wp - 1) > ' ')
                    text.append(' ');
                text.appendBase16(value, 2);
            } while (pos < end);
        }
    }

    private void copyToText(long pos, long tpos, int length) {
        if (tpos > 0 && text.readUnsignedByte(tpos) <= ' ')
            tpos++;
        while (length-- > 0) {
            int value = base.readUnsignedByte(pos++);
            text.writeUnsignedByte(tpos++, HEXADECIMAL[value >> 4]);
            text.writeUnsignedByte(tpos++, HEXADECIMAL[value & 0xF]);
            if (length > 0)
                text.writeUnsignedByte(tpos++, ' ');
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
    public Bytes<Void> writeShort(short i16) throws BufferOverflowException {
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
    public Bytes<Void> writeInt(int i) throws BufferOverflowException {
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
    public Bytes<Void> writeIntAdv(int i, int advance) throws BufferOverflowException {
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
    public Bytes<Void> writeLong(long i64) throws BufferOverflowException {
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
    public Bytes<Void> writeLongAdv(long i64, int advance) throws BufferOverflowException {
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
    public Bytes<Void> writeFloat(float f) throws BufferOverflowException {
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
    public Bytes<Void> writeDouble(double d) throws BufferOverflowException {
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
    public Bytes<Void> writeDoubleAndInt(double d, int i) throws BufferOverflowException {
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
    public long realWriteRemaining() {
        return base.realWriteRemaining();
    }

    @Override
    @NotNull
    public Bytes<Void> write(byte[] bytes, int offset, int length) throws BufferOverflowException, IllegalArgumentException {
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
    public Bytes<Void> writeSome(ByteBuffer buffer) throws BufferOverflowException {
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
    public Bytes<Void> writeOrderedInt(int i) throws BufferOverflowException {
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
    public Bytes<Void> writeOrderedLong(long i) throws BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.writeOrderedLong(i);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    public void nativeWrite(long address, long size) throws BufferOverflowException {
        long pos = base.writePosition();
        try {
            nativeWrite(address, size);

        } finally {
            copyToText(pos);
        }
    }

    @Override
    @NotNull
    public Bytes<Void> clearAndPad(long length) throws BufferOverflowException {
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
    public byte readByte(long offset) throws BufferUnderflowException {
        return base.readByte(offset);
    }

    @Override
    public int peekUnsignedByte(long offset) {
        return base.peekUnsignedByte(offset);
    }

    @Override
    public short readShort(long offset) throws BufferUnderflowException {
        return base.readShort(offset);
    }

    @Override
    public int readInt(long offset) throws BufferUnderflowException {
        return base.readInt(offset);
    }

    @Override
    public long readLong(long offset) throws BufferUnderflowException {
        return base.readLong(offset);
    }

    @Override
    public float readFloat(long offset) throws BufferUnderflowException {
        return base.readFloat(offset);
    }

    @Override
    public double readDouble(long offset) throws BufferUnderflowException {
        return base.readDouble(offset);
    }

    @Override
    public byte readVolatileByte(long offset) throws BufferUnderflowException {
        return base.readVolatileByte(offset);
    }

    @Override
    public short readVolatileShort(long offset) throws BufferUnderflowException {
        return base.readVolatileShort(offset);
    }

    @Override
    public int readVolatileInt(long offset) throws BufferUnderflowException {
        return base.readVolatileInt(offset);
    }

    @Override
    public long readVolatileLong(long offset) throws BufferUnderflowException {
        return base.readVolatileLong(offset);
    }

    @Override
    public void nativeRead(long position, long address, long size) throws BufferUnderflowException {
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
    public void writeMarshallableLength16(WriteBytesMarshallable marshallable) {
        long pos = base.writePosition();
        try {
            base.writeMarshallableLength16(marshallable);
        } finally {
            copyToText(pos);
        }
    }

    @Override
    public Bytes write(InputStream inputStream) throws IOException {
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
    public Bytes<Void> writeStopBit(long x) throws BufferOverflowException {
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
    public Bytes<Void> writeStopBit(char x) throws BufferOverflowException {
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
    public Bytes<Void> writeStopBit(double d) throws BufferOverflowException {
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
    public Bytes<Void> writeStopBitDecimal(double d) throws BufferOverflowException {
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
    public Bytes<Void> writeUtf8(CharSequence cs) throws BufferOverflowException {
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
    public Bytes<Void> writeUtf8(String s) throws BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.writeUtf8(s);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeUTFΔ(CharSequence cs) throws BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.writeUTFΔ(cs);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write8bit(@Nullable CharSequence cs) throws BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.write8bit(cs);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write8bit(@NotNull CharSequence s, int start, int length) throws BufferOverflowException, IllegalArgumentException, IndexOutOfBoundsException {
        long pos = base.writePosition();
        try {
            base.write8bit(s, start, length);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write(CharSequence cs) throws BufferOverflowException, BufferUnderflowException, IllegalArgumentException {
        long pos = base.writePosition();
        try {
            base.write(cs);
            return this;
        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write(@NotNull CharSequence s, int start, int length) throws BufferOverflowException, IllegalArgumentException, IndexOutOfBoundsException {
        long pos = base.writePosition();
        try {
            base.write(s, start, length);
            return this;
        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write8bit(@Nullable String s) throws BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.write8bit(s);
            return this;
        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> write8bit(@Nullable BytesStore bs) throws BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.write8bit(bs);
            return this;
        } finally {
            copyToText(pos);
        }
    }

    @NotNull
    @Override
    public Bytes<Void> writeUnsignedByte(int i) throws BufferOverflowException, IllegalArgumentException {
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
    public Bytes<Void> writeUnsignedShort(int u16) throws BufferOverflowException, IllegalArgumentException {
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
    public Bytes<Void> writeInt24(int i) throws BufferOverflowException {
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
    public Bytes<Void> writeUnsignedInt24(int i) throws BufferOverflowException {
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
    public Bytes<Void> writeUnsignedInt(long i) throws BufferOverflowException, IllegalArgumentException {
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
    public Bytes<Void> write(@NotNull RandomDataInput bytes) {
        long pos = base.writePosition();
        try {

            base.write(bytes);
            return this;
        } finally {
            copyToText(pos);
        }
    }

    @Override
    public Bytes<Void> write(@NotNull BytesStore bytes) {
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
    public Bytes<Void> writeSome(@NotNull Bytes bytes) {
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
    public Bytes<Void> write(@NotNull RandomDataInput bytes, long offset, long length) throws BufferOverflowException, BufferUnderflowException {
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
    public Bytes<Void> write(@NotNull BytesStore bytes, long offset, long length) throws BufferOverflowException, BufferUnderflowException {
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
    public Bytes<Void> write(@NotNull byte[] bytes) throws BufferOverflowException {
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
    public Bytes<Void> writeBoolean(boolean flag) throws BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.writeBoolean(flag);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    public <E extends Enum<E>> Bytes<Void> writeEnum(@NotNull E e) throws BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.writeEnum(e);
            return this;

        } finally {
            copyToText(pos);
        }
    }

    @Override
    public void writePositionRemaining(long position, long length) {
        writePosition(position);
        writeLimit(base.writePosition + length);
    }

    @Override
    public void writeHistogram(@NotNull Histogram histogram) {
        long pos = base.writePosition();
        try {
            base.writeHistogram(histogram);
        } finally {
            copyToText(pos);
        }
    }

    @Override
    public void writeBigDecimal(@NotNull BigDecimal bd) {
        long pos = base.writePosition();
        try {
            base.writeBigDecimal(bd);
        } finally {
            copyToText(pos);
        }
    }

    @Override
    public void writeBigInteger(@NotNull BigInteger bi) {
        long pos = base.writePosition();
        try {
            base.writeBigInteger(bi);
        } finally {
            copyToText(pos);
        }
    }

    @Override
    public void writeWithLength(RandomDataInput bytes) {
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
        public int read(@NotNull char[] cbuf, int off, int len) throws IOException {
            int len2 = reader.read(cbuf, off, len);
            base.append(new String(cbuf, off, len)); // TODO Optimise
            return len2;
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}

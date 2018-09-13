package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.regex.Pattern;

public class HexDumpBytes implements Bytes<ByteBuffer> {

    private static final char[] HEXADECIMAL = "0123456789abcdef".toCharArray();
    private static final Pattern HEX_PATTERN = Pattern.compile("[0-9a-fA-F]{1,2}");
    private static final Field LONG_VALUE = Jvm.getField(Long.class, "value");

    private final Bytes<ByteBuffer> base = Bytes.elasticHeapByteBuffer(128);
    private final Bytes<ByteBuffer> text = Bytes.elasticHeapByteBuffer(128);
    private final Bytes<ByteBuffer> comment = Bytes.elasticHeapByteBuffer(64);
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
                    tb.base.writeUnsignedByte(Integer.parseInt(sc.next(), 16));
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
    public boolean retainsComments() {
        return true;
    }

    @Override
    public Bytes<ByteBuffer> comment(CharSequence comment) {
        if (this.comment.readRemaining() > 0 || comment.length() == 0)
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
    public ByteBuffer underlyingObject() {
        return base.underlyingObject();
    }

    @Override
    public void move(long from, long to, long length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reserve() throws IllegalStateException {
        base.reserve();
        text.reserve();
    }

    @Override
    public void release() throws IllegalStateException {
        base.release();
        text.release();
    }

    @Override
    public long refCount() {
        return base.refCount();
    }

    @Override
    public boolean tryReserve() {
        text.tryReserve();
        return base.tryReserve();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeByte(long offset, byte i8) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeShort(long offset, short i) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeInt24(long offset, int i) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeInt(long offset, int i) throws BufferOverflowException {
        return writeOrderedInt(offset, i);
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeOrderedInt(long offset, int i) throws BufferOverflowException {
        base.writeOrderedInt(offset & 0xFFFFFFFFL, i);
        copyToText(offset & 0xFFFFFFFFL, offset >>> 32, 4);
        return this;
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeLong(long offset, long i) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeOrderedLong(long offset, long i) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeFloat(long offset, float d) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeDouble(long offset, double d) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeVolatileByte(long offset, byte i8) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeVolatileShort(long offset, short i16) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeVolatileInt(long offset, int i32) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeVolatileLong(long offset, long i64) throws BufferOverflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> write(long offsetInRDO, byte[] bytes, int offset, int length) throws BufferOverflowException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(long offsetInRDO, ByteBuffer bytes, int offset, int length) throws BufferOverflowException, IllegalArgumentException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> write(long writeOffset, RandomDataInput bytes, long readOffset, long length) throws BufferOverflowException, IllegalArgumentException, BufferUnderflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void nativeWrite(long address, long position, long size) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> readPosition(long position) throws BufferUnderflowException {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> readLimit(long limit) throws BufferUnderflowException {
        base.readLimit(limit);
        return this;
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> readSkip(long bytesToSkip) throws BufferUnderflowException {
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

    @Override
    @NotNull
    public Bytes<ByteBuffer> writePosition(long position) throws BufferOverflowException {
        base.writePosition(position);
        return this;
    }

    @Override
    @NotNull
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> writeLimit(long limit) throws BufferOverflowException {
        base.writeLimit(limit);
        return this;
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> writeSkip(long bytesToSkip) throws BufferOverflowException {
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
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> writeByte(byte i8) throws BufferOverflowException {
        long pos = base.writePosition();
        try {
            base.writeByte(i8);
            return this;
        } finally {
            copyToText(pos);
        }
    }

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
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> writeShort(short i16) throws BufferOverflowException {
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
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> writeInt(int i) throws BufferOverflowException {
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
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> writeIntAdv(int i, int advance) throws BufferOverflowException {
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
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> writeLong(long i64) throws BufferOverflowException {
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
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> writeLongAdv(long i64, int advance) throws BufferOverflowException {
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
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> writeFloat(float f) throws BufferOverflowException {
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
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> writeDouble(double d) throws BufferOverflowException {
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
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> writeDoubleAndInt(double d, int i) throws BufferOverflowException {
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
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> write(byte[] bytes, int offset, int length) throws BufferOverflowException, IllegalArgumentException {
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
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> writeSome(ByteBuffer buffer) throws BufferOverflowException {
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
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> writeOrderedInt(int i) throws BufferOverflowException {
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
    @net.openhft.chronicle.core.annotation.NotNull
    public Bytes<ByteBuffer> writeOrderedLong(long i) throws BufferOverflowException {
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
    public Bytes<ByteBuffer> clearAndPad(long length) throws BufferOverflowException {
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
    public Bytes<ByteBuffer> prewrite(byte[] bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> prewrite(BytesStore bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> prewriteByte(byte b) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> prewriteShort(short i) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> prewriteInt(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Bytes<ByteBuffer> prewriteLong(long l) {
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

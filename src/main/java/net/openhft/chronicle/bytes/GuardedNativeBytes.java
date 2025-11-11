/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.BinaryWireCode.*;

/**
 * Debugging wrapper that prefixes each primitive written with a type code and checks it on reads.
 * Useful for catching mismatched read/write pairs but adds considerable overhead and should not be
 * used in production.
 */
public class GuardedNativeBytes<U> extends NativeBytes<U> {
    /** type marker for a single byte */
    static final byte BYTE_T = (byte) INT8;
    /** type marker for a short */
    static final byte SHORT_T = (byte) INT16;
    /** type marker for an int */
    static final byte INT_T = (byte) INT32;
    /** type marker for a long */
    static final byte LONG_T = (byte) INT64;
    /** type marker for stop-bit encoded value */
    static final byte STOP_T = (byte) STOP_BIT;
    /** type marker for a float */
    static final byte FLOAT_T = (byte) FLOAT32;
    /** type marker for a double */
    static final byte DOUBLE_T = (byte) FLOAT64;

    private static final String[] STRING_FOR_CODE = _stringForCode(GuardedNativeBytes.class);

    /**
     * Constructs a new GuardedNativeBytes instance backed by the specified BytesStore and with the specified capacity.
     *
     * @param store    The backing BytesStore.
     * @param capacity The capacity of the new GuardedNativeBytes instance.
     * @throws IllegalArgumentException If the capacity is negative or exceeds the limit of the backing store.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public GuardedNativeBytes(@NotNull BytesStore<?, ?> store, long capacity)
            throws IllegalStateException, IllegalArgumentException {
        super(store, capacity);
    }

    @Override
    public BytesOut<U> writeHexDumpDescription(CharSequence comment) throws IllegalStateException {
        if (bytesStore instanceof HexDumpBytesDescription)
            ((HexDumpBytesDescription<?>) bytesStore).writeHexDumpDescription(comment);
        return this;
    }

    @Override
    protected void bytesStore(@NotNull BytesStore<?, U> bytesStore) {
        if (capacity() < bytesStore.capacity())
            capacity = bytesStore.capacity();
        this.bytesStore = bytesStore;
    }

    @NotNull
    @Override
    public Bytes<U> writeByte(byte i8)
            throws BufferOverflowException, IllegalStateException {
        super.writeByte(BYTE_T);
        return super.writeByte(i8);
    }

    @Override
    public Bytes<U> rawWriteByte(byte i8)
            throws BufferOverflowException, IllegalStateException {
        return super.writeByte(i8);
    }

    @Override
    public Bytes<U> rawWriteInt(int i)
            throws BufferOverflowException, IllegalStateException {
        return super.writeInt(i);
    }

    @Override
    public byte readByte() throws IllegalStateException {
        expectByte(BYTE_T);
        return super.readByte();
    }

    @Override
    public byte rawReadByte() throws IllegalStateException {
        return super.readByte();
    }

    @Override
    public int rawReadInt() throws IllegalStateException, BufferUnderflowException {
        return super.readInt();
    }

    @Override
    public int readUnsignedByte() throws IllegalStateException {
        expectByte(BYTE_T);
        return super.readUnsignedByte();
    }

    @NotNull
    @Override
    public Bytes<U> writeShort(short i16)
            throws BufferOverflowException, IllegalStateException {
        super.writeByte(SHORT_T);
        return super.writeShort(i16);
    }

    @Override
    public short readShort()
            throws BufferUnderflowException, IllegalStateException {
        expectByte(SHORT_T);
        return super.readShort();
    }

    @NotNull
    @Override
    public Bytes<U> writeStopBit(char x)
            throws BufferOverflowException, IllegalStateException {
        super.writeByte(STOP_T);
        return super.writeStopBit(x);
    }

    @NotNull
    @Override
    public Bytes<U> writeStopBit(long x)
            throws BufferOverflowException, IllegalStateException {
        super.writeByte(STOP_T);
        return super.writeStopBit(x);
    }

    @Override
    public long readStopBit()
            throws IORuntimeException, IllegalStateException, BufferUnderflowException {
        expectByte(STOP_T, SHORT_T);
        return super.readStopBit();
    }

    @Override
    public char readStopBitChar()
            throws IORuntimeException, IllegalStateException, BufferUnderflowException {
        expectByte(STOP_T);
        return super.readStopBitChar();
    }

    @NotNull
    @Override
    public Bytes<U> writeInt(int i)
            throws BufferOverflowException, IllegalStateException {
        super.writeByte(INT_T);
        return super.writeInt(i);
    }

    @Override
    public int readInt()
            throws BufferUnderflowException, IllegalStateException {
        expectByte(INT_T);
        return super.readInt();
    }

    @NotNull
    @Override
    public Bytes<U> writeLong(long i64)
            throws BufferOverflowException, IllegalStateException {
        super.writeByte(LONG_T);
        return super.writeLong(i64);
    }

    @Override
    public long readLong()
            throws BufferUnderflowException, IllegalStateException {
        expectByte(LONG_T);
        return super.readLong();
    }

    @NotNull
    @Override
    public Bytes<U> writeFloat(float f)
            throws BufferOverflowException, IllegalStateException {
        super.writeByte(FLOAT_T);
        return super.writeFloat(f);
    }

    @Override
    public float readFloat()
            throws BufferUnderflowException, IllegalStateException {
        expectByte(FLOAT_T);
        return super.readFloat();
    }

    @NotNull
    @Override
    public Bytes<U> writeDouble(double d)
            throws BufferOverflowException, IllegalStateException {
        super.writeByte(DOUBLE_T);
        return super.writeDouble(d);
    }

    @Override
    public double readDouble()
            throws BufferUnderflowException, IllegalStateException {
        expectByte(DOUBLE_T);
        return super.readDouble();
    }

    /**
     * Verifies the next type marker matches {@code expected}.
     */
    private void expectByte(byte expected) throws IllegalStateException {
        byte type = super.readByte();
        if (type != expected)
            throw new IllegalStateException("Expected " + STRING_FOR_CODE[expected & 0xFF]
                    + " but was " + STRING_FOR_CODE[type & 0xFF]);
    }

    /**
     * Verifies the next marker matches either {@code expected} or {@code expected2}.
     */
    private void expectByte(byte expected, byte expected2) throws IllegalStateException {
        byte type = super.readByte();
        if (type != expected && type != expected2)
            throw new IllegalStateException("Expected " + STRING_FOR_CODE[expected & 0xFF]
                    + " but was " + STRING_FOR_CODE[type & 0xFF]);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull String toHexString() {
        if (bytesStore instanceof Bytes)
            return ((Bytes<U>) bytesStore).toHexString();
        return super.toHexString();
    }
}

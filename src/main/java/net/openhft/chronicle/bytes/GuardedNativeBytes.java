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

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.BinaryWireCode.*;

/**
 * <p>
 * This class extends the {@link NativeBytes} class and provides an additional layer of safety by tracking the raw primitives written to the byte buffer.
 * <p>
 * The purpose of this class is to facilitate the detection of inconsistencies during testing. When inconsistencies are detected, they can be corrected before the code is moved into production.
 * <p>
 * GuardedNativeBytes records the type of each primitive written into the bytes buffer (byte, short, int, long, float, double). This tracking enables validation of the consistency of data written and read, which is critical for data integrity.
 * <p>
 * Please note that while this class is very useful for ensuring data consistency during testing, it may introduce a performance overhead and thus not recommended to be used in a production environment.
 *
 * @param <U> The type of the object that this byte buffer is bound to.
 */
public class GuardedNativeBytes<U> extends NativeBytes<U> {
    static final byte BYTE_T = (byte) INT8;
    static final byte SHORT_T = (byte) INT16;
    static final byte INT_T = (byte) INT32;
    static final byte LONG_T = (byte) INT64;
    static final byte STOP_T = (byte) STOP_BIT;
    static final byte FLOAT_T = (byte) FLOAT32;
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
            ((HexDumpBytesDescription) bytesStore).writeHexDumpDescription(comment);
        return this;
    }

    @Override
    protected void bytesStore(@NotNull BytesStore<Bytes<U>, U> bytesStore) {
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

    private void expectByte(byte expected) throws IllegalStateException {
        byte type = super.readByte();
        if (type != expected)
            throw new IllegalStateException("Expected " + STRING_FOR_CODE[expected & 0xFF]
                    + " but was " + STRING_FOR_CODE[type & 0xFF]);
    }

    private void expectByte(byte expected, byte expected2) throws IllegalStateException {
        byte type = super.readByte();
        if (type != expected && type != expected2)
            throw new IllegalStateException("Expected " + STRING_FOR_CODE[expected & 0xFF]
                    + " but was " + STRING_FOR_CODE[type & 0xFF]);
    }

    @Override
    public @NotNull String toHexString() {
        if (bytesStore instanceof Bytes)
            return ((Bytes<U>) bytesStore).toHexString();
        return super.toHexString();
    }
}

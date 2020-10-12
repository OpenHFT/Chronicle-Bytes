/*
 * Copyright 2016-2020 Chronicle Software
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
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.BinaryWireCode.*;

public class GuardedNativeBytes<Underlying> extends NativeBytes<Underlying> {
    static final byte BYTE_T = (byte) INT8;
    static final byte SHORT_T = (byte) INT16;
    static final byte INT_T = (byte) INT32;
    static final byte LONG_T = (byte) INT64;
    static final byte STOP_T = (byte) STOP_BIT;
    static final byte FLOAT_T = (byte) FLOAT32;
    static final byte DOUBLE_T = (byte) FLOAT64;

    private static final String[] STRING_FOR_CODE = _stringForCode(GuardedNativeBytes.class);

    public GuardedNativeBytes(@NotNull BytesStore store, long capacity) throws IllegalStateException {
        super(store, capacity);
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeByte(byte i8) throws BufferOverflowException {
        super.writeByte(BYTE_T);
        return super.writeByte(i8);
    }

    @Override
    public Bytes<Underlying> rawWriteByte(byte i8) throws BufferOverflowException {
        return super.writeByte(i8);
    }

    @Override
    public Bytes<Underlying> rawWriteInt(int i) throws BufferOverflowException {
        return super.writeInt(i);
    }

    @Override
    public byte readByte() {
        expectByte(BYTE_T);
        return super.readByte();
    }

    @Override
    public byte rawReadByte() {
        return super.readByte();
    }

    @Override
    public int rawReadInt() {
        return super.readInt();
    }

    @Override
    public int readUnsignedByte() {
        expectByte(BYTE_T);
        return super.readUnsignedByte();
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeShort(short i16) throws BufferOverflowException {
        super.writeByte(SHORT_T);
        return super.writeShort(i16);
    }

    @Override
    public short readShort() throws BufferUnderflowException {
        expectByte(SHORT_T);
        return super.readShort();
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeStopBit(char x) throws BufferOverflowException {
        super.writeByte(STOP_T);
        return super.writeStopBit(x);
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeStopBit(long x) throws BufferOverflowException {
        super.writeByte(STOP_T);
        return super.writeStopBit(x);
    }

    @Override
    public long readStopBit() throws IORuntimeException {
        expectByte(STOP_T);
        return super.readStopBit();
    }

    @Override
    public char readStopBitChar() throws IORuntimeException {
        expectByte(STOP_T);
        return super.readStopBitChar();
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeInt(int i) throws BufferOverflowException {
        super.writeByte(INT_T);
        return super.writeInt(i);
    }

    @Override
    public int readInt() throws BufferUnderflowException {
        expectByte(INT_T);
        return super.readInt();
    }

    @NotNull
    @Override
    public Bytes writeLong(long i64) throws BufferOverflowException {
        super.writeByte(LONG_T);
        return super.writeLong(i64);
    }

    @Override
    public long readLong() throws BufferUnderflowException {
        expectByte(LONG_T);
        return super.readLong();
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeFloat(float f) throws BufferOverflowException {
        super.writeByte(FLOAT_T);
        return super.writeFloat(f);
    }

    @Override
    public float readFloat() throws BufferUnderflowException {
        expectByte(FLOAT_T);
        return super.readFloat();
    }

    @NotNull
    @Override
    public Bytes<Underlying> writeDouble(double d) throws BufferOverflowException {
        super.writeByte(DOUBLE_T);
        return super.writeDouble(d);
    }

    @Override
    public double readDouble() throws BufferUnderflowException {
        expectByte(DOUBLE_T);
        return super.readDouble();
    }

    private void expectByte(byte expected) {
        byte type = super.readByte();
        if (type != expected)
            throw new IllegalStateException("Expected " + STRING_FOR_CODE[expected & 0xFF]
                    + " but was " + STRING_FOR_CODE[type & 0xFF]);
    }
}

package net.openhft.chronicle.bytes;

import java.nio.ByteBuffer;

/**
 * Created by peter.lawrey on 17/04/15.
 */
public class ExpectedBytesStore<B extends BytesStore<B, Underlying>, Underlying> implements BytesStore<B, Underlying> {
    public static final int NOT_READY = 1 << 31;
    final BytesStore<B, Underlying> underlyingBytesStore;

    ExpectedBytesStore(BytesStore<B, Underlying> underlyingBytesStore) {
        this.underlyingBytesStore = underlyingBytesStore;
    }

    @Override
    public long capacity() {
        return underlyingBytesStore.capacity();
    }

    @Override
    public Underlying underlyingObject() {
        return underlyingBytesStore.underlyingObject();
    }

    @Override
    public Access<Underlying> access() {
        return underlyingBytesStore.access();
    }

    @Override
    public void nativeWrite(long address, long position, long size) {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public void nativeRead(long position, long address, long size) {
        throw new UnsupportedOperationException("todo");
    }

    @Override
    public boolean compareAndSwapInt(long offset, int expected, int value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean compareAndSwapLong(long offset, long expected, long value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte readByte(long offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public short readShort(long offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readInt(long offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long readLong(long offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float readFloat(long offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double readDouble(long offset) {
        throw new UnsupportedOperationException();
    }    @Override
    public B writeByte(long offset, byte i8) {
        byte i8a = underlyingBytesStore.readByte(offset);
        if (i8a != i8) {
            Bytes<Underlying> bytes = underlyingBytesStore.bytes();
            bytes.position(offset);
            throw new AssertionError(bytes.toDebugString()+"\nExpected: " + i8a + "\nActual: " + i8);
        }
        return (B) this;
    }

    @Override
    public long address() throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }    @Override
    public B writeShort(long offset, short i) {
        short ia = underlyingBytesStore.readShort(offset);
        if (ia != i)
            throw new AssertionError("Expected: " + ia + "\nActual: " + i);
        return (B) this;
    }

    @Override
    public Underlying accessHandle() {
        throw new UnsupportedOperationException();
    }    @Override
    public B writeInt(long offset, int i) {
        int ia = underlyingBytesStore.readInt(offset);
        if (ia != i)
            throw new AssertionError("Expected: " + ia + "\nActual: " + i);
        return (B) this;
    }

    @Override
    public long accessOffset(long randomOffset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reserve() throws IllegalStateException {
    }    @Override
    public B writeOrderedInt(long offset, int i) {
        int ia = underlyingBytesStore.readInt(offset);
        if (ia != i) {
            if ((i & NOT_READY) == 0)
                throw new AssertionError("Expected: " + ia +" <" + Integer.toHexString(ia) + ">\nActual: " + i +" <" + Integer.toHexString(i) + ">");
        }
        return (B) this;
    }

    @Override
    public void release() throws IllegalStateException {
    }    @Override
    public B writeLong(long offset, long i) {
        long ia = underlyingBytesStore.readLong(offset);
        if (ia != i)
            throw new AssertionError("Expected: " + ia + "\nActual: " + i);
        return (B) this;
    }

    @Override
    public long refCount() {
        throw new UnsupportedOperationException();
    }    @Override
    public B writeOrderedLong(long offset, long i) {
        long ia = underlyingBytesStore.readLong(offset);
        if (ia != i)
            throw new AssertionError("Expected: " + ia + "\nActual: " + i);
        return (B) this;
    }

    @Override
    public B writeFloat(long offset, float d) {
        float ia = underlyingBytesStore.readFloat(offset);
        if (ia != d)
            throw new AssertionError("Expected: " + ia + "\nActual: " + d);
        return (B) this;
    }

    @Override
    public B writeDouble(long offset, double d) {
        double ia = underlyingBytesStore.readDouble(offset);
        if (ia != d)
            throw new AssertionError("Expected: " + ia + "\nActual: " + d);
        return (B) this;
    }

    @Override
    public B write(long offsetInRDO, byte[] bytes, int offset, int length) {
        for (int i = 0; i < length; i++)
            writeByte(offsetInRDO + i, bytes[offset + i]);
        return (B) this;
    }

    @Override
    public B write(long offsetInRDO, ByteBuffer bytes, int offset, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public B write(long offsetInRDO, Bytes bytes, long offset, long length) {
        throw new UnsupportedOperationException();
    }












}

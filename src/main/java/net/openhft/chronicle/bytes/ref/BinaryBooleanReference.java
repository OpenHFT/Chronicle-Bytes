package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.util.WeakReferenceCleaner;
import net.openhft.chronicle.core.values.BooleanValue;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Created by Rob Austin
 */
public class BinaryBooleanReference implements BooleanValue, Byteable {

    private BytesStore bytes;
    private long offset;

    private final StoreRef ref = new StoreRef();

    public BinaryBooleanReference()
    {
        WeakReferenceCleaner.newCleaner(this, ref::clean);
    }

    @Override
    public void bytesStore(final BytesStore bytes, final long offset, final long length) throws IllegalStateException, IllegalArgumentException, BufferOverflowException, BufferUnderflowException {
        if (length != maxSize())
            throw new IllegalArgumentException();

        acceptNewBytesStore(bytes);
        this.offset = offset;
    }

    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public long maxSize() {
        return 1;
    }

    private static final byte FALSE = (byte) 0xB0;
    private static final byte TRUE = (byte) 0xB1;

    @Override
    public boolean getValue() {
        byte b = bytes.readByte(offset);
        if (b == FALSE)
            return false;
        if (b == TRUE)
            return true;

        throw new IllegalStateException("unexpected code=" + b);

    }

    @Override
    public void setValue(final boolean flag) {
        bytes.writeByte(offset, flag ? TRUE : FALSE);
    }

    private void acceptNewBytesStore(final BytesStore bytes) {
        if (this.bytes != null) {
            this.bytes.release();
        }
        this.bytes = bytes.bytesStore();
        ref.b = this.bytes;
        this.bytes.reserve();
    }
}

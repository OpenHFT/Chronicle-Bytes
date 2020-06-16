package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.values.BooleanValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

public class BinaryBooleanReference extends AbstractReference implements BooleanValue {

    private static final byte FALSE = (byte) 0xB0;
    private static final byte TRUE = (byte) 0xB1;

    @SuppressWarnings("rawtypes")
    @Override
    public void bytesStore(@NotNull final BytesStore bytes, final long offset, final long length) throws IllegalStateException, IllegalArgumentException, BufferOverflowException, BufferUnderflowException {
        throwExceptionIfClosed();
        if (length != maxSize())
            throw new IllegalArgumentException();

        super.bytesStore(bytes, offset, length);
    }

    @Override
    public long maxSize() {
        return 1;
    }

    @Override
    public boolean getValue() {
        throwExceptionIfClosed();
        byte b = bytes.readByte(offset);
        if (b == FALSE)
            return false;
        if (b == TRUE)
            return true;

        throw new IllegalStateException("unexpected code=" + b);

    }

    @Override
    public void setValue(final boolean flag) {
        throwExceptionIfClosed();
        bytes.writeByte(offset, flag ? TRUE : FALSE);
    }
}

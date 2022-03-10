package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.HexDumpBytes;
import net.openhft.chronicle.core.values.BooleanValue;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.HexDumpBytes.MASK;

public class BinaryBooleanReference extends AbstractReference implements BooleanValue {

    private static final byte FALSE = (byte) 0xB0;
    private static final byte TRUE = (byte) 0xB1;

    @SuppressWarnings("rawtypes")
    @Override
    public void bytesStore(@NotNull final BytesStore bytes, long offset, final long length)
            throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
        throwExceptionIfClosedInSetter();

        if (length != maxSize())
            throw new IllegalArgumentException();
        if (bytes instanceof HexDumpBytes) {
            offset &= MASK;
        }
        super.bytesStore(bytes, offset, length);
    }

    @Override
    public long maxSize() {
        return 1;
    }

    @Override
    public boolean getValue()
            throws IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

        byte b = bytes.readByte(offset);
        if (b == FALSE)
            return false;
        if (b == TRUE)
            return true;

        throw new IllegalStateException("unexpected code=" + b);

    }

    @Override
    public void setValue(final boolean flag)
            throws IllegalStateException {
        throwExceptionIfClosed();

        try {
            bytes.writeByte(offset, flag ? TRUE : FALSE);
        } catch (BufferOverflowException e) {
            throw new AssertionError(e);
        }
    }
}

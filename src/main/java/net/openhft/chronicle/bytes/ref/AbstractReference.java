package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Created by Jerry Shea on 26/02/18.
 */
public abstract class AbstractReference implements Byteable, Closeable {

    @Nullable
    protected BytesStore bytes;
    protected long offset;

    @Override
    public void bytesStore(@NotNull final BytesStore bytes, final long offset, final long length) throws IllegalStateException, IllegalArgumentException, BufferOverflowException, BufferUnderflowException {
        acceptNewBytesStore(bytes);
        this.offset = offset;
    }

    @Nullable
    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public abstract long maxSize();

    protected void acceptNewBytesStore(@NotNull final BytesStore bytes) {
        if (this.bytes != null) {
            this.bytes.release();
        }
        this.bytes = bytes.bytesStore();
        this.bytes.reserve();
    }

    @Override
    public void close() {
        if (this.bytes != null) {
            this.bytes.release();
            this.bytes = null;
        }
    }
}

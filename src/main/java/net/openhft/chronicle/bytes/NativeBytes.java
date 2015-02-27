package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.NativeStore.nativeStore;
import static net.openhft.chronicle.bytes.NoBytesStore.noBytesStore;

/**
 * Created by peter.lawrey on 24/02/15.
 */
public class NativeBytes<Underlying> extends AbstractBytes<Underlying> {

    NativeBytes(BytesStore store) {
        super(store);
    }

    public static NativeBytes nativeBytes() {
        return new NativeBytes(noBytesStore());
    }

    public static NativeBytes nativeBytes(long initialCapacity) {
        return new NativeBytes(nativeStore(initialCapacity));
    }

    @Override
    protected long writeCheckOffset(long offset, int adding) {
        if (!bytesStore.inStore(offset))
            checkResize(offset);
        return offset;
    }

    private void checkResize(long offset) {
        if (isElastic())
            resize(offset);
        else
            throw new BufferOverflowException();
    }

    private void resize(long offset) {
        if (offset < 0)
            throw new IllegalArgumentException();
        // grow by 50% rounded up to the next pages size
        long ps = OS.pageSize();
        long size = (Math.max(offset, bytesStore.capacity() * 3 / 2) + ps) & ~(ps - 1);
        NativeStore store;
        if (bytesStore.underlyingObject() instanceof ByteBuffer) {
            store = NativeStore.elasticByteBuffer(Maths.toInt32(size));
        } else {
            store = NativeStore.lazyNativeStore(size);
        }
        bytesStore.copyTo(store);
        bytesStore.release();
        bytesStore = store;
    }

    @Override
    public long capacity() {
        return 1L << 40;
    }

    @Override
    public boolean isElastic() {
        return true;
    }
}

/*
 * Copyright 2016 higherfrequencytrading.com
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

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.NativeBytesStore.nativeStoreWithFixedCapacity;
import static net.openhft.chronicle.bytes.NoBytesStore.noBytesStore;

/**
 * Elastic native memory accessor which can wrap either a direct ByteBuffer or malloc'ed memory.
 */
public class NativeBytes<Underlying> extends VanillaBytes<Underlying> {

    private static final Logger LOG = LoggerFactory.getLogger(NativeBytes.class);

    NativeBytes(@NotNull BytesStore store) throws IllegalStateException {
        super(store, 0, MAX_CAPACITY);
    }

    @NotNull
    public static NativeBytes<Void> nativeBytes() {
        try {
            return new NativeBytes<>(noBytesStore());
        } catch (IllegalStateException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    public static NativeBytes<Void> nativeBytes(long initialCapacity) throws IllegalArgumentException {
        NativeBytesStore<Void> store = nativeStoreWithFixedCapacity(initialCapacity);
        try {
            return new NativeBytes<>(store);
        } catch (IllegalStateException e) {
            throw new AssertionError(e);
        } finally {
            store.release();
        }
    }

    public static BytesStore<Bytes<Void>, Void> copyOf(@NotNull Bytes bytes) {
        long remaining = bytes.readRemaining();
        NativeBytes<Void> bytes2;
        try {
            bytes2 = Bytes.allocateElasticDirect(remaining);
            bytes2.write(bytes, 0, remaining);
            return bytes2;
        } catch (BufferOverflowException | BufferUnderflowException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public long capacity() {
        return MAX_CAPACITY;
    }

    @Override
    protected void writeCheckOffset(long offset, long adding)
            throws BufferOverflowException, IllegalArgumentException {
        if (!bytesStore.inside(offset + adding - 1))
            checkResize(offset + adding);
    }

    @Override
    public void ensureCapacity(long size) throws IllegalArgumentException {
        try {
            writeCheckOffset(size, 1L);
        } catch (BufferOverflowException e) {
            throw new IllegalArgumentException("Bytes cannot be resized to " + size + " limit: " + capacity());
        }
    }

    private void checkResize(long endOfBuffer)
            throws BufferOverflowException, IllegalArgumentException {
        if (isElastic())
            resize(endOfBuffer);
        else
            throw new BufferOverflowException();
    }

    @Override
    public byte readVolatileByte(long offset) throws BufferUnderflowException {
        return bytesStore.readVolatileByte(offset);
    }

    @Override
    public short readVolatileShort(long offset) throws BufferUnderflowException {
        return bytesStore.readVolatileShort(offset);
    }

    @Override
    public int readVolatileInt(long offset) throws BufferUnderflowException {
        return bytesStore.readVolatileInt(offset);
    }

    @Override
    public long readVolatileLong(long offset) throws BufferUnderflowException {
        return bytesStore.readVolatileLong(offset);
    }

    @Override
    public boolean isElastic() {
        return true;
    }

    private void resize(long endOfBuffer)
            throws IllegalArgumentException, BufferOverflowException {
        if (endOfBuffer < 0)
            throw new IllegalArgumentException(endOfBuffer + " < 0");
        if (endOfBuffer > capacity())
            throw new BufferOverflowException();
        // grow by 50% rounded up to the next pages size
        long ps = OS.pageSize();
        long size = (Math.max(endOfBuffer, bytesStore.realCapacity() * 3 / 2) + ps) & ~(ps - 1);
        if (capacity() < Long.MAX_VALUE)
            size = Math.min(size, capacity());
        NativeBytesStore store;
        if (bytesStore.underlyingObject() instanceof ByteBuffer) {
            store = NativeBytesStore.elasticByteBuffer(Maths.toInt32(size), capacity());

        } else {
            store = NativeBytesStore.lazyNativeBytesStoreWithFixedCapacity(size);
        }
        try {
            bytesStore.copyTo(store);
            bytesStore.release();
        } catch (IllegalStateException e) {
            LOG.error("", e);
        }
        bytesStore = store;
    }

    @NotNull
    @Override
    public Bytes<Underlying> write(byte[] bytes, int offset, int length) throws BufferOverflowException, IllegalArgumentException {
        if (bytes.length > writeRemaining())
            throw new BufferOverflowException();
        long position = writePosition();
        ensureCapacity(position + length);
        super.write(bytes, offset, length);
        return this;
    }

    @NotNull
    public Bytes<Underlying> write(BytesStore bytes, long offset, long length) throws BufferOverflowException, IllegalArgumentException, BufferUnderflowException {
        long position = writePosition();
        ensureCapacity(position + length);
        super.write(bytes, offset, length);
        return this;
    }
}

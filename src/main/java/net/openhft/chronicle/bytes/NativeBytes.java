/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.NativeBytesStore.nativeStoreWithFixedCapacity;
import static net.openhft.chronicle.bytes.NoBytesStore.noBytesStore;

/**
 * Created by peter.lawrey on 24/02/15.
 */
public class NativeBytes<Underlying> extends ZeroedBytes<Underlying> {

    NativeBytes(BytesStore store) {
        super(store, UnderflowMode.PADDED);
    }

    public static NativeBytes<Void> nativeBytes() {
        return new NativeBytes<Void>(noBytesStore());
    }

    public static NativeBytes<Void> nativeBytes(long initialCapacity) {
        return new NativeBytes<Void>(nativeStoreWithFixedCapacity(initialCapacity));
    }

    @Override
    public long capacity() {
        return 1L << 40;
    }

    @Override
    protected void writeCheckOffset(long offset, long adding) {
        if (!bytesStore.inStore(offset + adding))
            checkResize(offset + adding);
    }



    @Override
    public void ensureCapacity(long size) {
        writeCheckOffset(size, 0);
    }

    private void checkResize(long endOfBuffer) {
        if (isElastic())
            resize(endOfBuffer);
        else
            throw new BufferOverflowException();
    }

    @Override
    public boolean isElastic() {
        return true;
    }

    private void resize(long endOfBuffer) {
        if (endOfBuffer < 0)
            throw new IllegalArgumentException();
        // grow by 50% rounded up to the next pages size
        long ps = OS.pageSize();
        long size = (Math.max(endOfBuffer, bytesStore.capacity() * 3 / 2) + ps) & ~(ps - 1);
        NativeBytesStore store;
        if (bytesStore.underlyingObject() instanceof ByteBuffer) {
            store = NativeBytesStore.elasticByteBuffer(Maths.toInt32(size));

        } else {
            store = NativeBytesStore.lazyNativeBytesStoreWithFixedCapacity(size);
        }
        bytesStore.copyTo(store);
        bytesStore.release();
        bytesStore = store;
    }

    @Override
    public boolean isNative() {
        return true;
    }

    @Override
    public Bytes<Underlying> write(Bytes bytes, long offset, long length) {
        if (bytes instanceof NativeBytes) {
            long len = Math.min(remaining(), Math.min(bytes.remaining(), length));
            writeCheckOffset(position(), len);
            NativeAccess.U.copyMemory(bytes.address() + offset, address() + position(), len);
            skip(len);
            return this;

        } else {
            return super.write(bytes, offset, length);
        }
    }

    public Bytes<Underlying> write(String str, int offset, int length) {
        char[] chars = HotSpotStringAccessor.INSTANCE.handle(str);
        long position = position();
        ensureCapacity(position + length);
        NativeBytesStore nbs = (NativeBytesStore) bytesStore;
        nbs.write8bit(position, chars, offset, length);
        skip(length);
        return this;
    }

    @Override
    public Bytes<Underlying> write(BytesStore bytes, long offset, long length) {
        if (bytes instanceof NativeBytesStore) {
            writeCheckOffset(position(), length);
            NativeAccess.U.copyMemory(bytes.address() + offset, address() + position(), length);
            skip(length);
            return this;

        } else {
            return super.write(bytes, offset, length);
        }
    }

    public static BytesStore<Bytes<Void>, Void> copyOf(Bytes bytes) {
        long remaining = bytes.remaining();
        NativeBytes<Void> bytes2 = NativeBytes.nativeBytes(remaining);
        bytes2.write(bytes, 0, remaining);
        bytes2.flip();
        return bytes2;
    }

    public void read8Bit(char[] chars, int length) {
        long position = position();
        skip(length);
        NativeBytesStore nbs = (NativeBytesStore) bytesStore;
        nbs.read8bit(position, chars, length);
    }
}

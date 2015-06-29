/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.OS;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.NativeBytesStore.nativeStoreWithFixedCapacity;
import static net.openhft.chronicle.bytes.NoBytesStore.noBytesStore;

public class NativeBytes<Underlying> extends VanillaBytes<Underlying> {

    NativeBytes(BytesStore store) {
        super(store, 0, MAX_CAPACITY);
    }

    public static NativeBytes<Void> nativeBytes() {
        return new NativeBytes<>(noBytesStore());
    }

    public static NativeBytes<Void> nativeBytes(long initialCapacity) {
        return new NativeBytes<>(nativeStoreWithFixedCapacity(initialCapacity));
    }

    public static BytesStore<Bytes<Void>, Void> copyOf(Bytes bytes) {
        long remaining = bytes.readRemaining();
        NativeBytes<Void> bytes2 = NativeBytes.nativeBytes(remaining);
        bytes2.write(bytes, 0, remaining);
        return bytes2;
    }

    @Override
    public long capacity() {
        return MAX_CAPACITY;
    }

    @Override
    protected void writeCheckOffset(long offset, long adding) {
        if (!bytesStore.inside(offset + adding))
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

    public int readVolatileInt(long offset) {
        return bytesStore.readVolatileInt(offset);
    }

    public long readVolatileLong(long offset) {
        return bytesStore.readVolatileLong(offset);
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

    public void write(String str, int offset, int length) {
        // todo optimise
        char[] chars = str.toCharArray();
        long position = writePosition();
        ensureCapacity(position + length);
        NativeBytesStore nbs = (NativeBytesStore) bytesStore;
        nbs.write8bit(position, chars, offset, length);
        writeSkip(length);
    }

    @Override
    public long readIncompleteLong(long offset) {
        return bytesStore.readIncompleteLong(offset);
    }

    public Bytes<Underlying> write(BytesStore bytes, long offset, long length) {
        long position = writePosition();
        ensureCapacity(position + length);
        super.write(bytes, offset, length);
        return this;
    }
}

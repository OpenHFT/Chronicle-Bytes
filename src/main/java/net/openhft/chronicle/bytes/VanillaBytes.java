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
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.NoBytesStore.noBytesStore;

public class VanillaBytes<Underlying> extends AbstractBytes<Underlying> implements Byteable<Underlying> {
    /**
     * @return a non elastic bytes.
     */
    public static VanillaBytes<Void> nativeBytes() {
        return new VanillaBytes<>(noBytesStore());
    }

    public VanillaBytes(@NotNull BytesStore bytesStore) {
        this(bytesStore, bytesStore.writePosition(), bytesStore.writeLimit());
    }

    public VanillaBytes(@NotNull BytesStore bytesStore, long writePosition, long writeLimit) {
        super(bytesStore, writePosition, writeLimit);
    }

    @Override
    public void bytesStore(BytesStore<Bytes<Underlying>, Underlying> byteStore, long offset, long length) {
        bytesStore(byteStore);
        // assume its read-only
        readLimit(offset + length);
        writeLimit(offset + length);
        readPosition(offset);
    }

    private void bytesStore(@NotNull BytesStore<Bytes<Underlying>, Underlying> bytesStore) {
        BytesStore oldBS = this.bytesStore;
        this.bytesStore = bytesStore;
        oldBS.release();
        clear();
    }
    @Override
    public long maxSize() {
        return readRemaining();
    }

    @Override
    public boolean isElastic() {
        return false;
    }

    @Override
    public Bytes<Underlying> bytesForRead() {
        return isClear()
                ? new VanillaBytes<>(bytesStore, bytesStore.writeLimit(), bytesStore.writeLimit())
                : new SubBytes<>(bytesStore, readPosition(), readLimit());
    }

    @Override
    public long realCapacity() {
        return bytesStore.realCapacity();
    }

    @Override
    public BytesStore<Bytes<Underlying>, Underlying> copy() {
        if (bytesStore.underlyingObject() instanceof ByteBuffer) {
            ByteBuffer bb = ByteBuffer.allocateDirect(Maths.toInt32(readRemaining()));
            ByteBuffer bbu = (ByteBuffer) bytesStore.underlyingObject();
            ByteBuffer slice = bbu.slice();
            slice.position((int) readPosition());
            slice.limit((int) readLimit());
            bb.put(slice);
            bb.clear();
            return (BytesStore) BytesStore.wrap(bb);

        } else {
            return (BytesStore) NativeBytes.copyOf(this);
        }
    }

    @Override
    public Bytes<Underlying> write(BytesStore bs, long offset, long length) {
        long i = 0;
        for (; i < length - 7; i += 8)
            writeLong(bs.readLong(offset + i));
        for (; i < length; i++)
            writeByte(bs.readByte(offset + i));
        return this;
    }

    public NativeBytesStore bytesStore() {
        return (NativeBytesStore) bytesStore;
    }

    public void read8Bit(char[] chars, int length) {
        long position = readPosition();
        NativeBytesStore nbs = bytesStore();
        nbs.read8bit(position, chars, length);
    }
}

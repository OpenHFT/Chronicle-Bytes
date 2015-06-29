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
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

import static net.openhft.chronicle.bytes.NoBytesStore.noBytesStore;

public class VanillaBytes<Underlying> extends AbstractBytes<Underlying> implements Byteable<Underlying> {
    /**
     * @return a non elastic bytes.
     */
    public static VanillaBytes<Void> vanillaBytes() {
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
    public Bytes<Underlying> write(BytesStore bytes, long offset, long length) {
        if (bytes.underlyingObject() == null) {
            long len = Math.min(writeRemaining(), Math.min(bytes.readRemaining(), length));
            if (len > 0) {
                writeCheckOffset(writePosition(), len);
                OS.memory().copyMemory(bytes.address(offset), address(writePosition()), len);
                writeSkip(len);
            }

        } else {
            super.write(bytes, offset, length);
        }
        return this;
    }

    public NativeBytesStore bytesStore() {
        return (NativeBytesStore) bytesStore;
    }

    @Override
    public boolean equalBytes(BytesStore b, long remaining) {
        if (b instanceof VanillaBytes) {
            VanillaBytes b2 = (VanillaBytes) b;
            NativeBytesStore nbs0 = bytesStore();
            NativeBytesStore nbs2 = b2.bytesStore();
            long i = 0;
            for (; i < remaining - 7; i++) {
                long addr0 = nbs0.address + readPosition() - nbs0.start() + i;
                long addr2 = nbs2.address + b2.readPosition() - nbs2.start() + i;
                long l0 = NativeBytesStore.MEMORY.readLong(addr0);
                long l2 = NativeBytesStore.MEMORY.readLong(addr2);
                if (l0 != l2)
                    return false;
            }
            for (; i < remaining; i++) {
                long offset2 = readPosition() + i - nbs0.start();
                long offset21 = b2.readPosition() + i - nbs2.start();
                byte b0 = NativeBytesStore.MEMORY.readByte(nbs0.address + offset2);
                byte b1 = NativeBytesStore.MEMORY.readByte(nbs2.address + offset21);
                if (b0 != b1)
                    return false;
            }
            return true;
        } else {
            return super.equalBytes(b, remaining);
        }
    }

    public void read8Bit(char[] chars, int length) {
        long position = readPosition();
        NativeBytesStore nbs = bytesStore();
        nbs.read8bit(position, chars, length);
    }

}

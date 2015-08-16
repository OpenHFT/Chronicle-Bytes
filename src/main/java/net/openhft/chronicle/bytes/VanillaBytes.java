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
    public VanillaBytes(@NotNull BytesStore bytesStore) {
        this(bytesStore, bytesStore.writePosition(), bytesStore.writeLimit());
    }

    public VanillaBytes(@NotNull BytesStore bytesStore, long writePosition, long writeLimit) {
        super(bytesStore, writePosition, writeLimit);
    }

    /**
     * @return a non elastic bytes.
     */
    @NotNull
    public static VanillaBytes<Void> vanillaBytes() {
        return new VanillaBytes<>(noBytesStore());
    }

    @Override
    public void bytesStore(@NotNull BytesStore<Bytes<Underlying>, Underlying> byteStore, long offset, long length) {
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

    @NotNull
    @Override
    public Bytes<Underlying> bytesForRead() {
        return isClear()
                ? new VanillaBytes<>(bytesStore, writePosition(), bytesStore.writeLimit())
                : new SubBytes<>(bytesStore, readPosition(), readLimit());
    }

    @Override
    public long realCapacity() {
        return bytesStore.realCapacity();
    }

    @NotNull
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

    @NotNull
    @Override
    public Bytes<Underlying> write(@NotNull BytesStore bytes, long offset, long length) {
        if (bytes.bytesStore() instanceof NativeBytesStore && length >= 64) {
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

    public Bytes<Underlying> write8bit(@NotNull CharSequence str, int offset, int length) {
        long position = writePosition();
        write(position, str, offset, length);
        writeSkip(length);
        return this;
    }

    public void write(long position, @NotNull CharSequence str, int offset, int length) {
        // todo optimise
        if (str instanceof String) {
            char[] chars = ((String) str).toCharArray();
            ensureCapacity(position + length);
            NativeBytesStore nbs = (NativeBytesStore) bytesStore;
            nbs.write8bit(position, chars, offset, length);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NotNull
    public VanillaBytes append(CharSequence str, int start, int end) {
        if (bytesStore() instanceof NativeBytesStore) {
            if (str instanceof BytesStore) {
                write((BytesStore) str, (long) start, end - start);
                return this;
            }
            if (str instanceof String) {
                write8bit(str, start, end - start);
                return this;
            }
        }
        super.append(str, start, end);
        return this;
    }

    @Override
    public boolean equalBytes(BytesStore b, long remaining) {
        if (bytesStore instanceof NativeBytesStore &&
                b instanceof VanillaBytes && b.bytesStore() instanceof NativeBytesStore) {
            VanillaBytes b2 = (VanillaBytes) b;
            NativeBytesStore nbs0 = (NativeBytesStore) bytesStore;
            NativeBytesStore nbs2 = (NativeBytesStore) b2.bytesStore();
            long i = 0;
            for (; i < remaining - 7; i++) {
                long addr0 = nbs0.address + readPosition() - nbs0.start() + i;
                long addr2 = nbs2.address + b2.readPosition() - nbs2.start() + i;
                long l0 = nbs0.memory.readLong(addr0);
                long l2 = nbs2.memory.readLong(addr2);
                if (l0 != l2)
                    return false;
            }
            for (; i < remaining; i++) {
                long offset2 = readPosition() + i - nbs0.start();
                long offset21 = b2.readPosition() + i - nbs2.start();
                byte b0 = nbs0.memory.readByte(nbs0.address + offset2);
                byte b1 = nbs2.memory.readByte(nbs2.address + offset21);
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
        NativeBytesStore nbs = (NativeBytesStore) bytesStore();
        nbs.read8bit(position, chars, length);
    }

    public int byteCheckSum() {
        if (readLimit() >= Integer.MAX_VALUE || start() != 0)
            return super.byteCheckSum();
        byte b = 0;
        NativeBytesStore bytesStore = (NativeBytesStore) bytesStore();
        for (int i = (int) readPosition(), lim = (int) readLimit(); i < lim; i++) {
            b += bytesStore.memory.readByte(bytesStore.address + i);
        }
        return b & 0xFF;
    }
}

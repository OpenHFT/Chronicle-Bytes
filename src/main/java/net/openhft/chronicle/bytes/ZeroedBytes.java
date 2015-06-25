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

import net.openhft.chronicle.core.annotation.ForceInline;

public class ZeroedBytes<Underlying> extends VanillaBytes<Underlying> {
    private final UnderflowMode underflowMode;

    public ZeroedBytes(BytesStore store, UnderflowMode underflowMode, long writeLimit) {
        super(store, store.writePosition(), writeLimit);
        this.underflowMode = underflowMode;
    }

    @Override
    public Bytes<Underlying> bytesForRead() {
        return isClear()
                ? new ZeroedBytes(bytesStore, underflowMode, writeLimit())
                : new SubZeroedBytes<>(bytesStore, underflowMode, readPosition(), readLimit());
    }

    @Override
    @ForceInline
    public byte readByte() {
        return positionOk(1) ? super.readByte() : (byte) 0;
    }

    @ForceInline
    private boolean positionOk(int needs) {
        return underflowMode.isRemainingOk(readRemaining(), needs);
    }

    @Override
    @ForceInline
    public short readShort() {
        return positionOk(2) ? super.readShort() : (short) 0;
    }

    @Override
    @ForceInline
    public int readInt() {
        return positionOk(4) ? super.readInt() : 0;
    }

    @Override
    @ForceInline
    public long readLong() {
        return positionOk(8) ? super.readLong() : 0L;
    }

    @Override
    @ForceInline
    public float readFloat() {
        return positionOk(4) ? super.readFloat() : 0.0f;
    }

    @Override
    @ForceInline
    public double readDouble() {
        return positionOk(8) ? super.readDouble() : 0.0;
    }
}

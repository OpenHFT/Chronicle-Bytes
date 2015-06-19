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

public class ZeroedBytes<Underlying> extends VanillaBytes<Underlying> {
    private final UnderflowMode underflowMode;

    public ZeroedBytes(BytesStore store, UnderflowMode underflowMode) {
        super(store);
        this.underflowMode = underflowMode;
    }

    @Override
    public Bytes<Underlying> bytes() {
        boolean isClear = start() == position() && limit() == capacity();
        return isClear
                ? new ZeroedBytes(bytesStore, underflowMode)
                : new SubZeroedBytes<>(bytesStore, underflowMode, position(), limit());
    }

    @Override
    public byte readByte() {
        return positionOk(1) ? super.readByte() : (byte) 0;
    }

    private boolean positionOk(int needs) {
        return underflowMode.isRemainingOk(remaining(), needs);
    }

    @Override
    public short readShort() {
        return positionOk(2) ? super.readShort() : (short) 0;
    }

    @Override
    public int readInt() {
        return positionOk(4) ? super.readInt() : 0;
    }

    @Override
    public long readLong() {
        return positionOk(8) ? super.readLong() : 0L;
    }

    @Override
    public float readFloat() {
        return positionOk(4) ? super.readFloat() : 0.0f;
    }

    @Override
    public double readDouble() {
        return positionOk(8) ? super.readDouble() : 0.0;
    }
}

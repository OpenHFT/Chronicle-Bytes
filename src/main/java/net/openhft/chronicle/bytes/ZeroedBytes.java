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

public class ZeroedBytes<Underlying> extends BytesStoreBytes<Underlying> {
    private final UnderflowMode underflowMode;

    public ZeroedBytes(BytesStore store, UnderflowMode underflowMode) {
        super(store);
        this.underflowMode = underflowMode;
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

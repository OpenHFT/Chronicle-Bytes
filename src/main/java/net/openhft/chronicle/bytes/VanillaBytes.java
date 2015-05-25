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
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

public class VanillaBytes<Underlying> extends AbstractBytes<Underlying> implements Byteable<Underlying> {
    public VanillaBytes(@NotNull BytesStore bytesStore) {
        super(bytesStore);
    }

    @Override
    public void bytesStore(BytesStore<Bytes<Underlying>, Underlying> byteStore, long offset, long length) {
        bytesStore(byteStore);
        limit(offset + length);
        position(offset);
    }
    
    public void bytesStore(@NotNull BytesStore<Bytes<Underlying>, Underlying> bytesStore) {
        BytesStore oldBS = this.bytesStore;
        this.bytesStore = bytesStore;
        oldBS.release();
        clear();
    }

    @Override
    public BytesStore<Bytes<Underlying>, Underlying> bytesStore() {
        return bytesStore;
    }

    @Override
    public long offset() {
        return position();
    }

    @Override
    public long maxSize() {
        return remaining();
    }

    @Override
    public boolean isElastic() {
        return false;
    }

    @Override
    public Bytes<Underlying> bytes() {
        boolean isClear = start() == position() && limit() == capacity();
        return isClear ? new VanillaBytes<>(bytesStore) : new SubBytes<>(bytesStore, position(), limit());
    }

    @Override
    public long realCapacity() {
        return bytesStore.realCapacity();
    }

    @Override
    public BytesStore<Bytes<Underlying>, Underlying> copy() {
        if (bytesStore.underlyingObject() instanceof ByteBuffer) {
            ByteBuffer bb = ByteBuffer.allocateDirect(Maths.toInt32(remaining()));
            ByteBuffer bbu = (ByteBuffer) bytesStore.underlyingObject();
            ByteBuffer slice = bbu.slice();
            slice.position((int) position());
            slice.limit((int) limit());
            bb.put(slice);
            bb.clear();
            return (BytesStore) BytesStore.wrap(bb);

        } else {
            return (BytesStore) NativeBytes.copyOf(this);
        }
    }
}

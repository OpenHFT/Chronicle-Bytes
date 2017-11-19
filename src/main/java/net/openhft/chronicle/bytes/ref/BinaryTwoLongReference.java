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
package net.openhft.chronicle.bytes.ref;

import org.jetbrains.annotations.NotNull;

public class BinaryTwoLongReference extends BinaryLongReference implements TwoLongReference {
    @Override
    public long maxSize() {
        return 2 * 8;
    }

    @NotNull
    public String toString() {
        return bytes == null ? "bytes is null" : "value: " + getValue() + ", value2: " + getValue2();
    }

    @Override
    public long getValue2() {
        return bytes.readLong(offset + 8);
    }

    @Override
    public void setValue2(long value) {
        bytes.writeLong(offset + 8, value);
    }

    @Override
    public long getVolatileValue2() {
        return bytes.readVolatileLong(offset + 8);
    }

    @Override
    public void setOrderedValue2(long value) {
        bytes.writeOrderedLong(offset + 8, value);
    }

    @Override
    public long addValue2(long delta) {
        return bytes.addAndGetLong(offset + 8, delta);
    }

    @Override
    public long addAtomicValue2(long delta) {
        return addValue2(delta);
    }

    @Override
    public boolean compareAndSwapValue2(long expected, long value) {
        return bytes.compareAndSwapLong(offset + 8, expected, value);
    }
}

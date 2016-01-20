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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.UnsafeMemory;
import net.openhft.chronicle.core.values.LongValue;
import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

public class UncheckedLongReference implements LongValue, Byteable {
    private long address;
    private Unsafe unsafe;

    @Override
    public void bytesStore(@NotNull BytesStore bytes, long offset, long length) {
        if (length != maxSize()) throw new IllegalArgumentException();
        address = bytes.address(offset);
        unsafe = UnsafeMemory.UNSAFE;
    }

    @Override
    public BytesStore bytesStore() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long offset() {
        return address;
    }

    @Override
    public long maxSize() {
        return 8;
    }

    @NotNull
    public String toString() {
        return address == 0 ? "address is 0" : "value: " + getValue();
    }

    @Override
    public long getValue() {
        return unsafe.getLong(address);
    }

    @Override
    public void setValue(long value) {
        unsafe.putLong(address, value);
    }

    @Override
    public long getVolatileValue() {
        return unsafe.getLong(address);
    }

    @Override
    public void setOrderedValue(long value) {
        unsafe.putOrderedLong(null, address, value);
    }

    @Override
    public long addValue(long delta) {
        return unsafe.getAndAddLong(null, address, delta) + delta;
    }

    @Override
    public long addAtomicValue(long delta) {
        return addValue(delta);
    }

    @Override
    public boolean compareAndSwapValue(long expected, long value) {
        return unsafe.compareAndSwapLong(null, address, expected, value);
    }
}

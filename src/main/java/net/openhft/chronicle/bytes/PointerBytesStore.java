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

import org.jetbrains.annotations.NotNull;

/**
 * A BytesStore which can point to arbitary memory.
 */
public class PointerBytesStore extends NativeBytesStore<Void> {
    public PointerBytesStore() {
        super(NoBytesStore.NO_PAGE, 0, null, false);
    }

    public void set(long address, long capacity) {
        setAddress(address);
        this.maximumLimit = capacity;
    }

    @NotNull
    @Override
    public VanillaBytes<Void> bytesForWrite() throws IllegalStateException {
        return new VanillaBytes<>(this);
    }

    @Override
    public long safeLimit() {
        return maximumLimit;
    }

    @Override
    public long start() {
        return 0;
    }
}

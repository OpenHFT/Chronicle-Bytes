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
 * Created by peter.lawrey on 19/04/15.
 */
public class SubZeroedBytes<Underlying> extends ZeroedBytes<Underlying> {
    private final long start;
    private final long capacity;

    public SubZeroedBytes(@NotNull BytesStore bytesStore, UnderflowMode underflowMode, long start, long capacity) {
        super(bytesStore, underflowMode);
        this.start = start;
        this.capacity = capacity;
        clear();
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @Override
    public long start() {
        return start;
    }

    @Override
    public long realCapacity() {
        return capacity;
    }
}

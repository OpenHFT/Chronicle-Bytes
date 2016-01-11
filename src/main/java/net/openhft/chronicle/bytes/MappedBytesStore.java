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

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.ReferenceCounted;
import org.jetbrains.annotations.NotNull;

/**
 * BytesStore to wrap memory mapped data.
 */
public class MappedBytesStore extends NativeBytesStore<Void> {
    private final long start;
    private final long safeLimit;
    private MappedFile mappedFile;

    protected MappedBytesStore(ReferenceCounted owner,
                               long start,
                               long address,
                               long capacity,
                               long safeCapacity,
                               @NotNull final MappedFile mappedFile) throws IllegalStateException {
        super(address, start + capacity, new OS.Unmapper(address, capacity, owner), false);
        this.start = start;
        this.safeLimit = start + safeCapacity;
        this.mappedFile = mappedFile;
    }

    @NotNull
    @Override
    public MappedBytes bytesForWrite() throws IllegalStateException {
        return new MappedBytes(mappedFile);
    }

    @Override
    public long safeLimit() {
        return safeLimit;
    }

    @Override
    public long start() {
        return start;
    }
}

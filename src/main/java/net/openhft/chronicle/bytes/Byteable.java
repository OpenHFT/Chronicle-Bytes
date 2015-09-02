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

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * This Interface allows a reference to off heap memory to be reassigned.
 * <p></p>
 * A reference to off heap memory is a proxy for some memory which sits outside the heap.
 */
public interface Byteable<Underlying> {
    /**
     * This setter for a data type which points to an underlying ByteStore.
     *
     * @param bytesStore the fix point ByteStore
     * @param offset the offset within the ByteStore
     * @param length the length in the ByteStore
     */
    void bytesStore(BytesStore<Bytes<Underlying>, Underlying> bytesStore, long offset, long length)
            throws IllegalStateException, IllegalArgumentException, BufferOverflowException, BufferUnderflowException;

    /**
     * @return the maximum size in byte for this reference.
     */
    long maxSize();
}
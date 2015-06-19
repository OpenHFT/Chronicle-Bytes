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

import java.nio.ByteOrder;

public interface RandomCommon<S extends RandomCommon<S, A, AT>, A extends AccessCommon<AT>, AT> {
    /**
     * @return the highest offset or position allowed for this buffer.
     */
    default long limit() {
        return capacity();
    }

    default long readLimit() {
        return Math.min(realCapacity(), limit());
    }

    /**
     * @return the actual amount of data which can be read.
     */
    long realCapacity();

    /**
     * @return the highest limit allowed for this buffer.
     */
    long capacity();

    /**
     * Obtain the underlying address.  This is for expert users only.
     *
     * @return the underlying address of the buffer
     * @throws UnsupportedOperationException if the underlying buffer is on the heap
     */
    long address() throws UnsupportedOperationException;

    boolean isNative();

    default ByteOrder byteOrder() {
        return ByteOrder.nativeOrder();
    }

    A access();

    AT accessHandle();

    long accessOffset(long randomOffset);

    // get a streaming bytes.
    Bytes bytes();
}

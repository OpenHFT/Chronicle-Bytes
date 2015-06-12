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

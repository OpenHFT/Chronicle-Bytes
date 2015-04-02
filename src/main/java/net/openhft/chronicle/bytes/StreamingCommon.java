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

import java.nio.BufferUnderflowException;
import java.util.function.Consumer;

public interface StreamingCommon<S extends StreamingCommon<S, A, AT>,
        A extends AccessCommon<AT>, AT> extends RandomCommon<S, A, AT> {
    /**
     * @return the number of bytes between the position and the limit.
     */
    default long remaining() {
        return limit() - position();
    }

    S position(long position);

    long position();

    S limit(long limit);

    S clear();

    /**
     * Perform a set of actions with a temporary bounds mode.
     */
    default S withLength(long length, Consumer<S> bytesConsumer) {
        if (length > remaining())
            throw new BufferUnderflowException();
        long limit0 = limit();
        long limit = position() + length;
        try {
            limit(limit);
            bytesConsumer.accept((S) this);
        } finally {
            limit(limit0);
            position(limit);
        }
        return (S) this;
    }

    S skip(long bytesToSkip);

    S flip();

    default String toDebugString(long maxLength) {
        return BytesUtil.toDebugString((RandomDataInput & StreamingCommon) this, maxLength);
    }

    default String toDebugString() {
        return toDebugString(128);
    }

    default long accessPositionOffset() {
        return accessOffset(position());
    }
}

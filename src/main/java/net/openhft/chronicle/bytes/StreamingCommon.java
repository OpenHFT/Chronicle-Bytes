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

import java.nio.BufferUnderflowException;
import java.util.function.Consumer;
import java.util.function.Function;

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

    default <S,R> R reply(long length, Function<S, R> bytesConsumer) {
        if (length > remaining())
            throw new BufferUnderflowException();
        long limit0 = limit();
        long limit = position() + length;
        try {
            limit(limit);
            return bytesConsumer.apply((S) this);
        } finally {
            limit(limit0);
            position(limit);
        }
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

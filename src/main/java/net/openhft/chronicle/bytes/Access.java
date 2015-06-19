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

import java.nio.ByteBuffer;

public interface Access<T> extends ReadAccess<T>, WriteAccess<T> {

    static <T> Access<T> nativeAccess() {
        return NativeAccess.instance();
    }

    static Access<ByteBuffer> checkedByteBufferAccess() {
        return ByteBufferAccess.INSTANCE;
    }

    static <B extends BytesStore<B, U>, U> Access<B> checkedBytesStoreAccess() {
        return BytesAccesses.Full.INSTANCE;
    }

    static <S, T> void copy(ReadAccess<S> sourceAccess, S source, long sourceOffset,
                            WriteAccess<T> targetAccess, T target, long targetOffset,
                            long len) {
        targetAccess.writeFrom(target, targetOffset, sourceAccess, source, sourceOffset, len);
    }

    static <S, ST, SA extends ReadAccess<ST>, T> void copy(
            Accessor<S, ST, SA> sourceAccessor, S source, long sourceIndex,
            WriteAccess<T> targetAccess, T target, long targetOffset, long size) {
        copy(sourceAccessor.access(source), sourceAccessor.handle(source),
                sourceAccessor.offset(source, sourceIndex),
                targetAccess, target, targetOffset, sourceAccessor.size(size));
    }

    static <S, T, TT, TA extends WriteAccess<TT>> void copy(
            ReadAccess<S> sourceAccess, S source, long sourceOffset,
            Accessor<T, TT, TA> targetAccessor, T target, long targetIndex,
            long len) {
        copy(sourceAccess, source, sourceOffset,
                targetAccessor.access(target), targetAccessor.handle(target),
                targetAccessor.offset(target, targetIndex), len);
    }

    static <T, TT, TA extends ReadAccess<TT>, U> boolean equivalent(
            Accessor<T, TT, TA> accessor1, T source1, long index1,
            ReadAccess<U> access2, U handle2, long offset2,
            long size) {
        return equivalent(accessor1.access(source1), accessor1.handle(source1),
                accessor1.offset(source1, index1), access2, handle2, offset2,
                accessor1.size(size));
    }

    static <T, U> boolean equivalent(ReadAccess<T> access1, T handle1, long offset1,
                                     ReadAccess<U> access2, U handle2, long offset2,
                                     long len) {
        return access1.compareTo(handle1, offset1, access2, handle2, offset2, len);
    }

    /**
     * Default implementation: throws {@code UnsupportedOperationException}.
     */
    default boolean compareAndSwapInt(T handle, long offset, int expected, int value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Default implementation: throws {@code UnsupportedOperationException}.
     */
    default boolean compareAndSwapLong(T handle, long offset, long expected, long value) {
        throw new UnsupportedOperationException();
    }
}
/*
 * Copyright 2014 Higher Frequency Trading http://www.higherfrequencytrading.com
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public interface Accessor<S, T, A extends AccessCommon<T>> {

    interface Read<S, T> extends Accessor<S, T, ReadAccess<T>> {}
    interface Full<S, T> extends Accessor<S, T, Access<T>> {}


    static <B extends BytesStore<B, U>, U> Accessor.Full<B, ?> uncheckedBytesStoreAccessor() {
        return BytesAccessors.Unchecked.INSTANCE;
    }

    static <B extends BytesStore<B, U>, U> Accessor.Full<B, ?> checkedBytesStoreAccessor() {
        return BytesAccessors.Generic.INSTANCE;
    }

    static Accessor.Full<ByteBuffer, ?> uncheckedByteBufferAccessor(
            ByteBuffer buffer) {
        return ByteBufferAccessor.unchecked(buffer);
    }

    static Accessor.Full<ByteBuffer, ByteBuffer> checkedByteBufferAccessor() {
        return ByteBufferAccessor.checked();
    }

    static Accessor.Full<boolean[], boolean[]> booleanArrayAccessor() {
        return ArrayAccessors.Boolean.INSTANCE;
    }

    static Accessor.Full<byte[], byte[]> byteArrayAccessor() {
        return ArrayAccessors.Byte.INSTANCE;
    }

    static Accessor.Full<char[], char[]> charArrayAccessor() {
        return ArrayAccessors.Char.INSTANCE;
    }

    static Accessor.Full<short[], short[]> shortArrayAccessor() {
        return ArrayAccessors.Short.INSTANCE;
    }

    static Accessor.Full<int[], int[]> intArrayAccessor() {
        return ArrayAccessors.Int.INSTANCE;
    }

    static Accessor.Full<long[], long[]> longArrayAccessor() {
        return ArrayAccessors.Long.INSTANCE;
    }

    static Accessor.Full<float[], float[]> floatArrayAccessor() {
        return ArrayAccessors.Float.INSTANCE;
    }

    static Accessor.Full<double[], double[]> doubleArrayAccessor() {
        return ArrayAccessors.Double.INSTANCE;
    }

    static Accessor.Read<? super String, ?> stringAccessor() {
        return CharSequenceAccessor.stringAccessor;
    }

    static Accessor.Read<CharSequence, CharSequence> checkedNativeCharSequenceAccessor() {
        return CharSequenceAccessor.nativeCharSequenceAccessor();
    }

    static Accessor.Read<CharSequence, CharSequence> checkedCharSequenceAccess(ByteOrder order) {
        return order == ByteOrder.LITTLE_ENDIAN ? CharSequenceAccessor.LITTLE_ENDIAN :
                CharSequenceAccessor.BIG_ENDIAN;
    }

    /**
     * Returns {@code Access} for the given source.
     *
     * @param source the source
     * @return {@code Access} for the given source
     */
    A access(S source);

    /**
     * Returns handle for {@code Access} to the given source.
     *
     * @param source the source
     * @return handle for {@code Access} to the given source
     */
    T handle(S source);

    /**
     * Convert index in the source domain to {@code Access} offset.
     *
     * @param source the source
     * @param index index in the source type domain
     * @return offset for {@code Access}, corresponding to the given index
     */
    long offset(S source, long index);

    /**
     * Convert size (length) in the source domain to size in bytes.
     *
     * The default implementation returns the given {@code size} back, i. e. assuming
     * byte-indexed source.
     *
     * @param size size (length) in the source type domain
     * @return number of bytes, corresponding to the given size in the source type domain
     */
    default long size(long size) {
        return size;
    }
}

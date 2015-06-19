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
    
    static Accessor.Read<Boolean, Boolean> booleanAccessor() {
        return PrimitiveAccessors.BooleanAccessor.INSTANCE;
    }

    static Accessor.Full<byte[], byte[]> byteArrayAccessor() {
        return ArrayAccessors.Byte.INSTANCE;
    }
    
    static Accessor.Read<Byte, Byte> byteAccessor() {
        return PrimitiveAccessors.ByteAccessor.INSTANCE;
    }

    static Accessor.Full<char[], char[]> charArrayAccessor() {
        return ArrayAccessors.Char.INSTANCE;
    }
    
    static Accessor.Read<Character, Character> characterAccessor() {
        return PrimitiveAccessors.CharacterAccessor.INSTANCE;
    }

    static Accessor.Full<short[], short[]> shortArrayAccessor() {
        return ArrayAccessors.Short.INSTANCE;
    }
    
    static Accessor.Read<Short, Short> shortAccessor() {
        return PrimitiveAccessors.ShortAccessor.INSTANCE;
    }

    static Accessor.Full<int[], int[]> intArrayAccessor() {
        return ArrayAccessors.Int.INSTANCE;
    }
    
    static Accessor.Read<Integer, Integer> integerAccessor() {
        return PrimitiveAccessors.IntegerAccessor.INSTANCE;
    }

    static Accessor.Full<long[], long[]> longArrayAccessor() {
        return ArrayAccessors.Long.INSTANCE;
    }
    
    static Accessor.Read<Long, Long> longAccessor() {
        return PrimitiveAccessors.LongAccessor.INSTANCE;
    }

    static Accessor.Full<float[], float[]> floatArrayAccessor() {
        return ArrayAccessors.Float.INSTANCE;
    }
    
    static Accessor.Read<Float, Float> floatAccessor() {
        return PrimitiveAccessors.FloatAccessor.INSTANCE;
    }

    static Accessor.Full<double[], double[]> doubleArrayAccessor() {
        return ArrayAccessors.Double.INSTANCE;
    }
    
    static Accessor.Read<Double, Double> doubleAccessor() {
        return PrimitiveAccessors.DoubleAccessor.INSTANCE;
    }

    static Accessor.Read<String, ?> stringAccessor() {
        return (Read<String, ?>) CharSequenceAccessor.stringAccessor;
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

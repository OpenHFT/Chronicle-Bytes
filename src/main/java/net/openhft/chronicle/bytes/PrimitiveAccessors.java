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

import static net.openhft.chronicle.bytes.Access.nativeAccess;
import static net.openhft.chronicle.bytes.NativeAccess.U;

final class PrimitiveAccessors {

    static final long BOOLEAN_VALUE_OFFSET;
    static final long BYTE_VALUE_OFFSET;
    static final long CHARACTER_VALUE_OFFSET;
    static final long SHORT_VALUE_OFFSET;
    static final long INTEGER_VALUE_OFFSET;
    static final long LONG_VALUE_OFFSET;
    static final long FLOAT_VALUE_OFFSET;
    static final long DOUBLE_VALUE_OFFSET;

    static {
        try {
            BOOLEAN_VALUE_OFFSET = U.objectFieldOffset(Boolean.class.getDeclaredField("value"));
            BYTE_VALUE_OFFSET = U.objectFieldOffset(Byte.class.getDeclaredField("value"));
            CHARACTER_VALUE_OFFSET = U.objectFieldOffset(Character.class.getDeclaredField("value"));
            SHORT_VALUE_OFFSET = U.objectFieldOffset(Short.class.getDeclaredField("value"));
            INTEGER_VALUE_OFFSET = U.objectFieldOffset(Integer.class.getDeclaredField("value"));
            LONG_VALUE_OFFSET = U.objectFieldOffset(Long.class.getDeclaredField("value"));
            FLOAT_VALUE_OFFSET = U.objectFieldOffset(Float.class.getDeclaredField("value"));
            DOUBLE_VALUE_OFFSET = U.objectFieldOffset(Double.class.getDeclaredField("value"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    
    interface PrimitiveAccessor<P> extends Accessor.Read<P, P> {
        @Override
        default ReadAccess<P> access(P source) {
            return nativeAccess();
        }

        @Override
        default P handle(P source) {
            return source;
        }
    }
    
    interface Size1PrimitiveAccessor<P> extends PrimitiveAccessor<P> {
        @Override
        default long size(long size) {
            assert size == 1;
            return 1;
        }
    }

    interface Size2PrimitiveAccessor<P> extends PrimitiveAccessor<P> {
        @Override
        default long size(long size) {
            assert size == 2;
            return 2;
        }
    }

    interface Size4PrimitiveAccessor<P> extends PrimitiveAccessor<P> {
        @Override
        default long size(long size) {
            assert size == 4;
            return 4;
        }
    }

    interface Size8PrimitiveAccessor<P> extends PrimitiveAccessor<P> {
        @Override
        default long size(long size) {
            assert size == 8;
            return 8;
        }
    }

    enum BooleanAccessor implements Size1PrimitiveAccessor<Boolean> {
        INSTANCE;

        @Override
        public long offset(Boolean source, long index) {
            return BOOLEAN_VALUE_OFFSET + index;
        }
    }
    
    enum ByteAccessor implements Size1PrimitiveAccessor<Byte> {
        INSTANCE;

        @Override
        public long offset(Byte source, long index) {
            return BYTE_VALUE_OFFSET + index;
        }
    }

    enum CharacterAccessor implements Size2PrimitiveAccessor<Character> {
        INSTANCE;

        @Override
        public long offset(Character source, long index) {
            return CHARACTER_VALUE_OFFSET + index;
        }
    }

    enum ShortAccessor implements Size2PrimitiveAccessor<Short> {
        INSTANCE;

        @Override
        public long offset(Short source, long index) {
            return SHORT_VALUE_OFFSET + index;
        }
    }
    
    enum IntegerAccessor implements Size4PrimitiveAccessor<Integer> {
        INSTANCE;

        @Override
        public long offset(Integer source, long index) {
            return INTEGER_VALUE_OFFSET + index;
        }
    }
    
    enum LongAccessor implements Size8PrimitiveAccessor<Long> {
        INSTANCE;
        
        @Override
        public long offset(Long source, long index) {
            return LONG_VALUE_OFFSET + index;
        }
    }
    
    enum FloatAccessor implements Size4PrimitiveAccessor<Float> {
        INSTANCE;
        
        @Override
        public long offset(Float source, long index) {
            return FLOAT_VALUE_OFFSET + index;
        }
    }
    
    enum DoubleAccessor implements Size8PrimitiveAccessor<Double> {
        INSTANCE;

        @Override
        public long offset(Double source, long index) {
            return DOUBLE_VALUE_OFFSET + index;
        }
    }

    private PrimitiveAccessors() {}
}

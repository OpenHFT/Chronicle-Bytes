/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.BytesInternal;
import net.openhft.chronicle.core.ClassLocal;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Maths;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.*;
import java.util.function.Supplier;

@SuppressWarnings({"rawtypes", "unchecked"})
public class BytesMarshaller<T> {
    public static final ClassLocal<BytesMarshaller> BYTES_MARSHALLER_CL
            = ClassLocal.withInitial(BytesMarshaller::new);
    private final FieldAccess[] fields;

    public BytesMarshaller(@NotNull final Class<T> tClass) {
        final Map<String, Field> map = new LinkedHashMap<>();
        getAllField(tClass, map);
        fields = map.values().stream()
                .map(FieldAccess::create)
                .toArray(FieldAccess[]::new);
    }

    public static void getAllField(@NotNull final Class clazz,
                                   @NotNull final Map<String, Field> map) {
        if (clazz != Object.class)
            getAllField(clazz.getSuperclass(), map);
        for (@NotNull Field field : clazz.getDeclaredFields()) {
            if ((field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) != 0)
                continue;
            Jvm.setAccessible(field);
            map.put(field.getName(), field);
        }
    }

    public void readMarshallable(ReadBytesMarshallable t, BytesIn in) {
        for (@NotNull FieldAccess field : fields) {
            field.read(t, in);
        }
    }

    public void writeMarshallable(WriteBytesMarshallable t, BytesOut out)
            throws IllegalArgumentException, IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
        out.indent(+1);
        for (@NotNull FieldAccess field : fields) {
            field.write(t, out);
        }
        out.indent(-1);
    }

    abstract static class FieldAccess {
        final Field field;

        FieldAccess(@NotNull final Field field) {
            this.field = field;
        }

        @NotNull
        public static Object create(@NotNull final Field field) {
            final Class<?> type = field.getType();
            switch (type.getName()) {
                case "boolean":
                    return new BooleanFieldAccess(field);
                case "byte":
                    return new ByteFieldAccess(field);
                case "char":
                    return new CharFieldAccess(field);
                case "short":
                    return new ShortFieldAccess(field);
                case "int":
                    return new IntegerFieldAccess(field);
                case "float":
                    return new FloatFieldAccess(field);
                case "long":
                    return new LongFieldAccess(field);
                case "double":
                    return new DoubleFieldAccess(field);
                default:
                    return nonPrimitiveFieldAccess(field, type);
            }
        }

        static Object nonPrimitiveFieldAccess(@NotNull final Field field,
                                              @NotNull final Class<?> type) {
            if (type.isArray()) {
                if (type.getComponentType().isPrimitive()) {
                    if (type == byte[].class)
                        return new ByteArrayFieldAccess(field);
                    if (type == int[].class)
                        return new IntArrayFieldAccess(field);
                    if (type == float[].class)
                        return new FloatArrayFieldAccess(field);
                    if (type == long[].class)
                        return new LongArrayFieldAccess(field);
                    if (type == double[].class)
                        return new DoubleArrayFieldAccess(field);
                    throw new UnsupportedOperationException("TODO " + field.getType());
                }
                return new ObjectArrayFieldAccess(field);
            }
            if (Collection.class.isAssignableFrom(type))
                return new CollectionFieldAccess(field);
            if (Map.class.isAssignableFrom(type))
                return new MapFieldAccess(field);
            if (BytesStore.class.isAssignableFrom(type))
                return new BytesFieldAccess(field);
            if (BytesMarshallable.class.isAssignableFrom(type))
                return new BytesMarshallableFieldAccess(field);
            return new ScalarFieldAccess(field);
        }

        @NotNull
        static Class extractClass(Type type0) {
            if (type0 instanceof Class)
                return (Class) type0;
            else if (type0 instanceof ParameterizedType)
                return (Class) ((ParameterizedType) type0).getRawType();
            else
                return Object.class;
        }

        @NotNull
        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "field=" + field +
                    '}';
        }

        void write(Object o, BytesOut write)
                throws IllegalArgumentException, IllegalStateException, BufferOverflowException, ArithmeticException, BufferUnderflowException {
            write.comment(field.getName());
            getValue(o, write);
        }

        protected abstract void getValue(Object o, BytesOut write)
                throws BufferOverflowException, IllegalArgumentException, IllegalStateException, BufferUnderflowException, ArithmeticException;

        void read(Object o, BytesIn read)
                throws IORuntimeException {
            try {
                setValue(o, read);
            } catch (BufferUnderflowException | IllegalArgumentException | ArithmeticException | IllegalStateException | BufferOverflowException iae) {
                throw new IORuntimeException(iae);
            }
        }

        protected abstract void setValue(Object o, BytesIn read)
                throws BufferUnderflowException, IllegalArgumentException, ArithmeticException, IllegalStateException, BufferOverflowException;
    }

    static final class ScalarFieldAccess extends FieldAccess {
        public ScalarFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws BufferOverflowException, IllegalStateException {
            try {
                Object o2 = field.get(o);
                @Nullable String s = o2 == null ? null : o2.toString();
                write.writeUtf8(s);
            } catch (IllegalArgumentException | IllegalAccessException | BufferUnderflowException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IORuntimeException, BufferUnderflowException, IllegalStateException, ArithmeticException, IllegalArgumentException {
            try {
                @Nullable String s = read.readUtf8();
                field.set(o, ObjectUtils.convertTo(field.getType(), s));
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class BytesMarshallableFieldAccess extends FieldAccess {
        public BytesMarshallableFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, BytesOut write)
                throws BufferUnderflowException, IllegalStateException, BufferOverflowException {
            try {
                @NotNull BytesMarshallable o2 = (BytesMarshallable) field.get(o);
                assert o2 != null;
                o2.writeMarshallable(write);
            } catch (IllegalArgumentException | IllegalAccessException | ArithmeticException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, BytesIn read)
                throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            try {
                @NotNull BytesMarshallable o2 = (BytesMarshallable) field.get(o);
                if (!field.getType().isInstance(o2)) {
                    o2 = (BytesMarshallable) ObjectUtils.newInstance((Class) field.getType());
                    field.set(o, o2);
                }

                o2.readMarshallable(read);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class BytesFieldAccess extends FieldAccess {
        public BytesFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalStateException, BufferOverflowException, IllegalArgumentException {
            @NotNull BytesStore bytes;
            try {
                bytes = (BytesStore) field.get(o);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
            if (bytes == null) {
                BytesInternal.writeStopBitNeg1(write);
                return;
            }
            long offset = bytes.readPosition();
            long length = bytes.readRemaining();
            write.writeStopBit(length);
            try {
                write.write(bytes, offset, length);
            } catch (BufferUnderflowException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IORuntimeException, IllegalArgumentException, IllegalStateException, ArithmeticException, BufferUnderflowException, BufferOverflowException {
            try {
                @NotNull Bytes bytes = (Bytes) field.get(o);
                long stopBit = read.readStopBit();
                if (stopBit == -1) {
                    if (bytes != null)
                        bytes.releaseLast();
                    field.set(o, null);
                    return;
                }
                int length = Maths.toUInt31(stopBit);
                @NotNull Bytes bs;
                if (bytes == null) {
                    bs = Bytes.allocateElasticOnHeap(length);
                    field.set(o, bs);
                } else {
                    bs = bytes;
                }
                bs.clear();
                read.read(bs, length);
                bs.readLimit(length);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class ObjectArrayFieldAccess extends FieldAccess {
        Class componentType;

        public ObjectArrayFieldAccess(Field field) {
            super(field);
            componentType = field.getType().getComponentType();
        }

        @Override
        protected void getValue(Object o, BytesOut write)
                throws IllegalStateException, BufferOverflowException, IllegalArgumentException, BufferUnderflowException, ArithmeticException {
            try {
                Object[] c = (Object[]) field.get(o);
                if (c == null) {
                    BytesInternal.writeStopBitNeg1(write);
                    return;
                }
                int size = c.length;
                write.writeStopBit(size);
                if (size == 0)
                    return;
                for (int i = 0; i < size; i++)
                    write.writeObject(componentType, c[i]);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, BytesIn read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException, ArithmeticException, BufferOverflowException {
            try {
                Object[] c = (Object[]) field.get(o);
                int length = Maths.toInt32(read.readStopBit());
                if (length < 0) {
                    if (c != null)
                        field.set(o, null);
                    return;
                }
                if (c == null) {
                    c = (Object[]) Array.newInstance(field.getType().getComponentType(), length);
                    field.set(o, c);
                } else if (c.length != length)
                    field.set(o, c = Arrays.copyOf(c, length));
                for (int i = 0; i < length; i++) {
                    Object o2 = c[i];
                    if (o2 instanceof BytesMarshallable)
                        ((BytesMarshallable) o2).readMarshallable(read);
                    else
                        c[i] = read.readObject(componentType);
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class CollectionFieldAccess extends FieldAccess {
        final Supplier<Collection> collectionSupplier;
        @NotNull
        private final Class componentType;
        private final Class<?> type;

        public CollectionFieldAccess(@NotNull Field field) {
            super(field);
            type = field.getType();
            if (type == List.class || type == Collection.class)
                collectionSupplier = ArrayList::new;
            else if (type == SortedSet.class || type == NavigableSet.class)
                collectionSupplier = TreeSet::new;
            else if (type == Set.class)
                collectionSupplier = LinkedHashSet::new;
            else
                collectionSupplier = () -> ObjectUtils.newInstance((Class<? extends Collection>) type);
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                @NotNull ParameterizedType pType = (ParameterizedType) genericType;
                Type type0 = pType.getActualTypeArguments()[0];
                componentType = extractClass(type0);
            } else {
                componentType = Object.class;
            }
        }

        @Override
        protected void getValue(Object o, BytesOut write)
                throws IllegalStateException, BufferOverflowException, IllegalArgumentException, BufferUnderflowException, ArithmeticException {
            try {
                Collection c = (Collection) field.get(o);
                if (c == null) {
                    BytesInternal.writeStopBitNeg1(write);
                    return;
                }
                write.writeStopBit(c.size());
                if (c.isEmpty())
                    return;
                if (c instanceof RandomAccess && c instanceof List) {
                    List l = (List) c;
                    for (int i = 0, size = l.size(); i < size; i++)
                        write.writeObject(componentType, l.get(i));
                } else {
                    for (Object o2 : c) {
                        write.writeObject(componentType, o2);
                    }
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, BytesIn read)
                throws IllegalStateException, ArithmeticException, IllegalArgumentException, BufferUnderflowException, BufferOverflowException {
            try {
                Collection c = (Collection) field.get(o);
                int length = Maths.toInt32(read.readStopBit());
                if (length < 0) {
                    if (c != null)
                        field.set(o, null);
                    return;
                }

                if (c == null)
                    field.set(o, c = collectionSupplier.get());
                else
                    c.clear();

                for (int i = 0; i < length; i++)
                    c.add(read.readObject(componentType));
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class MapFieldAccess extends FieldAccess {
        final Supplier<Map> collectionSupplier;
        private final Class<?> type;
        @NotNull
        private final Class keyType;
        @NotNull
        private final Class valueType;

        public MapFieldAccess(@NotNull Field field) {
            super(field);
            type = field.getType();
            if (type == Map.class)
                collectionSupplier = LinkedHashMap::new;
            else if (type == SortedMap.class || type == NavigableMap.class)
                collectionSupplier = TreeMap::new;
            else
                collectionSupplier = () -> ObjectUtils.newInstance((Class<? extends Map>) type);
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                @NotNull ParameterizedType pType = (ParameterizedType) genericType;
                Type[] actualTypeArguments = pType.getActualTypeArguments();
                keyType = extractClass(actualTypeArguments[0]);
                valueType = extractClass(actualTypeArguments[1]);

            } else {
                keyType = Object.class;
                valueType = Object.class;
            }
        }

        @Override
        protected void getValue(Object o, BytesOut write)
                throws IllegalStateException, BufferOverflowException, IllegalArgumentException, BufferUnderflowException, ArithmeticException {
            Map<?, ?> m = null;
            try {
                m = (Map) field.get(o);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
            if (m == null) {
                BytesInternal.writeStopBitNeg1(write);
                return;
            }
            write.writeStopBit(m.size());
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                write.writeObject(keyType, entry.getKey());
                write.writeObject(valueType, entry.getValue());
            }
        }

        @Override
        protected void setValue(Object o, BytesIn read)
                throws IllegalStateException, IllegalArgumentException, BufferUnderflowException, BufferOverflowException, ArithmeticException {
            try {
                Map m = (Map) field.get(o);
                long length = read.readStopBit();
                if (length < 0) {
                    if (m != null)
                        field.set(o, null);
                    return;
                }
                if (m == null) {
                    field.set(o, m = collectionSupplier.get());
                } else {
                    m.clear();
                }
                for (int i = 0; i < length; i++) {
                    m.put(read.readObject(keyType), read.readObject(valueType));
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class BooleanFieldAccess extends FieldAccess {
        public BooleanFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalArgumentException, BufferOverflowException, IllegalStateException {
            try {
                write.writeBoolean(field.getBoolean(o));
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IllegalArgumentException {
            try {
                field.setBoolean(o, read.readBoolean());
            } catch (IllegalAccessException | IllegalStateException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class ByteFieldAccess extends FieldAccess {
        public ByteFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
            try {
                write.writeByte(field.getByte(o));
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IllegalStateException, IllegalArgumentException {
            try {
                field.setByte(o, read.readByte());
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class ByteArrayFieldAccess extends FieldAccess {
        public ByteArrayFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
            try {
                byte[] array = (byte[]) field.get(o);
                if (array == null) {
                    write.writeInt(~0);
                } else {
                    write.writeInt(array.length);
                    write.write(array);
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException {
            try {
                int len = read.readInt();
                if (len == ~0) {
                    field.set(o, null);
                } else if (len >= 0) {
                    byte[] array = (byte[]) field.get(o);
                    if (array == null || array.length != len) {
                        array = new byte[len];
                        field.set(o, array);
                    }
                    read.read(array);
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class CharFieldAccess extends FieldAccess {
        public CharFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
            try {
                char aChar = field.getChar(o);
                if (aChar >= 65536 - 127)
                    write.writeStopBit(aChar - 65536L);
                else
                    write.writeStopBit(aChar);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IllegalStateException, IllegalArgumentException, BufferUnderflowException {
            try {
                field.setChar(o, read.readStopBitChar());
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class ShortFieldAccess extends FieldAccess {
        public ShortFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalArgumentException, BufferOverflowException, IllegalStateException {
            try {
                write.writeShort(field.getShort(o));
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException {
            try {
                field.setShort(o, read.readShort());
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class IntegerFieldAccess extends FieldAccess {
        public IntegerFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
            try {
                write.writeInt(field.getInt(o));
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException {
            try {
                field.setInt(o, read.readInt());
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class IntArrayFieldAccess extends FieldAccess {
        public IntArrayFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalStateException, BufferOverflowException, IllegalArgumentException {
            int[] array;
            try {
                array = (int[]) field.get(o);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
            if (array == null) {
                write.writeInt(~0);
            } else {
                write.writeInt(array.length);
                for (int i = 0; i < array.length; i++)
                    write.writeInt(array[i]);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException {
            try {
                int len = read.readInt();
                if (len == ~0) {
                    field.set(o, null);
                } else if (len >= 0) {
                    int[] array = (int[]) field.get(o);
                    if (array == null || array.length != len) {
                        array = new int[len];
                        field.set(o, array);
                    }
                    for (int i = 0; i < len; i++)
                        array[i] = read.readInt();
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class FloatFieldAccess extends FieldAccess {
        public FloatFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
            try {
                write.writeFloat(field.getFloat(o));
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException {
            try {
                field.setFloat(o, read.readFloat());
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class FloatArrayFieldAccess extends FieldAccess {
        public FloatArrayFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
            float[] array;
            try {
                array = (float[]) field.get(o);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
            if (array == null) {
                write.writeInt(~0);
            } else {
                write.writeInt(array.length);
                for (int i = 0; i < array.length; i++)
                    write.writeFloat(array[i]);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException {
            try {
                int len = read.readInt();
                if (len == ~0) {
                    field.set(o, null);
                } else if (len >= 0) {
                    float[] array = (float[]) field.get(o);
                    if (array == null || array.length != len) {
                        array = new float[len];
                        field.set(o, array);
                    }
                    for (int i = 0; i < len; i++)
                        array[i] = read.readFloat();
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class LongFieldAccess extends FieldAccess {
        public LongFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
            try {
                write.writeLong(field.getLong(o));
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException {
            try {
                field.setLong(o, read.readLong());
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class LongArrayFieldAccess extends FieldAccess {
        public LongArrayFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalStateException, BufferOverflowException, IllegalArgumentException {
            long[] array;
            try {
                array = (long[]) field.get(o);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
            if (array == null) {
                write.writeInt(~0);
            } else {
                write.writeInt(array.length);
                for (int i = 0; i < array.length; i++)
                    write.writeLong(array[i]);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException {
            try {
                int len = read.readInt();
                if (len == ~0) {
                    field.set(o, null);
                } else if (len >= 0) {
                    long[] array = (long[]) field.get(o);
                    if (array == null || array.length != len) {
                        array = new long[len];
                        field.set(o, array);
                    }
                    for (int i = 0; i < len; i++)
                        array[i] = read.readLong();
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class DoubleFieldAccess extends FieldAccess {
        public DoubleFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
            try {
                write.writeDouble(field.getDouble(o));
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException {
            try {
                field.setDouble(o, read.readDouble());
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }

    static final class DoubleArrayFieldAccess extends FieldAccess {
        public DoubleArrayFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException {
            double[] array;
            try {
                array = (double[]) field.get(o);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
            if (array == null) {
                write.writeInt(~0);
            } else {
                write.writeInt(array.length);
                for (int i = 0; i < array.length; i++)
                    write.writeDouble(array[i]);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException {
            try {
                int len = read.readInt();
                if (len == ~0) {
                    field.set(o, null);
                } else if (len >= 0) {
                    double[] array = (double[]) field.get(o);
                    if (array == null || array.length != len) {
                        array = new double[len];
                        field.set(o, array);
                    }
                    for (int i = 0; i < len; i++)
                        array[i] = read.readDouble();
                }
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }
    }
}

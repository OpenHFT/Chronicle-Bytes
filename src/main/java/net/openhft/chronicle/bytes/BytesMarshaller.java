/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.ValidatableUtil;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.*;
import java.util.function.Supplier;

/**
 * This class is used to marshal objects into bytes and unmarshal them from bytes.
 * The object's fields are read and written in a streaming manner, with the ability
 * to handle different types of fields. It uses a map to track the fields of the class
 * and their corresponding values. This class makes use of the {@link FieldAccess} for
 * actual field value extraction and setting.
 *
 * <p>This class also provides a method for extracting all fields from a class and its
 * superclasses (except transient and static fields). All the fields are made accessible
 * even if they are private, and are then stored in a map for future access.
 *
 * <p>Note: This class suppresses rawtypes and unchecked warnings.
 *
 * @param <T> the type of the object to be marshaled.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class BytesMarshaller<T> {

    /**
     * Provides a ClassLocal instance for holding a unique BytesMarshaller for each class.
     */
    public static final ClassLocal<BytesMarshaller> BYTES_MARSHALLER_CL
            = ClassLocal.withInitial(BytesMarshaller::new);
    private final FieldAccess[] fields;

    /**
     * Constructs a BytesMarshaller for the specified class.
     *
     * @param tClass the class for which the BytesMarshaller is to be created.
     */
    public BytesMarshaller(@NotNull Class<T> tClass) {
        final Map<String, Field> map = new LinkedHashMap<>();
        getAllField(tClass, map);
        fields = map.values().stream()
                .map(FieldAccess::create)
                .toArray(FieldAccess[]::new);
    }

    /**
     * Extracts all fields from the specified class and its superclasses and stores
     * them in the provided map. Only non-static and non-transient fields are considered.
     *
     * @param clazz the class from which to extract fields.
     * @param map   the map in which to store the fields.
     */
    public static void getAllField(@NotNull Class clazz, @NotNull Map<String, Field> map) {
        if (clazz != Object.class)
            getAllField(clazz.getSuperclass(), map);
        for (@NotNull Field field : clazz.getDeclaredFields()) {
            if ((field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) != 0)
                continue;
            Jvm.setAccessible(field);
            map.put(field.getName(), field);
        }
    }

    /**
     * Reads the state of the given ReadBytesMarshallable object from the specified BytesIn.
     *
     * @param t  the object to read into.
     * @param in the BytesIn from which to read.
     * @throws InvalidMarshallableException if the object cannot be read due to invalid data.
     */
    public void readMarshallable(ReadBytesMarshallable t, BytesIn<?> in) throws InvalidMarshallableException {
        for (@NotNull FieldAccess field : fields) {
            field.read(t, in);
        }
    }

    /**
     * Writes the state of the given WriteBytesMarshallable object to the specified BytesOut.
     *
     * @param t   the object to write.
     * @param out the BytesOut to which to write.
     * @throws IllegalArgumentException     if a method is invoked with an illegal or inappropriate argument.
     * @throws IllegalStateException        if there is an error in the internal state.
     * @throws BufferOverflowException      if there is not enough space in the buffer.
     * @throws BufferUnderflowException     if there is not enough data available in the buffer.
     * @throws ArithmeticException          if there is an arithmetic error.
     * @throws InvalidMarshallableException if the object cannot be written due to invalid data.
     */
    public void writeMarshallable(WriteBytesMarshallable t, BytesOut<?> out)
            throws IllegalArgumentException, IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException, InvalidMarshallableException {
        out.adjustHexDumpIndentation(+1);
        try {
            for (@NotNull FieldAccess field : fields) {
                field.write(t, out);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } finally {
            out.adjustHexDumpIndentation(-1);
        }
    }

    abstract static class FieldAccess {
        final Field field;

        FieldAccess(@NotNull Field field) {
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

        void write(Object o, BytesOut<?> write)
                throws IllegalAccessException, IllegalArgumentException, IllegalStateException, BufferOverflowException, ArithmeticException, BufferUnderflowException, InvalidMarshallableException {
            write.writeHexDumpDescription(field.getName());
            getValue(o, write);
        }

        protected abstract void getValue(Object o, BytesOut<?> write)
                throws IllegalAccessException, BufferOverflowException, IllegalArgumentException, IllegalStateException, BufferUnderflowException, ArithmeticException, InvalidMarshallableException;

        void read(Object o, BytesIn<?> read)
                throws IORuntimeException, InvalidMarshallableException {
            try {
                setValue(o, read);
            } catch (BufferUnderflowException | IllegalArgumentException | ArithmeticException | IllegalStateException |
                     BufferOverflowException | IllegalAccessException iae) {
                throw new IORuntimeException(iae);
            }
        }

        protected abstract void setValue(Object o, BytesIn<?> read)
                throws IllegalAccessException, BufferUnderflowException, IllegalArgumentException, ArithmeticException, IllegalStateException, BufferOverflowException, InvalidMarshallableException;
    }

    static class ScalarFieldAccess extends FieldAccess {
        public ScalarFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws BufferOverflowException, IllegalStateException, IllegalAccessException {
            Object o2 = field.get(o);
            @Nullable String s = o2 == null ? null : o2.toString();
            write.writeUtf8(s);
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IORuntimeException, BufferUnderflowException, IllegalStateException, ArithmeticException, IllegalArgumentException, IllegalAccessException {
            @Nullable String s = read.readUtf8();
            field.set(o, ObjectUtils.convertTo(field.getType(), s));
        }
    }

    static class BytesMarshallableFieldAccess extends FieldAccess {
        public BytesMarshallableFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, BytesOut<?> write)
                throws BufferUnderflowException, IllegalStateException, BufferOverflowException, InvalidMarshallableException, IllegalAccessException {
            @NotNull BytesMarshallable o2 = (BytesMarshallable) field.get(o);
            ValidatableUtil.validate(o2);
            assert o2 != null;
            o2.writeMarshallable(write);
        }

        @Override
        protected void setValue(Object o, BytesIn<?> read)
                throws IORuntimeException, BufferUnderflowException, IllegalStateException, InvalidMarshallableException, IllegalAccessException {
            @NotNull BytesMarshallable o2 = (BytesMarshallable) field.get(o);
            if (!field.getType().isInstance(o2)) {
                o2 = (BytesMarshallable) ObjectUtils.newInstance((Class) field.getType());
                field.set(o, o2);
            }

            assert o2 != null;
            o2.readMarshallable(read);
        }
    }

    static class BytesFieldAccess extends FieldAccess {
        public BytesFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalStateException, BufferOverflowException, IllegalArgumentException, IllegalAccessException {
            @NotNull BytesStore bytes;
            bytes = (BytesStore) field.get(o);
            if (bytes == null) {
                BytesInternal.writeStopBitNeg1(write);
                return;
            }
            long offset = bytes.readPosition();
            long length = bytes.readRemaining();
            write.writeStopBit(length);
            write.write(bytes, offset, length);
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IORuntimeException, IllegalArgumentException, IllegalStateException, ArithmeticException, BufferUnderflowException, BufferOverflowException, IllegalAccessException {
            @NotNull Bytes<?> bytes = (Bytes) field.get(o);
            long stopBit = read.readStopBit();
            if (stopBit == -1) {
                if (bytes != null)
                    bytes.releaseLast();
                field.set(o, null);
                return;
            }
            int length = Maths.toUInt31(stopBit);
            @NotNull Bytes<?> bs;
            if (bytes == null) {
                bs = Bytes.allocateElasticOnHeap(length);
                field.set(o, bs);
            } else {
                bs = bytes;
            }
            bs.clear();
            read.read(bs, length);
            bs.readLimit(length);
        }
    }

    static class ObjectArrayFieldAccess extends FieldAccess {
        Class componentType;

        public ObjectArrayFieldAccess(Field field) {
            super(field);
            componentType = field.getType().getComponentType();
        }

        @Override
        protected void getValue(Object o, BytesOut<?> write)
                throws IllegalStateException, BufferOverflowException, IllegalArgumentException, BufferUnderflowException, ArithmeticException, InvalidMarshallableException, IllegalAccessException {
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
        }

        @Override
        protected void setValue(Object o, BytesIn<?> read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException, ArithmeticException, BufferOverflowException, InvalidMarshallableException, IllegalAccessException {
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
        }
    }

    static class CollectionFieldAccess extends FieldAccess {
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
        protected void getValue(Object o, BytesOut<?> write)
                throws IllegalStateException, BufferOverflowException, IllegalArgumentException, BufferUnderflowException, ArithmeticException, InvalidMarshallableException, IllegalAccessException {
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
        }

        @Override
        protected void setValue(Object o, BytesIn<?> read)
                throws IllegalStateException, ArithmeticException, IllegalArgumentException, BufferUnderflowException, BufferOverflowException, InvalidMarshallableException, IllegalAccessException {
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
        }
    }

    static class MapFieldAccess extends FieldAccess {
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
        protected void getValue(Object o, BytesOut<?> write)
                throws IllegalStateException, BufferOverflowException, IllegalArgumentException, BufferUnderflowException, ArithmeticException, InvalidMarshallableException, IllegalAccessException {
            Map<?, ?> m = (Map) field.get(o);
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
        protected void setValue(Object o, BytesIn<?> read)
                throws IllegalStateException, IllegalArgumentException, BufferUnderflowException, BufferOverflowException, ArithmeticException, IllegalAccessException {
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
        }
    }

    static class BooleanFieldAccess extends FieldAccess {
        public BooleanFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalArgumentException, BufferOverflowException, IllegalStateException, IllegalAccessException {
            write.writeBoolean(field.getBoolean(o));

        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IllegalArgumentException, IllegalAccessException {
            field.setBoolean(o, read.readBoolean());
        }
    }

    static class ByteFieldAccess extends FieldAccess {
        public ByteFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException, IllegalAccessException {
            write.writeByte(field.getByte(o));
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IllegalStateException, IllegalArgumentException, IllegalAccessException {
            field.setByte(o, read.readByte());
        }
    }

    static class ByteArrayFieldAccess extends FieldAccess {
        public ByteArrayFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException, IllegalAccessException {
            byte[] array = (byte[]) field.get(o);
            if (array == null) {
                write.writeInt(~0);
            } else {
                write.writeInt(array.length);
                write.write(array);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException, IllegalAccessException {
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
        }
    }

    static class CharFieldAccess extends FieldAccess {
        public CharFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException, IllegalAccessException {
            char aChar = field.getChar(o);
            if (aChar >= 65536 - 127)
                write.writeStopBit(aChar - 65536L);
            else
                write.writeStopBit(aChar);
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IllegalStateException, IllegalArgumentException, BufferUnderflowException, IllegalAccessException {
            field.setChar(o, read.readStopBitChar());
        }
    }

    static class ShortFieldAccess extends FieldAccess {
        public ShortFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalArgumentException, BufferOverflowException, IllegalStateException, IllegalAccessException {
            write.writeShort(field.getShort(o));
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException, IllegalAccessException {
            field.setShort(o, read.readShort());
        }
    }

    static class IntegerFieldAccess extends FieldAccess {
        public IntegerFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException, IllegalAccessException {
            write.writeInt(field.getInt(o));
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException, IllegalAccessException {
            field.setInt(o, read.readInt());
        }
    }

    static class IntArrayFieldAccess extends FieldAccess {
        public IntArrayFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalStateException, BufferOverflowException, IllegalArgumentException, IllegalAccessException {
            int[] array;
            array = (int[]) field.get(o);
            if (array == null) {
                write.writeInt(~0);
            } else {
                write.writeInt(array.length);
                for (int i = 0; i < array.length; i++)
                    write.writeInt(array[i]);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException, IllegalAccessException {
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
        }
    }

    static class FloatFieldAccess extends FieldAccess {
        public FloatFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException, IllegalAccessException {
            write.writeFloat(field.getFloat(o));
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException, IllegalAccessException {
            field.setFloat(o, read.readFloat());
        }
    }

    static class FloatArrayFieldAccess extends FieldAccess {
        public FloatArrayFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException, IllegalAccessException {
            float[] array;
            array = (float[]) field.get(o);
            if (array == null) {
                write.writeInt(~0);
            } else {
                write.writeInt(array.length);
                for (int i = 0; i < array.length; i++)
                    write.writeFloat(array[i]);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException, IllegalAccessException {
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
        }
    }

    static class LongFieldAccess extends FieldAccess {
        public LongFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException, IllegalAccessException {
            write.writeLong(field.getLong(o));
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException, IllegalAccessException {
            field.setLong(o, read.readLong());
        }
    }

    static class LongArrayFieldAccess extends FieldAccess {
        public LongArrayFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalStateException, BufferOverflowException, IllegalArgumentException, IllegalAccessException {
            long[] array;
            array = (long[]) field.get(o);
            if (array == null) {
                write.writeInt(~0);
            } else {
                write.writeInt(array.length);
                for (int i = 0; i < array.length; i++)
                    write.writeLong(array[i]);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException, IllegalAccessException {
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
        }
    }

    static class DoubleFieldAccess extends FieldAccess {
        public DoubleFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException, IllegalAccessException {
            write.writeDouble(field.getDouble(o));
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException, IllegalAccessException {
            field.setDouble(o, read.readDouble());
        }
    }

    static class DoubleArrayFieldAccess extends FieldAccess {
        public DoubleArrayFieldAccess(Field field) {
            super(field);
        }

        @Override
        protected void getValue(Object o, @NotNull BytesOut<?> write)
                throws IllegalStateException, IllegalArgumentException, BufferOverflowException, IllegalAccessException {
            double[] array;
            array = (double[]) field.get(o);
            if (array == null) {
                write.writeInt(~0);
            } else {
                write.writeInt(array.length);
                for (int i = 0; i < array.length; i++)
                    write.writeDouble(array[i]);
            }
        }

        @Override
        protected void setValue(Object o, @NotNull BytesIn<?> read)
                throws IllegalStateException, BufferUnderflowException, IllegalArgumentException, IllegalAccessException {
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
        }
    }
}

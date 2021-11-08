/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

@SuppressWarnings({"rawtypes", "unchecked"})
public interface BytesIn<U> extends
        RandomDataInput,
        StreamingDataInput<Bytes<U>>,
        ByteStringParser<Bytes<U>> {
    /**
     * Reads messages from this tails as methods.  It returns a BooleanSupplier which returns
     *
     * @param objects which implement the methods serialized to the file.
     * @return a reader which will read one Excerpt at a time
     */
    @NotNull
    default MethodReader bytesMethodReader(@NotNull Object... objects) {
        return new BytesMethodReaderBuilder(this).build(objects);
    }

    @NotNull
    default BytesMethodReaderBuilder bytesMethodReaderBuilder() {
        return new BytesMethodReaderBuilder(this);
    }

    <T extends ReadBytesMarshallable> T readMarshallableLength16(@NotNull Class<T> tClass, @Nullable T using)
            throws BufferUnderflowException, IllegalStateException;

    default <T> T readObject(@NotNull Class<T> componentType0)
            throws BufferUnderflowException, IllegalArgumentException, IllegalStateException, ArithmeticException, BufferOverflowException {
        Class<T> componentType = ObjectUtils.implementationToUse(componentType0);
        if (BytesMarshallable.class.isAssignableFrom(componentType)) {
            BytesMarshallable bm = (BytesMarshallable) ObjectUtils.newInstance(componentType);
            bm.readMarshallable(this);
            return (T) bm;
        }
        if (Enum.class.isAssignableFrom(componentType)) {
            return (T) readEnum((Class) componentType);
        }
        switch (componentType.getName()) {
            case "java.lang.String":
                return (T) readUtf8();
            case "java.lang.Double":
                return (T) (Double) readDouble();
            case "java.lang.Long":
                return (T) (Long) readLong();
            case "java.lang.Integer":
                return (T) (Integer) readInt();

            default:
                throw new UnsupportedOperationException("Unsupported " + componentType);
        }
    }
}

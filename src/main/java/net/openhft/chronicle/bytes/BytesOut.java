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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

@SuppressWarnings({"rawtypes", "unchecked"})
public interface BytesOut<Underlying> extends
        ByteStringAppender<Bytes<Underlying>>,
        BytesPrepender<Bytes<Underlying>>,
        BytesComment<BytesOut<Underlying>> {

    /**
     * Proxy an interface so each message called is written to a file for replay.
     *
     * @param tClass     primary interface
     * @param additional any additional interfaces
     * @return a proxy which implements the primary interface (additional interfaces have to be
     * cast)
     */
    @NotNull
    default <T> T bytesMethodWriter(@NotNull Class<T> tClass, Class... additional) {
        Class[] interfaces = ObjectUtils.addAll(tClass, additional);

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), interfaces,
                new BinaryBytesMethodWriterInvocationHandler(MethodEncoderLookup.BY_ANNOTATION, this));
    }

    @Deprecated(/*is it used?*/)
    @NotNull
    default <T> MethodWriterBuilder<T> bytesMethodWriterBuilder(Function<Method, MethodEncoder> methodEncoderFunction, @NotNull Class<T> tClass) {
        return new BytesMethodWriterBuilder<>(tClass, new BinaryBytesMethodWriterInvocationHandler(methodEncoderFunction, this));
    }

    void writeMarshallableLength16(WriteBytesMarshallable marshallable);

    /**
     * Write a limit set of writeObject types.
     *
     * @param componentType expected.
     * @param obj           of componentType
     */
    default void writeObject(Class componentType, Object obj) {
        if (!componentType.isInstance(obj))
            throw new IllegalArgumentException("Cannot serialize " + obj.getClass() + " as an " + componentType);
        if (obj instanceof BytesMarshallable) {
            ((BytesMarshallable) obj).writeMarshallable(this);
            return;
        }
        if (obj instanceof Enum) {
            writeEnum((Enum) obj);
            return;
        }
        if (obj instanceof BytesStore) {
            BytesStore bs = (BytesStore) obj;
            writeStopBit(bs.readRemaining());
            write(bs);
            return;
        }
        switch (componentType.getName()) {
            case "java.lang.String":
                writeUtf8((String) obj);
                return;
            case "java.lang.Double":
                writeDouble((Double) obj);
                return;
            case "java.lang.Long":
                writeLong((Long) obj);
                return;
            case "java.lang.Integer":
                writeInt((Integer) obj);
                return;

            default:
                throw new UnsupportedOperationException("Not supported " + componentType);
        }
    }

}

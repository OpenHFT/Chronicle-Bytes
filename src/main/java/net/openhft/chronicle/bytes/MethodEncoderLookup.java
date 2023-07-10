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

import net.openhft.chronicle.core.util.Annotations;

import java.lang.reflect.Method;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.function.Function;

/**
 * Enum singleton that implements Function interface to lookup and return a {@link MethodEncoder} for a given method.
 * It applies the {@link MethodId} annotation found on the method to create the encoder.
 * If the method does not have the {@link MethodId} annotation, it will return null.
 * <p>
 * The returned encoder can then be used to encode method calls into {@link BytesOut} and decode method calls from {@link BytesIn},
 * which can be used for serialization or for sending method calls over a network for example.
 * The encoder supports objects that are instances of {@link BytesMarshallable}.
 * <p>
 * This enum is primarily used for encoding and decoding methods annotated with {@link MethodId} for efficient method representation.
 */
public enum MethodEncoderLookup implements Function<Method, MethodEncoder> {
    BY_ANNOTATION;

    @Override
    public MethodEncoder apply(Method method) {
        MethodId methodId = Annotations.getAnnotation(method, MethodId.class);
        if (methodId == null) return null;
        long messageId = methodId.value();
        return new MethodEncoder() {
            @Override
            public long messageId() {
                return messageId;
            }

            @Override
            public void encode(Object[] objects, BytesOut<?> out)
                    throws IllegalArgumentException, BufferUnderflowException, IllegalStateException, BufferOverflowException, ArithmeticException {
                for (Object object : objects) {
                    if (object instanceof BytesMarshallable) {
                        ((BytesMarshallable) object).writeMarshallable(out);
                        continue;
                    }
                    throw new IllegalArgumentException("Object type " + object + " not supported");
                }
            }

            @Override
            public Object[] decode(Object[] lastObjects, BytesIn<?> in)
                    throws BufferUnderflowException, IllegalStateException {
                for (Object lastObject : lastObjects)
                    ((BytesMarshallable) lastObject).readMarshallable(in);
                return lastObjects;
            }
        };
    }
}

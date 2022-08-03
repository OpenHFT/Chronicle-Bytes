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

            @SuppressWarnings("rawtypes")
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

            @SuppressWarnings("rawtypes")
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

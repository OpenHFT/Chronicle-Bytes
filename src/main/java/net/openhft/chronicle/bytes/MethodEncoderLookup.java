/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;

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
        MethodId methodId = Jvm.findAnnotation(method, MethodId.class);
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

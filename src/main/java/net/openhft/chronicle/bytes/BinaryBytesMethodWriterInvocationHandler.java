/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.util.AbstractInvocationHandler;

import java.lang.reflect.Method;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * {@link java.lang.reflect.InvocationHandler} that serialises method calls to a
 * {@link BytesOut} using a {@link MethodEncoder} per method. Intended for proxy
 * based one-way messaging.
 */
public class BinaryBytesMethodWriterInvocationHandler extends AbstractInvocationHandler implements BytesMethodWriterInvocationHandler {
    private final Function<Method, MethodEncoder> methodToId;
    private final BytesOut<?> out;
    private final Map<Method, MethodEncoder> methodToIdMap = new LinkedHashMap<>();

    /**
     * Creates an instance for the supplied interface.
     *
     * @param tClass     interface being proxied
     * @param methodToId lookup supplying a {@link MethodEncoder} per method
     * @param out        target stream for the encoded calls
     */
    public BinaryBytesMethodWriterInvocationHandler(Class<?> tClass, Function<Method, MethodEncoder> methodToId, BytesOut<?> out) {
        super(tClass);
        this.methodToId = methodToId;
        this.out = out;
    }

    /**
     * Encodes the invocation and writes it to {@link #out}. On failure the
     * write position is rolled back to preserve stream integrity.
     */
    @Override
    @SuppressWarnings("java:S1181") // Reset writePosition on any Throwable to preserve stream integrity; rethrow immediately
    protected Object doInvoke(Object proxy, Method method, Object[] args)
            throws IllegalStateException, BufferOverflowException, BufferUnderflowException, IllegalArgumentException, ArithmeticException, InvalidMarshallableException {
        MethodEncoder info = methodToIdMap.computeIfAbsent(method, methodToId);
        if (info == null) {
            Jvm.warn().on(getClass(), "Unknown method " + method + " ignored");
        } else {
            long pos = out.writePosition();
            try {
                out.writeHexDumpDescription(method.getName());
                out.writeStopBit(info.messageId());
                info.encode(args, out);
            } catch (Throwable t) {
                out.writePosition(pos);
                throw t;
            }
        }
        return null;
    }
}

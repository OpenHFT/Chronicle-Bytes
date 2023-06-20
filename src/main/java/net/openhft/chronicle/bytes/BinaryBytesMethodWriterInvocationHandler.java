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
 * Invocation handler that encodes method invocations as binary data written to a {@link BytesOut}.
 * This class is typically used in conjunction with a proxy to provide a binary "wire" protocol representation
 * of calls to an interface's methods.
 */
public class BinaryBytesMethodWriterInvocationHandler extends AbstractInvocationHandler implements BytesMethodWriterInvocationHandler {
    private final Function<Method, MethodEncoder> methodToId;
    private final BytesOut<?> out;
    private final Map<Method, MethodEncoder> methodToIdMap = new LinkedHashMap<>();

    /**
     * Create a new invocation handler.
     *
     * @param tClass    The type of the interface to be handled.
     * @param methodToId A function mapping Methods to corresponding {@link MethodEncoder} instances.
     * @param out      The output stream to which encoded invocations should be written.
     */
    public BinaryBytesMethodWriterInvocationHandler(Class tClass, Function<Method, MethodEncoder> methodToId, BytesOut<?> out) {
        super(tClass);
        this.methodToId = methodToId;
        this.out = out;
    }

    /**
     * Handle a method invocation on a proxy instance. The method invocation is encoded and written to the {@link BytesOut}.
     *
     * @param proxy  The proxy instance on which the method was invoked.
     * @param method The method that was invoked.
     * @param args   The arguments provided to the method.
     * @return Always null.
     * @throws IllegalStateException if the underlying bytes store is closed.
     * @throws BufferOverflowException if there isn't enough space in the {@link BytesOut} to write the value.
     * @throws BufferUnderflowException if the underlying bytes store cannot provide enough data.
     * @throws IllegalArgumentException if the arguments don't match the method's requirements.
     * @throws ArithmeticException if numeric values overflow or underflow.
     * @throws InvalidMarshallableException if a marshalling error occurs.
     */
    @Override
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

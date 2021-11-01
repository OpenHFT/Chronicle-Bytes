/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.AbstractInvocationHandler;

import java.lang.reflect.Method;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class BinaryBytesMethodWriterInvocationHandler extends AbstractInvocationHandler implements BytesMethodWriterInvocationHandler {
    private final Function<Method, MethodEncoder> methodToId;
    @SuppressWarnings("rawtypes")
    private final BytesOut out;
    private final Map<Method, MethodEncoder> methodToIdMap = new LinkedHashMap<>();

    @SuppressWarnings("rawtypes")
    public BinaryBytesMethodWriterInvocationHandler(Function<Method, MethodEncoder> methodToId, BytesOut out) {
        super(HashMap::new);
        this.methodToId = methodToId;
        this.out = out;
    }

    @Override
    protected Object doInvoke(Object proxy, Method method, Object[] args)
            throws IllegalStateException, BufferOverflowException, BufferUnderflowException, IllegalArgumentException, ArithmeticException {
        MethodEncoder info = methodToIdMap.computeIfAbsent(method, methodToId);
        if (info == null) {
            Jvm.warn().on(getClass(), "Unknown method " + method + " ignored");
        } else {
            out.comment(method.getName());
            out.writeStopBit(info.messageId());
            info.encode(args, out);
        }
        return null;
    }
}

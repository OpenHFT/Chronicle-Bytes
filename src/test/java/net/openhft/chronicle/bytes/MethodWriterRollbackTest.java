/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class MethodWriterRollbackTest extends BytesTestCommon {

    interface Failer {
        void go() throws Throwable; // declare Throwable to test non-Exception path
    }

    @Test
    public void writePositionIsRolledBackOnThrowable() {
        Bytes<?> out = Bytes.allocateElasticOnHeap(64);
        try {
            Function<Method, MethodEncoder> failing = m -> new MethodEncoder() {
                @Override
                public long messageId() {
                    return 1L;
                }

                @Override
                public void encode(Object[] args, BytesOut<?> bytesOut)
                        throws IllegalArgumentException, BufferUnderflowException, IllegalStateException, BufferOverflowException, ArithmeticException, InvalidMarshallableException {
                    bytesOut.writeInt(0xDEADBEEF); // advance position
                    net.openhft.chronicle.core.Jvm.rethrow(new Throwable("boom"));
                }

                @Override
                public Object[] decode(Object[] lastObjects, BytesIn<?> in) {
                    return lastObjects;
                }
            };
            BinaryBytesMethodWriterInvocationHandler h = new BinaryBytesMethodWriterInvocationHandler(
                    Failer.class, failing, out);
            @SuppressWarnings("unchecked")
            Failer proxy = (Failer) Proxy.newProxyInstance(
                    Failer.class.getClassLoader(), new Class<?>[]{Failer.class}, h);

            long pos0 = out.writePosition();
            assertThrows(Throwable.class, proxy::go);
            assertEquals("write position must be restored on failure", pos0, out.writePosition());
        } finally {
            out.releaseLast();
        }
    }
}

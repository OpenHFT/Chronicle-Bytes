/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests method writer rollback behaviour because write position restoration
 * on failure is essential to avoid partial writes corrupting the output
 * stream.
 */
@DisplayName("Method writer rollback restores write position")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class MethodWriterRollbackTest extends BytesTestCommon {

    interface Failer {
        void go() throws Throwable; // declare Throwable to test non-Exception path
    }

    @Test
    @DisplayName("write position is rolled back on throwable")
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
            assertThrows(Throwable.class, proxy::go,
                    "Method writer should rethrow failing invocation");
            assertEquals(pos0, out.writePosition(),
                    "Write position must be restored on failure");
        } finally {
            out.releaseLast();
        }
    }
}

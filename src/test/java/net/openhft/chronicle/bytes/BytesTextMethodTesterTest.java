/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings("deprecation")
public class BytesTextMethodTesterTest extends BytesTestCommon {
    @BeforeEach
    public void directEnabled() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @Test
    public void run()
            throws IOException {
        assumeFalse(NativeBytes.areNewGuarded());
        BytesTextMethodTester<IBytesMethod> tester = btmttTest("btmtt/prim-input.txt", "btmtt/prim-output.txt");
        assertEquals(tester.expected(), tester.actual(), "run: expected output matches actual");
    }

    @Test
    public void runInvalid()
            throws IOException {
        // invalid on read
        expectException(ek -> ek.throwable instanceof InvalidMarshallableException, "InvalidMarshallableException");
        // invalid on write
        expectException("Exception calling public void net.openhft.chronicle.bytes.BytesTextMethodTesterTest$IBMImpl.myByteable");
        assumeFalse(NativeBytes.areNewGuarded());

        BytesTextMethodTester<IBytesMethod> tester = btmttTest("btmtt-invalid/prim-input.txt", "btmtt-invalid/prim-output.txt");
        assertEquals(tester.expected(), tester.actual(), "runInvalid: expected output matches actual");
    }

    private BytesTextMethodTester<IBytesMethod> btmttTest(String input, String output)
            throws IOException {
        BytesTextMethodTester<IBytesMethod> tester = new BytesTextMethodTester<>(
                input,
                IBMImpl::new,
                IBytesMethod.class,
                output);
        tester.run();
        return tester;
    }

    static class IBMImpl implements IBytesMethod {
        final IBytesMethod out;

        IBMImpl(IBytesMethod out) {
            this.out = out;
        }

        @Override
        public void myByteable(MyByteable byteable) throws InvalidMarshallableException {
            byteable.b = (byte) byteable.s;
            out.myByteable(byteable);
        }

        @Override
        public void myScalars(MyScalars scalars) {
            out.myScalars(scalars);
        }

        @Override
        public void myNested(MyNested nested) {
            out.myNested(nested);
        }
    }
}

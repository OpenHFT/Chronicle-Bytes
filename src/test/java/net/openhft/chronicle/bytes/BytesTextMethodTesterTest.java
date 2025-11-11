/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assume.assumeFalse;

public class BytesTextMethodTesterTest extends BytesTestCommon {
    @Before
    public void directEnabled() {
        assumeFalse(Jvm.maxDirectMemory() == 0);
    }

    @Test
    public void run()
            throws IOException {
        assumeFalse(NativeBytes.areNewGuarded());
        btmttTest("btmtt/prim-input.txt", "btmtt/prim-output.txt");
    }

    @Test
    public void runInvalid()
            throws IOException {
        // invalid on read
        expectException(ek -> ek.throwable instanceof InvalidMarshallableException, "InvalidMarshallableException");
        // invalid on write
        expectException("Exception calling public void net.openhft.chronicle.bytes.BytesTextMethodTesterTest$IBMImpl.myByteable");
        assumeFalse(NativeBytes.areNewGuarded());

        btmttTest("btmtt-invalid/prim-input.txt", "btmtt-invalid/prim-output.txt");
    }

    private void btmttTest(String input, String output)
            throws IOException {
        BytesTextMethodTester<IBytesMethod> tester = new BytesTextMethodTester<>(
                input,
                IBMImpl::new,
                IBytesMethod.class,
                output);
        tester.run();
        assertEquals(tester.expected(), tester.actual());
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

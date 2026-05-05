/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings({"deprecation", "PMD.JUnit5TestShouldBePackagePrivate"})
@DisplayName("BytesTextMethodTester integration workflow scenarios for text IO")
class BytesTextMethodTesterTest extends BytesTestCommon {
    @BeforeEach
    public void directEnabled() {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Direct memory must be available for BytesTextMethodTester tests");
    }

    @Test
    @DisplayName("Run valid text method tester input and output")
    public void run()
            throws IOException {
        assumeFalse(NativeBytes.areNewGuarded(), "Guarded bytes output format differs for valid scenario");
        btmttTest("btmtt/prim-input.txt", "btmtt/prim-output.txt");
    }

    @Test
    @DisplayName("Run invalid text method tester input and output")
    public void runInvalid()
            throws IOException {
        // invalid on read
        expectException(ek -> ek.throwable instanceof InvalidMarshallableException, "InvalidMarshallableException");
        // invalid on write
        expectException("Exception calling public void net.openhft.chronicle.bytes.BytesTextMethodTesterTest$IBMImpl.myByteable");
        assumeFalse(NativeBytes.areNewGuarded(), "Guarded bytes output format differs for invalid scenario");

        btmttTest("btmtt-invalid/prim-input.txt", "btmtt-invalid/prim-output.txt");
    }

    @Test
    @DisplayName("Run with setup and post-processing using component arrays")
    public void runWithSetupAndAfterRun()
            throws IOException {
        assumeFalse(NativeBytes.areNewGuarded(), "Guarded bytes output format differs for setup scenario");
        BytesTextMethodTester<IBytesMethod> tester = new BytesTextMethodTester<>(
                "btmtt/prim-input.txt",
                writer -> new Object[]{new IBMImpl(writer)},
                IBytesMethod.class,
                "btmtt/prim-output.txt");
        tester.setup("btmtt/prim-input.txt");
        tester.afterRun(text -> text.replace("\r", ""));
        tester.run();
        assertEquals(tester.expected(),
                tester.actual(),
                "Text method tester output should match expected output after setup run");
    }

    private void btmttTest(String input, String output)
            throws IOException {
        BytesTextMethodTester<IBytesMethod> tester = new BytesTextMethodTester<>(
                input,
                IBMImpl::new,
                IBytesMethod.class,
                output);
        tester.run();
        assertEquals(tester.expected(), tester.actual(), "Text method tester output should match expected output");
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

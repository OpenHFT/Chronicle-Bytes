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

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assume.assumeFalse;

public class BytesTextMethodTesterTest extends BytesTestCommon {
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

    protected void btmttTest(String input, String output)
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

package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;

public class BytesTextMethodTesterTest {
    @Test
    public void run() throws IOException {
        btmttTest("btmtt/prim-input.txt", "btmtt/prim-output.txt");
    }

    @SuppressWarnings("rawtypes")
    protected void btmttTest(String input, String output) throws IOException {
        BytesTextMethodTester tester = new BytesTextMethodTester<>(
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
        public void myByteable(MyByteable byteable) {
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
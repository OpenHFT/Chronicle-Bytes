package net.openhft.chronicle.bytes;

import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;

public class BytesTextMethodTesterTest {
    @Test
    public void run() throws IOException {
        BytesTextMethodTester tester = new BytesTextMethodTester<>(
                "btmtt/prim-input.txt",
                IBMImpl::new,
                IBytesMethod.class,
                "btmtt/prim-output.txt");
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
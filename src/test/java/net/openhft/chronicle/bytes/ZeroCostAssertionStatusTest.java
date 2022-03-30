package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.util.AssertUtil;
import org.junit.jupiter.api.Test;

class ZeroCostAssertionStatusTest {

    @Test
    void show() {
        boolean ae = false;
        try {
            assert 0 != 0;
        } catch (AssertionError assertionError) {
            ae = true;
        }

        System.out.println("Normal assertions are " + (ae ? "ON" : "OFF"));
        System.out.println("Zero-cost assertions are " + (AssertUtil.SKIP_ASSERTIONS ? "OFF" : "ON"));
    }

}
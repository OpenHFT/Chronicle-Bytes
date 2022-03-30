package net.openhft.chronicle.bytes.internal;

import org.junit.jupiter.api.Test;

class ZeroCostSupportTest {

    @Test
    void isZeroCostAssertionsEnabled() {
        System.out.println("isZeroCostAssertionsEnabled " + new ZeroCostSupport().isZeroCostAssertionsEnabled());
    }
}
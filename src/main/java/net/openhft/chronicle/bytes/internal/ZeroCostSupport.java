package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.core.util.AssertUtil;

class ZeroCostSupport {

    boolean isZeroCostAssertionsEnabled() {
        return !AssertUtil.SKIP_ASSERTIONS;
    }

}

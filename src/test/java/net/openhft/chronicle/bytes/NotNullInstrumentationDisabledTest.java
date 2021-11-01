package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

class NotNullInstrumentationDisabledTest {

    @Test
    void assertNotNullInstrumentation() {
        try {
            foo(null);
        } catch (Throwable t) {
            fail("Instrumentaion of @NotNull is enabled. Please disable this in your environment. For IntelliJ, this can be done under Preferences/Compiler/Add runtime assertions...");
        }
    }

    void foo(@NotNull String bar) {
    }

}
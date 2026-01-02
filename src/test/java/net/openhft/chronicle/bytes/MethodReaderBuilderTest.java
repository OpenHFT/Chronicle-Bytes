/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.mockito.Mockito.*;

@DisplayName("MethodReaderBuilder warns on missing method handler behaviour")
public class MethodReaderBuilderTest {

    @BeforeEach
    public void setup() {
        assumeFalse(Jvm.isJava21Plus(),
                "Mockito real method calls fail on Java 21");
    }

    @Test
    @DisplayName("warnMissing toggles exception handler configuration flag")
    public void testWarnMissing() {
        MethodReaderBuilder builder = mock(MethodReaderBuilder.class, Mockito.CALLS_REAL_METHODS);

        when(builder.exceptionHandlerOnUnknownMethod(any())).thenReturn(builder);

        builder.warnMissing(true);

        verify(builder).exceptionHandlerOnUnknownMethod(Jvm.warn());

        builder.warnMissing(false);
        verify(builder).exceptionHandlerOnUnknownMethod(Jvm.debug());
    }
}

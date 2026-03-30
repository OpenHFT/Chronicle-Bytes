/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assumptions.*;

class MethodReaderBuilderTest {

    @BeforeEach
    void setup() {
        assumeFalse(Jvm.isJava21Plus());
    }

    @Test
    void testWarnMissing() {
        MethodReaderBuilder builder = mock(MethodReaderBuilder.class, Mockito.CALLS_REAL_METHODS);

        when(builder.exceptionHandlerOnUnknownMethod(any())).thenReturn(builder);

        builder.warnMissing(true);

        verify(builder).exceptionHandlerOnUnknownMethod(Jvm.warn());

        builder.warnMissing(false);
        verify(builder).exceptionHandlerOnUnknownMethod(Jvm.debug());
    }
}

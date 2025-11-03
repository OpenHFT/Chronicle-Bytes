/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import java.util.function.Predicate;

import static org.junit.Assume.assumeFalse;
import static org.mockito.Mockito.*;

public class MethodReaderBuilderTest {

    @Before
    public void setup() {
        assumeFalse(Jvm.isJava21Plus());
    }

    @Test
    public void testWarnMissing() {
        MethodReaderBuilder builder = mock(MethodReaderBuilder.class, Mockito.CALLS_REAL_METHODS);

        when(builder.exceptionHandlerOnUnknownMethod(any())).thenReturn(builder);

        builder.warnMissing(true);

        verify(builder).exceptionHandlerOnUnknownMethod(Jvm.warn());

        builder.warnMissing(false);
        verify(builder).exceptionHandlerOnUnknownMethod(Jvm.debug());
    }
}

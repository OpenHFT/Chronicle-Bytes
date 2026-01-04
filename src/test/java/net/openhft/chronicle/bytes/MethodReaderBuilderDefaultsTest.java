/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.onoes.ExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("MethodReaderBuilder default methods select expected handlers")
public class MethodReaderBuilderDefaultsTest {

    @Test
    @DisplayName("warnMissing selects warn and debug handlers")
    public void warnMissingSelectsExpectedHandlers() {
        CapturingBuilder builder = new CapturingBuilder();

        builder.warnMissing(true);
        assertSame(Jvm.warn(),
                builder.handler(),
                "warnMissing(true) should use the warn exception handler");

        builder.warnMissing(false);
        assertSame(Jvm.debug(),
                builder.handler(),
                "warnMissing(false) should use the debug exception handler");
    }

    @Test
    @DisplayName("predicate default returns the same builder instance")
    public void predicateDefaultReturnsSameBuilder() {
        CapturingBuilder builder = new CapturingBuilder();
        Predicate<MethodReader> predicate = reader -> true;

        MethodReaderBuilder result = builder.predicate(predicate);
        assertSame(builder,
                result,
                "predicate should return the same builder instance");
    }

    private static final class CapturingBuilder implements MethodReaderBuilder {
        private ExceptionHandler handler;

        @Override
        public MethodReaderBuilder methodReaderInterceptorReturns(MethodReaderInterceptorReturns methodReaderInterceptorReturns) {
            return this;
        }

        @Override
        public MethodReaderBuilder exceptionHandlerOnUnknownMethod(ExceptionHandler exceptionHandler) {
            handler = exceptionHandler;
            return this;
        }

        @Override
        public MethodReaderBuilder metaDataHandler(Object... components) {
            return this;
        }

        @Override
        public MethodReader build(Object... components) {
            return null;
        }

        private ExceptionHandler handler() {
            return handler;
        }
    }
}

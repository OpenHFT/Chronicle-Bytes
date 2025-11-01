/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.onoes.ExceptionHandler;

import java.util.function.Predicate;

/**
 * Interface for a builder that constructs instances of MethodReader.
 * <p>
 * The builder provides various options to customize the creation of a MethodReader,
 * including setting interceptors, handling unknown methods, handling metadata,
 * and setting a predicate to control the reading of messages.
 */
public interface MethodReaderBuilder {

    /**
     * Sets an interceptor for method calls made through the MethodReader.
     *
     * @param methodReaderInterceptorReturns the interceptor instance
     * @return the builder instance for method chaining
     */
    MethodReaderBuilder methodReaderInterceptorReturns(MethodReaderInterceptorReturns methodReaderInterceptorReturns);

    /**
     * Sets the level of logging for missing methods.
     *
     * @param warnMissing if {@code true}, warnings will be logged for missing methods;
     *                    if {@code false}, debug-level messages will be logged instead
     * @return the builder instance for method chaining
     */
    default MethodReaderBuilder warnMissing(boolean warnMissing) {
        return exceptionHandlerOnUnknownMethod(warnMissing ? Jvm.warn() : Jvm.debug());
    }

    /**
     * Sets the ExceptionHandler instance to use when an unknown method is encountered.
     * Use this to control how the builder handles such methods.
     *
     * @param exceptionHandler the ExceptionHandler instance
     * @return the builder instance for method chaining
     */
    default MethodReaderBuilder exceptionHandlerOnUnknownMethod(ExceptionHandler exceptionHandler) {
        return this;
    }

    /**
     * Sets the handlers for processing metadata messages.
     *
     * @param components the metadata handlers
     * @return the builder instance for method chaining
     */
    MethodReaderBuilder metaDataHandler(Object... components);

    /**
     * Builds the MethodReader instance with the specified components.
     *
     * @param components the components for the MethodReader
     * @return the built MethodReader instance
     */
    MethodReader build(Object... components);

    /**
     * Sets a predicate to be used by the readOne() method of the MethodReader.
     * This predicate determines whether a message should be read or not.
     * If the predicate returns {@code false}, readOne() will not read the message.
     * This feature can be used for flow control.
     *
     * @param predicate the predicate to set
     * @return the builder instance for method chaining
     */
    default MethodReaderBuilder predicate(Predicate<MethodReader> predicate) {
        return this;
    }
}

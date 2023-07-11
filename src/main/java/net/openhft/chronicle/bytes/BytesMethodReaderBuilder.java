/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.Mocker;
import net.openhft.chronicle.core.onoes.ExceptionHandler;
import net.openhft.chronicle.core.util.IgnoresEverything;

import static net.openhft.chronicle.bytes.internal.ReferenceCountedUtil.throwExceptionIfReleased;

/**
 * Concrete implementation of MethodReaderBuilder for constructing BytesMethodReader instances.
 * This builder offers several methods for customizing the creation of a BytesMethodReader,
 * including setting the methodEncoderLookup function, the default BytesParselet,
 * and the level of logging for unknown methods.
 */
public class BytesMethodReaderBuilder implements MethodReaderBuilder {
    private final BytesIn<?> in;
    private BytesParselet defaultParselet;
    private MethodEncoderLookup methodEncoderLookup = MethodEncoderLookup.BY_ANNOTATION;
    private ExceptionHandler exceptionHandlerOnUnknownMethod = Jvm.debug();

    /**
     * Constructor for BytesMethodReaderBuilder.
     *
     * @param in the BytesIn object from which serialized method calls are read.
     * @throws IllegalStateException if the provided BytesIn object is released
     */
    public BytesMethodReaderBuilder(BytesIn<?> in) {
        throwExceptionIfReleased(in);
        this.in = in;
    }

    /**
     * Sets the ExceptionHandler instance to use when an unknown method is encountered.
     * This instance controls how the builder handles unknown methods.
     *
     * @param exceptionHandler the ExceptionHandler instance
     * @return the builder instance for method chaining
     */
    @Override
    public MethodReaderBuilder exceptionHandlerOnUnknownMethod(ExceptionHandler exceptionHandler) {
        this.exceptionHandlerOnUnknownMethod = exceptionHandler;
        return this;
    }

    /**
     * Sets the level of logging for unknown methods.
     * <p>
     * This method is deprecated and will always return {@code this}, effectively making it a no-op.
     *
     * @param warnMissing if {@code true}, warnings will be logged for unknown methods;
     *                    if {@code false}, debug-level messages will be logged instead
     * @return the builder instance for method chaining
     */
    @Override
    public MethodReaderBuilder warnMissing(boolean warnMissing) {
        // always true
        return this;
    }

    /**
     * Returns the current MethodEncoderLookup function.
     *
     * @return the current MethodEncoderLookup function
     */
    public MethodEncoderLookup methodEncoderLookup() {
        return methodEncoderLookup;
    }

    /**
     * Sets the MethodEncoderLookup function for this builder.
     *
     * @param methodEncoderLookup the MethodEncoderLookup function
     * @return the builder instance for method chaining
     */
    public BytesMethodReaderBuilder methodEncoderLookup(MethodEncoderLookup methodEncoderLookup) {
        this.methodEncoderLookup = methodEncoderLookup;
        return this;
    }

    /**
     * Returns the default BytesParselet for this builder.
     * If not set, a default BytesParselet is initialized.
     *
     * @return the default BytesParselet
     */
    public BytesParselet defaultParselet() {
        if (defaultParselet == null)
            initDefaultParselet();

        return defaultParselet;
    }

    private void initDefaultParselet() {
        if (exceptionHandlerOnUnknownMethod instanceof IgnoresEverything)
            defaultParselet = Mocker.ignored(BytesParselet.class);
        else
            defaultParselet = (msg, in) -> {
                Bytes<?> bytes = (Bytes) in;
                exceptionHandlerOnUnknownMethod.on(getClass(), "Unknown message type " + msg + " " + bytes.toHexString());
            };
    }

    /**
     * Sets the default BytesParselet for this builder.
     *
     * @param defaultParselet the default BytesParselet
     * @return the builder instance for method chaining
     */
    public BytesMethodReaderBuilder defaultParselet(BytesParselet defaultParselet) {
        this.defaultParselet = defaultParselet;
        return this;
    }

    /**
     * Throws an UnsupportedOperationException when called.
     * This method is required to fulfill the MethodReaderBuilder interface, but it is not supported
     * in the BytesMethodReaderBuilder class.
     *
     * @return nothing
     * @throws UnsupportedOperationException always
     */
    @Override
    public MethodReaderBuilder methodReaderInterceptorReturns(MethodReaderInterceptorReturns methodReaderInterceptorReturns) {
        throw new UnsupportedOperationException();
    }

    /**
     * Throws an UnsupportedOperationException when called.
     * This method is required to fulfill the MethodReaderBuilder interface, but it is not supported
     * in the BytesMethodReaderBuilder class.
     *
     * @return nothing
     * @throws UnsupportedOperationException always
     */
    @Override
    public MethodReaderBuilder metaDataHandler(Object... components) {
        throw new UnsupportedOperationException();
    }

    /**
     * Constructs the BytesMethodReader instance with the specified components.
     *
     * @param objects the components for the BytesMethodReader
     * @return the built BytesMethodReader instance
     */
    public BytesMethodReader build(Object... objects) {
        return new BytesMethodReader(in, defaultParselet(), methodEncoderLookup, objects);
    }
}

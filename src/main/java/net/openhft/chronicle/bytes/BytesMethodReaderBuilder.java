/*
 * Copyright 2016-2025 chronicle.software
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
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.onoes.ExceptionHandler;
import net.openhft.chronicle.core.util.IgnoresEverything;
import net.openhft.chronicle.core.util.Mocker;

import static net.openhft.chronicle.bytes.internal.ReferenceCountedUtil.throwExceptionIfReleased;

/**
 * Builder for {@link BytesMethodReader} instances. It allows configuration of
 * the lookup mechanism for method decoders ({@link MethodEncoderLookup}), the
 * handler for unknown method calls ({@link BytesParselet}) and logging
 * behaviour for unrecognised methods. Implements {@link MethodReaderBuilder}.
 */
public class BytesMethodReaderBuilder implements MethodReaderBuilder {
    private final BytesIn<?> in;
    private BytesParselet defaultParselet;
    private MethodEncoderLookup methodEncoderLookup = MethodEncoderLookup.BY_ANNOTATION;
    private ExceptionHandler exceptionHandlerOnUnknownMethod = Jvm.debug();

    /**
     * @param in the {@link BytesIn} stream from which serialised method calls
     *           will be read. Must not be {@code null}.
     * @throws NullPointerException        if {@code in} is {@code null}
     * @throws ClosedIllegalStateException if {@code in} has been released
     * @throws ThreadingIllegalStateException if {@code in} is accessed by
     *                                        multiple threads unsafely
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
     * @return this builder for chained invocation
     */
    @Override
    public MethodReaderBuilder exceptionHandlerOnUnknownMethod(ExceptionHandler exceptionHandler) {
        this.exceptionHandlerOnUnknownMethod = exceptionHandler;
        return this;
    }

    /**
     * Returns the currently configured {@link MethodEncoderLookup} strategy used
     * to find decoders for method calls.
     */
    public MethodEncoderLookup methodEncoderLookup() {
        return methodEncoderLookup;
    }

    /**
     * Sets the MethodEncoderLookup function for this builder.
     *
     * @param methodEncoderLookup the MethodEncoderLookup function
     * @return this builder for chained invocation
     */
    public BytesMethodReaderBuilder methodEncoderLookup(MethodEncoderLookup methodEncoderLookup) {
        this.methodEncoderLookup = methodEncoderLookup;
        return this;
    }

    /**
     * Returns the {@link BytesParselet} to use when a message ID is encountered
     * for which no specific handler is registered. If not explicitly set,
     * it is initialised based on the
     * {@link #exceptionHandlerOnUnknownMethod(ExceptionHandler)} configuration.
     */
    public BytesParselet defaultParselet() {
        if (defaultParselet == null)
            initDefaultParselet();

        return defaultParselet;
    }

    /**
     * Initialises {@link #defaultParselet} depending on whether unknown methods
     * should be ignored or logged.
     */
    private void initDefaultParselet() {
        if (exceptionHandlerOnUnknownMethod instanceof IgnoresEverything)
            defaultParselet = Mocker.ignored(BytesParselet.class);
        else
            defaultParselet = (msg, in) -> {
                Bytes<?> bytes = (Bytes<?>) in;
                exceptionHandlerOnUnknownMethod.on(getClass(), "Unknown message type " + msg + " " + bytes.toHexString());
            };
    }

    /**
     * Sets the default BytesParselet for this builder.
     *
     * @param defaultParselet the default BytesParselet
     * @return this builder for chained invocation
     */
    public BytesMethodReaderBuilder defaultParselet(BytesParselet defaultParselet) {
        this.defaultParselet = defaultParselet;
        return this;
    }

    /**
     * This builder does not support interceptors for method reader returns.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public MethodReaderBuilder methodReaderInterceptorReturns(MethodReaderInterceptorReturns methodReaderInterceptorReturns) {
        throw new UnsupportedOperationException();
    }

    /**
     * This builder does not support metadata handlers.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public MethodReaderBuilder metaDataHandler(Object... components) {
        throw new UnsupportedOperationException();
    }

    /**
     * Constructs and returns a new {@link BytesMethodReader} configured with the
     * settings from this builder and the provided handler {@code objects}.
     *
     * @param objects the target objects whose methods will be invoked
     * @return a new configured {@link BytesMethodReader}
     */
    public BytesMethodReader build(Object... objects) {
        return new BytesMethodReader(in, defaultParselet(), methodEncoderLookup, objects);
    }
}

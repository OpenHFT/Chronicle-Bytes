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

public class BytesMethodReaderBuilder implements MethodReaderBuilder {
    private final BytesIn<?> in;
    private BytesParselet defaultParselet;
    private MethodEncoderLookup methodEncoderLookup = MethodEncoderLookup.BY_ANNOTATION;
    private ExceptionHandler exceptionHandlerOnUnknownMethod = Jvm.debug();

    public BytesMethodReaderBuilder(BytesIn<?> in) {
        throwExceptionIfReleased(in);
        this.in = in;
    }

    @Override
    public MethodReaderBuilder exceptionHandlerOnUnknownMethod(ExceptionHandler exceptionHandler) {
        this.exceptionHandlerOnUnknownMethod = exceptionHandler;
        return this;
    }

    public MethodEncoderLookup methodEncoderLookup() {
        return methodEncoderLookup;
    }

    public BytesMethodReaderBuilder methodEncoderLookup(MethodEncoderLookup methodEncoderLookup) {
        this.methodEncoderLookup = methodEncoderLookup;
        return this;
    }

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

    public BytesMethodReaderBuilder defaultParselet(BytesParselet defaultParselet) {
        this.defaultParselet = defaultParselet;
        return this;
    }

    @Override
    public MethodReaderBuilder methodReaderInterceptorReturns(MethodReaderInterceptorReturns methodReaderInterceptorReturns) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MethodReaderBuilder metaDataHandler(Object... components) {
        throw new UnsupportedOperationException();
    }

    public BytesMethodReader build(Object... objects) {
        return new BytesMethodReader(in, defaultParselet(), methodEncoderLookup, objects);
    }
}

/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("rawtypes")
public class BytesMethodReaderBuilder implements MethodReaderBuilder {
    private final BytesIn in;
    private BytesParselet defaultParselet = createDefaultParselet();
    private MethodEncoderLookup methodEncoderLookup = MethodEncoderLookup.BY_ANNOTATION;

    public BytesMethodReaderBuilder(BytesIn in) {
        this.in = in;
    }

    @NotNull
    static BytesParselet createDefaultParselet() {
        return (msg, in) -> {
            Bytes bytes = (Bytes) in;
            Jvm.rethrow(new IllegalArgumentException("Unknown message type " + msg + " " + bytes.toHexString()));
        };
    }

    @Override
    public MethodReaderBuilder warnMissing(boolean warnMissing) {
        // always true
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
        return defaultParselet;
    }

    public BytesMethodReaderBuilder defaultParselet(BytesParselet defaultParselet) {
        this.defaultParselet = defaultParselet;
        return this;
    }

    @Override
    public MethodReaderBuilder methodReaderInterceptorReturns(MethodReaderInterceptorReturns methodReaderInterceptorReturns) {
        throw new UnsupportedOperationException();
    }

    public BytesMethodReader build(Object... objects) {
        return new BytesMethodReader(in, defaultParselet, methodEncoderLookup, objects);
    }
}

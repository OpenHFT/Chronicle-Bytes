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
import net.openhft.chronicle.core.onoes.ExceptionHandler;

import java.util.function.Predicate;

/**
 * Builder for MethodReaders
 */
public interface MethodReaderBuilder {

    /**
     * Interceptor for methods called
     */
    MethodReaderBuilder methodReaderInterceptorReturns(MethodReaderInterceptorReturns methodReaderInterceptorReturns);

    default MethodReaderBuilder warnMissing(boolean warnMissing) {
        return exceptionHandlerOnUnknownMethod(warnMissing ? Jvm.warn() : Jvm.debug());
    }

    /**
     * setter to determine how unknown methods are logged or ExceptionHandler.ignoresEverything()
     *
     * @param exceptionHandler to call
     * @return this
     */
    default MethodReaderBuilder exceptionHandlerOnUnknownMethod(ExceptionHandler exceptionHandler) {
        return this;
    }

    /**
     * Handler for meta data messages
     *
     * @param components to call
     * @return this
     */
    MethodReaderBuilder metaDataHandler(Object... components);

    /**
     * Build a MethodReader using the following components to call
     *
     * @param components to call
     * @return this
     */
    MethodReader build(Object... components);

    default MethodReaderBuilder predicate(Predicate<?> predicate) {
        return this;
    }
}

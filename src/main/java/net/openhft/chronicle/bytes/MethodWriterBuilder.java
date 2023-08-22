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

import net.openhft.chronicle.core.io.Closeable;

import java.util.function.Supplier;

/**
 * Interface for creating a builder that will generate a MethodWriter instance of a specific type.
 * A MethodWriter is used to write methods into a binary stream.
 *
 * @param <T> the type of the MethodWriter that will be built by this builder
 */

public interface MethodWriterBuilder<T> extends Supplier<T> {

    /**
     * Specifies the identifier for generic events.
     * A generic event employs the first argument as the method name, providing a flexible way to write arbitrary methods at runtime.
     *
     * @param genericEvent the identifier used for generic events
     * @return this builder instance, allowing for method chaining
     */
    MethodWriterBuilder<T> genericEvent(String genericEvent);

    /**
     * Controls whether metadata should be included in the binary stream.
     *
     * @param metaData true if metadata should be included; false otherwise
     * @return this builder, so invocations can be chained
     */
    default MethodWriterBuilder<T> metaData(boolean metaData) {
        return this;
    }

    /**
     * Specifies a Closeable that should be invoked when the MethodWriter is closed.
     *
     * @param closeable the Closeable to invoke when the MethodWriter is closed
     * @return this builder, so invocations can be chained
     */
    MethodWriterBuilder<T> onClose(Closeable closeable);

    /**
     * Specifies an UpdateInterceptor for the MethodWriter.
     *
     * @param updateInterceptor the UpdateInterceptor to use
     * @return this builder, so invocations can be chained
     * @throws UnsupportedOperationException if the implementation does not support this operation
     */
    default MethodWriterBuilder<T> updateInterceptor(UpdateInterceptor updateInterceptor) {
        throw new UnsupportedOperationException();
    }

    /**
     * Controls whether type information should be included in a verbose manner.
     *
     * @param verboseTypes true if type information should be verbose; false otherwise
     * @return this builder, so invocations can be chained
     */
    default MethodWriterBuilder<T> verboseTypes(boolean verboseTypes) {
        return this;
    }

    /**
     * Builds the MethodWriter instance.
     *
     * @return the built MethodWriter instance
     */
    default T build() {
        return get();
    }
}

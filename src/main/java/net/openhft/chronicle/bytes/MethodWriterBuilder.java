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

import net.openhft.chronicle.core.io.Closeable;

import java.util.function.Supplier;

public interface MethodWriterBuilder<T> extends Supplier<T> {

    MethodWriterBuilder<T> genericEvent(String genericEvent);

    default MethodWriterBuilder<T> metaData(boolean metaData) {
        return this;
    }

    MethodWriterBuilder<T> onClose(Closeable closeable);

    default MethodWriterBuilder<T> updateInterceptor(UpdateInterceptor updateInterceptor) {
        throw new UnsupportedOperationException();
    }

    default T build() {
        return get();
    }
}

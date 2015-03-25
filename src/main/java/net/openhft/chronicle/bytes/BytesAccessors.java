/*
 * Copyright 2014 Higher Frequency Trading http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

final class BytesAccessors {

    static class Unchecked<S extends BytesStore<S, U>, U> implements Accessor.Full<S, U> {
        static final Generic INSTANCE = new Generic();

        @Override
        public Access<U> access(S source) {
            return source.access();
        }

        @Override
        public U handle(S source) {
            return source.accessHandle();
        }

        @Override
        public long offset(S source, long index) {
            return source.accessOffset(index);
        }
    }

    static class Generic<S extends BytesStore> implements Accessor.Full<S, S> {
        static final Generic INSTANCE = new Generic();

        @Override
        public Access<S> access(S source) {
            return BytesAccesses.Full.INSTANCE;
        }

        @Override
        public S handle(S source) {
            return source;
        }

        @Override
        public long offset(S source, long index) {
            return index;
        }
    }

    private BytesAccessors() {}
}

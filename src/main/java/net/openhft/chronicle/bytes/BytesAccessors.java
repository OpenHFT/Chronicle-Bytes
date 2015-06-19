/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

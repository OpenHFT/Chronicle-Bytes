/*
 * Copyright 2016-2020 Chronicle Software
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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

@SuppressWarnings("rawtypes")
public abstract class AbstractReference extends AbstractCloseable implements Byteable, Closeable {

    @Nullable
    protected BytesStore bytes;
    protected long offset;

    @Override
    public void bytesStore(@NotNull final BytesStore bytes, final long offset, final long length) throws IllegalStateException, IllegalArgumentException, BufferOverflowException, BufferUnderflowException {
        throwExceptionIfClosedInSetter();

        acceptNewBytesStore(bytes);
        this.offset = offset;
    }

    @Nullable
    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public abstract long maxSize();

    protected void acceptNewBytesStore(@NotNull final BytesStore bytes) {
        if (this.bytes != null) {
            this.bytes.release(this);
        }
        this.bytes = bytes.bytesStore();
        this.bytes.reserve(this);
    }

    @Override
    protected void performClose() {
        if (this.bytes != null) {
            this.bytes.release(this);
            this.bytes = null;
        }
    }

    public long address() {
        throwExceptionIfClosed();

        return bytesStore().addressForRead(offset);
    }

/* TODO FIX
        @Override
    protected void finalize() throws Throwable {
        warnIfNotClosed();
        super.finalize();
    }*/

    @Override
    protected boolean threadSafetyCheck(boolean isUsed) {
        // References are thread safe
        return true;
    }
}

/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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
package net.openhft.chronicle.bytes.pool;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IOTools;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("rawtypes")
public class BytesPool {
    final ThreadLocal<Bytes<?>> bytesTL = new ThreadLocal<>();

    public Bytes<?> acquireBytes() {
        Bytes<?> bytes = bytesTL.get();
        if (bytes != null) {
            try {
                return bytes.clear();
            } catch (IllegalStateException e) {
                // ignored
            }
        } else {
            bytes = createBytes();
            bytesTL.set(bytes);
        }
        return bytes;
    }

    @NotNull
    protected Bytes<?> createBytes() {
        Bytes<?> bbb = Bytes.allocateElasticDirect(256);
        IOTools.unmonitor(bbb);
        return bbb;
    }
}

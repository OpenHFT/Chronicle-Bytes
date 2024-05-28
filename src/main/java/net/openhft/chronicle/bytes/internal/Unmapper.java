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
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class Unmapper implements Runnable {
    private final long size;

    private final int pageSize;

    private volatile long address;

    public Unmapper(long address, long size, int pageSize) throws IllegalStateException {

        assert (address != 0);
        this.address = address;
        this.size = size;
        this.pageSize = pageSize;
    }

    @Override
    public void run() {
        if (address == 0)
            return;

        try {
            OS.unmap(address, size, pageSize);
            address = 0;

        } catch (@NotNull IOException e) {
            Jvm.warn().on(OS.class, "Error on unmap and release", e);
        }
    }
}

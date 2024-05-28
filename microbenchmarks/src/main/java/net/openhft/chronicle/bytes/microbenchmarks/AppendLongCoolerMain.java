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
package net.openhft.chronicle.bytes.microbenchmarks;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.cooler.CoolerTester;
import net.openhft.chronicle.core.cooler.CpuCoolers;
import org.jetbrains.annotations.NotNull;

public class AppendLongCoolerMain {

    static int i = 0;
    static long[] longs = {Integer.MIN_VALUE, -128, 0, 1, 11, 111, Integer.MAX_VALUE, Long.MAX_VALUE};

    public static void main(String[] args) {
        Bytes bytes = Bytes.allocateElasticDirect(32);

        new CoolerTester(
                CpuCoolers.PAUSE1,
                CpuCoolers.BUSY100,
                CpuCoolers.ALL
        ).add("write", () -> doWrite(bytes))
                .run();
    }

    @NotNull
    public static Object doWrite(Bytes<?> bytes) {
        bytes.clear();
        bytes.append(longs[i++ % longs.length]);
        return bytes;
    }

    @NotNull
    public static Object doRead(Bytes<?> bytes) {
        bytes.readPosition(0);
        double v = bytes.parseDouble();
        if (v < 0) throw new AssertionError();
        return bytes;
    }

    @NotNull
    public static Object doTest(Bytes<?> bytes) {
        doWrite(bytes);
        doRead(bytes);
        return bytes;
    }
}

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

public class BytesCoolerMain {

    public static void main(String[] args) {
        Bytes small = Bytes.allocateDirect(23);
        Bytes big = Bytes.allocateDirect(400);

        System.out.println("WITH COOLERS ACROSS MEMORY");
        new CoolerTester(
                CpuCoolers.BUSY100,
                CpuCoolers.PAUSE1,
                CpuCoolers.ALL
        )
                .add("direct", () -> doWrite(big, 21))
                .add("small", () -> doWrite(small, 3))
                .run();
    }

    @NotNull
    public static Object doWrite(Bytes bytes, int digits) {
        bytes.clear();
        bytes.append(123.456, digits);
        return bytes;
    }

    @NotNull
    public static Object doRead(Bytes bytes) {
        bytes.readPosition(0);
        double v = bytes.parseDouble();
        if (v < 0) throw new AssertionError();
        return bytes;
    }

    @NotNull
    public static Object doTest(Bytes bytes) {
        doWrite(bytes, 3);
        doRead(bytes);
        return bytes;
    }
}

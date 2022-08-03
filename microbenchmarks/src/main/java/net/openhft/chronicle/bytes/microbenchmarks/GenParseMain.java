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

import net.openhft.affinity.Affinity;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.cooler.CoolerTester;
import net.openhft.chronicle.core.cooler.CpuCooler;
import net.openhft.chronicle.core.cooler.CpuCoolers;

import java.util.concurrent.Callable;

/*
conservative power, no affinity

performance power no affinity

conservative power, affinity
simple BUSY100 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.15 / 0.17  0.19 / 0.25  0.39 / 0.78  0.97 / 0.97 - 0.97
simple PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 1.4 / 1.9  2.0 / 2.1  2.1 / 22  25 / 28 - 419
bytes BUSY100 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.085 / 0.091  0.096 / 0.11  0.36 / 0.46  0.92 / 0.92 - 0.92
bytes PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.73 / 0.98  1.0 / 1.0  1.1 / 1.2  22 / 23 - 33

performance power, affinity
simple BUSY100 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.19 / 0.22  0.24 / 0.32  0.69 / 0.81  1.1 / 1.1 - 1.1
simple PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.38 / 0.50  1.0 / 1.1  1.3 / 1.4  13 / 15 - 226
bytes BUSY100 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.095 / 0.11  0.11 / 0.16  0.36 / 0.65  0.65 / 0.65 - 0.65
bytes PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.26 / 0.34  0.59 / 0.66  0.71 / 0.75  12 / 13 - 16

no HT, affinity
simple BUSY100 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.18 / 0.22  0.22 / 0.25  0.39 / 1.3  9.6 / 9.6 - 9.6
simple PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.39 / 1.0  1.0 / 1.3  1.3 / 9.6  12 / 13 - 223
bytes BUSY100 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.10 / 0.11  0.11 / 0.12  0.24 / 0.35  0.54 / 0.54 - 0.54
bytes PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.37 / 0.57  0.59 / 0.69  0.73 / 0.76  12 / 12 - 21

warm HT, affinity
simple BUSY100 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.19 / 0.22  0.23 / 0.25  0.26 / 0.89  1.0 / 1.0 - 1.0
simple PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.14 / 0.16  0.17 / 0.21  0.27 / 0.54  5.1 / 7.2 - 13
bytes BUSY100 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.12 / 0.12  0.13 / 0.13  0.26 / 0.34  0.38 / 0.38 - 0.38
bytes PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.11 / 0.11  0.12 / 0.13  0.18 / 0.24  0.50 / 2.4 - 7.5
0.5ms PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.11 / 0.12  0.12 / 0.12  0.13 / 0.16  0.19 / 0.65 - 6.4
1ms   PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.12 / 0.13  0.14 / 0.14  0.15 / 0.20  0.35 / 2.7 - 6.5
10ms  PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.13 / 0.13  0.14 / 0.15  0.15 / 0.16  0.20 / 1.8 - 7.6
nocallPAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.13 / 0.15  0.15 / 0.16  0.16 / 0.17  0.19 / 1.5 - 8.3
14no  PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.085 / 0.093  0.096 / 0.099  0.10 / 0.13  0.16 / 0.57 - 5.9

hot HT
simple BUSY100 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.12 / 0.14  0.15 / 0.18  0.29 / 0.64  0.68 / 0.68 - 0.68
simple PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.15 / 0.16  0.16 / 0.17  0.17 / 0.19  0.22 / 2.3 - 136
PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.095 / 0.12  0.13 / 0.14  0.15 / 0.15  0.18 / 4.7 - 136
PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.096 / 0.11  0.13 / 0.14  0.14 / 0.14  0.16 / 3.5 - 136

bytes BUSY_3 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.098 / 0.11  0.12 / 0.12  0.12 / 0.13  0.13 / 0.13 - 5.0
bytes BUSY - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.089 / 0.098  0.10 / 0.11  0.11 / 0.12  0.12 / 0.12 - 7.2
bytes BUSY1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.067 / 0.069  0.076 / 0.083  0.092 / 0.10  0.11 / 0.13 - 5.7
bytes BUSY10 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.065 / 0.069  0.072 / 0.080  0.086 / 0.11  0.23 / 0.33 - 0.75
bytes BUSY100 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.066 / 0.068  0.083 / 0.10  0.22 / 0.35  0.45 / 0.45 - 0.45
bytes BUSY1000 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.068 / 0.084  0.12 / 0.21  0.99 / 1.0  1.0 / 1.0 - 1.0
bytes PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.065 / 0.068  0.074 / 0.076  0.082 / 0.086  0.091 / 0.13 - 6.1
bytes PAUSE1 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.064 / 0.068  0.069 / 0.082  0.083 / 0.095  0.096 / 0.11 - 7.5
bytes PAUSE10 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.069 / 0.072  0.084 / 0.085  0.086 / 0.095  0.21 / 0.46 - 0.96
bytes PAUSE100 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.073 / 0.077  0.086 / 0.093  0.19 / 0.49  0.93 / 0.93 - 0.93
bytes PAUSE1000 - 50/90 97/99 99.7/99.9 99.97/99.99 - worst was 0.066 / 0.080  0.090 / 0.14  0.83 / 0.89  0.89 / 0.89 - 0.89
*/
public class GenParseMain {
    static CodeNumber cn = new CodeNumber();
    static String input;
    static Bytes<?> bytes = Bytes.allocateDirect(32);
    static CodeNumber cn2 = new CodeNumber();
    static Bytes<?> input2;
    static Bytes<?> bytes2 = Bytes.allocateDirect(32);
    static CodeNumber cn3 = new CodeNumber();

    public static void main(String[] args) {
        Affinity.setAffinity(7);
        cn.ch = 'N';
        cn.number = 1234567890L;
        input = simpleEncode(cn);
        input2 = Bytes.from(input);
        Thread thread = new Thread(() -> {
//            Affinity.setAffinity(14);
            Callable callable = () -> bytesDecode(bytesEncode(cn, bytes2), cn3);
//            Callable callable = () -> simpleDecode(simpleEncode(cn));
            try {
                Thread thread1 = Thread.currentThread();
                while (!thread1.isInterrupted()) {
//                    Jvm.busyWaitMicros(1000);
//                    callable.call();
                }
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });
        thread.setDaemon(true);
        thread.start();
        new CoolerTester(new CpuCooler[]{CpuCoolers.PAUSE1000})
//                .add("encode", () -> bytesEncode(cn))
//                .add("decode", () -> bytesDecode(input2.readPosition(0)))
//                .add("simple", () -> simpleDecode(simpleEncode(cn)))
                .add("bytes", () -> bytesDecode(bytesEncode(cn, bytes), cn2))
                .repeat(30)
                .run();
    }

    public static String simpleEncode(CodeNumber cn) {
        return "" + cn.ch + cn.number;
    }

    public static CodeNumber simpleDecode(String input) {
        CodeNumber cn = new CodeNumber();
        cn.ch = input.charAt(0);
        cn.number = Long.parseLong(input.substring(1));
        return cn;
    }

    public static Bytes bytesEncode(CodeNumber cn, Bytes<?> bytes) {
        return bytes.clear().append(cn.ch).append(cn.number);
    }

    static CodeNumber bytesDecode(Bytes<?> input, CodeNumber cn2) {
        cn2.ch = (char) input.readUnsignedByte();
        GenParseMain.cn2.number = input.parseLong();
        return GenParseMain.cn2;
    }

    static class MyString {
        final char[] chars;

        public MyString(char[] chars) {
            this.chars = chars;
        }

//        char charAt
    }

    static class CodeNumber {
        char ch;
        long number;
    }
}

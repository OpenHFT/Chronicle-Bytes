/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.microbenchmarks;

import net.openhft.affinity.Affinity;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.cooler.CoolerTester;
import net.openhft.chronicle.core.cooler.CpuCooler;
import net.openhft.chronicle.core.cooler.CpuCoolers;

import java.util.concurrent.Callable;

/**
 * Experimental harness comparing {@link String} and {@link Bytes} parsing paths.
 */
/*
 * Benchmark notes recorded in the project docs.
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

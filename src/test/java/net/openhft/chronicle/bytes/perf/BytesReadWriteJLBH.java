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
package net.openhft.chronicle.bytes.perf;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.util.NanoSampler;
import net.openhft.chronicle.jlbh.JLBH;
import net.openhft.chronicle.jlbh.JLBHOptions;
import net.openhft.chronicle.jlbh.JLBHTask;
import net.openhft.chronicle.jlbh.TeamCityHelper;

import java.util.HashMap;
import java.util.Map;

import static java.lang.System.setProperty;

public class BytesReadWriteJLBH implements JLBHTask {

    public static final int SHORT_LENGTH = 37;
    public static final int LONG_LENGTH = 1_024;
    private static final int ARRAY_SIZE = 1024 * 50;
    private static final byte[] BYTE_ARRAY = new byte[ARRAY_SIZE];
    private static final int ITERATIONS = 50_000;

    private final Bytes<?> bytesImpl;
    private JLBH jlbh;
    private NanoSampler absoluteRead;
    private NanoSampler absoluteWrite;
    private NanoSampler relativeRead;
    private NanoSampler relativeWrite;
    private NanoSampler readASCII;
    private NanoSampler writeASCII;
    private NanoSampler copyTo;
    private NanoSampler readUTF8;
    private NanoSampler writeUTF8;
    private NanoSampler writeASCIIAsUTF8;
    private NanoSampler readASCIIAsUTF8;
    private Bytes<?> targetBytes;
    private int length;

    public BytesReadWriteJLBH(Bytes<?> bytes, int length) {
        this.bytesImpl = bytes;
        this.length = length;
    }

    public static void runForBytes(Bytes<?> bytes) {
        runForBytes(bytes, LONG_LENGTH);
    }

    public static void runForBytes(Bytes<?> bytes, int length) {
        setProperty("jvm.resource.tracing", "false");
        new JLBH(new JLBHOptions()
                .warmUpIterations(15_000)
                .iterations(ITERATIONS)
                .runs(3)
                .recordOSJitter(false)
                .throughput(5_000)
                .pauseAfterWarmupMS(100)
                .accountForCoordinatedOmission(true)
                .jlbhTask(new BytesReadWriteJLBH(bytes, length)))
                .start();
    }

    @Override
    public void init(JLBH jlbh) {
        this.jlbh = jlbh;
        absoluteRead = jlbh.addProbe("absoluteRead");
        absoluteWrite = jlbh.addProbe("absoluteWrite");
        relativeRead = jlbh.addProbe("relativeRead");
        relativeWrite = jlbh.addProbe("relativeWrite");
        readASCII = jlbh.addProbe("readASCII");
        writeASCII = jlbh.addProbe("writeASCII");
        readASCIIAsUTF8 = jlbh.addProbe("readASCIIAsUTF8");
        writeASCIIAsUTF8 = jlbh.addProbe("writeASCIIAsUTF8");
        readUTF8 = jlbh.addProbe("readUTF8");
        writeUTF8 = jlbh.addProbe("writeUTF8");
        copyTo = jlbh.addProbe("copyTo");
        targetBytes = Bytes.allocateDirect(
                Math.max(CharacterEncoding.ASCII.writeString(bytesImpl, length), CharacterEncoding.UTF8.writeString(bytesImpl, length)));
    }

    @Override
    public void complete() {
        TeamCityHelper.teamCityStatsLastRun(getClass().getSimpleName() + "-" + bytesImpl.getClass().getSimpleName(), jlbh, ITERATIONS, System.out);
    }

    @Override
    public void run(long startTimeNS) {
        populateBytes(CharacterEncoding.ASCII);
        bytesImpl.read(0, BYTE_ARRAY, 0, length);

        // Absolute reads
        populateBytes(CharacterEncoding.ASCII);
        long absoluteReadStart = System.nanoTime();
        long amountRead = bytesImpl.read(0, BYTE_ARRAY, 0, length);
        absoluteRead.sampleNanos(System.nanoTime() - absoluteReadStart);
        expectAmountActioned("read", length, amountRead);


        // Relative reads
        populateBytes(CharacterEncoding.ASCII);
        bytesImpl.readPosition(0);
        long relativeReadStart = System.nanoTime();
        amountRead = bytesImpl.read(BYTE_ARRAY, 0, length);
        relativeRead.sampleNanos(System.nanoTime() - relativeReadStart);
        expectAmountActioned("read", length, amountRead);

        // Absolute writes
        bytesImpl.clear();
        long absoluteWriteStart = System.nanoTime();
        bytesImpl.write(0, BYTE_ARRAY, 0, length);
        absoluteWrite.sampleNanos(System.nanoTime() - absoluteWriteStart);
        if (bytesImpl.readByte(length - 1) != BYTE_ARRAY[length - 1]) {
            throw new IllegalStateException("Didn't write expected amount");
        }

        // Relative writes
        bytesImpl.clear();
        long relativeWriteStart = System.nanoTime();
        bytesImpl.write(BYTE_ARRAY, 0, length);
        relativeWrite.sampleNanos(System.nanoTime() - relativeWriteStart);
        expectAmountActioned("write", length, bytesImpl.writePosition());

        // read ASCII
        populateBytes(CharacterEncoding.ASCII);
        targetBytes.clear();
        long readASCIIStart = System.nanoTime();
        bytesImpl.read8bit(targetBytes);
        readASCII.sampleNanos(System.nanoTime() - readASCIIStart);
        expectAmountActioned("read", CharacterEncoding.ASCII.lengthOfEncodedStringWithoutLength(length), targetBytes.writePosition());

        // write ASCII
        bytesImpl.clear();
        long writeASCIIStart = System.nanoTime();
        bytesImpl.write8bit(CharacterEncoding.ASCII.stringForLength(length));
        writeASCII.sampleNanos(System.nanoTime() - writeASCIIStart);
        expectAmountActioned("write", CharacterEncoding.ASCII.lengthOfEncodedString(length), bytesImpl.writePosition());

        // read ASCII as UTF8
        populateBytes(CharacterEncoding.ASCII);
        targetBytes.clear();
        long readASCIIAsUTF8Start = System.nanoTime();
        bytesImpl.readUtf8(targetBytes);
        readASCIIAsUTF8.sampleNanos(System.nanoTime() - readASCIIAsUTF8Start);
        expectAmountActioned("read", CharacterEncoding.ASCII.lengthOfEncodedStringWithoutLength(length), targetBytes.writePosition());

        // write ASCII as UTF8
        bytesImpl.clear();
        long wruteASCIIAsUTF8Start = System.nanoTime();
        bytesImpl.writeUtf8(CharacterEncoding.ASCII.stringForLength(length));
        writeASCIIAsUTF8.sampleNanos(System.nanoTime() - wruteASCIIAsUTF8Start);
        expectAmountActioned("write", CharacterEncoding.ASCII.lengthOfEncodedString(length), bytesImpl.writePosition());

        // read UTF-8
        populateBytes(CharacterEncoding.UTF8);
        targetBytes.clear();
        long readUTF8Start = System.nanoTime();
        bytesImpl.readUtf8(targetBytes);
        readUTF8.sampleNanos(System.nanoTime() - readUTF8Start);
        expectAmountActioned("read", CharacterEncoding.UTF8.lengthOfEncodedStringWithoutLength(length), targetBytes.writePosition());

        // write UTF-8
        bytesImpl.clear();
        long writeUTF8Start = System.nanoTime();
        bytesImpl.writeUtf8(CharacterEncoding.UTF8.stringForLength(length));
        writeUTF8.sampleNanos(System.nanoTime() - writeUTF8Start);
        expectAmountActioned("write", CharacterEncoding.UTF8.lengthOfEncodedString(length), bytesImpl.writePosition());

        // copyTo (not optimised by any implementation)
        populateBytes(CharacterEncoding.ASCII);
        long copyToStart = System.nanoTime();
        int copied = bytesImpl.copyTo(BYTE_ARRAY);
        copyTo.sampleNanos(System.nanoTime() - copyToStart);
        expectAmountActioned("copy", CharacterEncoding.ASCII.lengthOfEncodedString(length), copied);

        jlbh.sampleNanos(System.nanoTime() - startTimeNS);
    }

    private void expectAmountActioned(String action, long expected, long actual) {
        if (expected != actual) {
            throw new IllegalStateException("Didn't " + action + " expected amount: expected=" + expected + " actual=" + actual);
        }
    }

    private byte byteForIndex(int i) {
        return (byte) (i % Byte.MAX_VALUE);
    }

    private void populateBytes(CharacterEncoding characterEncoding) {
        // fill the bytes with an appropriately encoded string
        bytesImpl.clear();
        long lengthOfEncodedString = characterEncoding.writeString(bytesImpl, length);
        bytesImpl.writeSkip(lengthOfEncodedString);

        // populate the array
        for (int i = 0; i < BYTE_ARRAY.length; i++) {
            BYTE_ARRAY[i] = byteForIndex(i);
        }
    }

    /**
     * Generates and cache strings of specific lengths in the specified encoding
     */
    private enum CharacterEncoding {
        ASCII('0', 'z') {
            @Override
            long writeString(Bytes<?> bytes, String string) {
                return bytes.write8bit(0, string, 0, string.length());
            }
        },

        UTF8('Έ', 'Ͽ') {
            @Override
            long writeString(Bytes<?> bytes, String string) {
                return bytes.writeUtf8(0, string);
            }
        };

        private Map<Integer, String> stringsForLength = new HashMap<>();
        private Map<Integer, Integer> lengthOfEncodedStrings = new HashMap<>();
        private Map<Integer, Integer> lengthOfEncodedStringsWithoutLength = new HashMap<>();
        private final char firstChar;
        private final char lastChar;

        CharacterEncoding(char firstChar, char lastChar) {
            this.firstChar = firstChar;
            this.lastChar = lastChar;
        }

        abstract long writeString(Bytes<?> bytes, String string);

        public long writeString(Bytes<?> bytes, int length) {
            return writeString(bytes, stringForLength(length));
        }

        /**
         * Generate a string in this encoding of a specific byte-length
         *
         * @param lengthInBytes the length in bytes to generate to
         * @return the generated string
         */
        public String stringForLength(int lengthInBytes) {
            if (stringsForLength.containsKey(lengthInBytes)) {
                return stringsForLength.get(lengthInBytes);
            }
            Bytes<?> bytes = Bytes.allocateElasticOnHeap();
            StringBuilder stringBuilder = new StringBuilder();
            int i = 0;
            long lengthOfEncodedString = 0;
            while (lengthOfEncodedString < lengthInBytes) {
                stringBuilder.append(charForIndex(i++));
                lengthOfEncodedString = writeString(bytes.clear(), stringBuilder.toString());
            }
            stringsForLength.put(lengthInBytes, stringBuilder.toString());
            lengthOfEncodedStrings.put(lengthInBytes, (int) lengthOfEncodedString);
            // We need to know how big the length of the length is (https://github.com/OpenHFT/RFC/blob/master/Stop-Bit-Encoding/Stop-Bit-Encoding-1.0.adoc)
            bytes.writeSkip(lengthOfEncodedString).readPosition(0).readStopBit();
            lengthOfEncodedStringsWithoutLength.put(lengthInBytes, (int) (bytes.readRemaining()));
            return stringBuilder.toString();
        }

        /**
         * The length of the string generated for the specified length in bytes
         *
         * @param length The length in bytes requested
         * @return The actual length generated (may not == length)
         */
        public int lengthOfEncodedString(int length) {
            stringForLength(length);
            return lengthOfEncodedStrings.get(length);
        }

        /**
         * The length of the string, without the stop-bit-encoded length prefix
         *
         * @param length The length in bytes requested
         * @return The actual length generated, excluding the length prefix
         */
        public int lengthOfEncodedStringWithoutLength(int length) {
            stringForLength(length);
            return lengthOfEncodedStringsWithoutLength.get(length);
        }

        private char charForIndex(int i) {
            int range = lastChar - firstChar;
            return (char) (firstChar + i % range);
        }
    }
}

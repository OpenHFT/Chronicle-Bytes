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
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The {@code BytesTextMethodTester} class is a utility for comparing the expected and actual results
 * of executing a method from a given interface that manipulates bytes. It utilizes hexadecimal data
 * from the input and output files to perform this comparison.
 *
 * @param <T> the type of the interface that includes the methods to be tested.
 */
@SuppressWarnings("rawtypes")
public class BytesTextMethodTester<T> {
    private final String input;
    private final Class<T> outputClass;
    private final String output;
    private final Function<T, Object> componentFunction;

    private String setup;
    private Function<String, String> afterRun;

    private String expected;
    private String actual;

    /**
     * Constructs a {@code BytesTextMethodTester} instance with the provided parameters.
     *
     * @param input             The input file containing hexadecimal data to feed into the methods of the class under test.
     * @param componentFunction A function that defines how to instantiate or manipulate the object that the class under test needs.
     * @param outputClass       The class of the interface that includes the methods to be tested.
     * @param output            The output file containing the expected hexadecimal results of the tested methods.
     */
    public BytesTextMethodTester(String input, Function<T, Object> componentFunction, Class<T> outputClass, String output) {
        this.input = input;
        this.outputClass = outputClass;
        this.output = output;
        this.componentFunction = componentFunction;
    }

    public String setup() {
        return setup;
    }

    @NotNull
    public BytesTextMethodTester setup(String setup) {
        this.setup = setup;
        return this;
    }

    public Function<String, String> afterRun() {
        return afterRun;
    }

    @NotNull
    public BytesTextMethodTester afterRun(UnaryOperator<String> afterRun) {
        this.afterRun = afterRun;
        return this;
    }

    /**
     * Runs the methods of the class under test using the hexadecimal data from the input file,
     * then compares the results with the expected output from the output file.
     *
     * @return The instance of {@code BytesTextMethodTester}, allowing for method chaining.
     * @throws IOException              If an I/O error occurs when reading the files or writing the results.
     * @throws IllegalArgumentException If an illegal argument is used.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws BufferUnderflowException If there is no more data left to read in the buffer.
     */
    @NotNull
    public BytesTextMethodTester run()
            throws IOException, IllegalArgumentException, ClosedIllegalStateException, BufferUnderflowException {

        Bytes<?> bytes2 = new HexDumpBytes();
        T writer = bytes2.bytesMethodWriter(outputClass);

        Object component = componentFunction.apply(writer);
        Object[] components = component instanceof Object[]
                ? (Object[]) component
                : new Object[]{component};

        if (setup != null) {
            Bytes<?> bytes0 = HexDumpBytes.fromText(BytesUtil.readFile(setup));

            BytesMethodReader reader0 = bytes0.bytesMethodReaderBuilder()
                    .defaultParselet(this::unknownMessageId)
                    .build(components);
            while (reader0.readOne()) {
                bytes2.clear();
            }
            bytes2.clear();
        }

        // expected
        expected = BytesUtil.readFile(output).toString().trim().replace("\r", "");

        Bytes<?> text = BytesUtil.readFile(input);
        for (String text2 : text.toString().split("###[^\n]*\n")) {
            if (text2.trim().length() <= 0)
                continue;
            Bytes<?> bytes = HexDumpBytes.fromText(text2);

            BytesMethodReader reader = bytes.bytesMethodReaderBuilder()
                    .defaultParselet(this::unknownMessageId)
                    .build(components);

            while (reader.readOne()) {
                if (bytes.readRemaining() > 1)
                    bytes2.writeHexDumpDescription("## End Of Message");
            }
            bytes.releaseLast();
            bytes2.writeHexDumpDescription("## End Of Block");
        }
        bytes2.writeHexDumpDescription("## End Of Test");

        actual = bytes2.toHexString().trim();
        if (afterRun != null) {
            expected = afterRun.apply(expected);
            actual = afterRun.apply(actual);
        }
        bytes2.releaseLast();
        return this;
    }

    private void unknownMessageId(long id, BytesIn<?> b) {
        Jvm.warn().on(getClass(), "Unknown message id " + Long.toHexString(id));
        b.readPosition(b.readLimit());
    }

    public String expected() {
        return expected;
    }

    public String actual() {
        return actual;
    }
}

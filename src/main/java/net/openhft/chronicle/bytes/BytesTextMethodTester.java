/*
 * Copyright 2016-2025 chronicle.software
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
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Utility for exercising an interface backed by {@link Bytes} using a textual description of the
 * method calls.  The input file typically contains a hex dump with optional comments describing
 * the invocations.  These calls are dispatched to a component supplied by {@code componentFunction}
 * and the resulting output is serialised to text and compared with the expected output file.
 * It is a convenient way to perform data driven tests of writer/reader style APIs.
 *
 * @param <T> interface type whose methods are recorded and verified
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
     * Creates a tester configured with the given files and component builder.
     *
     * @param input             path to the text (usually hex) file describing the method invocations
     * @param componentFunction function producing the component(s) that will process the invocations
     * @param outputClass       interface class defining the methods encoded in the files
     * @param output            path to the expected output file
     */
    public BytesTextMethodTester(String input, Function<T, Object> componentFunction, Class<T> outputClass, String output) {
        this.input = input;
        this.outputClass = outputClass;
        this.output = output;
        this.componentFunction = componentFunction;
    }

    /**
     * Returns the path to the optional setup file.
     */
    public String setup() {
        return setup;
    }

    /**
     * Sets a setup file to be processed before the main input.
     *
     * @param setup path to the setup file
     * @return this tester for chaining
     */
    @NotNull
    public BytesTextMethodTester setup(String setup) {
        this.setup = setup;
        return this;
    }

    /**
     * Returns the post-processing function applied to actual and expected output.
     */
    public Function<String, String> afterRun() {
        return afterRun;
    }

    /**
     * Sets a post-processing function applied to both expected and actual output before comparison.
     *
     * @param afterRun normalising function
     * @return this tester for chaining
     */
    @NotNull
    public BytesTextMethodTester afterRun(UnaryOperator<String> afterRun) {
        this.afterRun = afterRun;
        return this;
    }

    /**
     * Executes the test.  The input (and optional setup) files are parsed and the resulting
     * invocations dispatched to the component.  All calls made to the output writer are captured
     * and written back to text for comparison with the expected output file.  The processed
     * strings can be retrieved via {@link #expected()} and {@link #actual()}.
     *
     * @return this tester for chaining
     * @throws IOException if any of the files cannot be read
     */
    @NotNull
    public BytesTextMethodTester run()
            throws IOException {

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

    /**
     * Default handler invoked when a message id is not recognised while parsing the input stream.
     * The remaining bytes of that message are skipped and a warning is logged.
     */
    private void unknownMessageId(long id, BytesIn<?> b) {
        Jvm.warn().on(getClass(), "Unknown message id " + Long.toHexString(id));
        b.readPosition(b.readLimit());
    }

    /**
     * Returns the contents of the expected output file after optional post-processing.
     */
    public String expected() {
        return expected;
    }

    /**
     * Returns the text generated from running the component under test.
     */
    public String actual() {
        return actual;
    }
}

package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Function;

/*
 * Created by Peter Lawrey on 17/05/2017.
 */
public class BytesTextMethodTester<T> {
    private final String input;
    private final Class<T> outputClass;
    private final String output;
    private final Function<T, Object> componentFunction;
    private BiConsumer<BytesMethodReader, T> exceptionHandlerSetup;
    private String genericEvent;

    private String setup;
    private Function<String, String> afterRun;

    private String expected;
    private String actual;

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
    public BytesTextMethodTester afterRun(Function<String, String> afterRun) {
        this.afterRun = afterRun;
        return this;
    }

    @NotNull
    public BytesTextMethodTester run() throws IOException {

        Bytes<?> bytes2 = new HexDumpBytes();
        T writer = bytes2.bytesMethodWriter(outputClass);

        Object component = componentFunction.apply(writer);
        Object[] components = component instanceof Object[]
                ? (Object[]) component
                : new Object[]{component};

        if (setup != null) {
            Bytes bytes0 = HexDumpBytes.fromText(BytesUtil.readFile(setup));

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

        Bytes text = BytesUtil.readFile(input);
        for (String text2 : text.toString().split("###[^\n]*\n")) {
            if (text2.trim().length() <= 0)
                continue;
            Bytes bytes = HexDumpBytes.fromText(text2);

            BytesMethodReader reader = bytes.bytesMethodReaderBuilder()
                    .defaultParselet(this::unknownMessageId)
                    .build(components);

            while (reader.readOne()) {
                if (bytes.readRemaining() > 1)
                    bytes2.comment("## End Of Message");
            }
            bytes2.comment("## End Of Block");
        }
        bytes2.comment("## End Of Test");

        actual = bytes2.toHexString().trim();
        if (afterRun != null) {
            expected = afterRun.apply(expected);
            actual = afterRun.apply(actual);
        }
        return this;
    }

    private void unknownMessageId(long id, BytesIn b) {
        System.out.println("Unknown message id " + Long.toHexString(id));
        b.readPosition(b.readLimit());
    }

    public String expected() {
        return expected;
    }

    public String actual() {
        return actual;
    }
}

package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("rawtypes")
public class BytesMethodReaderBuilder implements MethodReaderBuilder {
    private final BytesIn in;
    private BytesParselet defaultParselet = createDefaultParselet();
    private MethodEncoderLookup methodEncoderLookup = MethodEncoderLookup.BY_ANNOTATION;

    public BytesMethodReaderBuilder(BytesIn in) {
        this.in = in;
    }

    @NotNull
    static BytesParselet createDefaultParselet() {
        return (msg, in) -> {
            Bytes bytes = (Bytes) in;
            throw new IllegalArgumentException("Unknown message type " + msg + " " + bytes.toHexString());
        };
    }

    @Override
    public MethodReaderBuilder warnMissing(boolean warnMissing) {
        // always true
        return this;
    }

    public MethodEncoderLookup methodEncoderLookup() {
        return methodEncoderLookup;
    }

    public BytesMethodReaderBuilder methodEncoderLookup(MethodEncoderLookup methodEncoderLookup) {
        this.methodEncoderLookup = methodEncoderLookup;
        return this;
    }

    public BytesParselet defaultParselet() {
        return defaultParselet;
    }

    public BytesMethodReaderBuilder defaultParselet(BytesParselet defaultParselet) {
        this.defaultParselet = defaultParselet;
        return this;
    }

    @Override
    public MethodReaderBuilder methodReaderInterceptor(MethodReaderInterceptor methodReaderInterceptor) {
        throw new UnsupportedOperationException();
    }

    public BytesMethodReader build(Object... objects) {
        return new BytesMethodReader(in, defaultParselet, methodEncoderLookup, objects);
    }
}

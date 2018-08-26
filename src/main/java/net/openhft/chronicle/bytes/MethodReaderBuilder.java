package net.openhft.chronicle.bytes;

public interface MethodReaderBuilder {
    MethodReaderBuilder methodReaderInterceptor(MethodReaderInterceptor methodReaderInterceptor);

    MethodReaderBuilder warnMissing(boolean warnMissing);

    MethodReader build(Object... components);
}

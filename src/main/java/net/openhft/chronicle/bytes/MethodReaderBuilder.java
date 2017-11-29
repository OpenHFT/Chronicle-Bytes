package net.openhft.chronicle.bytes;

public interface MethodReaderBuilder {
    MethodReaderBuilder methodReaderInterceptor(MethodReaderInterceptor methodReaderInterceptor);

    MethodReader build(Object... components);
}

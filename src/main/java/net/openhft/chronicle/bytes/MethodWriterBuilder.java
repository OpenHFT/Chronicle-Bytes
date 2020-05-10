package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.Closeable;

import java.util.function.Supplier;

public interface MethodWriterBuilder<T> extends Supplier<T> {
    @Deprecated(/* use methodWriterInterceptorReturns */)
    MethodWriterBuilder<T> methodWriterListener(MethodWriterListener methodWriterListener);

    MethodWriterBuilder<T> genericEvent(String genericEvent);

    default MethodWriterBuilder<T> metaData(boolean metaData) {
        return this;
    }

    MethodWriterBuilder<T> useMethodIds(boolean useMethodIds);

    MethodWriterBuilder<T> onClose(Closeable closeable);

    MethodWriterBuilder<T> recordHistory(boolean recordHistory);

    MethodWriterBuilder<T> methodWriterInterceptorReturns(MethodWriterInterceptorReturns writeInterceptor);

    default T build() {
        return get();
    }
}

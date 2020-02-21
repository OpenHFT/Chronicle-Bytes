package net.openhft.chronicle.bytes;

public interface MethodReaderBuilder {
    default MethodReaderBuilder methodReaderInterceptor(MethodReaderInterceptor methodReaderInterceptor) {
        return methodReaderInterceptorReturns((m, o, a, i) -> {
            methodReaderInterceptor.intercept(m, o, a, i);
            return null;
        });
    }

    MethodReaderBuilder methodReaderInterceptorReturns(MethodReaderInterceptorReturns methodReaderInterceptorReturns);

    MethodReaderBuilder warnMissing(boolean warnMissing);

    MethodReader build(Object... components);
}

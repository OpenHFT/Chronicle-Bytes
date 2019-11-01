package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.function.BiConsumer;

/*
 * Created by Jerry Shea 25/10/2017
 * <p>
 * Invoked around method writing allowing you to take action before or after method invocation,
 * or even not to call the method
 */
@FunctionalInterface
public interface MethodWriterInterceptor {

    static MethodWriterInterceptor of(@Nullable final MethodWriterListener methodWriterListener, @Nullable final MethodWriterInterceptor interceptor) {
        if (methodWriterListener == null && interceptor == null)
            throw new IllegalArgumentException("both methodWriterListener and interceptor are NULL");

        if (methodWriterListener == null)
            return (method, args, invoker) -> {
                interceptor.intercept(method, args, invoker);
                invoker.accept(method, args);
            };

        if (interceptor == null)
            return (method, args, invoker) -> {
                methodWriterListener.onWrite(method.getName(), args);
                invoker.accept(method, args);
            };

        return (method, args, invoker) -> {
            interceptor.intercept(method, args, invoker);
            methodWriterListener.onWrite(method.getName(), args);
            invoker.accept(method, args);
        };
    }

    void intercept(Method method, Object[] args, BiConsumer<Method, Object[]> invoker);
}

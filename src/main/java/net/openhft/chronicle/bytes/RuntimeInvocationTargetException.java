package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;

/**
 * Thrown if the receiver of the {@link MethodReader} throws an exception on invocation
 */
@Deprecated(/* to be removed in x.21 */)
public class RuntimeInvocationTargetException extends RuntimeException {
    public RuntimeInvocationTargetException(@NotNull Throwable cause) {
        super(cause instanceof InvocationTargetException ? cause.getCause() : cause);
    }
}

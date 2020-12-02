package net.openhft.chronicle.bytes;

/**
 * Thrown if the receiver of the {@link MethodReader} throws an exception on invocation
 */
public class RuntimeInvocationTargetException extends RuntimeException {
    public RuntimeInvocationTargetException(Throwable cause) {
        super(cause);
    }
}

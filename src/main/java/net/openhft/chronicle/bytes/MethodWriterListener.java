package net.openhft.chronicle.bytes;

/**
 * Invoked before writing out this method and args.
 *
 * @deprecated Use MethodWriterInterceptorReturns
 */
@Deprecated
@FunctionalInterface
public interface MethodWriterListener {
    void onWrite(String name, Object[] args);
}

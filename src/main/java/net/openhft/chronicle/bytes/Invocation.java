package net.openhft.chronicle.bytes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@FunctionalInterface
public interface Invocation {
    Object invoke(Method m, Object o, Object[] args)
            throws InvocationTargetException;
}

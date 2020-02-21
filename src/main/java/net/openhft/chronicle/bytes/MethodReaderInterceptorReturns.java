package net.openhft.chronicle.bytes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
 * Created by peter.lawrey@chronicle.software on 31/07/2017
 */
@FunctionalInterface
public interface MethodReaderInterceptorReturns {
    Object intercept(Method m, Object o, Object[] args, Invocation invocation) throws InvocationTargetException;
}

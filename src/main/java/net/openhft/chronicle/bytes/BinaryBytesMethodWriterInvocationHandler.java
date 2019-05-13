package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.AbstractInvocationHandler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class BinaryBytesMethodWriterInvocationHandler extends AbstractInvocationHandler implements BytesMethodWriterInvocationHandler {
    private final Function<Method, MethodEncoder> methodToId;
    @SuppressWarnings("rawtypes")
    private final BytesOut out;
    private final Map<Method, MethodEncoder> methodToIdMap = new LinkedHashMap<>();

    @SuppressWarnings("rawtypes")
    public BinaryBytesMethodWriterInvocationHandler(Function<Method, MethodEncoder> methodToId, BytesOut out) {
        super(HashMap::new);
        this.methodToId = methodToId;
        this.out = out;
    }

    @Override
    protected Object doInvoke(Object proxy, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        MethodEncoder info = methodToIdMap.computeIfAbsent(method, methodToId);
        if (info == null) {
            Jvm.warn().on(getClass(), "Unknown method " + method + " ignored");
            return null;
        }
        out.comment(method.getName());
        out.writeStopBit(info.messageId());
        info.encode(args, out);
        return null;
    }
}

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.util.ObjectUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

public class BytesMethodReader implements MethodReader {
    private final BytesIn in;
    private final BytesParselet defaultParselet;
    private final MethodEncoderLookup methodEncoderLookup;
    private final Object[] objects;
    private final List<Consumer<BytesIn>> methodEncoders = new ArrayList<>();
    private final Map<Long, Consumer<BytesIn>> methodEncoderMap = new LinkedHashMap<>();

    public BytesMethodReader(BytesIn in,
                             BytesParselet defaultParselet,
                             MethodEncoderLookup methodEncoderLookup,
                             Object[] objects) {

        this.in = in;
        this.defaultParselet = defaultParselet;
        this.methodEncoderLookup = methodEncoderLookup;
        this.objects = objects;

        for (Object object : objects) {
            for (Method method : object.getClass().getMethods()) {
                MethodEncoder encoder = methodEncoderLookup.apply(method);
                if (encoder != null) {
                    addEncoder(object, method, encoder);
                }
            }
        }
    }

    private void addEncoder(Object object, Method method, MethodEncoder encoder) {
        method.setAccessible(true);
        Class<?>[] parameterTypes = method.getParameterTypes();
        int count = parameterTypes.length;
        BytesMarshallable[][] array = new BytesMarshallable[1][count];
        for (int i = 0; i < count; i++) {
            array[0][i] = (BytesMarshallable) ObjectUtils.newInstance(parameterTypes[i]);
        }
        Consumer<BytesIn> reader = in -> {
            array[0] = (BytesMarshallable[]) encoder.decode(array[0], in);
            try {
                method.invoke(object, array[0]);
            } catch (IllegalAccessException | InvocationTargetException e) {
                Jvm.warn().on(getClass(), "Exception calling " + method + " " + Arrays.toString(array[0]), e);
            }
        };
        long messageId = encoder.messageId();
        if (messageId >= 0 && messageId < 1000) {
            while (methodEncoders.size() <= messageId)
                methodEncoders.add(null);
            methodEncoders.set((int) messageId, reader);
        } else {
            methodEncoderMap.put(messageId, reader);
        }
    }

    public boolean readOne() {
        if (in.readRemaining() < 1)
            return false;
        long messageId = in.readStopBit();
        Consumer<BytesIn> consumer;
        if (messageId >= 0 && messageId < methodEncoders.size())
            consumer = methodEncoders.get((int) messageId);
        else
            consumer = methodEncoderMap.get(messageId);
        if (consumer == null) {
            defaultParselet.accept(messageId, in);
        } else {
            consumer.accept(in);
        }

        return true;
    }

    @Override
    public MethodReader closeIn(boolean closeIn) {
        return this;
    }

    @Override
    public void close() {

    }
}

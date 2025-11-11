/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.SimpleCloseable;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Concrete {@link MethodReader} that reads method calls serialised in a binary
 * format from a {@link BytesIn} stream and dispatches them to target objects.
 * {@link MethodEncoder} instances, typically obtained via
 * {@link MethodEncoderLookup}, are used to decode arguments. This reader is not
 * thread-safe and extends {@link SimpleCloseable}.
 */
@SuppressWarnings("rawtypes")
public class BytesMethodReader extends SimpleCloseable implements MethodReader {
    private final BytesIn<?> in;
    private final BytesParselet defaultParselet;
    private final List<Consumer<BytesIn>> methodEncoders = new ArrayList<>();
    private final Map<Long, Consumer<BytesIn>> methodEncoderMap = new LinkedHashMap<>();

    /**
     * @param in              the {@link BytesIn} stream from which messages are
     *                        read
     * @param defaultParselet handler for messages with unrecognised IDs
     * @param methodEncoderLookup strategy for obtaining {@link MethodEncoder}
     *                            instances
     * @param objects         target objects whose methods may be invoked
     */
    public BytesMethodReader(BytesIn<?> in,
                             BytesParselet defaultParselet,
                             MethodEncoderLookup methodEncoderLookup,
                             Object[] objects) {

        this.in = in;
        this.defaultParselet = defaultParselet;

        for (Object object : objects) {
            for (Method method : object.getClass().getMethods()) {
                MethodEncoder encoder = methodEncoderLookup.apply(method);
                if (encoder != null) {
                    addEncoder(object, method, encoder);
                }
            }
        }
    }

    /**
     * Prepares and stores a consumer for the supplied {@code method}. When
     * invoked it decodes arguments using {@code encoder} and calls the method on
     * {@code object}.
     */
    private void addEncoder(Object object, Method method, MethodEncoder encoder) {
        Jvm.setAccessible(method);
        Class<?>[] parameterTypes = method.getParameterTypes();
        int count = parameterTypes.length;
        BytesMarshallable[][] array = new BytesMarshallable[1][count];
        for (int i = 0; i < count; i++) {
            array[0][i] = (BytesMarshallable) ObjectUtils.newInstance(parameterTypes[i]);
        }
        Consumer<BytesIn> reader = bytesIn -> {
            try {
                array[0] = (BytesMarshallable[]) encoder.decode(array[0], bytesIn);
                method.invoke(object, (Object[]) array[0]);
            } catch (IllegalAccessException | InvocationTargetException | BufferUnderflowException |
                     IllegalArgumentException | IllegalStateException | InvalidMarshallableException e) {
                Jvm.warn().on(getClass(), "Exception calling " + method + " " + Arrays.toString(array[0]), e);
                bytesIn.readPosition(bytesIn.readLimit());
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

    /**
     * Interceptors for method reader returns are not supported by this
     * implementation.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public MethodReaderInterceptorReturns methodReaderInterceptorReturns() {
        throw new UnsupportedOperationException();
    }

    /**
     * Reads the next method call from the input stream. The message ID is read
     * using stop bit encoding and used to look up a handler. If none is found the
     * {@code defaultParselet} is invoked.
     *
     * @return {@code true} if a message was processed, {@code false} if no data
     *         was available
     * @throws InvocationTargetRuntimeException if the target method throws an
     *                                          exception
     * @throws BufferUnderflowException         if the stream ends prematurely
     * @throws ClosedIllegalStateException      if the {@link BytesIn} has been
     *                                          released
     * @throws ThreadingIllegalStateException   if accessed by multiple threads
     *                                          unsafely
     */
    public boolean readOne()
            throws InvocationTargetRuntimeException, IllegalStateException, BufferUnderflowException {
        throwExceptionIfClosed();

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

    /**
     * This implementation does not currently close the underlying
     * {@link BytesIn} when invoked. The parameter is ignored.
     *
     * @param closeIn ignored
     * @return this reader
     */
    @Override
    public MethodReader closeIn(boolean closeIn) {
        return this;
    }
}

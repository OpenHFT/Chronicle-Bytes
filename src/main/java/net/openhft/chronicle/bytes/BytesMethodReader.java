/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.SimpleCloseable;
import net.openhft.chronicle.core.util.InvocationTargetRuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.BufferUnderflowException;
import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("rawtypes")
/**
 * The BytesMethodReader class provides a concrete implementation of the MethodReader interface.
 * It extends SimpleCloseable and it's capable of reading serialized method calls from a BytesIn object.
 * The class uses an instance of BytesParselet to define a default behavior when reading method calls,
 * and a map of method encoders to decode serialized method calls into their original form.
 * <p>
 * The class also maintains an internal list of method encoders, which are stored in the order that
 * they were added. This allows the BytesMethodReader to efficiently look up the appropriate method
 * encoder based on a messageId.
 * <p>
 * The BytesMethodReader is not thread-safe.
 */
public class BytesMethodReader extends SimpleCloseable implements MethodReader {
    private final BytesIn<?> in;
    private final BytesParselet defaultParselet;
    private final List<Consumer<BytesIn>> methodEncoders = new ArrayList<>();
    private final Map<Long, Consumer<BytesIn>> methodEncoderMap = new LinkedHashMap<>();

    /**
     * Constructor for the BytesMethodReader class.
     * <p>
     * Initializes a new instance of the BytesMethodReader with a BytesIn object, a default BytesParselet,
     * and a methodEncoderLookup function. The constructor also accepts an array of objects which represent
     * the possible targets for the method calls that this reader will dispatch.
     *
     * @param in                  the BytesIn object from which serialized method calls are read.
     * @param defaultParselet     the default BytesParselet used when reading method calls.
     * @param methodEncoderLookup the lookup function used to find a method's encoder.
     * @param objects             the array of objects that can be targets of method calls.
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
     * Adds a method encoder to this reader for a specific method on a specific object.
     * <p>
     * This method creates a reader for the encoded method and then adds it to the appropriate
     * place in the list or map of method encoders.
     *
     * @param object  the target object of the method call.
     * @param method  the Method object representing the method being called.
     * @param encoder the MethodEncoder used to decode the method call.
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
                method.invoke(object, array[0]);
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
     * Throws an UnsupportedOperationException when called.
     * <p>
     * This method is required to fulfill the MethodReader interface, but it is not supported
     * in the BytesMethodReader class.
     *
     * @return nothing
     * @throws UnsupportedOperationException always
     */
    @Override
    public MethodReaderInterceptorReturns methodReaderInterceptorReturns() {
        throw new UnsupportedOperationException();
    }

    /**
     * Attempts to read a single message from the BytesIn object.
     * <p>
     * If a suitable method encoder is found, it is used to decode the message and the corresponding
     * method is invoked. Otherwise, the default parselet is used.
     * <p>
     * If the reader is closed or if there are no more messages to read, the method returns {@code false}.
     *
     * @return {@code true} if a message was successfully read, {@code false} if no more messages are available
     * @throws InvocationTargetRuntimeException if an exception is thrown by the target method
     * @throws IllegalStateException            if this method is invoked when the reader is closed
     * @throws BufferUnderflowException         if there is not enough data available in the buffer to read the next message
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
     * Sets whether the underlying input should be closed when the MethodReader is closed.
     * <p>
     * Note: This method currently does not alter the close behavior of the BytesMethodReader.
     * It returns the MethodReader itself for method chaining.
     *
     * @param closeIn {@code true} to close the input when this reader is closed, {@code false} otherwise
     * @return the MethodReader itself, for method chaining
     */
    @Override
    public MethodReader closeIn(boolean closeIn) {
        return this;
    }
}

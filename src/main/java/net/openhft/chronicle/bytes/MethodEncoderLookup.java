package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.util.Annotations;

import java.lang.reflect.Method;
import java.util.function.Function;

public enum MethodEncoderLookup implements Function<Method, MethodEncoder> {
    BY_ANNOTATION;

    @Override
    public MethodEncoder apply(Method method) {
        MethodId methodId = Annotations.getAnnotation(method, MethodId.class);
        if (methodId == null) return null;
        long messageId = methodId.value();
        return new MethodEncoder() {
            @Override
            public long messageId() {
                return messageId;
            }

            @SuppressWarnings("rawtypes")
            @Override
            public void encode(Object[] objects, BytesOut out) {
                for (Object object : objects) {
                    if (object instanceof BytesMarshallable) {
                        ((BytesMarshallable) object).writeMarshallable(out);
                        continue;
                    }
                    throw new IllegalArgumentException("Object type " + object + " not supported");
                }
            }

            @SuppressWarnings("rawtypes")
            @Override
            public Object[] decode(Object[] lastObjects, BytesIn in) {
                for (Object lastObject : lastObjects)
                    ((BytesMarshallable) lastObject).readMarshallable(in);
                return lastObjects;
            }
        };
    }

}

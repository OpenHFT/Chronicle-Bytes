package net.openhft.chronicle.bytes;

import java.lang.reflect.Method;
import java.util.function.Function;

public enum MethodEncoderLookup implements Function<Method, MethodEncoder> {
    BY_ANNOTATION;


    @Override
    public MethodEncoder apply(Method method) {
        MethodId methodId = method.getAnnotation(MethodId.class);
        if (methodId == null) {
            methodId = findAnnotation(method.getDeclaringClass(), method.getName(), method.getParameterTypes());
            if (methodId == null)
                return null;
        }
        long messageId = methodId.value();
        return new MethodEncoder() {
            @Override
            public long messageId() {
                return messageId;
            }

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

            @Override
            public Object[] decode(Object[] lastObjects, BytesIn in) {
                for (int i = 0; i < lastObjects.length; i++)
                    ((BytesMarshallable) lastObjects[i]).readMarshallable(in);
                return lastObjects;
            }
        };
    }

    private MethodId findAnnotation(Class<?> aClass, String name, Class<?>[] parameterTypes) {
        MethodId methodId = null;
        try {
            Method m = aClass.getMethod(name, parameterTypes);
            methodId = m.getAnnotation(MethodId.class);
            if (methodId != null)
                return methodId;
        } catch (NoSuchMethodException e) {
            // ignored
        }
        Class<?> superclass = aClass.getSuperclass();
        if (!(superclass == null || superclass == Object.class)) {
            methodId = findAnnotation(superclass, name, parameterTypes);
            if (methodId != null)
                return methodId;
        }
        for (Class<?> iClass : aClass.getInterfaces()) {
            methodId = findAnnotation(iClass, name, parameterTypes);
            if (methodId != null)
                return methodId;
        }
        return null;
    }

    private MethodId findAnnotation(Class aClass, Method method, Class<MethodId> methodIdClass) {
        return null;
    }
}

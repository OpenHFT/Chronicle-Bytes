package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class BytesMethodWriterBuilder<T> implements Supplier<T> {

    private final List<Class> interfaces = new ArrayList<>();
    @NotNull
    private final BytesMethodWriterInvocationHandler handler;
    private ClassLoader classLoader;

    public BytesMethodWriterBuilder(@NotNull Class<T> tClass, @NotNull BytesMethodWriterInvocationHandler handler) {
        interfaces.add(Closeable.class);
        interfaces.add(tClass);
        classLoader = tClass.getClassLoader();
        this.handler = handler;
    }

    @NotNull
    public BytesMethodWriterBuilder<T> classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @NotNull
    public BytesMethodWriterBuilder<T> addInterface(Class additionalClass) {
        interfaces.add(additionalClass);
        return this;
    }


    @NotNull
    public BytesMethodWriterBuilder<T> onClose(Closeable closeable) {
        handler.onClose(closeable);
        return this;
    }

    // Builder terminology
    @NotNull
    public T build() {
        return get();
    }

    // Supplier terminology
    @NotNull
    @Override
    public T get() {
        @NotNull Class[] interfacesArr = interfaces.toArray(new Class[interfaces.size()]);
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(classLoader, interfacesArr, handler);
    }
}

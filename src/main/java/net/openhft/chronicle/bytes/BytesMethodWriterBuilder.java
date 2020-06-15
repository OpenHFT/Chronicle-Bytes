/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.Closeable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
@Deprecated(/*is it used?*/)
public class BytesMethodWriterBuilder<T> implements MethodWriterBuilder<T> {

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

    @Override
    public MethodWriterBuilder<T> recordHistory(boolean recordHistory) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MethodWriterBuilder<T> methodWriterInterceptorReturns(MethodWriterInterceptorReturns writeInterceptor) {
        throw new UnsupportedOperationException();
    }

    // Supplier terminology
    @NotNull
    @Override
    public T get() {
        @NotNull Class[] interfacesArr = interfaces.toArray(new Class[interfaces.size()]);
        //noinspection unchecked
        return (T) Proxy.newProxyInstance(classLoader, interfacesArr, handler);
    }

    @Override
    public MethodWriterBuilder<T> methodWriterListener(MethodWriterListener methodWriterListener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MethodWriterBuilder<T> genericEvent(String genericEvent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MethodWriterBuilder<T> useMethodIds(boolean useMethodIds) {
        // always true
        return this;
    }

}

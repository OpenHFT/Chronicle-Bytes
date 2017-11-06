/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

/*
 * Created by Peter Lawrey on 20/04/2016.
 */
public interface BytesOut<Underlying> extends
        StreamingDataOutput<Bytes<Underlying>>,
        ByteStringAppender<Bytes<Underlying>>,
        BytesPrepender<Bytes<Underlying>> {

    /**
     * Proxy an interface so each message called is written to a file for replay.
     *
     * @param tClass     primary interface
     * @param additional any additional interfaces
     * @return a proxy which implements the primary interface (additional interfaces have to be
     * cast)
     */
    @NotNull
    default <T> T bytesMethodWriter(@NotNull Class<T> tClass, Class... additional) {
        Class[] interfaces = ObjectUtils.addAll(tClass, additional);

        //noinspection unchecked
        return (T) Proxy.newProxyInstance(tClass.getClassLoader(), interfaces,
                new BinaryBytesMethodWriterInvocationHandler(MethodEncoderLookup.BY_ANNOTATION, this));
    }

    @NotNull
    default <T> BytesMethodWriterBuilder<T> bytesMethodWriterBuilder(Function<Method, MethodEncoder> methodEncoderFunction, @NotNull Class<T> tClass) {
        return new BytesMethodWriterBuilder<>(tClass, new BinaryBytesMethodWriterInvocationHandler(methodEncoderFunction, this));
    }

    /**
     * Do these Bytes support saving comments
     *
     * @return true if comments are kept
     */
    default boolean retainsComments() {
        return false;
    }

    /**
     * Add comment as approriate for the toHexString format
     *
     * @param comment to add (or ignore)
     * @return this
     */
    default BytesOut<Underlying> comment(CharSequence comment) {
        return this;
    }

    /**
     * Adjust the indent for nested data
     *
     * @param n +1 indent in, -1 reduce indenting
     * @return this.
     */
    default BytesOut<Underlying> indent(int n) {
        return this;
    }
}

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

import java.lang.annotation.*;

/**
 * Annotation that denotes a method as having a numeric identifier for efficient encoding.
 *
 * <p>
 * Applying this annotation to a method allows it to be associated with a numeric value,
 * which can be leveraged during the encoding process to enhance efficiency. Numeric values
 * are more efficient to encode and decode than string representations, especially in
 * high-performance or resource-constrained environments.
 * <p>
 * The numeric identifier is user-defined and should be unique to ensure correct mapping.
 * For simpler decoding, a character can be used as the numeric identifier, leveraging
 * its underlying ASCII or Unicode numeric value.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface MethodId {
    /**
     * The unique numeric identifier associated with the method.
     *
     * @return The unique numeric identifier associated with the method.
     */
    long value();
}

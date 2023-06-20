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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Annotation used for grouping fields within a class. This can be useful for managing related fields
 * as a single entity, particularly in classes that need precise control over the memory layout of their fields,
 * or in scenarios where grouping fields can simplify code that operates over these fields.
 *
 * <p>The {@link FieldGroup} annotation is retained at runtime and can be used in any field declarations. The
 * 'value' method should be used to specify the name of the group.</p>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldGroup {
    String HEADER = "header";

    /**
     * Defines the name of the field group.
     *
     * @return the name of the group
     */
    String value();
}

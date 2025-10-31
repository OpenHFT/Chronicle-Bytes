/*
 * Copyright 2016-2025 chronicle.software
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
/**
 * Utility classes that convert floating point values to fixed-width decimal byte sequences.
 * <p>
 * The package offers several {@link Decimaliser} implementations ranging from the lightweight
 * {@link SimpleDecimaliser} through to the object allocating {@link UsesBigDecimal}. These allow
 * callers to balance performance against precision when rendering {@code double} and {@code float}
 * values.
 *
 * <p>Most implementations are stateless singletons and therefore thread-safe. The exception is
 * {@link MaximumPrecision}, which is stateful and not thread-safe.
 *
 * <p>For a worked example see {@code decimal-rendering.adoc}. For integration
 * with text wire formats refer to the Wire Integration guide.
 * The {@linkplain net.openhft.chronicle.bytes.render.StandardDecimaliser
 * standard decimaliser} class is the usual entry point.
 */
package net.openhft.chronicle.bytes.render;

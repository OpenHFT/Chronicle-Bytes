/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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

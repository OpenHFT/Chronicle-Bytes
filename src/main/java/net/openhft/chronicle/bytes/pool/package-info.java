/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
/**
 * Pooling and reuse of Chronicle {@code Bytes} instances and related resources.
 * <p>
 * Pools here are used to reduce allocation on hot paths by recycling
 * buffers and temporary objects.
 */
package net.openhft.chronicle.bytes.pool;

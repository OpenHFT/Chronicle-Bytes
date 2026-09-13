/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
/**
 * Internal helpers used when migrating Chronicle Bytes behaviour.
 *
 * <p>This package currently contains small utilities that provide
 * safe implementations of operations such as {@code hashCode} and
 * {@code equals} for {@code BytesStore} instances while respecting
 * reference counting and off heap lifecycles.
 *
 * <p>It is strictly internal to Chronicle Bytes and is not part of
 * the public API. Types may be added, removed, or changed between
 * releases as migration strategies evolve.
 */
package net.openhft.chronicle.bytes.internal.migration;

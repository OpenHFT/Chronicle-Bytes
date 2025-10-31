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
 * Provides classes for non-cryptographic hash computations on {@link net.openhft.chronicle.bytes.BytesStore} objects.
 * <p>
 * The package offers three implementations:
 * {@link net.openhft.chronicle.bytes.algo.VanillaBytesStoreHash},
 * {@link net.openhft.chronicle.bytes.algo.XxHash} and
 * {@link net.openhft.chronicle.bytes.algo.OptimisedBytesStoreHash}. They all
 * implement {@link net.openhft.chronicle.bytes.algo.BytesStoreHash}.
 * VanillaBytesStoreHash uses a fixed set of mixing constants for quick hashing
 * while XxHash supports a seed. OptimisedBytesStoreHash chooses the most
 * efficient strategy based on size and direct memory access.
 * <p>
 * {@code XxHash} was migrated from the Zero-Allocation-Hashing project and is
 * renowned for speed. These algorithms are deterministic across platforms but
 * should not be used for security or privacy-sensitive computations.
 * <p>
 * See the AsciiDoc module overview {@code algo-overview.adoc} for examples and
 * performance notes.
 *
 * @see net.openhft.chronicle.bytes.BytesStore
 * @see net.openhft.chronicle.bytes.algo.BytesStoreHash
 * @see <a href="../../../../src/main/docs/algo-overview.adoc">algo-overview.adoc</a>
 */
package net.openhft.chronicle.bytes.algo;

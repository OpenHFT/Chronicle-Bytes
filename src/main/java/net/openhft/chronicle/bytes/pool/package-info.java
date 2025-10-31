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
 * This package provides a pool for managing {@link net.openhft.chronicle.bytes.Bytes} instances.
 * <p>
 * {@link net.openhft.chronicle.bytes.pool.BytesPool} exposes factory methods
 * such as {@link net.openhft.chronicle.bytes.pool.BytesPool#createThreadLocal()}
 * that create thread-local caches of {@link net.openhft.chronicle.bytes.Bytes}
 * objects. Pooling helps reduce allocation churn even with modern garbage
 * collectors when latencies of only a few micro-seconds are required.
 * <p>
 * See the AsciiDoc module overview {@code pool-overview.adoc} for configuration
 * examples and tuning hints.
 *
 */
package net.openhft.chronicle.bytes.pool;

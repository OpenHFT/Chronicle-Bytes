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

/**
 * An interface that indicates an implementor of {@link Byteable} has dynamic size requirements.
 *
 * <p>
 * A class implementing the {@code DynamicallySized} interface indicates that
 * the size of its instances in bytes cannot be determined statically (at compile time) but
 * instead is dependent on the state of the individual object instances.
 * 
 */
public interface DynamicallySized {
}

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

import net.openhft.chronicle.core.onoes.ExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BytesMethodReaderBuilderTest {

    private BytesIn<?> mockBytesIn;

    @BeforeEach
    void setUp() {
        mockBytesIn = mock(BytesIn.class);
    }

    @Test
    void constructorWithBytesInShouldNotThrow() {
        assertDoesNotThrow(() -> new BytesMethodReaderBuilder(mockBytesIn));
    }

    @Test
    void settingExceptionHandlerOnUnknownMethod() {
        BytesMethodReaderBuilder builder = new BytesMethodReaderBuilder(mockBytesIn);
        ExceptionHandler mockHandler = mock(ExceptionHandler.class);
        assertDoesNotThrow(() -> builder.exceptionHandlerOnUnknownMethod(mockHandler));
    }

    @Test
    void settingMethodEncoderLookup() {
        BytesMethodReaderBuilder builder = new BytesMethodReaderBuilder(mockBytesIn);
        MethodEncoderLookup lookup = MethodEncoderLookup.BY_ANNOTATION;
        assertEquals(builder, builder.methodEncoderLookup(lookup), "Builder should support method chaining for methodEncoderLookup setting.");
    }

    @Test
    void settingAndInitializingDefaultParselet() {
        BytesMethodReaderBuilder builder = new BytesMethodReaderBuilder(mockBytesIn);
        BytesParselet mockParselet = mock(BytesParselet.class);
        builder.defaultParselet(mockParselet);
        assertEquals(mockParselet, builder.defaultParselet(), "The set defaultParselet should be returned.");
    }
}

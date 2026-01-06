/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.onoes.ExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@SuppressWarnings("deprecation")
class BytesMethodReaderBuilderTest {

    private BytesIn<?> mockBytesIn;

    @BeforeEach
    void setUp() {
        mockBytesIn = mock(BytesIn.class);
    }

    @Test
    @DisplayName("constructor accepts bytes input without throwing")
    void constructorWithBytesInShouldNotThrow() {
        assertDoesNotThrow(() -> new BytesMethodReaderBuilder(mockBytesIn),
                "Builder constructor should accept a BytesIn instance");
    }

    @Test
    @DisplayName("exception handler for unknown methods can be configured")
    void settingExceptionHandlerOnUnknownMethod() {
        BytesMethodReaderBuilder builder = new BytesMethodReaderBuilder(mockBytesIn);
        ExceptionHandler mockHandler = mock(ExceptionHandler.class);
        assertDoesNotThrow(() -> builder.exceptionHandlerOnUnknownMethod(mockHandler),
                "Builder should accept an exception handler for unknown methods");
    }

    @Test
    @DisplayName("method encoder lookup configuration supports chaining")
    void settingMethodEncoderLookup() {
        BytesMethodReaderBuilder builder = new BytesMethodReaderBuilder(mockBytesIn);
        MethodEncoderLookup lookup = MethodEncoderLookup.BY_ANNOTATION;
        assertEquals(builder,
                builder.methodEncoderLookup(lookup),
                "Builder should return itself after method encoder lookup configuration");
    }

    @Test
    @DisplayName("default parselet is stored and returned")
    void settingAndInitializingDefaultParselet() {
        BytesMethodReaderBuilder builder = new BytesMethodReaderBuilder(mockBytesIn);
        BytesParselet mockParselet = mock(BytesParselet.class);
        builder.defaultParselet(mockParselet);
        assertEquals(mockParselet,
                builder.defaultParselet(),
                "Builder should return the configured default parselet");
    }
}

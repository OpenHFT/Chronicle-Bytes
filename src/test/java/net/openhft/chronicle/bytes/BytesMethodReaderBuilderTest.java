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

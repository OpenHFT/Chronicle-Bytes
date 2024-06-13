package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;
import org.mockito.Mockito;
import java.util.function.Predicate;
import static org.mockito.Mockito.*;

public class MethodReaderBuilderTest {

    @Test
    public void testWarnMissing() {
        MethodReaderBuilder builder = mock(MethodReaderBuilder.class, Mockito.CALLS_REAL_METHODS);

        when(builder.exceptionHandlerOnUnknownMethod(any())).thenReturn(builder);

        builder.warnMissing(true);

        verify(builder).exceptionHandlerOnUnknownMethod(Jvm.warn());

        builder.warnMissing(false);
        verify(builder).exceptionHandlerOnUnknownMethod(Jvm.debug());
    }

    @Test
    public void testPredicate() {
        MethodReaderBuilder builder = mock(MethodReaderBuilder.class, Mockito.CALLS_REAL_METHODS);

        Predicate<Object> predicate = o -> true;

        builder.predicate(predicate);
    }
}

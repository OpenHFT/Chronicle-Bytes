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

        Predicate<MethodReader> predicate = o -> true;

        builder.predicate(predicate);
    }
}

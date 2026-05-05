/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.UUID;

public final class TestSampleValues {

    private TestSampleValues() {
    }

    // CPD-OFF
    public static Samples samples() {
        final MyByteable mb1 = new MyByteable(false, (byte) 1, (short) 2, '3', 4, 5.5f, 6, 7.7);
        final MyByteable mb2 = new MyByteable(true, (byte) 11, (short) 22, 'T', 44, 5.555f, 66, 77.77);
        final ZonedDateTime zdt1 = ZonedDateTime.parse("2017-11-06T12:35:56.775Z[Europe/London]");
        final ZonedDateTime zdt2 = ZonedDateTime.parse("2016-10-05T01:34:56.775Z[Europe/London]");
        final UUID uuid1 = new UUID(0x123456789L, 0xABCDEF);
        final UUID uuid2 = new UUID(0x1111111111111111L, 0x2222222222222222L);
        final MyScalars ms1 = new MyScalars("Hello", BigInteger.ONE, BigDecimal.TEN, zdt1.toLocalDate(), zdt1.toLocalTime(), zdt1.toLocalDateTime(), zdt1, uuid1);
        final MyScalars ms2 = new MyScalars("World", BigInteger.ZERO, BigDecimal.ZERO, zdt2.toLocalDate(), zdt2.toLocalTime(), zdt2.toLocalDateTime(), zdt2, uuid2);
        final MyNested mn1 = new MyNested(mb1, ms1);
        final MyNested mn2 = new MyNested(mb2, ms2);

        return new Samples(mb1, mb2, ms1, ms2, mn1, mn2);
    }
    // CPD-ON

    public static final class Samples {
        private final MyByteable mb1;
        private final MyByteable mb2;
        private final MyScalars ms1;
        private final MyScalars ms2;
        private final MyNested mn1;
        private final MyNested mn2;

        public Samples(MyByteable mb1,
                       MyByteable mb2,
                       MyScalars ms1,
                       MyScalars ms2,
                       MyNested mn1,
                       MyNested mn2) {
            this.mb1 = mb1;
            this.mb2 = mb2;
            this.ms1 = ms1;
            this.ms2 = ms2;
            this.mn1 = mn1;
            this.mn2 = mn2;
        }

        public MyByteable mb1() {
            return mb1;
        }

        public MyByteable mb2() {
            return mb2;
        }

        public MyScalars ms1() {
            return ms1;
        }

        public MyScalars ms2() {
            return ms2;
        }

        public MyNested mn1() {
            return mn1;
        }

        public MyNested mn2() {
            return mn2;
        }
    }
}

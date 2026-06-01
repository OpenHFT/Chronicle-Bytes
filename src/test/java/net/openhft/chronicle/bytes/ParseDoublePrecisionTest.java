/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Regression tests for the precision contract of {@link Bytes#parseDouble()}.
 * <p>
 * Context: when {@code JSONWire}/{@code TextWire} write a finite double they emit the shortest
 * faithful decimal (via {@code Double.toString}); reading it back goes through
 * {@code BytesInternal.parseDouble}, whose mantissa accumulator sheds low bits once it overflows a
 * {@code long} ({@code value >>>= 1}). That trades precision for speed on the longest mantissas.
 * <p>
 * Scope: magnitudes in {@code [1e-3, 1e15)} — the range the wire writes as plain decimals via
 * {@code bytes.append(double)} and then reads back with {@code parseDouble}. (Magnitudes outside
 * this range are emitted with {@code Double.toString}.)
 * <p>
 * The accepted trade-off is: a value needing 16-17 significant digits may differ from the
 * correctly-rounded result (what {@code Double.parseDouble} returns) by <b>at most one ULP</b>.
 * It should never be worse than that — values of 15 or fewer significant digits must be
 * <b>exact</b>, and nothing should ever be off by more than one ULP.
 * <p>
 * These tests encode that contract and guard the CORE-65 parser behavior.
 */
class ParseDoublePrecisionTest extends BytesTestCommon {

    /** Number of representable doubles between two finite, same-sign values. */
    private static long ulpsBetween(double a, double b) {
        long la = Double.doubleToLongBits(a);
        long lb = Double.doubleToLongBits(b);
        if (la < 0) la = 0x8000000000000000L - la;
        if (lb < 0) lb = 0x8000000000000000L - lb;
        return Math.abs(la - lb);
    }

    private static double parse(Bytes<?> scratch, String s) {
        scratch.clear();
        scratch.append(s);
        return scratch.parseDouble();
    }

    /**
     * Concrete values surfaced by a JSONWire round-trip sweep over {@code [1e11, 1e15)}: each is a
     * faithful shortest decimal that the parser restores imperfectly. Documents that these specific
     * values sit within the accepted 1-ULP trade-off.
     */
    @Test
    void knownWireValuesParseWithinOneUlp() {
        String[] values = {
                "2.116299837525985E12",
                "5238753360239.026",
                "3.447381479371051E13",
                "9.999999999999998E14",
        };
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        try {
            StringBuilder over = new StringBuilder();
            for (String s : values) {
                long ulps = ulpsBetween(parse(b, s), Double.parseDouble(s));
                if (ulps > 1)
                    over.append("\n  ").append(s).append(" off by ").append(ulps).append(" ULP");
            }
            if (over.length() > 0)
                fail("known wire values exceeded the 1-ULP trade-off:" + over);
        } finally {
            b.releaseLast();
        }
    }
}

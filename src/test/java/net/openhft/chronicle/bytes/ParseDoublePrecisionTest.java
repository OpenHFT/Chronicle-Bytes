/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Explorative tests characterising the precision of {@link Bytes#parseDouble()}.
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
 * These tests encode that contract. Those that currently fail reproduce the issue under
 * investigation on this branch.
 */
public class ParseDoublePrecisionTest extends BytesTestCommon {

    private static final int SAMPLES = 1_000_000;

    private static final double MIN_MAGNITUDE = 1e-3;
    private static final double MAX_MAGNITUDE = 1e15;

    /** Number of representable doubles between two finite, same-sign values. */
    private static long ulpsBetween(double a, double b) {
        long la = Double.doubleToLongBits(a);
        long lb = Double.doubleToLongBits(b);
        if (la < 0) la = 0x8000000000000000L - la;
        if (lb < 0) lb = 0x8000000000000000L - lb;
        return Math.abs(la - lb);
    }

    /** Count of significant decimal digits in a shortest-form decimal string. */
    private static int significantDigits(String s) {
        String m = s.split("[eE]")[0].replace("-", "").replace(".", "");
        m = m.replaceFirst("^0+", "").replaceFirst("0+$", "");
        return m.isEmpty() ? 1 : m.length();
    }

    private static double parse(Bytes<?> scratch, String s) {
        scratch.clear();
        scratch.append(s);
        return scratch.parseDouble();
    }

    /**
     * A clean decimal of 15 or fewer significant digits fits in the {@code long} mantissa without
     * the lossy {@code value >>>= 1} shedding, so the parser reproduces the correctly-rounded result
     * exactly (0 ULP from {@link Double#parseDouble}). Passes since the Clinger fast path was added
     * to {@code Maths.asDouble} (CORE-65); before that ~451/1M such decimals were 1 ULP off.
     */
    @Test
    public void fifteenOrFewerSignificantDigitsParseExactly() {
        Random r = new Random(1);
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        try {
            long failures = 0, worstUlps = 0;
            String worst = null;
            for (int i = 0; i < SAMPLES; i++) {
                String s = randomDecimal(r, 1 + r.nextInt(15));
                double ref = Double.parseDouble(s);
                if (!Double.isFinite(ref) || ref == 0)
                    continue;
                double got = parse(b, s);
                long ulps = ulpsBetween(got, ref);
                if (ulps != 0) {
                    failures++;
                    if (ulps > worstUlps) { worstUlps = ulps; worst = s + " -> " + got + " (jdk " + ref + ")"; }
                }
            }
            assertEquals(0, failures,
                    "<=15 significant digit decimals must parse exactly; " + failures + " were off, worst "
                            + worstUlps + " ULP: " + worst);
        } finally {
            b.releaseLast();
        }
    }

    /**
     * No double in {@code [1e-3, 1e15)}, however many significant digits its shortest form carries,
     * may parse more than one ULP away from the correctly-rounded result. This currently passes and
     * guards the accepted trade-off; the worse 2-ULP cases only occur at extreme exponents outside
     * the practical range.
     */
    @Test
    public void parseErrorNeverExceedsOneUlp() {
        Random r = new Random(2);
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        try {
            long over = 0, worstUlps = 0;
            String worst = null;
            for (int i = 0; i < SAMPLES; i++) {
                double d = randomInRange(r);            // magnitude in [1e-3, 1e15)
                String s = Double.toString(d);          // shortest faithful form
                double got = parse(b, s);
                long ulps = ulpsBetween(got, d);
                if (ulps > 1) {
                    over++;
                    if (ulps > worstUlps) { worstUlps = ulps; worst = s + " -> " + got; }
                }
            }
            assertEquals(0, over,
                    over + " values parsed more than 1 ULP from the correctly-rounded result, worst "
                            + worstUlps + " ULP: " + worst);
        } finally {
            b.releaseLast();
        }
    }

    /**
     * Focused on the large end of the range {@code [1e11, 1e15)}, where the pre-fix parser was worst
     * (~5.5% of values 1 ULP off at 1e12). Each value is parsed in both forms it can be written:
     * plain {@code xxxxx.yyy} (decimal-places path) and scientific {@code xxxx.yyEn} (E-exponent
     * path). Asserts the accepted contract -- neither form exceeds 1 ULP -- and prints the per-decade
     * mismatch rate so the improvement over the pre-fix ~5.5% is visible.
     */
    @Test
    public void parseLargeValuesBothFormsWithinOneUlp() {
        Random r = new Random(11);
        Bytes<?> b = Bytes.allocateElasticOnHeap(64);
        // decade -> [samples, plainMismatch, plainMaxUlp, sciMismatch, sciMaxUlp]
        TreeMap<Integer, long[]> byDecade = new TreeMap<>();
        long over1 = 0;
        String worst = null;
        try {
            for (int i = 0; i < SAMPLES; i++) {
                int decade = 11 + r.nextInt(4);                 // 1e11 .. 1e14
                double low = Math.pow(10, decade);
                double d = low + r.nextDouble() * (9 * low);    // full mantissa precision in the decade
                if (r.nextBoolean())
                    d = -d;
                if (!Double.isFinite(d) || Math.abs(d) >= 1e15)
                    continue;
                String sci = Double.toString(d);                // xxxx.yyEn
                String plain = new BigDecimal(sci).toPlainString(); // xxxxx.yyy
                long up = ulpsBetween(parse(b, plain), d);
                long us = ulpsBetween(parse(b, sci), d);
                long[] row = byDecade.computeIfAbsent(decade, k -> new long[5]);
                row[0]++;
                if (up != 0) row[1]++;
                if (up > row[2]) row[2] = up;
                if (us != 0) row[3]++;
                if (us > row[4]) row[4] = us;
                if (up > 1 || us > 1) {
                    over1++;
                    if (worst == null)
                        worst = "plain=" + plain + "(" + up + " ULP) sci=" + sci + "(" + us + " ULP)";
                }
            }
        } finally {
            b.releaseLast();
        }
        System.out.println("decade  samples   plainMiss%  plainMaxUlp  sciMiss%  sciMaxUlp");
        for (Map.Entry<Integer, long[]> e : byDecade.entrySet()) {
            long[] row = e.getValue();
            System.out.printf("1e%-4d  %-8d  %-10.4f  %-11d  %-8.4f  %d%n",
                    e.getKey(), row[0], 100.0 * row[1] / row[0], row[2], 100.0 * row[3] / row[0], row[4]);
        }
        final long o = over1;
        final String w = worst;
        assertEquals(0, o, () -> o + " large values exceeded 1 ULP in some form, e.g. " + w);
    }

    /**
     * Pure measurement (always passes): prints max ULP error and mismatch rate per significant-digit
     * count, to guide the fix and show where the accepted 1-ULP trade-off begins.
     */
    @Test
    public void characteriseParseErrorByDigitCount() {
        Random r = new Random(3);
        Bytes<?> b = Bytes.allocateElasticOnHeap(32);
        // digits -> [count, mismatches, maxUlps]
        TreeMap<Integer, long[]> byDigits = new TreeMap<>();
        try {
            for (int i = 0; i < SAMPLES; i++) {
                double d = randomInRange(r);            // magnitude in [1e-3, 1e15)
                String s = Double.toString(d);
                double got = parse(b, s);
                long ulps = ulpsBetween(got, d);
                long[] row = byDigits.computeIfAbsent(significantDigits(s), k -> new long[3]);
                row[0]++;
                if (ulps != 0) row[1]++;
                if (ulps > row[2]) row[2] = ulps;
            }
        } finally {
            b.releaseLast();
        }
        System.out.println("sigDigits  samples      mismatches   mismatch%   maxUlps");
        for (java.util.Map.Entry<Integer, long[]> e : byDigits.entrySet()) {
            long[] row = e.getValue();
            System.out.printf("%-9d  %-11d  %-11d  %-9.4f   %d%n",
                    e.getKey(), row[0], row[1], 100.0 * row[1] / row[0], row[2]);
        }
    }

    /**
     * Concrete values surfaced by a JSONWire round-trip sweep over {@code [1e11, 1e15)}: each is a
     * faithful shortest decimal that the parser restores imperfectly. Documents that these specific
     * values sit within the accepted 1-ULP trade-off (the broader contract is asserted, and
     * currently fails, in the disabled tests above).
     */
    @Test
    public void knownWireValuesParseWithinOneUlp() {
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
                System.out.printf("%-24s ulps=%d%n", s, ulps);
                if (ulps > 1)
                    over.append("\n  ").append(s).append(" off by ").append(ulps).append(" ULP");
            }
            if (over.length() > 0)
                fail("known wire values exceeded the 1-ULP trade-off:" + over);
        } finally {
            b.releaseLast();
        }
    }

    /**
     * Build a clean decimal string with exactly {@code n} significant digits and a magnitude in
     * {@code [1e-3, 1e15)} (capital-E scientific; a mantissa in {@code [1,10)} times {@code 10^exp}
     * with {@code exp} in {@code [-3, 14]}).
     */
    private static String randomDecimal(Random r, int n) {
        StringBuilder m = new StringBuilder();
        m.append((char) ('1' + r.nextInt(9)));
        for (int i = 1; i < n; i++)
            m.append((char) ('0' + r.nextInt(10)));
        String mant = n == 1 ? m.toString() : m.charAt(0) + "." + m.substring(1);
        int exp = -3 + r.nextInt(18);
        return (r.nextBoolean() ? "-" : "") + mant + "E" + exp;
    }

    /** A finite double whose magnitude lies in {@code [1e-3, 1e15)}, with full mantissa precision. */
    private static double randomInRange(Random r) {
        int decade = -3 + r.nextInt(18);           // 1e-3 .. 1e14
        double low = Math.pow(10, decade);          // [low, 10*low) stays within [1e-3, 1e15)
        double v = low + r.nextDouble() * (9 * low);
        return r.nextBoolean() ? -v : v;
    }
}

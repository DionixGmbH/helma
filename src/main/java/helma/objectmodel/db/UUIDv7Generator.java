/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 */

package helma.objectmodel.db;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Thread-safe, monotonic generator for UUID v7 (RFC 9562) identifiers.
 *
 * Layout (128 bits):
 *   Bits  0-47:  Unix timestamp in milliseconds
 *   Bits 48-51:  Version = 0b0111 (7)
 *   Bits 52-63:  Random / counter (12 bits, rand_a)
 *   Bits 64-65:  Variant = 0b10
 *   Bits 66-127: Random / counter (62 bits, rand_b)
 *
 * Within the same millisecond, rand_b is incremented to guarantee
 * monotonically increasing values (lexicographic sort order).
 */
public final class UUIDv7Generator {

    private static final SecureRandom random = new SecureRandom();
    private static long lastTimestamp = -1;
    private static long lastMsb;
    private static long lastLsb;

    private UUIDv7Generator() {}

    /**
     * Generate a new UUID v7 and return its standard string representation.
     * Guaranteed to be monotonically increasing across calls.
     */
    public static synchronized String generate() {
        long timestamp = System.currentTimeMillis();
        long msb, lsb;

        if (timestamp > lastTimestamp) {
            // New millisecond - fresh random bits
            lastTimestamp = timestamp;
            msb = ((timestamp & 0xFFFFFFFFFFFFL) << 16)
                | 0x7000L
                | (random.nextLong() & 0x0FFFL);
            lsb = 0x8000000000000000L | (random.nextLong() & 0x3FFFFFFFFFFFFFFFL);
        } else {
            // Same or earlier millisecond - increment for monotonicity
            long randB = (lastLsb & 0x3FFFFFFFFFFFFFFFL) + 1;
            if ((randB & 0x4000000000000000L) != 0) {
                // Overflow rand_b (62 bits) - increment rand_a
                randB = 0;
                long randA = (lastMsb & 0x0FFFL) + 1;
                if ((randA & 0x1000L) != 0) {
                    // Overflow rand_a (12 bits) - advance timestamp
                    lastTimestamp++;
                    msb = ((lastTimestamp & 0xFFFFFFFFFFFFL) << 16) | 0x7000L;
                } else {
                    msb = (lastMsb & 0xFFFFFFFFFFFFF000L) | randA;
                }
            } else {
                msb = lastMsb;
            }
            lsb = 0x8000000000000000L | randB;
        }

        lastMsb = msb;
        lastLsb = lsb;
        return new UUID(msb, lsb).toString();
    }
}

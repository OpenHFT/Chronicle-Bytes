package net.openhft.chronicle.bytes.internal.migration;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.StackTrace;

public final class HashCodeEqualsMigrationWarner {

    private static final String MSG =
            "**********" +
                    "The behaviour of Bytes::hashCode/Bytes::equals() will/has change(d) " +
                    "to Object::hashCode/Object::equals in a future/recent version." +
                    "Make sure to update your application if it depends on the old behaviour where" +
                    "the contents of the Bytes object affected the outcome of these methods.";

    static {
        Jvm.warn().on(HashCodeEqualsMigrationWarner.class, MSG, new StackTrace());
    }

    private HashCodeEqualsMigrationWarner() {
    }

    public static void warnOnce() {
        // Everything is made in the static block above as the
        // class is first used
    }

}

package net.openhft.chronicle.bytes;

public interface BytesComment<B extends BytesComment<B>> {
    /**
     * Do these Bytes support saving comments
     *
     * @return true if comments are kept
     */
    default boolean retainsComments() {
        return false;
    }

    /**
     * Add comment as approriate for the toHexString format
     *
     * @param comment to add (or ignore)
     * @return this
     */
    default B comment(CharSequence comment) {
        return (B) this;
    }

    /**
     * Adjust the indent for nested data
     *
     * @param n +1 indent in, -1 reduce indenting
     * @return this.
     */
    default B indent(int n) {
        return (B) this;
    }
}

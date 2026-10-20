package net.onestorm.bend.client.api;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The public id of an endpoint, in the form {@code namespace:key}.
 *
 * <p>An id follows the namespaced id rule of the Bend protocol (protocol-v1, "Namespaced Id/Key"):
 * a namespace and a key, separated by exactly one colon, each made of one or more lowercase
 * letters {@code a-z}, digits {@code 0-9}, dashes {@code -} and underscores {@code _}, and at most
 * 64 characters in total. An id that breaks this rule can't be created, so every
 * {@code NamespacedId} is one the protocol accepts.
 *
 * <p>Instances are immutable and safe to share between threads. Two ids are equal when their
 * namespace and key are equal.
 */
public final class NamespacedId {

    private static final int MAXIMUM_LENGTH = 64;
    // namespace:key, both lowercase a-z/0-9/dash/underscore, exactly one colon separator
    private static final Pattern PATTERN = Pattern.compile("([a-z0-9_-]+):([a-z0-9_-]+)");

    private final String namespace;
    private final String key;

    private NamespacedId(String namespace, String key) {
        this.namespace = namespace;
        this.key = key;
    }

    /**
     * Creates an id from its full form, {@code namespace:key}.
     *
     * @param id the full id, for example {@code "bend:chat"}
     * @return the id
     * @throws NullPointerException if {@code id} is {@code null}
     * @throws IllegalArgumentException if {@code id} is longer than 64 characters or is not a
     *         valid {@code namespace:key}
     */
    public static NamespacedId of(String id) {
        Objects.requireNonNull(id, "id");
        if (id.length() > MAXIMUM_LENGTH) {
            throw new IllegalArgumentException(
                    "Namespaced id \"" + id + "\" exceeds maximum length " + MAXIMUM_LENGTH);
        }

        Matcher matcher = PATTERN.matcher(id);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Namespaced id \"" + id
                    + "\" is not a valid namespaced id (namespace:key, lowercase a-z/0-9/dash/underscore only)");
        }

        return new NamespacedId(matcher.group(1), matcher.group(2));
    }

    /**
     * Returns the namespace, the part before the colon.
     *
     * @return the namespace, for example {@code "bend"} for {@code bend:chat}
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the key, the part after the colon.
     *
     * @return the key, for example {@code "chat"} for {@code bend:chat}
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the full id, {@code namespace:key}.
     *
     * @return the full id, for example {@code "bend:chat"}
     */
    public String asString() {
        return namespace + ":" + key;
    }

    /**
     * Returns whether {@code other} is a {@code NamespacedId} with the same namespace and key.
     *
     * @param other the object to compare with
     * @return {@code true} if both ids have the same namespace and key
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NamespacedId that)) {
            return false;
        }
        return namespace.equals(that.namespace) && key.equals(that.key);
    }

    /**
     * Returns a hash code based on the namespace and key.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(namespace, key);
    }

    /**
     * Returns the full id, {@code namespace:key}; the same as {@link #asString()}.
     *
     * @return the full id
     */
    @Override
    public String toString() {
        return asString();
    }
}

package net.onestorm.bend.protocol.endpoint;

import java.util.regex.Pattern;
import lombok.Getter;
import net.onestorm.bend.protocol.exception.ProtocolException;

@Getter
@SuppressWarnings("ClassCanBeRecord")
public class Endpoint {

    private static final int CLIENT_BOUND = 0b001;
    private static final int SERVER_BOUND = 0b010;
    // private static final int REQUEST_RESPONSE = 0b100; // future feature
    private static final int DIRECTION_BITS = CLIENT_BOUND | SERVER_BOUND;

    private static final int MAXIMUM_NAME_LENGTH = 64;
    // namespace:key, both lowercase a-z/0-9/dash/underscore, exactly one colon separator
    private static final Pattern NAMESPACED_ID_PATTERN = Pattern.compile("[a-z0-9_-]+:[a-z0-9_-]+");

    private final int id;
    private final String name;
    private final int flags;

    public Endpoint(int id, String name, int flags) {
        if ((flags & DIRECTION_BITS) == 0) {
            throw new ProtocolException(
                    "Endpoint flags must set at least one direction bit (clientbound/serverbound), got " + flags);
        }
        if (name.length() > MAXIMUM_NAME_LENGTH) {
            throw new ProtocolException("Endpoint name \"" + name + "\" exceeds maximum length " + MAXIMUM_NAME_LENGTH);
        }
        if (!NAMESPACED_ID_PATTERN.matcher(name).matches()) {
            throw new ProtocolException("Endpoint name \"" + name
                    + "\" is not a valid namespaced id (namespace:key, lowercase a-z/0-9/dash/underscore only)");
        }

        this.id = id;
        this.name = name;
        this.flags = flags;
    }

    public boolean isClientBound() {
        return (flags & CLIENT_BOUND) != 0;
    }

    public boolean isServerBound() {
        return (flags & SERVER_BOUND) != 0;
    }

    public static Endpoint of(int id, String name, boolean isClientBound, boolean isServerBound) {
        int flags = 0;

        if (isClientBound) {
            flags |= CLIENT_BOUND;
        }

        if (isServerBound) {
            flags |= SERVER_BOUND;
        }

        return new Endpoint(id, name, flags);
    }
}

package net.onestorm.bend.protocol.exception;

import lombok.Getter;
import lombok.Setter;

public class ProtocolException extends RuntimeException {

    @Getter
    @Setter
    private static volatile boolean debugStackTraces = false;

    public ProtocolException(String message) {
        super(message, null, false, debugStackTraces);
    }
}

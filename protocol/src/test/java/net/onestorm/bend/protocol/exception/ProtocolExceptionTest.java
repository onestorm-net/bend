package net.onestorm.bend.protocol.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProtocolExceptionTest {

    @BeforeEach
    @AfterEach
    void resetDebugFlag() {
        // debugStackTraces is process-wide static state, shared with every other test in this
        // module — always leave it back at the real default so other tests aren't order-dependent.
        ProtocolException.setDebugStackTraces(false);
    }

    @Test
    void message_isPreserved() {
        ProtocolException exception = new ProtocolException("something went wrong");
        assertThat(exception.getMessage()).isEqualTo("something went wrong");
    }

    @Test
    void debugStackTraces_offByDefault() {
        assertThat(ProtocolException.isDebugStackTraces()).isFalse();
    }

    @Test
    void debugStackTraces_disabled_noStackTraceCaptured() {
        ProtocolException exception = new ProtocolException("no trace");
        assertThat(exception.getStackTrace()).isEmpty();
    }

    @Test
    void debugStackTraces_enabled_capturesRealStackTrace() {
        ProtocolException.setDebugStackTraces(true);
        assertThat(ProtocolException.isDebugStackTraces()).isTrue();

        ProtocolException exception = new ProtocolException("with trace");
        assertThat(exception.getStackTrace()).isNotEmpty();
        assertThat(exception.getStackTrace()[0].getMethodName())
                .isEqualTo("debugStackTraces_enabled_capturesRealStackTrace");
    }
}

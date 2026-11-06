package net.onestorm.bend.client.api;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class ConnectionListenerTest {

    // A listener overrides only the method it needs; the other one must do nothing.
    private final ConnectionListener listener = new ConnectionListener() {
    };

    @Test
    void onConnect_notOverridden_doesNothing() {
        assertThatCode(listener::onConnect).doesNotThrowAnyException();
    }

    @Test
    void onDisconnect_notOverridden_doesNothing() {
        DisconnectInfo info = DisconnectInfo.of(DisconnectOrigin.CLOSED, DisconnectType.GENERIC, "");

        assertThatCode(() -> listener.onDisconnect(info)).doesNotThrowAnyException();
    }
}

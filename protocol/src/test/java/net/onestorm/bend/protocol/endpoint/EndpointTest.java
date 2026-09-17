package net.onestorm.bend.protocol.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.onestorm.bend.protocol.exception.ProtocolException;
import org.junit.jupiter.api.Test;

class EndpointTest {

    @Test
    void of_bothBound_setsBothFlags() {
        Endpoint endpoint = Endpoint.of(1, "test:endpoint", true, true);

        assertThat(endpoint.getId()).isEqualTo(1);
        assertThat(endpoint.getName()).isEqualTo("test:endpoint");
        assertThat(endpoint.isClientBound()).isTrue();
        assertThat(endpoint.isServerBound()).isTrue();
    }

    @Test
    void of_clientBoundOnly() {
        Endpoint endpoint = Endpoint.of(1, "test:endpoint", true, false);

        assertThat(endpoint.isClientBound()).isTrue();
        assertThat(endpoint.isServerBound()).isFalse();
    }

    @Test
    void of_serverBoundOnly() {
        Endpoint endpoint = Endpoint.of(1, "test:endpoint", false, true);

        assertThat(endpoint.isClientBound()).isFalse();
        assertThat(endpoint.isServerBound()).isTrue();
    }

    @Test
    void of_neitherBound_throws() {
        // spec: "At least one direction bit MUST be set"
        assertThatThrownBy(() -> Endpoint.of(1, "test:endpoint", false, false))
                .isInstanceOf(ProtocolException.class)
                .hasMessageContaining("direction bit");
    }

    @Test
    void flags_constructedDirectly_areInterpretedAsBitset() {
        Endpoint endpoint = new Endpoint(1, "test:endpoint", 0b011);

        assertThat(endpoint.isClientBound()).isTrue();
        assertThat(endpoint.isServerBound()).isTrue();
        assertThat(endpoint.getFlags()).isEqualTo(0b011);
    }

    @Test
    void flags_zero_throws() {
        assertThatThrownBy(() -> new Endpoint(1, "test:endpoint", 0))
                .isInstanceOf(ProtocolException.class)
                .hasMessageContaining("direction bit");
    }

    @Test
    void name_withoutColon_throws() {
        assertThatThrownBy(() -> new Endpoint(1, "noNamespace", 0b011))
                .isInstanceOf(ProtocolException.class)
                .hasMessageContaining("namespaced id");
    }

    @Test
    void name_uppercase_throws() {
        assertThatThrownBy(() -> new Endpoint(1, "Test:Endpoint", 0b011))
                .isInstanceOf(ProtocolException.class)
                .hasMessageContaining("namespaced id");
    }

    @Test
    void name_tooLong_throws() {
        String longName = "a".repeat(60) + ":" + "b".repeat(10);
        assertThatThrownBy(() -> new Endpoint(1, longName, 0b011))
                .isInstanceOf(ProtocolException.class)
                .hasMessageContaining("exceeds maximum length");
    }
}

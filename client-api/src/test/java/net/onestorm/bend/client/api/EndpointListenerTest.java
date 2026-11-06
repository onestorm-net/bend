package net.onestorm.bend.client.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class EndpointListenerTest {

    private static final IncomingEndpoint<String> ENDPOINT = new IncomingEndpoint<>() {
        @Override
        public NamespacedId getId() {
            return NamespacedId.of("bend:test");
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void unregister() {
        }
    };

    // A listener overrides only the method it needs; the other one must do nothing.
    @Test
    void onAvailable_notOverridden_doesNothing() {
        EndpointListener<IncomingEndpoint<String>> listener = new EndpointListener<>() {
        };

        assertThatCode(() -> listener.onAvailable(ENDPOINT)).doesNotThrowAnyException();
    }

    @Test
    void onUnavailable_notOverridden_doesNothing() {
        EndpointListener<IncomingEndpoint<String>> listener = new EndpointListener<>() {
        };

        assertThatCode(() -> listener.onUnavailable(ENDPOINT)).doesNotThrowAnyException();
    }

    // An incoming endpoint's handler is its listener; a lambda for handle() must stay enough.
    @Test
    void endpointHandler_lambda_handlesAndIgnoresAvailability() {
        List<String> received = new ArrayList<>();
        EndpointHandler<String> handler = received::add;

        handler.onAvailable(ENDPOINT);
        handler.handle("hello");
        handler.onUnavailable(ENDPOINT);

        assertThat(received).containsExactly("hello");
    }
}

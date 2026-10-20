package net.onestorm.bend.client.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NamespacedIdTest {

    // spec: protocol-v1.md § Namespaced Id/Key, "The total length is maximum 64 characters".
    // Written out, not read from NamespacedId, so a changed limit in the class fails these tests.
    private static final int MAXIMUM_LENGTH = 64;

    @Test
    void of_validId_exposesParts() {
        NamespacedId id = NamespacedId.of("bend:chat");

        assertThat(id.getNamespace()).isEqualTo("bend");
        assertThat(id.getKey()).isEqualTo("chat");
        assertThat(id.asString()).isEqualTo("bend:chat");
        assertThat(id.toString()).isEqualTo("bend:chat");
    }

    @Test
    void of_allAllowedCharacters_accepted() {
        NamespacedId id = NamespacedId.of("az09_-:az09_-");

        assertThat(id.getNamespace()).isEqualTo("az09_-");
        assertThat(id.getKey()).isEqualTo("az09_-");
    }

    @Test
    void of_exactlyMaximumLength_accepted() {
        String value = "a".repeat(MAXIMUM_LENGTH - 2) + ":b";

        assertThat(NamespacedId.of(value).asString()).isEqualTo(value);
    }

    @Test
    void of_overMaximumLength_throws() {
        String value = "a".repeat(MAXIMUM_LENGTH - 1) + ":b";

        assertThatThrownBy(() -> NamespacedId.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maximum length");
    }

    // spec: namespace:key, both lowercase a-z/0-9/dash/underscore, exactly one colon
    @ParameterizedTest
    @ValueSource(strings = {"", ":", "a:", ":b", "ab", "a:b:c", "A:b", "a:B", "a b:c", "a.b:c", "a:b/c", "é:b"})
    void of_invalidFormat_throws(String value) {
        assertThatThrownBy(() -> NamespacedId.of(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\"" + value + "\"");
    }

    @Test
    void of_null_throws() {
        assertThatThrownBy(() -> NamespacedId.of(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void equals_sameValue_equalWithSameHashCode() {
        NamespacedId first = NamespacedId.of("bend:chat");
        NamespacedId second = NamespacedId.of("bend:chat");

        assertThat(first).isEqualTo(second);
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    @Test
    void equals_differentValue_notEqual() {
        NamespacedId id = NamespacedId.of("bend:chat");

        assertThat(id).isNotEqualTo(NamespacedId.of("bend:chat2"));
        assertThat(id).isNotEqualTo(NamespacedId.of("other:chat"));
    }

    @Test
    void equals_otherTypeOrNull_notEqual() {
        NamespacedId id = NamespacedId.of("bend:chat");

        assertThat(id.equals("bend:chat")).isFalse();
        assertThat(id.equals(null)).isFalse();
    }
}

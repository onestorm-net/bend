package net.onestorm.bend.client.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class DisconnectTypeTest {

    // spec: protocol-v1.md § Disconnect, type table: 0 Generic, 1 Protocol error,
    // 2 Unsupported version, 3 Timeout. Written out, not read from DisconnectType, so a changed id
    // in the enum fails this test.
    @Test
    void getId_eachType_matchesSpecTable() {
        assertThat(DisconnectType.GENERIC.getId()).isEqualTo(0);
        assertThat(DisconnectType.PROTOCOL_ERROR.getId()).isEqualTo(1);
        assertThat(DisconnectType.UNSUPPORTED_VERSION.getId()).isEqualTo(2);
        assertThat(DisconnectType.TIMEOUT.getId()).isEqualTo(3);
        assertThat(DisconnectType.values()).hasSize(4);
    }

    @ParameterizedTest
    @EnumSource(DisconnectType.class)
    void fromId_eachSpecId_returnsType(DisconnectType type) {
        assertThat(DisconnectType.fromId(type.getId())).isSameAs(type);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 4, Integer.MAX_VALUE})
    void fromId_unknownId_throws(int id) {
        assertThatThrownBy(() -> DisconnectType.fromId(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(id));
    }
}

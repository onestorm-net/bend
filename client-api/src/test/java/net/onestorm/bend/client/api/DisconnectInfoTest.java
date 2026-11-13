package net.onestorm.bend.client.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DisconnectInfoTest {

    @Test
    void of_validArguments_returnsThem() {
        DisconnectInfo info = DisconnectInfo.of(DisconnectType.TIMEOUT, "no keep alive");

        assertThat(info.getType()).isEqualTo(DisconnectType.TIMEOUT);
        assertThat(info.getReason()).isEqualTo("no keep alive");
    }

    @Test
    void of_emptyReason_accepted() {
        DisconnectInfo info = DisconnectInfo.of(DisconnectType.GENERIC, "");

        assertThat(info.getReason()).isEmpty();
    }

    @Test
    void of_nullType_throws() {
        assertThatThrownBy(() -> DisconnectInfo.of(null, "reason"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");
    }

    @Test
    void of_nullReason_throws() {
        assertThatThrownBy(() -> DisconnectInfo.of(DisconnectType.GENERIC, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void equals_sameFields_equalWithSameHashCode() {
        DisconnectInfo info = DisconnectInfo.of(DisconnectType.PROTOCOL_ERROR, "bad");
        DisconnectInfo same = DisconnectInfo.of(DisconnectType.PROTOCOL_ERROR, "bad");

        assertThat(info).isEqualTo(same).hasSameHashCodeAs(same);
    }

    @Test
    void equals_differentField_notEqual() {
        DisconnectInfo info = DisconnectInfo.of(DisconnectType.PROTOCOL_ERROR, "bad");

        assertThat(info)
                .isNotEqualTo(DisconnectInfo.of(DisconnectType.TIMEOUT, "bad"))
                .isNotEqualTo(DisconnectInfo.of(DisconnectType.PROTOCOL_ERROR, "other"))
                .isNotEqualTo("bad");
    }

    @Test
    void toString_containsFields() {
        DisconnectInfo info = DisconnectInfo.of(DisconnectType.GENERIC, "connection reset");

        assertThat(info.toString()).contains("GENERIC", "connection reset");
    }
}

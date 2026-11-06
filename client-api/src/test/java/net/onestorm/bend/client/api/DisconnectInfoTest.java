package net.onestorm.bend.client.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DisconnectInfoTest {

    @Test
    void of_validArguments_returnsThem() {
        DisconnectInfo info = DisconnectInfo.of(DisconnectOrigin.SERVER, DisconnectType.TIMEOUT,
                "no keep alive");

        assertThat(info.getOrigin()).isEqualTo(DisconnectOrigin.SERVER);
        assertThat(info.getType()).isEqualTo(DisconnectType.TIMEOUT);
        assertThat(info.getReason()).isEqualTo("no keep alive");
    }

    @Test
    void of_emptyReason_accepted() {
        DisconnectInfo info = DisconnectInfo.of(DisconnectOrigin.SERVER, DisconnectType.GENERIC, "");

        assertThat(info.getReason()).isEmpty();
    }

    @Test
    void of_nullOrigin_throws() {
        assertThatThrownBy(() -> DisconnectInfo.of(null, DisconnectType.GENERIC, "reason"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("origin");
    }

    @Test
    void of_nullType_throws() {
        assertThatThrownBy(() -> DisconnectInfo.of(DisconnectOrigin.CLIENT, null, "reason"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");
    }

    @Test
    void of_nullReason_throws() {
        assertThatThrownBy(() -> DisconnectInfo.of(DisconnectOrigin.CLIENT, DisconnectType.GENERIC, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("reason");
    }

    @Test
    void equals_sameFields_equalWithSameHashCode() {
        DisconnectInfo info = DisconnectInfo.of(DisconnectOrigin.CLIENT, DisconnectType.PROTOCOL_ERROR, "bad");
        DisconnectInfo same = DisconnectInfo.of(DisconnectOrigin.CLIENT, DisconnectType.PROTOCOL_ERROR, "bad");

        assertThat(info).isEqualTo(same).hasSameHashCodeAs(same);
    }

    @Test
    void equals_differentField_notEqual() {
        DisconnectInfo info = DisconnectInfo.of(DisconnectOrigin.CLIENT, DisconnectType.PROTOCOL_ERROR, "bad");

        assertThat(info)
                .isNotEqualTo(DisconnectInfo.of(DisconnectOrigin.SERVER, DisconnectType.PROTOCOL_ERROR, "bad"))
                .isNotEqualTo(DisconnectInfo.of(DisconnectOrigin.CLIENT, DisconnectType.TIMEOUT, "bad"))
                .isNotEqualTo(DisconnectInfo.of(DisconnectOrigin.CLIENT, DisconnectType.PROTOCOL_ERROR, "other"))
                .isNotEqualTo("bad");
    }

    @Test
    void toString_containsFields() {
        DisconnectInfo info = DisconnectInfo.of(DisconnectOrigin.CONNECTION, DisconnectType.GENERIC,
                "connection reset");

        assertThat(info.toString()).contains("CONNECTION", "GENERIC", "connection reset");
    }
}

package uk.gov.hmcts.reform.sendletter.model;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class EncryptedInfoTest {

    @Test
    void should_set_values_when_initialised() {
        String fileName = "test.zip";
        byte[] bytes = "test data".getBytes(StandardCharsets.UTF_8);

        EncryptedInfo encryptedInfo = new EncryptedInfo(
                fileName,
                bytes
        );

        assertThat(encryptedInfo.getFileName())
                .isEqualTo(fileName);
        assertThat(encryptedInfo.getData())
                .isEqualTo(bytes);
    }

}
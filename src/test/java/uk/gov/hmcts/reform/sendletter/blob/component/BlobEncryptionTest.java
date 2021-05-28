package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.specialized.BlobInputStream;
import com.google.common.io.Resources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.exceptions.EncryptionException;
import uk.gov.hmcts.reform.sendletter.model.EncryptedInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.google.common.io.Resources.getResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BlobEncryptionTest {
    @Mock
    private BlobClient blobClient;
    @Mock
    private BlobInputStream blobInputStream;

    private BlobEncryption blobEncryption;

    @BeforeEach
    void setUp() {
        try {
            blobEncryption = new BlobEncryption(
                    new String(loadPublicKey())
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void should_encrypt_zip_file_when_for_valid_blob() throws IOException {
        given(blobInputStream.readAllBytes())
                .willReturn("test data".getBytes(StandardCharsets.UTF_8));
        given(blobClient.openInputStream())
                .willReturn(blobInputStream);
        given(blobClient.getBlobName())
                .willReturn("BULKPRINT001_sendlettertests_27052021124153_ddcea411-42a8-4134-bad8-e9b5bee84d24.zip");
        EncryptedInfo process = blobEncryption.process(blobClient);
        assertThat(process.getFileName())
                .isNotEqualTo("BULKPRINT001_sendlettertests_27052021124153_ddcea411-42a8-4134-bad8-e9b5bee84d24.pgp");

        String[] encryptedFile = process.getFileName().split("_");
        assertThat(LocalDateTime.parse(
                encryptedFile[2],
                BlobEncryption.dateTimeFormatter)
                .toLocalDate())
                .isEqualTo(LocalDate.now());

        assertThat(process.getFileName())
                .matches("BULKPRINT001_sendlettertests_\\d{14}_ddcea411-42a8-4134-bad8-e9b5bee84d24.pgp");
        assertThat(process.getData()).isNotNull();
    }

    @Test
    void should_encrypted_exception_when_blob_input_strem_is_invaid() {
        given(blobClient.openInputStream())
                .willThrow(new RuntimeException("Invalid data"));
        assertThatThrownBy(() -> blobEncryption.process(blobClient))
                .isInstanceOf(EncryptionException.class)
                .hasMessage("java.lang.RuntimeException: Invalid data");
    }




    private byte[] loadPublicKey() throws IOException {
        return Resources.toByteArray(getResource("encryption/pubkey.asc"));
    }
}
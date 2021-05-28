package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.model.EncryptedInfo;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BiFunction;

import static java.util.List.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BlobUploadTest {
    @Mock
    private BlobManager blobManager;
    @Mock
    private SasTokenGeneratorService sasTokenGeneratorService;
    @Mock
    private BlobClient blobClient;
    private BlobUpload blobUpload;
    private AccessTokenProperties accessTokenProperties;

    @BeforeEach
    void setUp() {
        createAccessTokenConfig();
        blobUpload = new BlobUpload(
            blobManager,
            sasTokenGeneratorService,
            accessTokenProperties
        );
    }

    @Test
    void should_upload_blob_when_sas_token_is_valid() {
        var encryptedInfo = new EncryptedInfo(
                "encrypted.pgp",
                "Encrypted Data".getBytes(StandardCharsets.UTF_8));
        String encrypted = "encrypted";
        String sasToken = "sasToken";
        given(sasTokenGeneratorService.generateSasToken(encrypted))
                .willReturn(sasToken);
        given(blobManager.getBlobClient(
                encrypted,
                sasToken,
                encryptedInfo.getFileName()
        ))
                .willReturn(blobClient);

        blobUpload.process(encryptedInfo);

        verify(sasTokenGeneratorService).generateSasToken(encrypted);
        verify(blobManager).getBlobClient(
                encrypted,
                sasToken,
                encryptedInfo.getFileName()
        );
        ArgumentCaptor<ByteArrayInputStream> byteCaptor = ArgumentCaptor.forClass(ByteArrayInputStream.class);
        long dataLength = encryptedInfo.getData().length;
        verify(blobClient).upload(
                byteCaptor.capture(),
                eq(dataLength));
        ByteArrayInputStream value = byteCaptor.getValue();
        byte[] bytes = value.readAllBytes();
        assertThat(bytes).contains(encryptedInfo.getData());
    }

    private void createAccessTokenConfig() {
        BiFunction<String, String, AccessTokenProperties.TokenConfig> tokenFunction = (type, container) -> {
            AccessTokenProperties.TokenConfig tokenConfig = new AccessTokenProperties.TokenConfig();
            tokenConfig.setValidity(300);
            tokenConfig.setContainerType(type);
            tokenConfig.setContainerName(container);
            return tokenConfig;
        };
        accessTokenProperties = new AccessTokenProperties();
        accessTokenProperties.setServiceConfig(
                of(
                        tokenFunction.apply("source", "zipped"),
                        tokenFunction.apply("destination", "encrypted")

                )
        );
    }
}
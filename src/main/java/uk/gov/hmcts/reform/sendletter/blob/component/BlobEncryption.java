package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.exceptions.EncryptionException;
import uk.gov.hmcts.reform.sendletter.model.EncryptedInfo;

@Component
public class BlobEncryption {
    private final String encryptionPublicKey;

    public BlobEncryption(
            @Value("${encryption.publicKey}") String encryptionPublicKey) {
        this.encryptionPublicKey = encryptionPublicKey;
    }

    public EncryptedInfo process(BlobClient sourceBlobClient) {
        try (var data = sourceBlobClient.openInputStream()) {
            byte[] bytes = data.readAllBytes();
            //TODO: Encyrpt above data
            return new EncryptedInfo(
                    sourceBlobClient.getBlobName(),
                    bytes
            );
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }
}

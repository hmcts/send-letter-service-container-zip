package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import org.apache.http.util.Asserts;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.exceptions.EncryptionException;
import uk.gov.hmcts.reform.sendletter.model.EncryptedInfo;
import uk.gov.hmcts.reform.sendletter.services.encryption.PgpEncryptionUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ofPattern;

@Component
public class BlobEncryption {
    public static final DateTimeFormatter dateTimeFormatter = ofPattern("ddMMyyyyHHmmss");
    private final PGPPublicKey pgpPublicKey;

    public BlobEncryption(
            @Value("${encryption.publicKey}") String encryptionPublicKey) {
        this.pgpPublicKey = loadPgpPublicKey(encryptionPublicKey);
    }

    public EncryptedInfo process(BlobClient sourceBlobClient) {
        try (var data = sourceBlobClient.openInputStream()) {
            byte[] bytes = data.readAllBytes();
            byte[] encryptData = PgpEncryptionUtil.encryptFile(
                    bytes,
                    sourceBlobClient.getBlobName(),
                    pgpPublicKey);

            return new EncryptedInfo(
                    getEncryptedFileName(sourceBlobClient.getBlobName()),
                    encryptData
            );
        } catch (Exception e) {
            throw new EncryptionException(e);
        }
    }

    private PGPPublicKey loadPgpPublicKey(String encryptionPublicKey) {
        Asserts.notNull(encryptionPublicKey, "encryptionPublicKey");
        return PgpEncryptionUtil.loadPublicKey(encryptionPublicKey.getBytes());
    }

    private String getEncryptedFileName(String zipFileName) {
        String fileName = zipFileName.replace(".zip", ".pgp");
        String[] fileSplit = fileName.split("_");
        var now = LocalDateTime.now();
        return String.join("_",
                fileSplit[0],
                fileSplit[1],
                now.format(dateTimeFormatter),
                fileSplit[3]
        );
    }
}

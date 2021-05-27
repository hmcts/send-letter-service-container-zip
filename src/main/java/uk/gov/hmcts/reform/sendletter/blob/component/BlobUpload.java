package uk.gov.hmcts.reform.sendletter.blob.component;

import com.azure.storage.blob.BlobClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.config.AccessTokenProperties;
import uk.gov.hmcts.reform.sendletter.model.EncryptedInfo;
import uk.gov.hmcts.reform.sendletter.services.SasTokenGeneratorService;

import java.io.ByteArrayInputStream;

@Component
public class BlobUpload {
    private static final Logger LOG = LoggerFactory.getLogger(BlobUpload.class);
    private final BlobManager blobManager;
    private final SasTokenGeneratorService sasTokenGeneratorService;
    private final String destinationContainer;

    public BlobUpload(
            BlobManager blobManager,
            SasTokenGeneratorService sasTokenGeneratorService,
            AccessTokenProperties accessTokenProperties) {
        this.blobManager = blobManager;
        this.sasTokenGeneratorService = sasTokenGeneratorService;
        this.destinationContainer = accessTokenProperties
                .getContainerForGivenType("destination");
    }

    public boolean process(
            EncryptedInfo encryptedInfo) {
        LOG.info("About to upload blob {} to container {}",
                encryptedInfo.getFileName(),
                destinationContainer);
        String sasToken = sasTokenGeneratorService.generateSasToken(destinationContainer);
        String fileName = encryptedInfo.getFileName();
        BlobClient blobClient = blobManager.getBlobClient(
                destinationContainer,
                sasToken,
                fileName
        );

        blobClient.upload(
                new ByteArrayInputStream(
                        encryptedInfo.getData()
                ),
                encryptedInfo.getData().length);
        return true;
    }
}

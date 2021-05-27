package uk.gov.hmcts.reform.sendletter.blob;

import com.azure.core.util.Context;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobEncryption;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobReader;
import uk.gov.hmcts.reform.sendletter.blob.component.BlobUpload;
import uk.gov.hmcts.reform.sendletter.exceptions.LeaseIdNotPresentException;
import uk.gov.hmcts.reform.sendletter.model.BlobInfo;

import java.util.Optional;

@Service
public class BlobProcessor {
    private static final Logger LOG = LoggerFactory.getLogger(BlobProcessor.class);
    private final BlobReader blobReader;
    private final BlobUpload blobUpload;
    private final BlobEncryption blobEncryption;

    public BlobProcessor(
            BlobReader blobReader,
            BlobUpload blobUpload,
            BlobEncryption blobEncryption) {
        this.blobReader = blobReader;
        this.blobUpload = blobUpload;
        this.blobEncryption = blobEncryption;
    }

    public boolean read() {
        LOG.info("BlobProcessor:: processing blob");
        Optional<BlobInfo> mayBeBlobInfo = blobReader.retrieveBlobToProcess();
        if (mayBeBlobInfo.isPresent()) {
            var blobInfo = mayBeBlobInfo.get();
            var blobClient = blobInfo.getBlobClient();
            var encryptedInfo = blobEncryption.process(blobClient);
            blobUpload.process(encryptedInfo);
            String leaseId = blobInfo.getLeaseId()
                    .orElseThrow(() ->
                            new LeaseIdNotPresentException("Lease not present"));
            blobClient.deleteWithResponse(
                    DeleteSnapshotsOptionType.INCLUDE,
                    new BlobRequestConditions().setLeaseId(leaseId),
                    null,
                    Context.NONE);
        }
        return mayBeBlobInfo.isPresent();
    }
}

package uk.gov.hmcts.reform.sendletter.model;

import com.azure.storage.blob.BlobClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class BlobInfoTest {

    @Mock
    private BlobClient blobClient;

    private BlobInfo blobInfo;

    @BeforeEach
    void setUp() {
        blobInfo = new BlobInfo(blobClient);
    }

    @Test
    void should_state_lease_id_is_not_present_when_not_initialised() {
        assertThat(blobInfo.isLeased())
                .isFalse();
    }

    @Test
    void should_state_lease_id_is_present_when_leased_is_set() {
        blobInfo.setLeaseId("lease_id");
        assertThat(blobInfo.isLeased())
                .isTrue();
    }
}
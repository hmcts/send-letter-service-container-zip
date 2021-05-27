package uk.gov.hmcts.reform.sendletter.exceptions;

public class EncryptionException extends RuntimeException {
    public EncryptionException(Exception e) {
        super(e);
    }
}

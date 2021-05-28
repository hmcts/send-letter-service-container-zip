package uk.gov.hmcts.reform.sendletter.model;

public class EncryptedInfo {
    private final String fileName;
    private final byte[] data;

    public EncryptedInfo(String fileName, byte[] data) {
        this.fileName = fileName;
        this.data = data;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getData() {
        return data;
    }
}

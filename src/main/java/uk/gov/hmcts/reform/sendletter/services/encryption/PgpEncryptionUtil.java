package uk.gov.hmcts.reform.sendletter.services.encryption;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.bc.BcPGPPublicKeyRing;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.security.Security;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Optional;

import static org.bouncycastle.bcpg.CompressionAlgorithmTags.ZIP;
import static org.bouncycastle.openpgp.PGPEncryptedData.AES_256;
import static org.bouncycastle.openpgp.PGPUtil.getDecoderStream;

public final class PgpEncryptionUtil {

    // Prevent instantiation.
    private PgpEncryptionUtil() {
    }

    /**
     * Encrypts the given byte array using PGP encryption using AES 256 algorithm.
     * This method assumes that the temporary volume is writable
     *
     * @param inputFile     input file byte array
     * @param inputFileName input file name to be encrypted
     * @param pgpPublicKey  key used to encrypt file
     *
     * @return PGP encrypted byte array
     */
    public static byte[] encryptFile(
        byte[] inputFile,
        String inputFileName,
        PGPPublicKey pgpPublicKey
    ) {
        try {
            Security.addProvider(new BouncyCastleProvider());

            var byteArrayOutputStream =
                compressAndWriteFileToLiteralData(
                    inputFile,
                    inputFileName
                );

            PGPEncryptedDataGenerator encryptedDataGenerator = prepareDataEncryptor(pgpPublicKey);

            return writeEncryptedDataToOutputStream(byteArrayOutputStream, encryptedDataGenerator);
        } catch (Exception exc) {
            throw new UnableToPgpEncryptZipFileException(exc);
        }
    }

    /**
     * Returns raw key bytes as a Bouncy Castle PGP public key.
     */
    public static PGPPublicKey loadPublicKey(byte[] data) {
        try {
            return lookupPublicSubkey(
                new BcPGPPublicKeyRing(
                    getDecoderStream(new ByteArrayInputStream(data))
                )
            ).orElseThrow(() -> new UnableToLoadPgpPublicKeyException(null));
        } catch (IOException e) {
            throw new UnableToLoadPgpPublicKeyException(e);
        }
    }

    /**
     * Return appropriate key or subkey for given task from public key.
     * Weirder older PGP public keys will actually have multiple keys. The main key will usually
     * be sign-only in such situations. So you've gotta go digging in through the key packets and
     * make sure you get the one that's valid for encryption.
     */
    private static Optional<PGPPublicKey> lookupPublicSubkey(PGPPublicKeyRing ring) {
        Iterator<PGPPublicKey> keys = ring.getPublicKeys();
        while (keys.hasNext()) {
            PGPPublicKey key = keys.next();
            if (key.isEncryptionKey()) {
                return Optional.of(key);
            }
        }
        return Optional.empty();
    }

    private static byte[] writeEncryptedDataToOutputStream(
        ByteArrayOutputStream bout,
        PGPEncryptedDataGenerator encryptedDataGenerator
    ) throws IOException, PGPException {
        byte[] bytes = bout.toByteArray();

        var byteArrayOutputStream = new ByteArrayOutputStream();
        try (var outputStream = encryptedDataGenerator.open(byteArrayOutputStream, bytes.length)) {
            outputStream.write(bytes);
        }

        return byteArrayOutputStream.toByteArray();
    }

    private static PGPEncryptedDataGenerator prepareDataEncryptor(PGPPublicKey pgpPublicKey) {
        var dataEncryptor = new BcPGPDataEncryptorBuilder(AES_256);
        dataEncryptor.setWithIntegrityPacket(true);
        dataEncryptor.setSecureRandom(new SecureRandom());

        var encryptedDataGenerator = new PGPEncryptedDataGenerator(dataEncryptor);
        encryptedDataGenerator.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(pgpPublicKey));
        return encryptedDataGenerator;
    }

    private static ByteArrayOutputStream compressAndWriteFileToLiteralData(
        byte[] inputFile,
        String fileName
    ) throws IOException {
        var byteArrayOutputStream = new ByteArrayOutputStream();

        var pgpCompressedDataGenerator =
            new PGPCompressedDataGenerator(ZIP);

        //Creates an empty file in the default temporary-file directory
        var tempFile = createTempFile(inputFile, fileName);

        try (var out = pgpCompressedDataGenerator.open(byteArrayOutputStream)) {
            PGPUtil.writeFileToLiteralData(
                out,
                PGPLiteralData.BINARY,
                tempFile
            );
        }

        return byteArrayOutputStream;
    }

    private static File createTempFile(
            byte[] inputFile,
            String fileName
    ) throws IOException {
        var tempDir = Files.createTempDirectory("pg",
                PosixFilePermissions.asFileAttribute(EnumSet.allOf(PosixFilePermission.class)));
        var tempFile = new File(tempDir.toAbsolutePath().toFile(), fileName);
        try (var fos = new FileOutputStream(tempFile)) {
            fos.write(inputFile);
            return tempFile;
        }
    }
}

package io.stackgres.slon.ssl;

import sun.security.x509.*;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.UserPrincipal;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.Set;

import static java.nio.file.attribute.PosixFilePermission.*;

public class TlsCertificates {

    private static final System.Logger logger = System.getLogger("TlsCertificates");

    private static final String KEY_ALGORITHM = "RSA";
    private static final int KEY_SIZE = 2048;
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final long VALIDITY_DAYS = 3650;

    private TlsCertificates() {
    }

    public static void generateSelfSigned(String clusterName, Path pgData) {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);
            keyGen.initialize(KEY_SIZE, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();

            X509Certificate cert = generateCertificate(clusterName, keyPair);

            Path certPath = pgData.resolve("server.crt");
            Path keyPath = pgData.resolve("server.key");

            writePem(certPath, "CERTIFICATE", cert.getEncoded());
            writePem(keyPath, "PRIVATE KEY", keyPair.getPrivate().getEncoded());

            Files.setPosixFilePermissions(keyPath, Set.of(OWNER_READ, OWNER_WRITE));
            Files.setPosixFilePermissions(certPath, Set.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ));
            chownPostgres(keyPath);
            chownPostgres(certPath);

            logger.log(System.Logger.Level.INFO, "Generated self-signed TLS certificate for CN={0}", clusterName);
        } catch (NoSuchAlgorithmException | CertificateException | IOException | InvalidKeyException
                 | NoSuchProviderException | SignatureException e) {
            throw new RuntimeException("Failed to generate self-signed TLS certificate", e);
        }
    }

    private static X509Certificate generateCertificate(String clusterName, KeyPair keyPair)
            throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, SignatureException {
        X500Name owner = new X500Name("CN=" + clusterName + ", O=stackgres.io");

        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + VALIDITY_DAYS * 24L * 60L * 60L * 1000L);

        BigInteger serial = new BigInteger(160, new SecureRandom());

        X509CertInfo info = new X509CertInfo();
        info.setVersion(new CertificateVersion(CertificateVersion.V3));
        info.setSerialNumber(new CertificateSerialNumber(serial));
        info.setSubject(owner);
        info.setIssuer(owner);
        info.setValidity(new CertificateValidity(notBefore, notAfter));
        info.setKey(new CertificateX509Key(keyPair.getPublic()));
        info.setAlgorithmId(new CertificateAlgorithmId(AlgorithmId.get(SIGNATURE_ALGORITHM)));

        return X509CertImpl.newSigned(info, keyPair.getPrivate(), SIGNATURE_ALGORITHM);
    }

    private static void chownPostgres(Path path) throws IOException {
        UserPrincipal postgres = FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName("postgres");
        Files.setOwner(path, postgres);
    }

    private static void writePem(Path path, String type, byte[] encoded) throws IOException {
        String base64 = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(encoded);
        String pem = "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
        Files.writeString(path, pem);
    }

    public static void writeProvidedCertificates(byte[] certificate, byte[] privateKey, Path pgData) {
        try {
            Path certPath = pgData.resolve("server.crt");
            Path keyPath = pgData.resolve("server.key");

            Files.write(certPath, certificate);
            Files.write(keyPath, privateKey);

            Files.setPosixFilePermissions(keyPath, Set.of(OWNER_READ, OWNER_WRITE));
            Files.setPosixFilePermissions(certPath, Set.of(OWNER_READ, OWNER_WRITE, GROUP_READ, OTHERS_READ));
            chownPostgres(keyPath);
            chownPostgres(certPath);

            logger.log(System.Logger.Level.INFO, "Wrote provided TLS certificate and key");
        } catch (IOException e) {
            throw new RuntimeException("Failed to write provided TLS certificates", e);
        }
    }

}

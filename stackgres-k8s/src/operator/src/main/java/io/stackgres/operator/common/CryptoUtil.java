/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple3;

public interface CryptoUtil {

  String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE-----";
  String END_CERTIFICATE = "-----END CERTIFICATE-----";
  String BEGIN_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----";
  String END_PRIVATE_KEY = "-----END PRIVATE KEY-----";
  String BEGIN_PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----";
  String END_PUBLIC_KEY = "-----END PUBLIC KEY-----";

  static String generatePassword() {
    return UUID.randomUUID().toString().substring(4, 22);
  }

  static Tuple3<String, String, String> generateCaCertificateAndPrivateKey(
      String subjectString, List<String> dnsList, Instant notAfter) {
    return generateCertificateAndPrivateKey(
        subjectString,
        dnsList,
        notAfter,
        true,
        "SHA256withRSA");
  }

  static Tuple3<String, String, String> generateCertificateAndPrivateKey(
      Instant notAfter) {
    return generateCertificateAndPrivateKey(
        "CN=stackgres",
        List.of("stackgres"),
        notAfter,
        false,
        "SHA256withRSA");
  }

  static Tuple3<String, String, String> generateCertificateAndPrivateKey(
      String subjectString,
      List<String> dnsList,
      Instant notAfter,
      boolean isCertificationAuthority,
      String signatureAlgorithm) {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(4096, new SecureRandom());
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      Security.addProvider(new BouncyCastleProvider());

      X500Principal subject = new X500Principal(subjectString);

      Instant notBefore = Instant.now();

      ASN1Encodable[] encodableAltNames = dnsList.stream()
          .map(name -> new GeneralName(GeneralName.dNSName, name))
          .toArray(ASN1Encodable[]::new);
      KeyPurposeId[] purposes = new KeyPurposeId[]{
          KeyPurposeId.id_kp_serverAuth,
          KeyPurposeId.id_kp_clientAuth
      };

      X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
          subject,
          BigInteger.ONE,
          Date.from(notBefore),
          Date.from(notAfter),
          subject, keyPair.getPublic());

      certBuilder.addExtension(Extension.basicConstraints, true,
          new BasicConstraints(isCertificationAuthority));
      certBuilder.addExtension(Extension.keyUsage, true,
          new KeyUsage(KeyUsage.nonRepudiation
              + KeyUsage.digitalSignature + KeyUsage.keyEncipherment));
      certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(purposes));
      certBuilder.addExtension(Extension.subjectAlternativeName, false,
          new DERSequence(encodableAltNames));

      final ContentSigner signer = new JcaContentSignerBuilder(signatureAlgorithm)
          .build(keyPair.getPrivate());
      X509CertificateHolder certHolder = certBuilder.build(signer);
      return Tuple.tuple(
          getCertificatePem(certHolder),
          getPrivateKeyPem(keyPair),
          getPublicKeyPem(keyPair));
    } catch (IOException | OperatorCreationException | NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
  }

  static String getCertificatePem(X509CertificateHolder certHolder) throws IOException {
    return BEGIN_CERTIFICATE + System.lineSeparator()
        + Base64.getEncoder().encodeToString(
            certHolder.getEncoded()) + System.lineSeparator()
        + END_CERTIFICATE + System.lineSeparator();
  }

  static String getPrivateKeyPem(KeyPair keyPair) throws IOException {
    return BEGIN_PRIVATE_KEY + System.lineSeparator()
        + Base64.getEncoder().encodeToString(
            keyPair.getPrivate().getEncoded()) + System.lineSeparator()
        + END_PRIVATE_KEY + System.lineSeparator();
  }

  static String getPublicKeyPem(KeyPair keyPair) throws IOException {
    return BEGIN_PUBLIC_KEY + System.lineSeparator()
        + Base64.getEncoder().encodeToString(
            keyPair.getPublic().getEncoded()) + System.lineSeparator()
        + END_PUBLIC_KEY + System.lineSeparator();
  }

  static boolean isCertificateAndKeyValid(String certPem, String privateKeyPem) {
    try {
      byte[] challenge = new byte[10000];
      ThreadLocalRandom.current().nextBytes(challenge);

      X509Certificate x509Cert = (X509Certificate) CertificateFactory
          .getInstance("X509")
          .generateCertificate(new ByteArrayInputStream(certPem.getBytes(
              StandardCharsets.UTF_8)));
      Instant now = Instant.now();
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      byte[] privateKeyEncoded = Base64.getDecoder().decode(privateKeyPem
          .replaceAll("-+[^-]+-+", "")
          .replaceAll(System.lineSeparator(), ""));
      PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyEncoded);
      PrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);

      Signature sig = Signature.getInstance("SHA256withRSA");
      sig.initSign(privateKey);
      sig.update(challenge);
      byte[] signature = sig.sign();

      sig.initVerify(x509Cert.getPublicKey());
      sig.update(challenge);

      return x509Cert.getNotBefore().toInstant().isBefore(now)
          && x509Cert.getNotAfter().toInstant().isAfter(now)
          && sig.verify(signature);
    } catch (CertificateException | InvalidKeyException | NoSuchAlgorithmException
        | InvalidKeySpecException | SignatureException ex) {
      return false;
    }
  }

  static boolean isRsaKeyPairValid(String privateKeyPem, String publicKeyPem) {
    try {
      byte[] challenge = new byte[10000];
      ThreadLocalRandom.current().nextBytes(challenge);

      byte[] privateKeyEncoded = Base64.getDecoder().decode(privateKeyPem
          .replaceAll("-+[^-]+-+", "")
          .replaceAll(System.lineSeparator(), ""));
      byte[] publicKeyEncoded = Base64.getDecoder().decode(publicKeyPem
          .replaceAll("-+[^-]+-+", "")
          .replaceAll(System.lineSeparator(), ""));

      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyEncoded);
      PrivateKey privateKey = (RSAPrivateKey) keyFactory.generatePrivate(privateKeySpec);
      X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyEncoded);
      PublicKey publicKey = (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);

      Signature sig = Signature.getInstance("SHA256withRSA");
      sig.initSign(privateKey);
      sig.update(challenge);
      byte[] signature = sig.sign();

      sig.initVerify(publicKey);
      sig.update(challenge);

      return sig.verify(signature);
    } catch (InvalidKeyException | NoSuchAlgorithmException | InvalidKeySpecException
        | SignatureException ex) {
      return false;
    }
  }

  static String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(
          value.getBytes(StandardCharsets.UTF_8));
      return Hex.encodeHexString(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
  }

}

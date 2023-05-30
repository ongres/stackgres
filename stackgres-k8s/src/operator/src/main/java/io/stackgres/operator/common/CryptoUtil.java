/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.common;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

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
import org.jooq.lambda.tuple.Tuple2;

public interface CryptoUtil {

  static Tuple2<String, String> generateCertificateAndPrivateKey() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048, new SecureRandom());
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      Security.addProvider(new BouncyCastleProvider());

      X500Principal subject = new X500Principal("CN=stackgres");

      long notBefore = System.currentTimeMillis();
      long notAfter = notBefore + (1000L * 3600L * 24 * 365 * 7500);

      ASN1Encodable[] encodableAltNames = new ASN1Encodable[] {
          new GeneralName(GeneralName.dNSName, "stackgres")
          };
      KeyPurposeId[] purposes = new KeyPurposeId[]{
          KeyPurposeId.id_kp_serverAuth,
          KeyPurposeId.id_kp_clientAuth
          };

      X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(subject,
            BigInteger.ONE, new Date(notBefore), new Date(notAfter), subject, keyPair.getPublic());

      certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
      certBuilder.addExtension(Extension.keyUsage, true,
          new KeyUsage(KeyUsage.digitalSignature + KeyUsage.keyEncipherment));
      certBuilder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(purposes));
      certBuilder.addExtension(Extension.subjectAlternativeName, false,
          new DERSequence(encodableAltNames));

      final ContentSigner signer = new JcaContentSignerBuilder(("SHA256withRSA"))
          .build(keyPair.getPrivate());
      X509CertificateHolder certHolder = certBuilder.build(signer);
      return Tuple.tuple(
          getCertificatePem(certHolder),
          getPrivateKeyPem(keyPair));
    } catch (IOException | OperatorCreationException | NoSuchAlgorithmException ex) {
      throw new RuntimeException(ex);
    }
  }

  static String getCertificatePem(X509CertificateHolder certHolder) throws IOException {
    return "-----BEGIN CERTIFICATE-----\n"
        + Base64.getEncoder().encodeToString(certHolder.getEncoded())
        + "\n-----END CERTIFICATE-----\n";
  }

  static String getPrivateKeyPem(KeyPair keyPair) throws IOException {
    return "-----BEGIN RSA PRIVATE KEY-----\n"
        + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
        + "\n-----END RSA PRIVATE KEY-----\n";
  }

}

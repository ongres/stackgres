/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.x500.X500Principal;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPostgres;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpec;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.OperatorVersionBinder;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.ImmutableVolumePair;
import io.stackgres.operator.conciliation.factory.VolumeFactory;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operatorframework.resource.ResourceUtil;
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
import org.jetbrains.annotations.NotNull;
import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

@Singleton
@OperatorVersionBinder
public class PostgresSslSecret
    implements VolumeFactory<StackGresClusterContext> {

  public static final String CERTIFICATE_KEY = "tls.crt";
  public static final String PRIVATE_KEY_KEY = "tls.key";

  private static final String SSL_SUFFIX = "-ssl";

  private final LabelFactoryForCluster<StackGresCluster> labelFactory;

  public static String name(StackGresClusterContext clusterContext) {
    return name(clusterContext.getSource());
  }

  public static String name(StackGresCluster cluster) {
    return ResourceUtil.resourceName(cluster.getMetadata().getName()
        + SSL_SUFFIX);
  }

  @Inject
  public PostgresSslSecret(LabelFactoryForCluster<StackGresCluster> labelFactory) {
    this.labelFactory = labelFactory;
  }

  @Override
  public @NotNull Stream<VolumePair> buildVolumes(StackGresClusterContext context) {
    return Stream.of(
        ImmutableVolumePair.builder()
            .volume(buildVolume(context))
            .source(buildSource(context))
            .build()
    );
  }

  public @NotNull Volume buildVolume(StackGresClusterContext context) {
    return new VolumeBuilder()
        .withName(StackGresVolume.POSTGRES_SSL.getName())
        .withSecret(new SecretVolumeSourceBuilder()
            .withSecretName(name(context))
            .withDefaultMode(400)
            .build())
        .build();
  }

  public @NotNull Secret buildSource(StackGresClusterContext context) {
    final StackGresCluster cluster = context.getSource();
    final String name = name(cluster);
    final String namespace = cluster.getMetadata().getNamespace();
    final Map<String, String> labels = labelFactory.genericLabels(cluster);

    final Map<String, String> data = new HashMap<>();

    if (Optional.of(context.getSource())
        .map(StackGresCluster::getSpec)
        .map(StackGresClusterSpec::getPostgres)
        .map(StackGresClusterPostgres::getSsl)
        .map(StackGresClusterSsl::getEnabled)
        .orElse(false)) {
      setCertificateAndPrivateKey(context, data);
    }

    return new SecretBuilder()
        .withNewMetadata()
        .withNamespace(namespace)
        .withName(name)
        .withLabels(labels)
        .endMetadata()
        .withType("Opaque")
        .withData(ResourceUtil.encodeSecret(StackGresUtil.addMd5Sum(data)))
        .build();
  }

  private void setCertificateAndPrivateKey(StackGresClusterContext context,
      Map<String, String> data) {
    var certificate = context.getPostgresSslCertificate();
    var privateKey = context.getPostgresSslPrivateKey();
    if (certificate.isEmpty() || privateKey.isEmpty()) {
      var certificateAndPrivateKey = generateCertificateAndPrivateKey();
      certificate = Optional.of(certificateAndPrivateKey.v1);
      privateKey = Optional.of(certificateAndPrivateKey.v2);
    }
    data.put(CERTIFICATE_KEY, certificate.orElseThrow());
    data.put(PRIVATE_KEY_KEY, privateKey.orElseThrow());
  }

  private Tuple2<String, String> generateCertificateAndPrivateKey() {
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

  private static String getCertificatePem(X509CertificateHolder certHolder) throws IOException {
    return "-----BEGIN CERTIFICATE-----\n"
        + Base64.getEncoder().encodeToString(certHolder.getEncoded())
        + "\n-----END CERTIFICATE-----\n";
  }

  private static String getPrivateKeyPem(KeyPair keyPair) throws IOException {
    return "-----BEGIN RSA PRIVATE KEY-----\n"
        + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded())
        + "\n-----END RSA PRIVATE KEY-----\n";
  }

}

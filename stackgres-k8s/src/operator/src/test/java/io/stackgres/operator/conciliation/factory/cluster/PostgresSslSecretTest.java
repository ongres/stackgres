/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostgresSslSecretTest {

  private PostgresSslSecret postgresSslSecret;

  @Mock
  private StackGresClusterContext context;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    postgresSslSecret = new PostgresSslSecret(new ClusterLabelFactory(new ClusterLabelMapper()));
    cluster = Fixtures.cluster().loadDefault().get();
  }

  @Test
  void givenAClusterWithoutSsl_itShouldNotGenerateSslSecret() {
    when(context.getSource()).thenReturn(cluster);

    var secretVolumePairs = postgresSslSecret.buildVolumes(context).toList();

    Assertions.assertEquals(1, secretVolumePairs.size());
    Assertions.assertEquals(PostgresSslSecret.name(cluster),
        secretVolumePairs.getFirst().getSource()
        .orElseThrow()
        .getMetadata()
        .getName());
    Assertions.assertEquals(2, secretVolumePairs.getFirst().getSource()
        .map(Secret.class::cast)
        .orElseThrow()
        .getData()
        .size());
  }

  @Test
  void givenAClusterWithSslAndNoSecret_itShouldGenerateSslSecret() throws Exception {
    cluster.getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    cluster.getSpec().getPostgres().getSsl().setEnabled(true);
    when(context.getSource()).thenReturn(cluster);

    var secretVolumePairs = postgresSslSecret.buildVolumes(context).toList();

    Assertions.assertEquals(PostgresSslSecret.name(cluster),
        secretVolumePairs.getFirst().getSource()
        .orElseThrow()
        .getMetadata()
        .getName());
    Assertions.assertEquals(1, secretVolumePairs.size());
    Assertions.assertTrue(secretVolumePairs.getFirst().getSource()
        .map(Secret.class::cast)
        .map(Secret::getData)
        .map(data -> data.get(PostgresSslSecret.CERTIFICATE_KEY))
        .isPresent());
    Assertions.assertTrue(secretVolumePairs.getFirst().getSource()
        .map(Secret.class::cast)
        .map(Secret::getData)
        .map(data -> data.get(PostgresSslSecret.PRIVATE_KEY_KEY))
        .isPresent());
    checkCertificateAndPrivateKey(
        ResourceUtil.decodeSecret(
            secretVolumePairs.getFirst().getSource()
            .map(Secret.class::cast)
            .map(Secret::getData)
            .map(data -> data.get(PostgresSslSecret.CERTIFICATE_KEY))
            .orElseThrow()),
        ResourceUtil.decodeSecret(
            secretVolumePairs.getFirst().getSource()
            .map(Secret.class::cast)
            .map(Secret::getData)
            .map(data -> data.get(PostgresSslSecret.PRIVATE_KEY_KEY))
            .orElseThrow()));
  }

  @Test
  void givenAClusterWithSslAndSecret_itShouldReuseSslSecret() {
    cluster.getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    cluster.getSpec().getPostgres().getSsl().setEnabled(true);
    when(context.getSource()).thenReturn(cluster);
    when(context.getPostgresSslCertificate()).thenReturn(Optional.of("test-certificate"));
    when(context.getPostgresSslPrivateKey()).thenReturn(Optional.of("test-private-key"));

    var secretVolumePairs = postgresSslSecret.buildVolumes(context).toList();

    Assertions.assertEquals(PostgresSslSecret.name(cluster),
        secretVolumePairs.getFirst().getSource()
        .orElseThrow()
        .getMetadata()
        .getName());
    Assertions.assertEquals(1, secretVolumePairs.size());
    Assertions.assertTrue(secretVolumePairs.getFirst().getSource()
        .map(Secret.class::cast)
        .map(Secret::getData)
        .map(data -> data.get(PostgresSslSecret.CERTIFICATE_KEY))
        .isPresent());
    Assertions.assertEquals("test-certificate",
        ResourceUtil.decodeSecret(
        secretVolumePairs.getFirst().getSource()
        .map(Secret.class::cast)
        .map(Secret::getData)
        .map(data -> data.get(PostgresSslSecret.CERTIFICATE_KEY))
        .orElseThrow()));
    Assertions.assertTrue(secretVolumePairs.getFirst().getSource()
        .map(Secret.class::cast)
        .map(Secret::getData)
        .map(data -> data.get(PostgresSslSecret.PRIVATE_KEY_KEY))
        .isPresent());
    Assertions.assertEquals("test-private-key",
        ResourceUtil.decodeSecret(
        secretVolumePairs.getFirst().getSource()
        .map(Secret.class::cast)
        .map(Secret::getData)
        .map(data -> data.get(PostgresSslSecret.PRIVATE_KEY_KEY))
        .orElseThrow()));
  }

  private void checkCertificateAndPrivateKey(String certificate, String privateKey)
      throws Exception {
    String strippedPrivateKey = privateKey
            .replace("-----BEGIN PRIVATE KEY-----\n", "")
            .replace("\n-----END PRIVATE KEY-----\n", "");
    byte[] decoded = Base64.getDecoder().decode(strippedPrivateKey);

    PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
    KeyFactory kf = KeyFactory.getInstance("RSA");

    PrivateKey pk = kf.generatePrivate(keySpec);

    RSAPublicKeySpec pks = new RSAPublicKeySpec(
        ((RSAPrivateCrtKey) pk).getModulus(),
        ((RSAPrivateCrtKey) pk).getPublicExponent());

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    PublicKey pubk = keyFactory.generatePublic(pks);
    final Certificate cert;
    String strippedCertificate = certificate
        .replace("-----BEGIN CERTIFICATE-----\n", "")
        .replace("\n-----END CERTIFICATE-----\n", "");
    try (ByteArrayInputStream is = new ByteArrayInputStream(
        Base64.getDecoder().decode(strippedCertificate))) {
      CertificateFactory cf = CertificateFactory.getInstance("X.509");
      cert = cf.generateCertificate(is);
    }
    cert.verify(pubk);
  }
}

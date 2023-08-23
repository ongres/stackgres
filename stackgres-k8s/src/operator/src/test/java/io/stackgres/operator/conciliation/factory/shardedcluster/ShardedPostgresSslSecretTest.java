/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static io.stackgres.common.StackGresShardedClusterForCitusUtil.CERTIFICATE_KEY;
import static io.stackgres.common.StackGresShardedClusterForCitusUtil.PRIVATE_KEY_KEY;
import static io.stackgres.common.StackGresShardedClusterForCitusUtil.postgresSslSecretName;
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
import io.stackgres.common.crd.sgcluster.StackGresClusterSsl;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedPostgresSslSecretTest {

  private ShardedPostgresSslSecret postgresSslSecret;

  @Mock
  private StackGresShardedClusterContext context;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    postgresSslSecret = new ShardedPostgresSslSecret(
        new ShardedClusterLabelFactory(new ShardedClusterLabelMapper()));
    cluster = Fixtures.shardedCluster().loadDefault().get();
  }

  @Test
  void givenAClusterWithoutSsl_itShouldNotGenerateSslSecret() {
    when(context.getSource()).thenReturn(cluster);

    var generateResources = postgresSslSecret.generateResource(context).toList();

    Assertions.assertEquals(1, generateResources.size());
    Assertions.assertEquals(postgresSslSecretName(cluster),
        generateResources.get(0)
        .getMetadata()
        .getName());
    Assertions.assertEquals(1, Optional.of(generateResources.get(0))
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

    var generateResources = postgresSslSecret.generateResource(context).toList();

    Assertions.assertEquals(postgresSslSecretName(cluster),
        generateResources.get(0)
        .getMetadata()
        .getName());
    Assertions.assertEquals(1, generateResources.size());
    Assertions.assertTrue(Optional.of(generateResources.get(0))
        .map(Secret.class::cast)
        .map(Secret::getData)
        .map(data -> data.get(CERTIFICATE_KEY))
        .isPresent());
    Assertions.assertTrue(Optional.of(generateResources.get(0))
        .map(Secret.class::cast)
        .map(Secret::getData)
        .map(data -> data.get(PRIVATE_KEY_KEY))
        .isPresent());
    checkCertificateAndPrivateKey(
        ResourceUtil.decodeSecret(
            Optional.of(generateResources.get(0))
            .map(Secret.class::cast)
            .map(Secret::getData)
            .map(data -> data.get(CERTIFICATE_KEY))
            .orElseThrow()),
        ResourceUtil.decodeSecret(
            Optional.of(generateResources.get(0))
            .map(Secret.class::cast)
            .map(Secret::getData)
            .map(data -> data.get(PRIVATE_KEY_KEY))
            .orElseThrow()));
  }

  @Test
  void givenAClusterWithSslAndSecret_itShouldReuseSslSecret() {
    cluster.getSpec().getPostgres().setSsl(new StackGresClusterSsl());
    cluster.getSpec().getPostgres().getSsl().setEnabled(true);
    when(context.getSource()).thenReturn(cluster);
    when(context.getPostgresSslCertificate()).thenReturn(Optional.of("test-certificate"));
    when(context.getPostgresSslPrivateKey()).thenReturn(Optional.of("test-private-key"));

    var generateResources = postgresSslSecret.generateResource(context).toList();

    Assertions.assertEquals(postgresSslSecretName(cluster),
        generateResources.get(0)
        .getMetadata()
        .getName());
    Assertions.assertEquals(1, generateResources.size());
    Assertions.assertTrue(Optional.of(generateResources.get(0))
        .map(Secret.class::cast)
        .map(Secret::getData)
        .map(data -> data.get(CERTIFICATE_KEY))
        .isPresent());
    Assertions.assertEquals("test-certificate",
        ResourceUtil.decodeSecret(
            Optional.of(generateResources.get(0))
            .map(Secret.class::cast)
            .map(Secret::getData)
            .map(data -> data.get(CERTIFICATE_KEY))
            .orElseThrow()));
    Assertions.assertTrue(Optional.of(generateResources.get(0))
        .map(Secret.class::cast)
        .map(Secret::getData)
        .map(data -> data.get(PRIVATE_KEY_KEY))
        .isPresent());
    Assertions.assertEquals("test-private-key",
        ResourceUtil.decodeSecret(
            Optional.of(generateResources.get(0))
            .map(Secret.class::cast)
            .map(Secret::getData)
            .map(data -> data.get(PRIVATE_KEY_KEY))
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

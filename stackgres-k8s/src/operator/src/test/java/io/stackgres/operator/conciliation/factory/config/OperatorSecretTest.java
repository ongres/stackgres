/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ConfigLabelFactory;
import io.stackgres.common.labels.ConfigLabelMapper;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.common.CryptoUtil;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperatorSecretTest {

  private final LabelFactoryForConfig labelFactory =
      new ConfigLabelFactory(new ConfigLabelMapper());

  @Mock
  private StackGresConfigContext context;

  private OperatorSecret operatorSecret;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    operatorSecret = new OperatorSecret(labelFactory);
    config = Fixtures.config().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(config);
    lenient().when(context.getOperatorSecret()).thenReturn(Optional.empty());
  }

  @Test
  void generateResource_shouldGenerateOneSecret() {
    when(context.getOperatorSecret()).thenReturn(Optional.empty());

    List<HasMetadata> resources = operatorSecret.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof Secret);
  }

  @Test
  void generateResource_whenCreateForOperatorFalse_shouldReturnEmpty() {
    config.getSpec().getCert().setCreateForOperator(false);

    List<HasMetadata> resources = operatorSecret.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenCertManagerAutoConfigure_shouldReturnEmpty() {
    config.getSpec().getCert().getCertManager().setAutoConfigure(true);

    List<HasMetadata> resources = operatorSecret.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldHaveCorrectType() {
    List<HasMetadata> resources = operatorSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.get(0);
    assertEquals("kubernetes.io/tls", secret.getType());
  }

  @Test
  void generateResource_shouldContainTlsCrtAndKey() {
    List<HasMetadata> resources = operatorSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.get(0);
    Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertTrue(data.containsKey("tls.crt"));
    assertTrue(data.containsKey("tls.key"));
  }

  @Test
  void generateResource_whenPreviousSecretValid_shouldReuseIt() {
    var certPair = CryptoUtil.generateCaCertificateAndPrivateKey(
        "CN=test", List.of("test"),
        Instant.now().plus(730, ChronoUnit.DAYS));

    Secret previousSecret = new SecretBuilder()
        .withNewMetadata()
        .withName("stackgres-operator-certs")
        .withNamespace("stackgres")
        .endMetadata()
        .withType("kubernetes.io/tls")
        .withData(ResourceUtil.encodeSecret(Map.of(
            "tls.crt", certPair.v1,
            "tls.key", certPair.v2)))
        .build();

    when(context.getOperatorSecret()).thenReturn(Optional.of(previousSecret));

    List<HasMetadata> resources = operatorSecret.generateResource(context).toList();

    assertEquals(1, resources.size());
    Secret secret = (Secret) resources.get(0);
    Map<String, String> data = ResourceUtil.decodeSecret(secret.getData());
    assertEquals(certPair.v1, data.get("tls.crt"));
    assertEquals(certPair.v2, data.get("tls.key"));
  }

}

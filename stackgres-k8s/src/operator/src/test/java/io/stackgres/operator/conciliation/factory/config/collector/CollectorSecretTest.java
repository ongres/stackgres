/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.collector;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ConfigLabelFactory;
import io.stackgres.common.labels.ConfigLabelMapper;
import io.stackgres.operator.common.ObservedClusterContext;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectorSecretTest {

  private CollectorSecret collectorSecret;

  @Mock
  private StackGresConfigContext context;

  @Mock
  private ObservedClusterContext observedClusterContext;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    collectorSecret = new CollectorSecret(
        new ConfigLabelFactory(new ConfigLabelMapper()));
    config = Fixtures.config().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(config);
    lenient().when(context.getConfig()).thenReturn(config);
  }

  @Test
  void generateResource_shouldGenerateOneSecret() {
    when(context.getObservedClusters()).thenReturn(List.of(observedClusterContext));
    when(context.getCollectorSecret()).thenReturn(Optional.empty());

    List<HasMetadata> resources = collectorSecret.generateResource(context).toList();

    Assertions.assertEquals(1, resources.size());
    Assertions.assertInstanceOf(Secret.class, resources.getFirst());
  }

  @Test
  void generateResource_whenNoObservedClusters_shouldReturnEmpty() {
    when(context.getObservedClusters()).thenReturn(List.of());

    List<HasMetadata> resources = collectorSecret.generateResource(context).toList();

    Assertions.assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenDeployCollectorFalse_shouldReturnEmpty() {
    config.getSpec().getDeploy().setCollector(false);

    List<HasMetadata> resources = collectorSecret.generateResource(context).toList();

    Assertions.assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldHaveCorrectType() {
    when(context.getObservedClusters()).thenReturn(List.of(observedClusterContext));
    when(context.getCollectorSecret()).thenReturn(Optional.empty());

    List<HasMetadata> resources = collectorSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.getFirst();
    Assertions.assertEquals("kubernetes.io/tls", secret.getType());
  }

  @Test
  void generateResource_shouldContainTlsCrtAndKey() {
    when(context.getObservedClusters()).thenReturn(List.of(observedClusterContext));
    when(context.getCollectorSecret()).thenReturn(Optional.empty());

    List<HasMetadata> resources = collectorSecret.generateResource(context).toList();

    Secret secret = (Secret) resources.getFirst();
    var decodedData = ResourceUtil.decodeSecret(secret.getData());
    Assertions.assertTrue(decodedData.containsKey("tls.crt"));
    Assertions.assertTrue(decodedData.containsKey("tls.key"));
  }

}

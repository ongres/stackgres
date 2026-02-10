/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.config.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.stackgres.common.crd.LocalObjectReference;
import io.stackgres.common.crd.sgconfig.StackGresConfig;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ConfigLabelFactory;
import io.stackgres.common.labels.ConfigLabelMapper;
import io.stackgres.common.labels.LabelFactoryForConfig;
import io.stackgres.operator.common.ObservedClusterContext;
import io.stackgres.operator.conciliation.config.StackGresConfigContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectorServiceAccountTest {

  private final LabelFactoryForConfig labelFactory =
      new ConfigLabelFactory(new ConfigLabelMapper());

  @Mock
  private StackGresConfigContext context;

  @Mock
  private ObservedClusterContext observedClusterContext;

  private CollectorServiceAccount collectorServiceAccount;

  private StackGresConfig config;

  @BeforeEach
  void setUp() {
    collectorServiceAccount = new CollectorServiceAccount(labelFactory);
    config = Fixtures.config().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(config);
    lenient().when(context.getObservedClusters()).thenReturn(List.of(observedClusterContext));
  }

  @Test
  void generateResource_shouldGenerateOneServiceAccount() {
    List<HasMetadata> resources = collectorServiceAccount.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof ServiceAccount);
  }

  @Test
  void generateResource_whenNoObservedClusters_shouldReturnEmpty() {
    when(context.getObservedClusters()).thenReturn(List.of());

    List<HasMetadata> resources = collectorServiceAccount.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_shouldHaveCorrectName() {
    List<HasMetadata> resources = collectorServiceAccount.generateResource(context).toList();

    ServiceAccount serviceAccount = (ServiceAccount) resources.get(0);
    assertEquals("stackgres-collector", serviceAccount.getMetadata().getName());
    assertEquals("stackgres", serviceAccount.getMetadata().getNamespace());
  }

  @Test
  void generateResource_withImagePullSecrets_shouldIncludeThem() {
    config.getSpec().setImagePullSecrets(List.of(new LocalObjectReference("my-secret")));

    List<HasMetadata> resources = collectorServiceAccount.generateResource(context).toList();

    ServiceAccount serviceAccount = (ServiceAccount) resources.get(0);
    assertNotNull(serviceAccount.getImagePullSecrets());
    assertEquals(1, serviceAccount.getImagePullSecrets().size());
    assertEquals("my-secret", serviceAccount.getImagePullSecrets().get(0).getName());
  }

}

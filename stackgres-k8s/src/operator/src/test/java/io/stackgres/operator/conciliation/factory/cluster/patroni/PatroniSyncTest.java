/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.PatroniUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecAnnotationsBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecLabelsBuilder;
import io.stackgres.common.crd.sgcluster.StackGresClusterSpecMetadataBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.ClusterLabelFactory;
import io.stackgres.common.labels.ClusterLabelMapper;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniSyncTest {

  private final LabelFactoryForCluster labelFactory =
      new ClusterLabelFactory(new ClusterLabelMapper());

  @Mock
  private StackGresClusterContext context;

  private PatroniSync patroniSync;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    patroniSync = new PatroniSync(labelFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);
  }

  @Test
  void generateResource_shouldGenerateOneEndpoint() {
    List<HasMetadata> resources = patroniSync.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof Endpoints);
  }

  @Test
  void generateResource_shouldHaveCorrectName() {
    List<HasMetadata> resources = patroniSync.generateResource(context).toList();

    Endpoints endpoints = (Endpoints) resources.get(0);
    String expectedName = PatroniUtil.readWriteName(cluster) + "-sync";
    assertEquals(expectedName, endpoints.getMetadata().getName());
    assertEquals(cluster.getMetadata().getNamespace(), endpoints.getMetadata().getNamespace());
  }

  @Test
  void generateResource_shouldMergeCustomServiceAnnotations() {
    cluster.getSpec().setMetadata(new StackGresClusterSpecMetadataBuilder()
        .withAnnotations(new StackGresClusterSpecAnnotationsBuilder()
            .withServices(Map.of("custom-annotation", "value"))
            .build())
        .withLabels(new StackGresClusterSpecLabelsBuilder()
            .withServices(Map.of("custom-label", "value"))
            .build())
        .build());

    List<HasMetadata> resources = patroniSync.generateResource(context).toList();

    Endpoints endpoints = (Endpoints) resources.get(0);
    assertEquals("value", endpoints.getMetadata().getAnnotations().get("custom-annotation"));
    assertEquals("value", endpoints.getMetadata().getLabels().get("custom-label"));
  }

  @Test
  void generateResource_withNullMetadata_shouldUseDefaults() {
    cluster.getSpec().setMetadata(null);

    List<HasMetadata> resources = patroniSync.generateResource(context).toList();

    assertEquals(1, resources.size());
    Endpoints endpoints = (Endpoints) resources.get(0);
    assertNotNull(endpoints.getMetadata());
    assertNotNull(endpoints.getMetadata().getLabels());
    assertFalse(endpoints.getMetadata().getLabels().isEmpty());
  }
}

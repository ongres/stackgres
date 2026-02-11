/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.autoscaling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.stackgres.common.crd.external.keda.ScaledObject;
import io.stackgres.common.crd.external.keda.TriggerAuthentication;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterAutoscaling;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HorizontalAutoscalingTest {

  @Mock
  private LabelFactoryForCluster labelFactory;

  @Mock
  private StackGresClusterContext context;

  private HorizontalAutoscaling horizontalAutoscaling;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    horizontalAutoscaling = new HorizontalAutoscaling(labelFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(labelFactory.genericLabels(any(StackGresCluster.class)))
        .thenReturn(ImmutableMap.of());
    lenient().when(context.getDatabaseSecret()).thenReturn(Optional.empty());
    lenient().when(context.getSuperuserUsername()).thenReturn(Optional.of("postgres"));
    lenient().when(context.getSuperuserPassword()).thenReturn(Optional.of("password"));
    lenient().when(context.getGeneratedSuperuserPassword()).thenReturn("generated-password");
  }

  @Test
  void generateResource_whenHpaEnabled_shouldGenerateResources() {
    StackGresClusterAutoscaling autoscaling = new StackGresClusterAutoscaling();
    autoscaling.setMode("horizontal");
    autoscaling.setMinInstances(1);
    autoscaling.setMaxInstances(5);
    cluster.getSpec().setAutoscaling(autoscaling);

    List<HasMetadata> resources = horizontalAutoscaling.generateResource(context).toList();

    assertEquals(3, resources.size());
    assertTrue(resources.stream().anyMatch(Secret.class::isInstance));
    assertTrue(resources.stream().anyMatch(TriggerAuthentication.class::isInstance));
    assertTrue(resources.stream().anyMatch(ScaledObject.class::isInstance));
  }

  @Test
  void generateResource_whenHpaDisabled_shouldReturnEmpty() {
    cluster.getSpec().setAutoscaling(null);

    List<HasMetadata> resources = horizontalAutoscaling.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenModeIsNone_shouldReturnEmpty() {
    StackGresClusterAutoscaling autoscaling = new StackGresClusterAutoscaling();
    autoscaling.setMode("none");
    cluster.getSpec().setAutoscaling(autoscaling);

    List<HasMetadata> resources = horizontalAutoscaling.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenModeIsVertical_shouldReturnEmpty() {
    StackGresClusterAutoscaling autoscaling = new StackGresClusterAutoscaling();
    autoscaling.setMode("vertical");
    cluster.getSpec().setAutoscaling(autoscaling);

    List<HasMetadata> resources = horizontalAutoscaling.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenModeIsAll_shouldGenerateResources() {
    StackGresClusterAutoscaling autoscaling = new StackGresClusterAutoscaling();
    autoscaling.setMode("all");
    autoscaling.setMinInstances(2);
    autoscaling.setMaxInstances(10);
    cluster.getSpec().setAutoscaling(autoscaling);

    List<HasMetadata> resources = horizontalAutoscaling.generateResource(context).toList();

    assertEquals(3, resources.size());
  }

  @Test
  void generateResource_scaledObjectShouldHaveCorrectMinMaxReplicas() {
    StackGresClusterAutoscaling autoscaling = new StackGresClusterAutoscaling();
    autoscaling.setMode("horizontal");
    autoscaling.setMinInstances(3);
    autoscaling.setMaxInstances(8);
    cluster.getSpec().setAutoscaling(autoscaling);

    List<HasMetadata> resources = horizontalAutoscaling.generateResource(context).toList();

    ScaledObject scaledObject = resources.stream()
        .filter(ScaledObject.class::isInstance)
        .map(ScaledObject.class::cast)
        .findFirst()
        .orElseThrow();

    assertEquals(3, scaledObject.getSpec().getMinReplicaCount());
    assertEquals(8, scaledObject.getSpec().getMaxReplicaCount());
  }

  @Test
  void generateResource_allResourcesShouldHaveCorrectNamespace() {
    StackGresClusterAutoscaling autoscaling = new StackGresClusterAutoscaling();
    autoscaling.setMode("horizontal");
    autoscaling.setMinInstances(1);
    autoscaling.setMaxInstances(5);
    cluster.getSpec().setAutoscaling(autoscaling);

    List<HasMetadata> resources = horizontalAutoscaling.generateResource(context).toList();

    String expectedNamespace = cluster.getMetadata().getNamespace();
    for (HasMetadata resource : resources) {
      assertEquals(expectedNamespace, resource.getMetadata().getNamespace());
    }
  }

  @Test
  void name_shouldReturnClusterName() {
    String name = HorizontalAutoscaling.name(cluster);

    assertEquals(cluster.getMetadata().getName(), name);
  }

  @Test
  void secretName_shouldReturnClusterNameWithAutoscalingSuffix() {
    String secretName = HorizontalAutoscaling.secretName(cluster);

    assertEquals(cluster.getMetadata().getName() + "-autoscaling", secretName);
  }

  @Test
  void generateResource_secretShouldContainConnectionKey() {
    StackGresClusterAutoscaling autoscaling = new StackGresClusterAutoscaling();
    autoscaling.setMode("horizontal");
    autoscaling.setMinInstances(1);
    autoscaling.setMaxInstances(5);
    cluster.getSpec().setAutoscaling(autoscaling);

    List<HasMetadata> resources = horizontalAutoscaling.generateResource(context).toList();

    Secret secret = resources.stream()
        .filter(Secret.class::isInstance)
        .map(Secret.class::cast)
        .findFirst()
        .orElseThrow();

    assertTrue(secret.getData().containsKey("connection"));
  }

  @Test
  void generateResource_scaledObjectShouldTargetSgCluster() {
    StackGresClusterAutoscaling autoscaling = new StackGresClusterAutoscaling();
    autoscaling.setMode("horizontal");
    autoscaling.setMinInstances(1);
    autoscaling.setMaxInstances(5);
    cluster.getSpec().setAutoscaling(autoscaling);

    List<HasMetadata> resources = horizontalAutoscaling.generateResource(context).toList();

    ScaledObject scaledObject = resources.stream()
        .filter(ScaledObject.class::isInstance)
        .map(ScaledObject.class::cast)
        .findFirst()
        .orElseThrow();

    assertEquals(cluster.getMetadata().getName(),
        scaledObject.getSpec().getScaleTargetRef().getName());
    assertEquals(HasMetadata.getKind(StackGresCluster.class),
        scaledObject.getSpec().getScaleTargetRef().getKind());
  }
}

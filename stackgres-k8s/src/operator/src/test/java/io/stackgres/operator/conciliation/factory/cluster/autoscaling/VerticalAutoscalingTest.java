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
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.StackGresContainer;
import io.stackgres.common.crd.external.autoscaling.VerticalPodAutoscaler;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterAutoscaling;
import io.stackgres.common.crd.sgcluster.StackGresClusterAutoscalingVerticalBound;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForCluster;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VerticalAutoscalingTest {

  @Mock
  private LabelFactoryForCluster labelFactory;

  @Mock
  private StackGresClusterContext context;

  private VerticalAutoscaling verticalAutoscaling;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    verticalAutoscaling = new VerticalAutoscaling(labelFactory);
    cluster = Fixtures.cluster().loadDefault().get();
    lenient().when(context.getSource()).thenReturn(cluster);
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(labelFactory.genericLabels(any(StackGresCluster.class)))
        .thenReturn(ImmutableMap.of());
  }

  @Test
  void generateResource_whenVpaEnabled_shouldGenerateVpa() {
    setupVerticalAutoscaling();

    List<HasMetadata> resources = verticalAutoscaling.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof VerticalPodAutoscaler);
  }

  @Test
  void generateResource_whenVpaDisabled_shouldReturnEmpty() {
    cluster.getSpec().setAutoscaling(null);

    List<HasMetadata> resources = verticalAutoscaling.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenModeIsNone_shouldReturnEmpty() {
    StackGresClusterAutoscaling autoscaling = new StackGresClusterAutoscaling();
    autoscaling.setMode("none");
    cluster.getSpec().setAutoscaling(autoscaling);

    List<HasMetadata> resources = verticalAutoscaling.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenModeIsHorizontal_shouldReturnEmpty() {
    StackGresClusterAutoscaling autoscaling = new StackGresClusterAutoscaling();
    autoscaling.setMode("horizontal");
    cluster.getSpec().setAutoscaling(autoscaling);

    List<HasMetadata> resources = verticalAutoscaling.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_whenModeIsAll_shouldGenerateVpa() {
    StackGresClusterAutoscaling autoscaling = new StackGresClusterAutoscaling();
    autoscaling.setMode("all");
    setupMinMaxAllowed(autoscaling);
    cluster.getSpec().setAutoscaling(autoscaling);

    List<HasMetadata> resources = verticalAutoscaling.generateResource(context).toList();

    assertEquals(1, resources.size());
    assertTrue(resources.get(0) instanceof VerticalPodAutoscaler);
  }

  @Test
  void generateResource_vpaShouldHaveCorrectNamespace() {
    setupVerticalAutoscaling();

    List<HasMetadata> resources = verticalAutoscaling.generateResource(context).toList();

    assertEquals(cluster.getMetadata().getNamespace(),
        resources.get(0).getMetadata().getNamespace());
  }

  @Test
  void generateResource_vpaShouldTargetSgCluster() {
    setupVerticalAutoscaling();

    List<HasMetadata> resources = verticalAutoscaling.generateResource(context).toList();
    VerticalPodAutoscaler vpa = (VerticalPodAutoscaler) resources.get(0);

    assertEquals(cluster.getMetadata().getName(),
        vpa.getSpec().getTargetRef().getName());
    assertEquals(HasMetadata.getKind(StackGresCluster.class),
        vpa.getSpec().getTargetRef().getKind());
  }

  @Test
  void generateResource_vpaShouldHaveResourcePolicyWithPatroniContainer() {
    setupVerticalAutoscaling();

    List<HasMetadata> resources = verticalAutoscaling.generateResource(context).toList();
    VerticalPodAutoscaler vpa = (VerticalPodAutoscaler) resources.get(0);

    assertTrue(vpa.getSpec().getResourcePolicy().getContainerPolicies().stream()
        .anyMatch(policy ->
            StackGresContainer.PATRONI.getName().equals(policy.getContainerName())));
  }

  @Test
  void generateResource_vpaShouldHaveWildcardContainerPolicyOff() {
    setupVerticalAutoscaling();

    List<HasMetadata> resources = verticalAutoscaling.generateResource(context).toList();
    VerticalPodAutoscaler vpa = (VerticalPodAutoscaler) resources.get(0);

    assertTrue(vpa.getSpec().getResourcePolicy().getContainerPolicies().stream()
        .anyMatch(policy ->
            "*".equals(policy.getContainerName())
                && "Off".equals(policy.getMode())));
  }

  @Test
  void generateResource_vpaShouldHavePatroniContainerPolicyAuto() {
    setupVerticalAutoscaling();

    List<HasMetadata> resources = verticalAutoscaling.generateResource(context).toList();
    VerticalPodAutoscaler vpa = (VerticalPodAutoscaler) resources.get(0);

    assertTrue(vpa.getSpec().getResourcePolicy().getContainerPolicies().stream()
        .anyMatch(policy ->
            StackGresContainer.PATRONI.getName().equals(policy.getContainerName())
                && "Auto".equals(policy.getMode())));
  }

  @Test
  void generateResource_vpaShouldHavePgbouncerContainerPolicy() {
    setupVerticalAutoscaling();

    List<HasMetadata> resources = verticalAutoscaling.generateResource(context).toList();
    VerticalPodAutoscaler vpa = (VerticalPodAutoscaler) resources.get(0);

    assertTrue(vpa.getSpec().getResourcePolicy().getContainerPolicies().stream()
        .anyMatch(policy ->
            StackGresContainer.PGBOUNCER.getName().equals(policy.getContainerName())));
  }

  @Test
  void generateResource_vpaShouldHaveEnvoyContainerPolicy() {
    setupVerticalAutoscaling();

    List<HasMetadata> resources = verticalAutoscaling.generateResource(context).toList();
    VerticalPodAutoscaler vpa = (VerticalPodAutoscaler) resources.get(0);

    assertTrue(vpa.getSpec().getResourcePolicy().getContainerPolicies().stream()
        .anyMatch(policy ->
            StackGresContainer.ENVOY.getName().equals(policy.getContainerName())));
  }

  @Test
  void generateResource_vpaShouldHaveUpdatePolicyAuto() {
    setupVerticalAutoscaling();

    List<HasMetadata> resources = verticalAutoscaling.generateResource(context).toList();
    VerticalPodAutoscaler vpa = (VerticalPodAutoscaler) resources.get(0);

    assertEquals("Auto", vpa.getSpec().getUpdatePolicy().getUpdateMode());
  }

  @Test
  void name_shouldReturnClusterName() {
    String name = VerticalAutoscaling.name(cluster);

    assertEquals(cluster.getMetadata().getName(), name);
  }

  private void setupVerticalAutoscaling() {
    StackGresClusterAutoscaling autoscaling = new StackGresClusterAutoscaling();
    autoscaling.setMode("vertical");
    setupMinMaxAllowed(autoscaling);
    cluster.getSpec().setAutoscaling(autoscaling);
  }

  private void setupMinMaxAllowed(StackGresClusterAutoscaling autoscaling) {
    StackGresClusterAutoscalingVerticalBound patroniBound =
        new StackGresClusterAutoscalingVerticalBound();
    patroniBound.setCpu("100m");
    patroniBound.setMemory("256Mi");

    autoscaling.setMinAllowed(Map.of(
        StackGresContainer.PATRONI.getName(), patroniBound));

    StackGresClusterAutoscalingVerticalBound maxPatroniBound =
        new StackGresClusterAutoscalingVerticalBound();
    maxPatroniBound.setCpu("4");
    maxPatroniBound.setMemory("8Gi");

    autoscaling.setMaxAllowed(Map.of(
        StackGresContainer.PATRONI.getName(), maxPatroniBound));
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.stackgres.common.crd.CustomEnvVar;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterPods;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClusterStatefulSetContainerCustomEnvDecoratorTest {

  private final ClusterStatefulSetContainerCustomEnvDecorator decorator =
      new ClusterStatefulSetContainerCustomEnvDecorator();

  @Mock
  private StackGresClusterContext context;

  private StackGresCluster cluster;

  @BeforeEach
  void setUp() {
    cluster = Fixtures.cluster().loadDefault().get();
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getSource()).thenReturn(cluster);
  }

  @Test
  void decorate_whenStatefulSetWithCustomEnv_shouldAddEnvVarsToContainers() {
    String containerName = "patroni";
    CustomEnvVar customEnv = new CustomEnvVar("CUSTOM_VAR", "custom-value", null);

    ensurePodsNotNull();
    cluster.getSpec().getPods().setCustomEnv(
        Map.of(containerName, List.of(customEnv)));

    StatefulSet statefulSet = buildStatefulSetWithContainer(containerName);

    HasMetadata result = decorator.decorate(context, statefulSet);

    assertTrue(result instanceof StatefulSet);
    StatefulSet resultSts = (StatefulSet) result;
    Container container = resultSts.getSpec().getTemplate().getSpec()
        .getContainers().getFirst();
    assertNotNull(container.getEnv());
    assertTrue(container.getEnv().stream()
        .anyMatch(env -> "CUSTOM_VAR".equals(env.getName())
            && "custom-value".equals(env.getValue())));
  }

  @Test
  void decorate_whenStatefulSetWithCustomInitEnv_shouldAddEnvVarsToInitContainers() {
    String initContainerName = "init-container";
    CustomEnvVar customEnv = new CustomEnvVar("INIT_VAR", "init-value", null);

    ensurePodsNotNull();
    cluster.getSpec().getPods().setCustomInitEnv(
        Map.of(initContainerName, List.of(customEnv)));

    StatefulSet statefulSet = new StatefulSetBuilder()
        .withNewMetadata().withName("test-sts").withNamespace("test-ns").endMetadata()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewInitContainer()
        .withName(initContainerName)
        .endInitContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();

    HasMetadata result = decorator.decorate(context, statefulSet);

    assertTrue(result instanceof StatefulSet);
    StatefulSet resultSts = (StatefulSet) result;
    Container initContainer = resultSts.getSpec().getTemplate().getSpec()
        .getInitContainers().getFirst();
    assertNotNull(initContainer.getEnv());
    assertTrue(initContainer.getEnv().stream()
        .anyMatch(env -> "INIT_VAR".equals(env.getName())
            && "init-value".equals(env.getValue())));
  }

  @Test
  void decorate_whenNonStatefulSetResource_shouldPassThroughUnchanged() {
    HasMetadata service = new ServiceBuilder()
        .withNewMetadata().withName("test-svc").withNamespace("test-ns").endMetadata()
        .build();

    HasMetadata result = decorator.decorate(context, service);

    assertEquals(service, result);
  }

  @Test
  void decorate_whenConfigMapResource_shouldPassThroughUnchanged() {
    HasMetadata configMap = new ConfigMapBuilder()
        .withNewMetadata().withName("test-cm").withNamespace("test-ns").endMetadata()
        .build();

    HasMetadata result = decorator.decorate(context, configMap);

    assertEquals(configMap, result);
  }

  @Test
  void decorate_whenCustomEnvIsNull_shouldNotModifyContainers() {
    ensurePodsNotNull();
    cluster.getSpec().getPods().setCustomEnv(null);

    String containerName = "patroni";
    StatefulSet statefulSet = buildStatefulSetWithContainer(containerName);

    HasMetadata result = decorator.decorate(context, statefulSet);

    assertTrue(result instanceof StatefulSet);
    StatefulSet resultSts = (StatefulSet) result;
    Container container = resultSts.getSpec().getTemplate().getSpec()
        .getContainers().getFirst();
    assertTrue(container.getEnv() == null || container.getEnv().isEmpty());
  }

  @Test
  void decorate_whenPodsIsNull_shouldNotThrow() {
    cluster.getSpec().setPods(null);
    cluster.getSpec().setPods(new StackGresClusterPods());

    StatefulSet statefulSet = buildStatefulSetWithContainer("patroni");

    HasMetadata result = decorator.decorate(context, statefulSet);

    assertNotNull(result);
  }

  @Test
  void decorate_whenMultipleCustomEnvVars_shouldAddAllToContainer() {
    String containerName = "patroni";
    CustomEnvVar env1 = new CustomEnvVar("VAR_1", "value-1", null);
    CustomEnvVar env2 = new CustomEnvVar("VAR_2", "value-2", null);

    ensurePodsNotNull();
    cluster.getSpec().getPods().setCustomEnv(
        Map.of(containerName, List.of(env1, env2)));

    StatefulSet statefulSet = buildStatefulSetWithContainer(containerName);

    HasMetadata result = decorator.decorate(context, statefulSet);

    StatefulSet resultSts = (StatefulSet) result;
    Container container = resultSts.getSpec().getTemplate().getSpec()
        .getContainers().getFirst();
    assertNotNull(container.getEnv());
    assertEquals(2, container.getEnv().size());
  }

  @Test
  void decorate_whenCustomEnvForDifferentContainer_shouldNotAddToMismatchedContainer() {
    String actualContainerName = "patroni";
    String otherContainerName = "other-container";
    CustomEnvVar customEnv = new CustomEnvVar("OTHER_VAR", "other-value", null);

    ensurePodsNotNull();
    cluster.getSpec().getPods().setCustomEnv(
        Map.of(otherContainerName, List.of(customEnv)));

    StatefulSet statefulSet = buildStatefulSetWithContainer(actualContainerName);

    HasMetadata result = decorator.decorate(context, statefulSet);

    StatefulSet resultSts = (StatefulSet) result;
    Container container = resultSts.getSpec().getTemplate().getSpec()
        .getContainers().getFirst();
    assertTrue(container.getEnv() == null || container.getEnv().isEmpty());
  }

  @Test
  void decorate_whenContainerHasExistingEnvVars_shouldAppendCustomEnvVars() {
    String containerName = "patroni";
    CustomEnvVar customEnv = new CustomEnvVar("CUSTOM_VAR", "custom-value", null);

    ensurePodsNotNull();
    cluster.getSpec().getPods().setCustomEnv(
        Map.of(containerName, List.of(customEnv)));

    StatefulSet statefulSet = new StatefulSetBuilder()
        .withNewMetadata().withName("test-sts").withNamespace("test-ns").endMetadata()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .withName(containerName)
        .withEnv(new ArrayList<>(List.of(
            new io.fabric8.kubernetes.api.model.EnvVarBuilder()
                .withName("EXISTING_VAR")
                .withValue("existing-value")
                .build())))
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();

    HasMetadata result = decorator.decorate(context, statefulSet);

    StatefulSet resultSts = (StatefulSet) result;
    Container container = resultSts.getSpec().getTemplate().getSpec()
        .getContainers().getFirst();
    assertNotNull(container.getEnv());
    assertEquals(2, container.getEnv().size());
    assertTrue(container.getEnv().stream()
        .anyMatch(env -> "EXISTING_VAR".equals(env.getName())));
    assertTrue(container.getEnv().stream()
        .anyMatch(env -> "CUSTOM_VAR".equals(env.getName())));
  }

  private StatefulSet buildStatefulSetWithContainer(String containerName) {
    return new StatefulSetBuilder()
        .withNewMetadata().withName("test-sts").withNamespace("test-ns").endMetadata()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .withName(containerName)
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();
  }

  private void ensurePodsNotNull() {
    if (cluster.getSpec().getPods() == null) {
      cluster.getSpec().setPods(new StackGresClusterPods());
    }
  }

}

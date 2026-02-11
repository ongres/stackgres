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
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.stackgres.common.crd.CustomVolumeMount;
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
class ClusterStatefulSetContainerCustomVolumeMountDecoratorTest {

  private final ClusterStatefulSetContainerCustomVolumeMountDecorator decorator =
      new ClusterStatefulSetContainerCustomVolumeMountDecorator();

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
  void decorate_whenStatefulSetWithCustomVolumeMounts_shouldAddMountsToContainers() {
    String containerName = "patroni";
    CustomVolumeMount customMount = new CustomVolumeMount(
        "/custom/path", null, "custom-volume", false, null, null, null);

    ensurePodsNotNull();
    cluster.getSpec().getPods().setCustomVolumeMounts(
        Map.of(containerName, List.of(customMount)));

    StatefulSet statefulSet = buildStatefulSetWithContainer(containerName);

    HasMetadata result = decorator.decorate(context, statefulSet);

    assertTrue(result instanceof StatefulSet);
    StatefulSet resultSts = (StatefulSet) result;
    Container container = resultSts.getSpec().getTemplate().getSpec()
        .getContainers().getFirst();
    assertNotNull(container.getVolumeMounts());
    assertTrue(container.getVolumeMounts().stream()
        .anyMatch(vm -> "/custom/path".equals(vm.getMountPath())
            && "custom-volume".equals(vm.getName())));
  }

  @Test
  void decorate_whenStatefulSetWithCustomInitVolumeMounts_shouldAddMountsToInitContainers() {
    String initContainerName = "init-container";
    CustomVolumeMount customMount = new CustomVolumeMount(
        "/init/path", null, "init-volume", false, null, null, null);

    ensurePodsNotNull();
    cluster.getSpec().getPods().setCustomInitVolumeMounts(
        Map.of(initContainerName, List.of(customMount)));

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
    assertNotNull(initContainer.getVolumeMounts());
    assertTrue(initContainer.getVolumeMounts().stream()
        .anyMatch(vm -> "/init/path".equals(vm.getMountPath())
            && "init-volume".equals(vm.getName())));
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
  void decorate_whenCustomVolumeMountsIsNull_shouldNotModifyContainers() {
    ensurePodsNotNull();
    cluster.getSpec().getPods().setCustomVolumeMounts(null);

    String containerName = "patroni";
    StatefulSet statefulSet = buildStatefulSetWithContainer(containerName);

    HasMetadata result = decorator.decorate(context, statefulSet);

    assertTrue(result instanceof StatefulSet);
    StatefulSet resultSts = (StatefulSet) result;
    Container container = resultSts.getSpec().getTemplate().getSpec()
        .getContainers().getFirst();
    assertTrue(container.getVolumeMounts() == null || container.getVolumeMounts().isEmpty());
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
  void decorate_whenMultipleCustomVolumeMounts_shouldAddAllToContainer() {
    String containerName = "patroni";
    CustomVolumeMount mount1 = new CustomVolumeMount(
        "/path/one", null, "vol-1", false, null, null, null);
    CustomVolumeMount mount2 = new CustomVolumeMount(
        "/path/two", null, "vol-2", false, null, null, null);

    ensurePodsNotNull();
    cluster.getSpec().getPods().setCustomVolumeMounts(
        Map.of(containerName, List.of(mount1, mount2)));

    StatefulSet statefulSet = buildStatefulSetWithContainer(containerName);

    HasMetadata result = decorator.decorate(context, statefulSet);

    StatefulSet resultSts = (StatefulSet) result;
    Container container = resultSts.getSpec().getTemplate().getSpec()
        .getContainers().getFirst();
    assertNotNull(container.getVolumeMounts());
    assertEquals(2, container.getVolumeMounts().size());
  }

  @Test
  void decorate_whenCustomMountForDifferentContainer_shouldNotAddToMismatchedContainer() {
    String actualContainerName = "patroni";
    String otherContainerName = "other-container";
    CustomVolumeMount customMount = new CustomVolumeMount(
        "/other/path", null, "other-volume", false, null, null, null);

    ensurePodsNotNull();
    cluster.getSpec().getPods().setCustomVolumeMounts(
        Map.of(otherContainerName, List.of(customMount)));

    StatefulSet statefulSet = buildStatefulSetWithContainer(actualContainerName);

    HasMetadata result = decorator.decorate(context, statefulSet);

    StatefulSet resultSts = (StatefulSet) result;
    Container container = resultSts.getSpec().getTemplate().getSpec()
        .getContainers().getFirst();
    assertTrue(container.getVolumeMounts() == null || container.getVolumeMounts().isEmpty());
  }

  @Test
  void decorate_whenContainerHasExistingVolumeMounts_shouldAppendCustomMounts() {
    String containerName = "patroni";
    CustomVolumeMount customMount = new CustomVolumeMount(
        "/custom/path", null, "custom-volume", false, null, null, null);

    ensurePodsNotNull();
    cluster.getSpec().getPods().setCustomVolumeMounts(
        Map.of(containerName, List.of(customMount)));

    VolumeMount existingMount = new VolumeMountBuilder()
        .withName("existing-volume")
        .withMountPath("/existing/path")
        .build();

    StatefulSet statefulSet = new StatefulSetBuilder()
        .withNewMetadata().withName("test-sts").withNamespace("test-ns").endMetadata()
        .withNewSpec()
        .withNewTemplate()
        .withNewSpec()
        .addNewContainer()
        .withName(containerName)
        .withVolumeMounts(new ArrayList<>(List.of(existingMount)))
        .endContainer()
        .endSpec()
        .endTemplate()
        .endSpec()
        .build();

    HasMetadata result = decorator.decorate(context, statefulSet);

    StatefulSet resultSts = (StatefulSet) result;
    Container container = resultSts.getSpec().getTemplate().getSpec()
        .getContainers().getFirst();
    assertNotNull(container.getVolumeMounts());
    assertEquals(2, container.getVolumeMounts().size());
    assertTrue(container.getVolumeMounts().stream()
        .anyMatch(vm -> "existing-volume".equals(vm.getName())));
    assertTrue(container.getVolumeMounts().stream()
        .anyMatch(vm -> "custom-volume".equals(vm.getName())));
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

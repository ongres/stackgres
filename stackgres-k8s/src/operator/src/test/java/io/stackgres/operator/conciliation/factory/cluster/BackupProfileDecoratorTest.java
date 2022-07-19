/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.batch.v1beta1.CronJob;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresKind;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.PatroniStaticVolume;
import io.stackgres.testutil.JsonUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupProfileDecoratorTest {

  private static final StackGresKind KIND = StackGresKind.BACKUP;

  private final BackupProfileDecorator profileDecorator = new BackupProfileDecorator();

  @Mock
  private StackGresClusterContext context;

  private StackGresCluster defaultCluster;

  private StackGresProfile defaultProfile;

  private CronJob cronJob;

  private List<HasMetadata> resources;

  @BeforeEach
  void setUp() {
    defaultCluster = JsonUtil
        .readFromJson("stackgres_cluster/default.json", StackGresCluster.class);
    defaultProfile = JsonUtil
        .readFromJson("stackgres_profiles/size-xs.json", StackGresProfile.class);

    final ObjectMeta metadata = defaultCluster.getMetadata();
    metadata.getAnnotations().put(StackGresContext.VERSION_KEY,
        StackGresProperty.OPERATOR_VERSION.getString());
    resources = KubernetessMockResourceGenerationUtil
        .buildResources(metadata.getName(), metadata.getNamespace());
    cronJob = resources.stream()
        .filter(CronJob.class::isInstance)
        .map(CronJob.class::cast)
        .findFirst()
        .orElseThrow();
    defaultProfile.getSpec().setContainers(new HashMap<>());
    defaultProfile.getSpec().setInitContainers(new HashMap<>());
    Seq.seq(cronJob.getSpec().getJobTemplate().getSpec()
            .getTemplate().getSpec().getContainers())
        .forEach(container -> {
          StackGresProfileContainer containerProfile = new StackGresProfileContainer();
          containerProfile.setCpu(new Random().nextInt(32000) + "m");
          containerProfile.setMemory(new Random().nextInt(32) + "Gi");
          defaultProfile.getSpec().getContainers().put(
              KIND.getContainerPrefix() + container.getName(), containerProfile);
        });
    Seq.seq(cronJob.getSpec().getJobTemplate().getSpec()
            .getTemplate().getSpec().getInitContainers())
        .forEach(container -> {
          StackGresProfileContainer containerProfile = new StackGresProfileContainer();
          containerProfile.setCpu(new Random().nextInt(32000) + "m");
          containerProfile.setMemory(new Random().nextInt(32) + "Gi");
          defaultProfile.getSpec().getInitContainers().put(
              KIND.getContainerPrefix() + container.getName(), containerProfile);
        });
    StackGresProfileContainer containerProfile = new StackGresProfileContainer();
    containerProfile.setCpu(new Random().nextInt(32000) + "m");
    containerProfile.setMemory(new Random().nextInt(32) + "Gi");
    defaultProfile.getSpec().getContainers().put(
        KIND.getContainerPrefix() + StringUtil.generateRandom(), containerProfile);
    defaultProfile.getSpec().getInitContainers().put(
        KIND.getContainerPrefix() + StringUtil.generateRandom(), containerProfile);

    when(context.getSource()).thenReturn(defaultCluster);
    when(context.getProfile()).thenReturn(defaultProfile);
  }

  @Test
  void withCpuAndMemoryForAllContainers_shouldBeAppliedToAllExceptPatroni() {
    profileDecorator.decorate(context, resources);

    assertTrue(cronJob.getSpec().getJobTemplate().getSpec().getTemplate()
        .getSpec().getVolumes().isEmpty());

    cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().stream()
        .forEach(container -> {
          defaultProfile.getSpec().getContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), KIND.getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerCpuAndMemory(entry, container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });
  }

  @Test
  void withCpuAndMemoryAndHegePagesForAllContainers_shouldBeAppliedToAllExceptPatroni() {
    Seq.seq(defaultProfile.getSpec().getContainers().values())
        .append(defaultProfile.getSpec().getInitContainers().values())
        .forEach(containerProfile -> {
          var hugePages = new StackGresProfileHugePages();
          hugePages.setHugepages2Mi(new Random().nextInt(32) + "Gi");
          hugePages.setHugepages1Gi(new Random().nextInt(32) + "Gi");
          containerProfile.setHugePages(hugePages);
        });

    profileDecorator.decorate(context, resources);

    assertEquals(
        2 * (
            cronJob.getSpec().getJobTemplate().getSpec().getTemplate()
                .getSpec().getContainers().size()
            + cronJob.getSpec().getJobTemplate().getSpec().getTemplate()
                .getSpec().getInitContainers().size()
            ),
        cronJob.getSpec().getJobTemplate().getSpec().getTemplate()
            .getSpec().getVolumes().size());

    cronJob.getSpec().getJobTemplate().getSpec().getTemplate().getSpec().getContainers().stream()
        .forEach(container -> {
          defaultProfile.getSpec().getContainers().entrySet().stream()
              .filter(entry -> Objects.equals(
                  entry.getKey(), KIND.getContainerPrefix() + container.getName()))
              .findAny()
              .ifPresentOrElse(
                  entry -> assertContainerCpuAndMemoryAndHugePages(entry, container),
                  () -> fail("Container profile " + container.getName() + " not found"));
        });
  }

  private void assertContainerCpuAndMemory(Entry<String, StackGresProfileContainer> entry,
      Container container) {
    assertNotNull(container.getResources());
    assertEquals(
        Map.of(
            "cpu", new Quantity(entry.getValue().getCpu()),
            "memory", new Quantity(entry.getValue().getMemory())
            ),
        container.getResources().getRequests());
    assertTrue(container.getVolumeMounts().isEmpty());
  }

  private void assertContainerCpuAndMemoryAndHugePages(
      Entry<String, StackGresProfileContainer> entry, Container container) {
    assertNotNull(container.getResources());
    assertEquals(
        Map.of(
            "cpu", new Quantity(entry.getValue().getCpu()),
            "memory", new Quantity(entry.getValue().getMemory()),
            "hugepages-2Mi", new Quantity(entry.getValue().getHugePages().getHugepages2Mi()),
            "hugepages-1Gi", new Quantity(entry.getValue().getHugePages().getHugepages1Gi())
            ),
        container.getResources().getRequests());
    assertEquals(2, container.getVolumeMounts().size());
    assertTrue(container.getVolumeMounts().stream().anyMatch(volumeMount -> Objects.equals(
        PatroniStaticVolume.HUGEPAGES_2M.getVolumeName() + "-" + entry.getKey(),
        volumeMount.getName())));
    assertTrue(container.getVolumeMounts().stream().anyMatch(volumeMount -> Objects.equals(
        PatroniStaticVolume.HUGEPAGES_1G.getVolumeName() + "-" + entry.getKey(),
        volumeMount.getName())));
  }

}
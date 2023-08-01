/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.backup;

import static org.mockito.Mockito.lenient;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.stackgres.common.StackGresContext;
import io.stackgres.common.StackGresGroupKind;
import io.stackgres.common.StackGresProperty;
import io.stackgres.common.StringUtil;
import io.stackgres.common.crd.sgbackup.StackGresBackup;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgcluster.StackGresClusterNonProduction;
import io.stackgres.common.crd.sgcluster.StackGresClusterResources;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileContainer;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.backup.StackGresBackupContext;
import io.stackgres.operator.conciliation.factory.AbstractProfileDecoratorTestCase;
import io.stackgres.operator.conciliation.factory.cluster.KubernetessMockResourceGenerationUtil;
import org.jooq.lambda.Seq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BackupJobContainerProfileDecoratorTest extends AbstractProfileDecoratorTestCase {

  private static final StackGresGroupKind KIND = StackGresGroupKind.BACKUP;

  private final BackupJobContainerProfileDecorator profileDecorator =
      new BackupJobContainerProfileDecorator();

  @Mock
  private StackGresBackupContext context;

  private StackGresBackup backup;

  private StackGresCluster cluster;

  private StackGresProfile profile;

  private Job job;

  private List<HasMetadata> resources;

  @BeforeEach
  void setUp() {
    backup = Fixtures.backup().loadDefault().get();
    cluster = Fixtures.cluster().loadDefault().get();
    profile = Fixtures.instanceProfile().loadSizeXs().get();

    final ObjectMeta metadata = backup.getMetadata();
    metadata.getAnnotations().put(StackGresContext.VERSION_KEY,
        StackGresProperty.OPERATOR_VERSION.getString());
    resources = KubernetessMockResourceGenerationUtil
        .buildResources(metadata.getName(), metadata.getNamespace());
    job = resources.stream()
        .filter(Job.class::isInstance)
        .map(Job.class::cast)
        .findFirst()
        .orElseThrow();
    profile.getSpec().setContainers(new HashMap<>());
    profile.getSpec().setInitContainers(new HashMap<>());
    Seq.seq(job.getSpec()
            .getTemplate().getSpec().getContainers())
        .forEach(container -> {
          StackGresProfileContainer containerProfile = new StackGresProfileContainer();
          containerProfile.setCpu(new Random().nextInt(32000) + "m");
          containerProfile.setMemory(new Random().nextInt(32) + "Gi");
          profile.getSpec().getContainers().put(
              KIND.getContainerPrefix() + container.getName(), containerProfile);
        });
    Seq.seq(job.getSpec()
            .getTemplate().getSpec().getInitContainers())
        .forEach(container -> {
          StackGresProfileContainer containerProfile = new StackGresProfileContainer();
          containerProfile.setCpu(new Random().nextInt(32000) + "m");
          containerProfile.setMemory(new Random().nextInt(32) + "Gi");
          profile.getSpec().getInitContainers().put(
              KIND.getContainerPrefix() + container.getName(), containerProfile);
        });
    StackGresProfileContainer containerProfile = new StackGresProfileContainer();
    containerProfile.setCpu(new Random().nextInt(32000) + "m");
    containerProfile.setMemory(new Random().nextInt(32) + "Gi");
    profile.getSpec().getContainers().put(
        KIND.getContainerPrefix() + StringUtil.generateRandom(), containerProfile);
    profile.getSpec().getInitContainers().put(
        KIND.getContainerPrefix() + StringUtil.generateRandom(), containerProfile);
    backup.setStatus(null);

    lenient().when(context.getObjectStorage()).thenReturn(
        Optional.of(new StackGresObjectStorage()));
    lenient().when(context.getSource()).thenReturn(backup);
    lenient().when(context.getCluster()).thenReturn(cluster);
    lenient().when(context.getProfile()).thenReturn(profile);
  }

  @Override
  protected StackGresProfile getProfile() {
    return profile;
  }

  @Override
  protected PodSpec getPodSpec() {
    return job.getSpec().getTemplate().getSpec();
  }

  @Override
  protected StackGresGroupKind getKind() {
    return KIND;
  }

  @Override
  protected void decorate() {
    resources.forEach(resource -> profileDecorator.decorate(context, resource));
  }

  @Override
  protected void disableResourceRequirements() {
    cluster.getSpec().setNonProductionOptions(new StackGresClusterNonProduction());
    cluster.getSpec().getNonProductionOptions().setDisableClusterResourceRequirements(true);
  }

  @Override
  protected void enableRequests() {
    cluster.getSpec().setNonProductionOptions(new StackGresClusterNonProduction());
    cluster.getSpec().getNonProductionOptions().setEnableSetClusterCpuRequests(true);
    cluster.getSpec().getNonProductionOptions().setEnableSetClusterMemoryRequests(true);
  }

  @Override
  protected void enableLimits() {
    cluster.getSpec().getPod().setResources(new StackGresClusterResources());
    cluster.getSpec().getPod().getResources().setEnableClusterLimitsRequirements(true);
  }

}

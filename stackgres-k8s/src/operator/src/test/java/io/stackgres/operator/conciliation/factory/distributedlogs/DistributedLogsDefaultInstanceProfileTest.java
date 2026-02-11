/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgdistributedlogs.StackGresDistributedLogs;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileBuilder;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.DistributedLogsLabelFactory;
import io.stackgres.common.labels.DistributedLogsLabelMapper;
import io.stackgres.common.labels.LabelFactoryForDistributedLogs;
import io.stackgres.operator.conciliation.distributedlogs.StackGresDistributedLogsContext;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DistributedLogsDefaultInstanceProfileTest {

  private final LabelFactoryForDistributedLogs labelFactory =
      new DistributedLogsLabelFactory(new DistributedLogsLabelMapper());

  @Mock
  private DefaultProfileFactory defaultProfileFactory;

  @Mock
  private StackGresDistributedLogsContext context;

  private DistributedLogsDefaultInstanceProfile distributedLogsDefaultInstanceProfile;

  private StackGresDistributedLogs distributedLogs;

  @BeforeEach
  void setUp() {
    distributedLogsDefaultInstanceProfile =
        new DistributedLogsDefaultInstanceProfile(labelFactory, defaultProfileFactory);
    distributedLogs = Fixtures.distributedLogs().loadDefault().get();
    when(context.getSource()).thenReturn(distributedLogs);

    lenient().when(defaultProfileFactory.buildResource(any())).thenReturn(
        new StackGresProfileBuilder().withNewSpec().endSpec().build());
  }

  @Test
  void generateResource_whenProfileEmpty_shouldGenerateDefault() {
    when(context.getProfile()).thenReturn(Optional.empty());

    List<HasMetadata> resources =
        distributedLogsDefaultInstanceProfile.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresProfile profile = (StackGresProfile) resources.getFirst();
    assertEquals(distributedLogs.getSpec().getSgInstanceProfile(),
        profile.getMetadata().getName());
  }

  @Test
  void generateResource_whenProfileExistsWithDefaultLabelsAndOwner_shouldGenerate() {
    StackGresProfile existingProfile = new StackGresProfileBuilder()
        .withNewMetadata()
        .withLabels(Map.copyOf(labelFactory.defaultConfigLabels(distributedLogs)))
        .withOwnerReferences(
            List.of(ResourceUtil.getControllerOwnerReference(distributedLogs)))
        .endMetadata()
        .build();
    when(context.getProfile()).thenReturn(Optional.of(existingProfile));

    List<HasMetadata> resources =
        distributedLogsDefaultInstanceProfile.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresProfile profile = (StackGresProfile) resources.getFirst();
    assertEquals(distributedLogs.getSpec().getSgInstanceProfile(),
        profile.getMetadata().getName());
  }

  @Test
  void generateResource_whenProfileExistsWithoutMatchingLabels_shouldNotGenerate() {
    StackGresProfile existingProfile = new StackGresProfileBuilder()
        .withNewMetadata()
        .withLabels(Map.of("other-label", "other-value"))
        .withOwnerReferences(
            List.of(ResourceUtil.getControllerOwnerReference(distributedLogs)))
        .endMetadata()
        .build();
    when(context.getProfile()).thenReturn(Optional.of(existingProfile));

    List<HasMetadata> resources =
        distributedLogsDefaultInstanceProfile.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_generatedProfileHasCorrectNameAndNamespace() {
    when(context.getProfile()).thenReturn(Optional.empty());

    List<HasMetadata> resources =
        distributedLogsDefaultInstanceProfile.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresProfile profile = (StackGresProfile) resources.getFirst();
    assertEquals("size-s", profile.getMetadata().getName());
    assertEquals("distributed-logs", profile.getMetadata().getNamespace());
  }

}

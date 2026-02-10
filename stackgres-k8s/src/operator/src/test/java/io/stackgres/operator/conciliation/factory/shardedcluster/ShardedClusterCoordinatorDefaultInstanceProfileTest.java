/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.shardedcluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileBuilder;
import io.stackgres.common.crd.sgshardedcluster.StackGresShardedCluster;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.common.labels.LabelFactoryForShardedCluster;
import io.stackgres.common.labels.ShardedClusterLabelFactory;
import io.stackgres.common.labels.ShardedClusterLabelMapper;
import io.stackgres.operator.conciliation.shardedcluster.StackGresShardedClusterContext;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import io.stackgres.operatorframework.resource.ResourceUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShardedClusterCoordinatorDefaultInstanceProfileTest {

  private final LabelFactoryForShardedCluster labelFactory =
      new ShardedClusterLabelFactory(new ShardedClusterLabelMapper());

  @Mock
  private DefaultProfileFactory defaultProfileFactory;

  @Mock
  private StackGresShardedClusterContext context;

  private ShardedClusterCoordinatorDefaultInstanceProfile
      shardedClusterCoordinatorDefaultInstanceProfile;

  private StackGresShardedCluster cluster;

  @BeforeEach
  void setUp() {
    shardedClusterCoordinatorDefaultInstanceProfile =
        new ShardedClusterCoordinatorDefaultInstanceProfile(labelFactory, defaultProfileFactory);
    cluster = Fixtures.shardedCluster().loadDefault().get();
    when(context.getSource()).thenReturn(cluster);

    lenient().when(defaultProfileFactory.buildResource(any())).thenReturn(
        new StackGresProfileBuilder().withNewSpec().endSpec().build());
  }

  @Test
  void generateResource_whenProfileEmpty_shouldGenerateDefault() {
    when(context.getCoordinatorProfile()).thenReturn(Optional.empty());

    List<HasMetadata> resources =
        shardedClusterCoordinatorDefaultInstanceProfile.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresProfile profile = (StackGresProfile) resources.getFirst();
    assertEquals(cluster.getSpec().getCoordinator().getSgInstanceProfile(),
        profile.getMetadata().getName());
  }

  @Test
  void generateResource_whenProfileExistsWithDefaultLabelsAndOwner_shouldGenerate() {
    StackGresProfile existingProfile = new StackGresProfileBuilder()
        .withNewMetadata()
        .withLabels(Map.copyOf(labelFactory.defaultConfigLabels(cluster)))
        .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
        .endMetadata()
        .build();
    when(context.getCoordinatorProfile()).thenReturn(Optional.of(existingProfile));

    List<HasMetadata> resources =
        shardedClusterCoordinatorDefaultInstanceProfile.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresProfile profile = (StackGresProfile) resources.getFirst();
    assertEquals(cluster.getSpec().getCoordinator().getSgInstanceProfile(),
        profile.getMetadata().getName());
  }

  @Test
  void generateResource_whenProfileExistsWithoutMatchingLabels_shouldNotGenerate() {
    StackGresProfile existingProfile = new StackGresProfileBuilder()
        .withNewMetadata()
        .withLabels(Map.of("other-label", "other-value"))
        .withOwnerReferences(List.of(ResourceUtil.getControllerOwnerReference(cluster)))
        .endMetadata()
        .build();
    when(context.getCoordinatorProfile()).thenReturn(Optional.of(existingProfile));

    List<HasMetadata> resources =
        shardedClusterCoordinatorDefaultInstanceProfile.generateResource(context).toList();

    assertTrue(resources.isEmpty());
  }

  @Test
  void generateResource_generatedProfileHasCorrectNameAndNamespace() {
    when(context.getCoordinatorProfile()).thenReturn(Optional.empty());

    List<HasMetadata> resources =
        shardedClusterCoordinatorDefaultInstanceProfile.generateResource(context).toList();

    assertEquals(1, resources.size());
    StackGresProfile profile = (StackGresProfile) resources.getFirst();
    assertEquals("size-s", profile.getMetadata().getName());
    assertEquals("stackgres", profile.getMetadata().getNamespace());
  }

}

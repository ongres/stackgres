/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Optional;

import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgcluster.StackGresCluster;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.fixture.Fixtures;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HugePagesMountsTest {

  private HugePagesMounts hugePagesMounts;

  @Mock
  private ClusterContainerContext clusterContainerContext;

  @Mock
  private StackGresClusterContext clusterContext;

  private StackGresProfile profile;

  @BeforeEach
  void setUp() {
    DefaultProfileFactory defaultProfileFactory = new DefaultProfileFactory();
    hugePagesMounts = new HugePagesMounts(defaultProfileFactory);
    profile = Fixtures.instanceProfile().loadSizeM().get();
    when(clusterContainerContext.getClusterContext()).thenReturn(clusterContext);
  }

  @Test
  void givenAClusterWithAProfileWithHugePages_itShouldCreateTheMountsWithHugePages() {
    profile.getSpec().setHugePages(new StackGresProfileHugePages());
    profile.getSpec().getHugePages().setHugepages2Mi("2Mi");
    profile.getSpec().getHugePages().setHugepages1Gi("1Gi");
    when(clusterContext.getProfile()).thenReturn(Optional.of(profile));

    var volumeMounts = hugePagesMounts.getVolumeMounts(clusterContainerContext);

    assertEquals(2, volumeMounts.size());

    assertTrue(volumeMounts.stream()
        .anyMatch(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_2M.getName())));
    assertTrue(volumeMounts.stream()
        .filter(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_2M.getName()))
        .anyMatch(volumeMount -> volumeMount.getMountPath()
            .equals(ClusterPath.HUGEPAGES_2M_PATH.path())));
    assertTrue(volumeMounts.stream()
        .anyMatch(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_1G.getName())));
    assertTrue(volumeMounts.stream()
        .filter(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_1G.getName()))
        .anyMatch(volumeMount -> volumeMount.getMountPath()
            .equals(ClusterPath.HUGEPAGES_1G_PATH.path())));

    var envVars = hugePagesMounts.getDerivedEnvVars(clusterContainerContext);

    assertTrue(envVars.stream()
        .anyMatch(envVar -> envVar
            .equals(ClusterPath.HUGEPAGES_2M_PATH.envVar())));
    assertTrue(envVars.stream()
        .anyMatch(envVar -> envVar
            .equals(ClusterPath.HUGEPAGES_1G_PATH.envVar())));
  }

  @Test
  void givenAClusterWithoutAProfileWithHugePages_itShouldNotCreateTheMountsWithHugePages() {
    when(clusterContext.getProfile()).thenReturn(Optional.of(profile));

    var volumeMounts = hugePagesMounts.getVolumeMounts(clusterContainerContext);

    assertFalse(volumeMounts.stream()
        .anyMatch(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_2M.getName())));
    assertFalse(volumeMounts.stream()
        .filter(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_2M.getName()))
        .anyMatch(volumeMount -> volumeMount.getMountPath()
            .equals(ClusterPath.HUGEPAGES_2M_PATH.path())));
    assertFalse(volumeMounts.stream()
        .anyMatch(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_1G.getName())));
    assertFalse(volumeMounts.stream()
        .filter(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_1G.getName()))
        .anyMatch(volumeMount -> volumeMount.getMountPath()
            .equals(ClusterPath.HUGEPAGES_1G_PATH.path())));

    var envVars = hugePagesMounts.getDerivedEnvVars(clusterContainerContext);

    assertTrue(envVars.stream()
        .anyMatch(envVar -> envVar
            .equals(ClusterPath.HUGEPAGES_2M_PATH.envVar())));
    assertTrue(envVars.stream()
        .anyMatch(envVar -> envVar
            .equals(ClusterPath.HUGEPAGES_1G_PATH.envVar())));
  }

  @Test
  void getVolumeMounts_whenOnly2MiSet_shouldMountOnly2Mi() {
    profile.getSpec().setHugePages(new StackGresProfileHugePages());
    profile.getSpec().getHugePages().setHugepages2Mi("2Mi");
    when(clusterContext.getProfile()).thenReturn(Optional.of(profile));

    var volumeMounts = hugePagesMounts.getVolumeMounts(clusterContainerContext);

    assertEquals(1, volumeMounts.size(),
        "Only the 2Mi hugepages volume mount should be present");
    assertTrue(volumeMounts.stream()
        .anyMatch(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_2M.getName())),
        "2Mi hugepages mount should be present");
    assertTrue(volumeMounts.stream()
        .filter(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_2M.getName()))
        .anyMatch(volumeMount -> volumeMount.getMountPath()
            .equals(ClusterPath.HUGEPAGES_2M_PATH.path())),
        "2Mi hugepages mount path should be correct");
    assertFalse(volumeMounts.stream()
        .anyMatch(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_1G.getName())),
        "1Gi hugepages mount should not be present when only 2Mi is set");
  }

  @Test
  void getVolumeMounts_whenOnly1GiSet_shouldMountOnly1Gi() {
    profile.getSpec().setHugePages(new StackGresProfileHugePages());
    profile.getSpec().getHugePages().setHugepages1Gi("1Gi");
    when(clusterContext.getProfile()).thenReturn(Optional.of(profile));

    var volumeMounts = hugePagesMounts.getVolumeMounts(clusterContainerContext);

    assertEquals(1, volumeMounts.size(),
        "Only the 1Gi hugepages volume mount should be present");
    assertFalse(volumeMounts.stream()
        .anyMatch(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_2M.getName())),
        "2Mi hugepages mount should not be present when only 1Gi is set");
    assertTrue(volumeMounts.stream()
        .anyMatch(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_1G.getName())),
        "1Gi hugepages mount should be present");
    assertTrue(volumeMounts.stream()
        .filter(volumeMount -> volumeMount.getName()
            .equals(StackGresVolume.HUGEPAGES_1G.getName()))
        .anyMatch(volumeMount -> volumeMount.getMountPath()
            .equals(ClusterPath.HUGEPAGES_1G_PATH.path())),
        "1Gi hugepages mount path should be correct");
  }

  @Test
  void getVolumeMounts_whenNullProfile_shouldReturnEmpty() {
    StackGresCluster cluster = Fixtures.cluster().loadDefault().get();
    when(clusterContext.getProfile()).thenReturn(Optional.empty());
    lenient().when(clusterContext.getSource()).thenReturn(cluster);

    var volumeMounts = hugePagesMounts.getVolumeMounts(clusterContainerContext);

    assertTrue(volumeMounts.isEmpty(),
        "When profile is null (defaults used), volume mounts should be empty "
            + "since default profile has no hugepages");
  }

}

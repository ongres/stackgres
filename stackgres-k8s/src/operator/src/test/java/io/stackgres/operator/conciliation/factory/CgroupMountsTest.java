/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.stackgres.common.ClusterPath;
import io.stackgres.common.StackGresVolume;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CgroupMountsTest {

  private CgroupMounts cgroupMounts;

  @Mock
  private ContainerContext context;

  @BeforeEach
  void setUp() {
    cgroupMounts = new CgroupMounts();
  }

  @Test
  void getVolumeMounts_shouldReturnCgroupVolumeMount() {
    var volumeMounts = cgroupMounts.getVolumeMounts(context);

    assertEquals(1, volumeMounts.size());
    assertTrue(volumeMounts.stream()
        .anyMatch(vm -> StackGresVolume.CGROUP.getName().equals(vm.getName())));
  }

  @Test
  void getVolumeMounts_shouldHaveCorrectMountPath() {
    var volumeMounts = cgroupMounts.getVolumeMounts(context);

    assertTrue(volumeMounts.stream()
        .filter(vm -> StackGresVolume.CGROUP.getName().equals(vm.getName()))
        .anyMatch(vm -> ClusterPath.HOST_CGROUP_PATH.path().equals(vm.getMountPath())));
  }

  @Test
  void getVolumeMounts_shouldNotBeReadOnly() {
    var volumeMounts = cgroupMounts.getVolumeMounts(context);

    assertTrue(volumeMounts.stream()
        .filter(vm -> StackGresVolume.CGROUP.getName().equals(vm.getName()))
        .anyMatch(vm -> Boolean.FALSE.equals(vm.getReadOnly())));
  }

  @Test
  void getDerivedEnvVars_shouldReturnCgroupEnvVars() {
    var envVars = cgroupMounts.getDerivedEnvVars(context);

    assertEquals(2, envVars.size());
    assertTrue(envVars.stream()
        .anyMatch(env -> env.equals(ClusterPath.CGROUP_PATH.envVar())));
    assertTrue(envVars.stream()
        .anyMatch(env -> env.equals(ClusterPath.HOST_CGROUP_PATH.envVar())));
  }

  @Test
  void getDerivedEnvVars_shouldNotBeEmpty() {
    var envVars = cgroupMounts.getDerivedEnvVars(context);

    assertFalse(envVars.isEmpty());
  }

}

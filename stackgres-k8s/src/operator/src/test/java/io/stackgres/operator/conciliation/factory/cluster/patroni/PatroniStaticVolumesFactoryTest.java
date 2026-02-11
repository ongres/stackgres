/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.common.StackGresVolume;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.VolumePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniStaticVolumesFactoryTest {

  @Mock
  private StackGresClusterContext context;

  private PatroniStaticVolumesFactory factory;

  @BeforeEach
  void setUp() {
    factory = new PatroniStaticVolumesFactory();
  }

  @Test
  void buildVolumes_shouldReturnNineVolumes() {
    List<VolumePair> volumes = factory.buildVolumes(context).toList();

    assertEquals(9, volumes.size());
  }

  @Test
  void buildVolumes_shouldIncludeDshmVolume() {
    List<VolumePair> volumes = factory.buildVolumes(context).toList();

    assertTrue(volumes.stream()
        .map(VolumePair::getVolume)
        .anyMatch(v -> StackGresVolume.DSHM.getName().equals(v.getName())));
  }

  @Test
  void buildVolumes_shouldIncludeLogVolume() {
    List<VolumePair> volumes = factory.buildVolumes(context).toList();

    assertTrue(volumes.stream()
        .map(VolumePair::getVolume)
        .anyMatch(v -> StackGresVolume.LOG.getName().equals(v.getName())));
  }

  @Test
  void buildVolumes_shouldIncludePostgresSocketVolume() {
    List<VolumePair> volumes = factory.buildVolumes(context).toList();

    assertTrue(volumes.stream()
        .map(VolumePair::getVolume)
        .anyMatch(v -> StackGresVolume.POSTGRES_SOCKET.getName().equals(v.getName())));
  }

  @Test
  void buildVolumes_shouldIncludeAllExpectedVolumes() {
    Set<String> expectedVolumeNames = Set.of(
        StackGresVolume.POSTGRES_SOCKET.getName(),
        StackGresVolume.DSHM.getName(),
        StackGresVolume.SHARED.getName(),
        StackGresVolume.EMPTY_BASE.getName(),
        StackGresVolume.USER.getName(),
        StackGresVolume.LOCAL_BIN.getName(),
        StackGresVolume.LOG.getName(),
        StackGresVolume.PATRONI_CONFIG.getName(),
        StackGresVolume.POSTGRES_SSL_COPY.getName());

    Set<String> actualVolumeNames = factory.buildVolumes(context)
        .map(VolumePair::getVolume)
        .map(Volume::getName)
        .collect(Collectors.toSet());

    assertEquals(expectedVolumeNames, actualVolumeNames);
  }

  @Test
  void buildVolumes_dshmShouldBeInMemoryDir() {
    Volume dshm = factory.buildVolumes(context)
        .map(VolumePair::getVolume)
        .filter(v -> StackGresVolume.DSHM.getName().equals(v.getName()))
        .findFirst()
        .orElseThrow();

    assertNotNull(dshm.getEmptyDir());
    assertEquals("Memory", dshm.getEmptyDir().getMedium());
  }

  @Test
  void buildVolumes_postgresSocketShouldBeInMemoryDir() {
    Volume socket = factory.buildVolumes(context)
        .map(VolumePair::getVolume)
        .filter(v -> StackGresVolume.POSTGRES_SOCKET.getName().equals(v.getName()))
        .findFirst()
        .orElseThrow();

    assertNotNull(socket.getEmptyDir());
    assertEquals("Memory", socket.getEmptyDir().getMedium());
  }

  @Test
  void buildVolumes_logShouldBeEmptyDir() {
    Volume log = factory.buildVolumes(context)
        .map(VolumePair::getVolume)
        .filter(v -> StackGresVolume.LOG.getName().equals(v.getName()))
        .findFirst()
        .orElseThrow();

    assertNotNull(log.getEmptyDir());
    assertEquals(null, log.getEmptyDir().getMedium());
  }

  @Test
  void buildVolumes_sharedShouldBeEmptyDir() {
    Volume shared = factory.buildVolumes(context)
        .map(VolumePair::getVolume)
        .filter(v -> StackGresVolume.SHARED.getName().equals(v.getName()))
        .findFirst()
        .orElseThrow();

    assertNotNull(shared.getEmptyDir());
    assertEquals(null, shared.getEmptyDir().getMedium());
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.cluster.patroni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.Volume;
import io.stackgres.common.StackGresVolume;
import io.stackgres.common.crd.sgprofile.StackGresProfile;
import io.stackgres.common.crd.sgprofile.StackGresProfileHugePages;
import io.stackgres.common.crd.sgprofile.StackGresProfileSpec;
import io.stackgres.operator.conciliation.cluster.StackGresClusterContext;
import io.stackgres.operator.conciliation.factory.VolumePair;
import io.stackgres.operator.initialization.DefaultProfileFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatroniHugePages2MiTest {

  @Mock
  private StackGresClusterContext context;

  @Mock
  private DefaultProfileFactory defaultProfileFactory;

  private PatroniHugePages2Mi patroniHugePages2Mi;

  @BeforeEach
  void setUp() {
    patroniHugePages2Mi = new PatroniHugePages2Mi(defaultProfileFactory);
  }

  @Test
  void buildVolumes_whenProfileHasHugepages2Mi_shouldReturnVolume() {
    StackGresProfile profile = new StackGresProfile();
    StackGresProfileSpec spec = new StackGresProfileSpec();
    StackGresProfileHugePages hugePages = new StackGresProfileHugePages();
    hugePages.setHugepages2Mi("2Mi");
    spec.setHugePages(hugePages);
    profile.setSpec(spec);
    when(context.getProfile()).thenReturn(Optional.of(profile));

    List<VolumePair> volumes = patroniHugePages2Mi.buildVolumes(context).toList();

    assertEquals(1, volumes.size());
    Volume volume = volumes.get(0).getVolume();
    assertEquals(StackGresVolume.HUGEPAGES_2M.getName(), volume.getName());
    assertEquals("HugePages-2Mi", volume.getEmptyDir().getMedium());
  }

  @Test
  void buildVolumes_whenProfileDoesNotHaveHugepages2Mi_shouldReturnEmpty() {
    StackGresProfile profile = new StackGresProfile();
    StackGresProfileSpec spec = new StackGresProfileSpec();
    profile.setSpec(spec);
    when(context.getProfile()).thenReturn(Optional.of(profile));

    List<VolumePair> volumes = patroniHugePages2Mi.buildVolumes(context).toList();

    assertTrue(volumes.isEmpty());
  }

  @Test
  void buildVolumes_whenProfileHasOnlyHugepages1Gi_shouldReturnEmpty() {
    StackGresProfile profile = new StackGresProfile();
    StackGresProfileSpec spec = new StackGresProfileSpec();
    StackGresProfileHugePages hugePages = new StackGresProfileHugePages();
    hugePages.setHugepages1Gi("1Gi");
    spec.setHugePages(hugePages);
    profile.setSpec(spec);
    when(context.getProfile()).thenReturn(Optional.of(profile));

    List<VolumePair> volumes = patroniHugePages2Mi.buildVolumes(context).toList();

    assertTrue(volumes.isEmpty());
  }

  @Test
  void buildVolumes_whenPatroniResourceRequirementsDisabled_shouldReturnEmpty() {
    when(context.calculateDisablePatroniResourceRequirements()).thenReturn(true);

    List<VolumePair> volumes = patroniHugePages2Mi.buildVolumes(context).toList();

    assertTrue(volumes.isEmpty());
  }

  @Test
  void buildVolume_shouldReturnVolumeWithCorrectNameAndMedium() {
    Volume volume = patroniHugePages2Mi.buildVolume(context);

    assertEquals(StackGresVolume.HUGEPAGES_2M.getName(), volume.getName());
    assertEquals("HugePages-2Mi", volume.getEmptyDir().getMedium());
  }
}

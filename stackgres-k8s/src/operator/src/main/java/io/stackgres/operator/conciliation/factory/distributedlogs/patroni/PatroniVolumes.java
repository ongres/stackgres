/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory.distributedlogs.patroni;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;

public enum PatroniVolumes {

  SOCKET("socket", new VolumeBuilder()
      .withNewEmptyDir()
      .withMedium("Memory")
      .endEmptyDir()),
  SHARED_MEMORY("dshm", new VolumeBuilder()
      .withNewEmptyDir()
      .withMedium("Memory")
      .endEmptyDir()),
  SHARED("shared", new VolumeBuilder()
      .withNewEmptyDir()
      .endEmptyDir()),
  LOCAL("local", new VolumeBuilder()
      .withNewEmptyDir()
      .endEmptyDir()),
  PATRONI_ENV("patroni-env", new VolumeBuilder()
      .withNewConfigMap()
      .withDefaultMode(444)
      .endConfigMap()),
  PATRONI_CONFIG("patroni-config", new VolumeBuilder()
      .withNewEmptyDir()
      .endEmptyDir());

  private String name;
  private VolumeBuilder volumeBuilder;

  PatroniVolumes(String name, VolumeBuilder volumeBuilder) {
    this.name = name;
    this.volumeBuilder = volumeBuilder;
  }

  public String getName() {
    return name;
  }

  public Volume buildVolume() {
    return volumeBuilder.withName(this.name).build();
  }
}

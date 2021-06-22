/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import io.fabric8.kubernetes.api.model.VolumeBuilder;

public interface StaticVolumeFactory<T> extends VolumeFactory<T> {

  default VolumePair inMemoryDir(String name) {
    return ImmutableVolumePair.builder()
        .volume(new VolumeBuilder()
            .withName(name)
            .withNewEmptyDir()
            .withMedium("Memory")
            .endEmptyDir()
            .build())
        .build();
  }

  default VolumePair emptyDir(String name) {
    return ImmutableVolumePair.builder()
        .volume(new VolumeBuilder()
            .withName(name)
            .withNewEmptyDir()
            .endEmptyDir()
            .build())
        .build();
  }

}

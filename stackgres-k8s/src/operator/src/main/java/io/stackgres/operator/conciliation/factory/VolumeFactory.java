/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.conciliation.factory;

import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.stackgres.common.StackGresGroupKind;
import org.jetbrains.annotations.NotNull;

public interface VolumeFactory<T> {

  @NotNull Stream<VolumePair> buildVolumes(@NotNull T context);

  default StackGresGroupKind kind() {
    return StackGresGroupKind.CLUSTER;
  }

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

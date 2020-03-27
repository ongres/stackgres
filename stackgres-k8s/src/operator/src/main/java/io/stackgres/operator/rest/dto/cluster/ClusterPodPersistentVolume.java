/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.cluster;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterPodPersistentVolume {

  @JsonProperty("size")
  @NotNull
  private String volumeSize;

  @JsonProperty("storageClass")
  private String storageClass;

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setVolumeSize(String volumeSize) {
    this.volumeSize = volumeSize;
  }

  public String getVolumeSize() {
    return volumeSize;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("volumeSize", volumeSize)
        .add("storageClass", storageClass)
        .toString();
  }
}

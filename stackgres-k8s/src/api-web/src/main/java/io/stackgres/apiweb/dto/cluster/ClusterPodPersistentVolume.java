/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterPodPersistentVolume {

  @JsonProperty("size")
  private String size;

  @JsonProperty("storageClass")
  private String storageClass;

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getSize() {
    return size;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterPodPersistentVolume that = (ClusterPodPersistentVolume) o;
    return Objects.equals(size, that.size)
        && Objects.equals(storageClass, that.storageClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(size, storageClass);
  }
}

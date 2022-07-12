/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgdistributedlogs;

import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresDistributedLogsPersistentVolume {

  @JsonProperty("size")
  @NotNull(message = "Volume size must be specified")
  @Pattern(regexp = "^[0-9]+(\\.[0-9]+)?(Mi|Gi|Ti)$",
      message = "Volume size must be specified in Mi, Gi or Ti")
  private String size;

  @JsonProperty("storageClass")
  private String storageClass;

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  @Override
  public int hashCode() {
    return Objects.hash(storageClass, size);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresDistributedLogsPersistentVolume)) {
      return false;
    }
    StackGresDistributedLogsPersistentVolume other = (StackGresDistributedLogsPersistentVolume) obj;
    return Objects.equals(storageClass, other.storageClass)
        && Objects.equals(size, other.size);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

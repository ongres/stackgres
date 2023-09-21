/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class BackupVolume {

  @NotNull(message = "The volume size is required")
  private String size;

  private String writeManyStorageClass;

  public String getSize() {
    return size;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public String getWriteManyStorageClass() {
    return writeManyStorageClass;
  }

  public void setWriteManyStorageClass(String writeManyStorageClass) {
    this.writeManyStorageClass = writeManyStorageClass;
  }

  @Override
  public int hashCode() {
    return Objects.hash(size, writeManyStorageClass);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof BackupVolume)) {
      return false;
    }
    BackupVolume other = (BackupVolume) obj;
    return Objects.equals(size, other.size)
        && Objects.equals(writeManyStorageClass, other.writeManyStorageClass);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

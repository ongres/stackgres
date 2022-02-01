/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.objectstorage;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.dto.storages.BackupStorageDto;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
public class ObjectStorageDto extends ResourceDto {

  @JsonProperty("spec")
  @NotNull(message = "The spec is required")
  @Valid
  private BackupStorageDto spec;

  @JsonProperty("status")
  private ObjectStorageStatus status;

  public BackupStorageDto getSpec() {
    return spec;
  }

  public void setSpec(BackupStorageDto spec) {
    this.spec = spec;
  }

  public ObjectStorageStatus getStatus() {
    return status;
  }

  public void setStatus(ObjectStorageStatus status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ObjectStorageDto that = (ObjectStorageDto) o;
    return Objects.equals(spec, that.spec) && Objects.equals(status, that.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(spec, status);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

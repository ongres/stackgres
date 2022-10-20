/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.objectstorage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.dto.storages.BackupStorageDto;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ObjectStorageDto extends ResourceDto {

  @JsonProperty("spec")
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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

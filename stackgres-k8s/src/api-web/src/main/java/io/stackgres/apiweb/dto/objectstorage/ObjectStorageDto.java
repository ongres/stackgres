/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.objectstorage;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.apiweb.dto.ResourceClassForDto;
import io.stackgres.apiweb.dto.ResourceDto;
import io.stackgres.apiweb.dto.storages.BackupStorageDto;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.sgobjectstorage.StackGresObjectStorage;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@ResourceClassForDto(StackGresObjectStorage.class)
public class ObjectStorageDto extends ResourceDto {

  private BackupStorageDto spec;

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

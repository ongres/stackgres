/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackupconfig;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.storages.BackupStorage;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresBackupConfigSpec {

  @NotNull(message = "The storage is required")
  @Valid
  private BackupStorage storage;

  @NotNull(message = "Base backup configuration is required")
  @Valid
  private StackGresBaseBackupConfig baseBackups;

  public BackupStorage getStorage() {
    return storage;
  }

  public void setStorage(BackupStorage storage) {
    this.storage = storage;
  }

  public StackGresBaseBackupConfig getBaseBackups() {
    return baseBackups;
  }

  public void setBaseBackups(StackGresBaseBackupConfig baseBackups) {
    this.baseBackups = baseBackups;
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseBackups, storage);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresBackupConfigSpec)) {
      return false;
    }
    StackGresBackupConfigSpec other = (StackGresBackupConfigSpec) obj;
    return Objects.equals(baseBackups, other.baseBackups) && Objects.equals(storage, other.storage);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

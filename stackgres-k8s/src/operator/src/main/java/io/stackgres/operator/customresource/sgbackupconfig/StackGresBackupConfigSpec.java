/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackupconfig;

import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.customresource.storages.BackupStorage;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresBackupConfigSpec implements KubernetesResource {

  private static final long serialVersionUID = 4124027524757318245L;

  @JsonProperty("storage")
  @NotNull(message = "The storage is required")
  @Valid
  private BackupStorage storage;

  @JsonProperty("baseBackups")
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
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("storage", storage)
        .add("baseBackups", baseBackups)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresBackupConfigSpec spec = (StackGresBackupConfigSpec) o;
    return Objects.equals(storage, spec.storage)
        && Objects.equals(baseBackups, spec.baseBackups);
  }

  @Override
  public int hashCode() {
    return Objects.hash(storage, baseBackups);
  }
}

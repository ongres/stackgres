/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgrestoreconfig;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.customresource.storages.BackupStorage;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackgresRestoreConfigSource {

  @JsonProperty("fromBackup")
  private String stackgresBackup;

  @JsonProperty("autoCopySecrets")
  private boolean autoCopySecretsEnabled;

  @JsonProperty("fromStorage")
  private BackupStorage storage;

  @JsonProperty("backupName")
  private String backupName;

  public String getStackgresBackup() {
    return stackgresBackup;
  }

  public void setStackgresBackup(String stackgresBackup) {
    this.stackgresBackup = stackgresBackup;
  }

  public boolean isAutoCopySecretsEnabled() {
    return autoCopySecretsEnabled;
  }

  public void setAutoCopySecretsEnabled(boolean autoCopySecretsEnabled) {
    this.autoCopySecretsEnabled = autoCopySecretsEnabled;
  }

  public BackupStorage getStorage() {
    return storage;
  }

  public void setStorage(BackupStorage storage) {
    this.storage = storage;
  }

  public String getBackupName() {
    return backupName;
  }

  public void setBackupName(String backupName) {
    this.backupName = backupName;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("stackgresBackup", stackgresBackup)
        .add("autoCopySecrets", autoCopySecretsEnabled)
        .add("storage", storage)
        .add("backupName", backupName)
        .toString();
  }
}

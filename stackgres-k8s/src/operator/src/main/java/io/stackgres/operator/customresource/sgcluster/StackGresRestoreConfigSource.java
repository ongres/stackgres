/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgcluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.customresource.storages.BackupStorage;
import io.stackgres.operator.customresource.storages.PgpConfiguration;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresRestoreConfigSource {

  @JsonProperty("fromBackup")
  private String stackgresBackup;

  @JsonProperty("autoCopySecrets")
  private boolean autoCopySecretsEnabled;

  @JsonProperty("fromStorage")
  private BackupStorage storage;

  @JsonProperty("backupName")
  private String backupName;

  @JsonProperty("compressionMethod")
  private String compressionMethod;

  @JsonProperty("pgpConfiguration")
  private PgpConfiguration pgpConfiguration;

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

  public String getCompressionMethod() {
    return compressionMethod;
  }

  public void setCompressionMethod(String compressionMethod) {
    this.compressionMethod = compressionMethod;
  }

  public PgpConfiguration getPgpConfiguration() {
    return pgpConfiguration;
  }

  public void setPgpConfiguration(PgpConfiguration pgpConfiguration) {
    this.pgpConfiguration = pgpConfiguration;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("stackgresBackup", stackgresBackup)
        .add("autoCopySecrets", autoCopySecretsEnabled)
        .add("storage", storage)
        .add("backupName", backupName)
        .add("compressionMethod", compressionMethod)
        .add("pgpConfiguration", pgpConfiguration)
        .toString();
  }
}

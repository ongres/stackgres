/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.cluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterRestore {

  @JsonProperty("downloadDiskConcurrency")
  private int downloadDiskConcurrency;

  @JsonProperty("fromBackup")
  private String backupUid;

  @JsonProperty("autoCopySecrets")
  private boolean autoCopySecretsEnabled;

  public int getDownloadDiskConcurrency() {
    return downloadDiskConcurrency;
  }

  public void setDownloadDiskConcurrency(int downloadDiskConcurrency) {
    this.downloadDiskConcurrency = downloadDiskConcurrency;
  }

  public String getBackupUid() {
    return backupUid;
  }

  public void setBackupUid(String backupUid) {
    this.backupUid = backupUid;
  }

  public boolean isAutoCopySecretsEnabled() {
    return autoCopySecretsEnabled;
  }

  public void setAutoCopySecretsEnabled(boolean autoCopySecretsEnabled) {
    this.autoCopySecretsEnabled = autoCopySecretsEnabled;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("backupUid", backupUid)
        .add("autoCopySecrets", autoCopySecretsEnabled)
        .add("downloadDiskConcurrency", downloadDiskConcurrency)
        .toString();
  }
}

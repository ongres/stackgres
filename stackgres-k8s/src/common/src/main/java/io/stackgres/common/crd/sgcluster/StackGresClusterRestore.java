/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterRestore implements KubernetesResource {

  private static final long serialVersionUID = 1L;

  @JsonProperty("downloadDiskConcurrency")
  private Integer downloadDiskConcurrency;

  @JsonProperty("fromBackup")
  private String backupUid;

  public Integer getDownloadDiskConcurrency() {
    return downloadDiskConcurrency;
  }

  public void setDownloadDiskConcurrency(Integer downloadDiskConcurrency) {
    this.downloadDiskConcurrency = downloadDiskConcurrency;
  }

  public String getBackupUid() {
    return backupUid;
  }

  public void setBackupUid(String backupUid) {
    this.backupUid = backupUid;
  }

  @Override
  public int hashCode() {
    return Objects.hash(backupUid, downloadDiskConcurrency);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterRestore)) {
      return false;
    }
    StackGresClusterRestore other = (StackGresClusterRestore) obj;
    return Objects.equals(backupUid, other.backupUid)
        && Objects.equals(downloadDiskConcurrency, other.downloadDiskConcurrency);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

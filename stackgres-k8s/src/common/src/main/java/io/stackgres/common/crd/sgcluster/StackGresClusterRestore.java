/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterRestore {

  @JsonProperty("downloadDiskConcurrency")
  private Integer downloadDiskConcurrency;

  @JsonProperty("fromBackup")
  @Valid
  @NotNull(message = "fromBackup cannot be null")
  private StackGresClusterRestoreFromBackup fromBackup;

  public Integer getDownloadDiskConcurrency() {
    return downloadDiskConcurrency;
  }

  public void setDownloadDiskConcurrency(Integer downloadDiskConcurrency) {
    this.downloadDiskConcurrency = downloadDiskConcurrency;
  }

  public StackGresClusterRestoreFromBackup getFromBackup() {
    return fromBackup;
  }

  public void setFromBackup(StackGresClusterRestoreFromBackup fromBackup) {
    this.fromBackup = fromBackup;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromBackup, downloadDiskConcurrency);
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
    return Objects.equals(fromBackup, other.fromBackup)
        && Objects.equals(downloadDiskConcurrency, other.downloadDiskConcurrency);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

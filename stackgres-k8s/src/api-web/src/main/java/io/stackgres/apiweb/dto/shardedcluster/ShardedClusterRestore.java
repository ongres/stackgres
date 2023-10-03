/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.shardedcluster;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ShardedClusterRestore {

  private Integer downloadDiskConcurrency;

  private ShardedClusterRestoreFromBackup fromBackup;

  public Integer getDownloadDiskConcurrency() {
    return downloadDiskConcurrency;
  }

  public void setDownloadDiskConcurrency(Integer downloadDiskConcurrency) {
    this.downloadDiskConcurrency = downloadDiskConcurrency;
  }

  public ShardedClusterRestoreFromBackup getFromBackup() {
    return fromBackup;
  }

  public void setFromBackup(ShardedClusterRestoreFromBackup fromBackup) {
    this.fromBackup = fromBackup;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

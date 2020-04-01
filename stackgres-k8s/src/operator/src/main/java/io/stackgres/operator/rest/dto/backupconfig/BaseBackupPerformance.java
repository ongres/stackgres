/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.backupconfig;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class BaseBackupPerformance {

  @JsonProperty("networkRateLimit")
  private long networkRateLimit;

  @JsonProperty("diskRateLimit")
  private long diskRateLimit;

  @JsonProperty("uploadDiskConcurrency")
  private int uploadDiskConcurrency;

  public long getNetworkRateLimit() {
    return networkRateLimit;
  }

  public void setNetworkRateLimit(long networkRateLimit) {
    this.networkRateLimit = networkRateLimit;
  }

  public long getDiskRateLimit() {
    return diskRateLimit;
  }

  public void setDiskRateLimit(long diskRateLimit) {
    this.diskRateLimit = diskRateLimit;
  }

  public int getUploadDiskConcurrency() {
    return uploadDiskConcurrency;
  }

  public void setUploadDiskConcurrency(int uploadDiskConcurrency) {
    this.uploadDiskConcurrency = uploadDiskConcurrency;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("networkRateLimit", networkRateLimit)
        .add("diskRateLimit", diskRateLimit)
        .add("uploadDiskConcurrency", uploadDiskConcurrency)
        .toString();
  }
}

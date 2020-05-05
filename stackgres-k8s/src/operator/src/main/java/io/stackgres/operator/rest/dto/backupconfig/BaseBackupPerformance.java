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

  @JsonProperty("maxNetworkBandwitdh")
  private long maxNetworkBandwitdh;

  @JsonProperty("maxDiskBandwitdh")
  private long maxDiskBandwitdh;

  @JsonProperty("uploadDiskConcurrency")
  private int uploadDiskConcurrency;

  public long getNetworkRateLimit() {
    return maxNetworkBandwitdh;
  }

  public void setNetworkRateLimit(long maxNetworkBandwitdh) {
    this.maxNetworkBandwitdh = maxNetworkBandwitdh;
  }

  public long getDiskRateLimit() {
    return maxDiskBandwitdh;
  }

  public void setDiskRateLimit(long maxDiskBandwitdh) {
    this.maxDiskBandwitdh = maxDiskBandwitdh;
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
        .add("maxNetworkBandwitdh", maxNetworkBandwitdh)
        .add("maxDiskBandwitdh", maxDiskBandwitdh)
        .add("uploadDiskConcurrency", uploadDiskConcurrency)
        .toString();
  }
}

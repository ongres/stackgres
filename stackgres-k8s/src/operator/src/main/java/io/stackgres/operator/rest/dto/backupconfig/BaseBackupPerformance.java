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
  private Long maxNetworkBandwitdh;

  @JsonProperty("maxDiskBandwitdh")
  private Long maxDiskBandwitdh;

  @JsonProperty("uploadDiskConcurrency")
  private Integer uploadDiskConcurrency;

  public Long getNetworkRateLimit() {
    return maxNetworkBandwitdh;
  }

  public void setNetworkRateLimit(Long maxNetworkBandwitdh) {
    this.maxNetworkBandwitdh = maxNetworkBandwitdh;
  }

  public Long getDiskRateLimit() {
    return maxDiskBandwitdh;
  }

  public void setDiskRateLimit(Long maxDiskBandwitdh) {
    this.maxDiskBandwitdh = maxDiskBandwitdh;
  }

  public Integer getUploadDiskConcurrency() {
    return uploadDiskConcurrency;
  }

  public void setUploadDiskConcurrency(Integer uploadDiskConcurrency) {
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

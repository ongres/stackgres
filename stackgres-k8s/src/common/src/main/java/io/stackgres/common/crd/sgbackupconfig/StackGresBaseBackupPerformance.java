/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackupconfig;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresBaseBackupPerformance {

  @JsonProperty("maxNetworkBandwitdh")
  private long maxNetworkBandwitdh;

  @JsonProperty("maxDiskBandwitdh")
  private long maxDiskBandwitdh;

  @JsonProperty("uploadDiskConcurrency")
  private int uploadDiskConcurrency;

  public long getMaxNetworkBandwitdh() {
    return maxNetworkBandwitdh;
  }

  public void setMaxNetworkBandwitdh(Long maxNetworkBandwitdh) {
    this.maxNetworkBandwitdh = maxNetworkBandwitdh;
  }

  public long getMaxDiskBandwitdh() {
    return maxDiskBandwitdh;
  }

  public void setMaxDiskBandwitdh(Long maxDiskBandwitdh) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresBaseBackupPerformance that = (StackGresBaseBackupPerformance) o;
    return maxNetworkBandwitdh == that.maxNetworkBandwitdh
        && maxDiskBandwitdh == that.maxDiskBandwitdh
        && uploadDiskConcurrency == that.uploadDiskConcurrency;
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxNetworkBandwitdh, maxDiskBandwitdh, uploadDiskConcurrency);
  }
}

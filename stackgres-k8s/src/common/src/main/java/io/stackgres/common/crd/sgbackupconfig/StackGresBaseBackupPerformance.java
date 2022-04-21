/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackupconfig;

import java.util.Objects;

import javax.validation.constraints.Null;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresBaseBackupPerformance {

  @JsonProperty("maxNetworkBandwitdh")
  @Null
  private Long maxNetworkBandwitdh;

  @JsonProperty("maxDiskBandwitdh")
  @Null
  private Long maxDiskBandwitdh;

  @JsonProperty("maxNetworkBandwidth")
  private Long maxNetworkBandwidth;

  @JsonProperty("maxDiskBandwidth")
  private Long maxDiskBandwidth;

  @JsonProperty("uploadDiskConcurrency")
  private Integer uploadDiskConcurrency;

  public Long getMaxNetworkBandwitdh() {
    return maxNetworkBandwitdh;
  }

  public void setMaxNetworkBandwitdh(Long maxNetworkBandwitdh) {
    this.maxNetworkBandwitdh = maxNetworkBandwitdh;
  }

  public Long getMaxDiskBandwitdh() {
    return maxDiskBandwitdh;
  }

  public void setMaxDiskBandwitdh(Long maxDiskBandwitdh) {
    this.maxDiskBandwitdh = maxDiskBandwitdh;
  }

  public Long getMaxNetworkBandwidth() {
    return maxNetworkBandwidth;
  }

  public void setMaxNetworkBandwidth(Long maxNetworkBandwidth) {
    this.maxNetworkBandwidth = maxNetworkBandwidth;
  }

  public Long getMaxDiskBandwidth() {
    return maxDiskBandwidth;
  }

  public void setMaxDiskBandwidth(Long maxDiskBandwidth) {
    this.maxDiskBandwidth = maxDiskBandwidth;
  }

  public Integer getUploadDiskConcurrency() {
    return uploadDiskConcurrency;
  }

  public void setUploadDiskConcurrency(Integer uploadDiskConcurrency) {
    this.uploadDiskConcurrency = uploadDiskConcurrency;
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxDiskBandwidth, maxDiskBandwitdh, maxNetworkBandwidth,
        maxNetworkBandwitdh, uploadDiskConcurrency);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresBaseBackupPerformance)) {
      return false;
    }
    StackGresBaseBackupPerformance other = (StackGresBaseBackupPerformance) obj;
    return Objects.equals(maxDiskBandwidth, other.maxDiskBandwidth)
        && Objects.equals(maxDiskBandwitdh, other.maxDiskBandwitdh)
        && Objects.equals(maxNetworkBandwidth, other.maxNetworkBandwidth)
        && Objects.equals(maxNetworkBandwitdh, other.maxNetworkBandwitdh)
        && Objects.equals(uploadDiskConcurrency, other.uploadDiskConcurrency);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

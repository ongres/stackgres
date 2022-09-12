/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackupconfig;

import java.util.Objects;

import javax.validation.constraints.Null;

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
public class StackGresBaseBackupPerformance {

  @JsonProperty("maxNetworkBandwitdh")
  @Null
  @Deprecated(forRemoval = true)
  private Long maxNetworkBandwitdh;

  @JsonProperty("maxDiskBandwitdh")
  @Null
  @Deprecated(forRemoval = true)
  private Long maxDiskBandwitdh;

  @JsonProperty("maxNetworkBandwidth")
  private Long maxNetworkBandwidth;

  @JsonProperty("maxDiskBandwidth")
  private Long maxDiskBandwidth;

  @JsonProperty("uploadDiskConcurrency")
  private Integer uploadDiskConcurrency;

  @Deprecated(forRemoval = true)
  public Long getMaxNetworkBandwitdh() {
    return maxNetworkBandwitdh;
  }

  @Deprecated(forRemoval = true)
  public void setMaxNetworkBandwitdh(Long maxNetworkBandwitdh) {
    this.maxNetworkBandwitdh = maxNetworkBandwitdh;
  }

  @Deprecated(forRemoval = true)
  public Long getMaxDiskBandwitdh() {
    return maxDiskBandwitdh;
  }

  @Deprecated(forRemoval = true)
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
    return Objects.hash(maxDiskBandwidth, maxNetworkBandwidth, uploadDiskConcurrency);
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
        && Objects.equals(maxNetworkBandwidth, other.maxNetworkBandwidth)
        && Objects.equals(uploadDiskConcurrency, other.uploadDiskConcurrency);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

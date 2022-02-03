/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.backupconfig;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

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

  public Integer getUploadDiskConcurrency() {
    return uploadDiskConcurrency;
  }

  public void setUploadDiskConcurrency(Integer uploadDiskConcurrency) {
    this.uploadDiskConcurrency = uploadDiskConcurrency;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BaseBackupPerformance that = (BaseBackupPerformance) o;
    return Objects.equals(maxNetworkBandwitdh, that.maxNetworkBandwitdh)
        && Objects.equals(maxDiskBandwitdh, that.maxDiskBandwitdh)
        && Objects.equals(uploadDiskConcurrency, that.uploadDiskConcurrency);
  }

  @Override
  public int hashCode() {
    return Objects.hash(maxNetworkBandwitdh, maxDiskBandwitdh, uploadDiskConcurrency);
  }
}

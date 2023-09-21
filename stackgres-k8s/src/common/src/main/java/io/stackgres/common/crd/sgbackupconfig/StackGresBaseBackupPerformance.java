/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgbackupconfig;

import java.util.Objects;

import javax.validation.constraints.Null;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresBaseBackupPerformance {

  @Null
  @Deprecated(forRemoval = true)
  private Long maxNetworkBandwitdh;

  @Null
  @Deprecated(forRemoval = true)
  private Long maxDiskBandwitdh;

  private Long maxNetworkBandwidth;

  private Long maxDiskBandwidth;

  private Integer uploadDiskConcurrency;

  private Integer uploadConcurrency;

  private Integer downloadConcurrency;

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

  public Integer getUploadConcurrency() {
    return uploadConcurrency;
  }

  public void setUploadConcurrency(Integer uploadConcurrency) {
    this.uploadConcurrency = uploadConcurrency;
  }

  public Integer getDownloadConcurrency() {
    return downloadConcurrency;
  }

  public void setDownloadConcurrency(Integer downloadConcurrency) {
    this.downloadConcurrency = downloadConcurrency;
  }

  @Override
  public int hashCode() {
    return Objects.hash(downloadConcurrency, maxDiskBandwidth, maxDiskBandwitdh,
        maxNetworkBandwidth, maxNetworkBandwitdh, uploadConcurrency, uploadDiskConcurrency);
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
    return Objects.equals(downloadConcurrency, other.downloadConcurrency)
        && Objects.equals(maxDiskBandwidth, other.maxDiskBandwidth)
        && Objects.equals(maxDiskBandwitdh, other.maxDiskBandwitdh)
        && Objects.equals(maxNetworkBandwidth, other.maxNetworkBandwidth)
        && Objects.equals(maxNetworkBandwitdh, other.maxNetworkBandwitdh)
        && Objects.equals(uploadConcurrency, other.uploadConcurrency)
        && Objects.equals(uploadDiskConcurrency, other.uploadDiskConcurrency);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

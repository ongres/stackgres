/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.backupconfig;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class BaseBackupPerformance {

  private Long maxNetworkBandwidth;

  private Long maxDiskBandwidth;

  private Integer uploadDiskConcurrency;

  private Integer uploadConcurrency;

  private Integer downloadConcurrency;

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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

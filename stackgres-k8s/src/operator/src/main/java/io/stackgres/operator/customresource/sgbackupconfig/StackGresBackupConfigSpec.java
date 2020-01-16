/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackupconfig;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.fabric8.kubernetes.api.model.KubernetesResource;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.operator.customresource.storages.BackupStorage;
import io.stackgres.operator.customresource.storages.PgpConfiguration;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresBackupConfigSpec implements KubernetesResource {

  private static final long serialVersionUID = 4124027524757318245L;

  @JsonProperty("storage")
  @NotNull(message = "The storage is required")
  private BackupStorage storage;

  @JsonProperty("retention")
  @Positive(message = "retention should be greater than zero")
  private int retention;

  @JsonProperty("fullSchedule")
  private String fullSchedule;

  @JsonProperty("fullWindow")
  private int fullWindow;

  @JsonProperty("compressionMethod")
  private String compressionMethod;

  @JsonProperty("networkRateLimit")
  private long networkRateLimit;

  @JsonProperty("diskRateLimit")
  private long diskRateLimit;

  @JsonProperty("uploadDiskConcurrency")
  private int uploadDiskConcurrency;

  @JsonProperty("pgpConfiguration")
  private PgpConfiguration pgpConfiguration;

  @JsonProperty("tarSizeThreshold")
  private long tarSizeThreshold;

  public BackupStorage getStorage() {
    return storage;
  }

  public void setStorage(BackupStorage storage) {
    this.storage = storage;
  }

  public int getRetention() {
    return retention;
  }

  public void setRetention(int retention) {
    this.retention = retention;
  }

  public String getFullSchedule() {
    return fullSchedule;
  }

  public void setFullSchedule(String fullSchedule) {
    this.fullSchedule = fullSchedule;
  }

  public int getFullWindow() {
    return fullWindow;
  }

  public void setFullWindow(int fullWindow) {
    this.fullWindow = fullWindow;
  }

  public String getCompressionMethod() {
    return compressionMethod;
  }

  public void setCompressionMethod(String compressionMethod) {
    this.compressionMethod = compressionMethod;
  }

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

  public PgpConfiguration getPgpConfiguration() {
    return pgpConfiguration;
  }

  public void setPgpConfiguration(PgpConfiguration pgpConfiguration) {
    this.pgpConfiguration = pgpConfiguration;
  }

  public long getTarSizeThreshold() {
    return tarSizeThreshold;
  }

  public void setTarSizeThreshold(long tarSizeThreshold) {
    this.tarSizeThreshold = tarSizeThreshold;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("storage", storage)
        .add("fullSchedule", fullSchedule)
        .add("fullWindow", fullWindow)
        .add("compressionMethod", compressionMethod)
        .add("networkRateLimit", networkRateLimit)
        .add("diskRateLimit", diskRateLimit)
        .add("uploadDiskConcurrency", uploadDiskConcurrency)
        .add("pgpConfiguration", pgpConfiguration)
        .add("tarSizeThreshold", tarSizeThreshold)
        .toString();
  }

}

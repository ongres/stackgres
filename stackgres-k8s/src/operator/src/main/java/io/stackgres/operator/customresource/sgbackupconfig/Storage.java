/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackupconfig;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class Storage {

  @JsonProperty("type")
  @NotNull(message = "The storage type is required")
  private String type;

  @JsonProperty("volume")
  private BackupVolume volume;

  @JsonProperty("s3")
  private AwsS3Storage s3;

  @JsonProperty("gcs")
  private GoogleCloudStorage gcs;

  @JsonProperty("azureblob")
  private AzureBlobStorage azureblob;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public BackupVolume getVolume() {
    return volume;
  }

  public void setVolume(BackupVolume volume) {
    this.volume = volume;
  }

  public AwsS3Storage getS3() {
    return s3;
  }

  public void setS3(AwsS3Storage s3) {
    this.s3 = s3;
  }

  public GoogleCloudStorage getGcs() {
    return gcs;
  }

  public void setGcs(GoogleCloudStorage gcs) {
    this.gcs = gcs;
  }

  public AzureBlobStorage getAzureblob() {
    return azureblob;
  }

  public void setAzureblob(AzureBlobStorage azureblob) {
    this.azureblob = azureblob;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("type", type)
        .add("volume", volume)
        .add("s3", s3)
        .add("gcs", gcs)
        .add("azureblob", azureblob)
        .toString();
  }

}

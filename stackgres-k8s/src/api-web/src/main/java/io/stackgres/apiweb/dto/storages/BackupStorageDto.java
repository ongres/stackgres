/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class BackupStorageDto {

  @JsonProperty("type")
  @NotNull(message = "The storage type is required")
  private String type;

  @JsonProperty("s3")
  @Valid
  private AwsS3StorageDto s3;

  @JsonProperty("s3Compatible")
  @Valid
  private AwsS3CompatibleStorageDto s3Compatible;

  @JsonProperty("gcs")
  @Valid
  private GoogleCloudStorageDto gcs;

  @JsonProperty("azureBlob")
  @Valid
  private AzureBlobStorageDto azureBlob;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public AwsS3StorageDto getS3() {
    return s3;
  }

  public void setS3(AwsS3StorageDto s3) {
    this.s3 = s3;
  }

  public AwsS3CompatibleStorageDto getS3Compatible() {
    return s3Compatible;
  }

  public void setS3Compatible(AwsS3CompatibleStorageDto s3Compatible) {
    this.s3Compatible = s3Compatible;
  }

  public GoogleCloudStorageDto getGcs() {
    return gcs;
  }

  public void setGcs(GoogleCloudStorageDto gcs) {
    this.gcs = gcs;
  }

  public AzureBlobStorageDto getAzureBlob() {
    return azureBlob;
  }

  public void setAzureBlob(AzureBlobStorageDto azureBlob) {
    this.azureBlob = azureBlob;
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
    BackupStorageDto that = (BackupStorageDto) o;
    return Objects.equals(type, that.type) && Objects.equals(s3, that.s3)
        && Objects.equals(s3Compatible, that.s3Compatible)
        && Objects.equals(gcs, that.gcs)
        && Objects.equals(azureBlob, that.azureBlob);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, s3, s3Compatible, gcs, azureBlob);
  }
}

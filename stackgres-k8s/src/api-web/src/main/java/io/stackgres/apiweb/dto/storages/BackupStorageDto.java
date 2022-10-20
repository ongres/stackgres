/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class BackupStorageDto {

  @JsonProperty("type")
  private String type;

  @JsonProperty("s3")
  private AwsS3StorageDto s3;

  @JsonProperty("s3Compatible")
  private AwsS3CompatibleStorageDto s3Compatible;

  @JsonProperty("gcs")
  private GoogleCloudStorageDto gcs;

  @JsonProperty("azureBlob")
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

}

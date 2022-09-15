/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AzureBlobStorageDto {

  @JsonProperty("bucket")
  private String bucket;

  @JsonProperty("path")
  private String path;

  @JsonProperty("azureCredentials")
  private AzureBlobStorageCredentialsDto credentials;

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public AzureBlobStorageCredentialsDto getCredentials() {
    return credentials;
  }

  public void setCredentials(AzureBlobStorageCredentialsDto credentials) {
    this.credentials = credentials;
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
    AzureBlobStorageDto that = (AzureBlobStorageDto) o;
    return Objects.equals(bucket, that.bucket) && Objects.equals(path, that.path)
        && Objects.equals(credentials, that.credentials);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bucket, path, credentials);
  }
}

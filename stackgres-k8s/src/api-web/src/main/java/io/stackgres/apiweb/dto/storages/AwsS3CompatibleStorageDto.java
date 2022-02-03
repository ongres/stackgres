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
public class AwsS3CompatibleStorageDto {

  @JsonProperty("bucket")
  @NotNull(message = "The bucket is required")
  private String bucket;

  @JsonProperty("path")
  private String path;

  @JsonProperty("awsCredentials")
  @NotNull(message = "The credentials is required")
  @Valid
  private AwsCredentialsDto credentials;

  @JsonProperty("region")
  private String region;

  @JsonProperty("endpoint")
  private String endpoint;

  @JsonProperty("enablePathStyleAddressing")
  private Boolean enablePathStyleAddressing;

  @JsonProperty("storageClass")
  private String storageClass;

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

  public AwsCredentialsDto getCredentials() {
    return credentials;
  }

  public void setCredentials(AwsCredentialsDto credentials) {
    this.credentials = credentials;
  }

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public String getEndpoint() {
    return endpoint;
  }

  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }

  public Boolean isForcePathStyle() {
    return enablePathStyleAddressing;
  }

  public void setForcePathStyle(Boolean enablePathStyleAddressing) {
    this.enablePathStyleAddressing = enablePathStyleAddressing;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
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
    AwsS3CompatibleStorageDto that = (AwsS3CompatibleStorageDto) o;
    return Objects.equals(bucket, that.bucket) && Objects.equals(path, that.path)
        && Objects.equals(credentials, that.credentials)
        && Objects.equals(region, that.region)
        && Objects.equals(endpoint, that.endpoint)
        && Objects.equals(enablePathStyleAddressing, that.enablePathStyleAddressing)
        && Objects.equals(storageClass, that.storageClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bucket, path, credentials, region, endpoint,
        enablePathStyleAddressing, storageClass);
  }
}

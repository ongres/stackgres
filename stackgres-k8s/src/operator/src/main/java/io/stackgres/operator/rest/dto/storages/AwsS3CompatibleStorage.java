/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.rest.dto.storages;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class AwsS3CompatibleStorage {

  @JsonProperty("bucket")
  @NotNull(message = "The bucket is required")
  private String bucket;

  @JsonProperty("path")
  private String path;

  @JsonProperty("awsCredentials")
  @NotNull(message = "The credentials is required")
  private AwsCredentials credentials;

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

  public AwsCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(AwsCredentials credentials) {
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
  public int hashCode() {
    return Objects.hash(credentials, endpoint, enablePathStyleAddressing, bucket,
        region, storageClass, path);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof AwsS3CompatibleStorage)) {
      return false;
    }
    AwsS3CompatibleStorage other = (AwsS3CompatibleStorage) obj;
    return Objects.equals(credentials, other.credentials)
        && Objects.equals(endpoint, other.endpoint) && enablePathStyleAddressing == other.enablePathStyleAddressing
        && Objects.equals(bucket, other.bucket) && Objects.equals(region, other.region)
        && Objects.equals(storageClass, other.storageClass) && Objects.equals(path, other.path);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("bucket", bucket)
        .add("path", path)
        .add("credentials", credentials)
        .add("region", region)
        .add("endpoint", endpoint)
        .add("enablePathStyleAddressing", enablePathStyleAddressing)
        .add("storageClass", storageClass)
        .toString();
  }

}

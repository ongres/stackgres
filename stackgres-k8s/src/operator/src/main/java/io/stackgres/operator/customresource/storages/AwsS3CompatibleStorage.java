/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.storages;

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
public class AwsS3CompatibleStorage implements PrefixedStorage {

  @JsonProperty("bucket")
  @NotNull(message = "The bucket is required")
  private String bucket;

  @JsonProperty("path")
  private String path;

  @JsonProperty("credentials")
  @NotNull(message = "The credentials is required")
  private AwsCredentials credentials;

  @JsonProperty("region")
  private String region;

  @JsonProperty("endpoint")
  private String endpoint;

  @JsonProperty("forcePathStyle")
  private Boolean forcePathStyle;

  @JsonProperty("storageClass")
  private String storageClass;

  @Override
  public String getSchema() {
    return "s3";
  }

  @Override
  public String getBucket() {
    return bucket;
  }

  @Override
  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
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
    return forcePathStyle;
  }

  public void setForcePathStyle(Boolean forcePathStyle) {
    this.forcePathStyle = forcePathStyle;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  @Override
  public int hashCode() {
    return Objects.hash(credentials, endpoint, forcePathStyle, bucket,
        region, storageClass);
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
        && Objects.equals(endpoint, other.endpoint) && forcePathStyle == other.forcePathStyle
        && Objects.equals(bucket, other.bucket) && Objects.equals(path, other.path)
        && Objects.equals(region, other.region)
        && Objects.equals(storageClass, other.storageClass);
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
        .add("forcePathStyle", forcePathStyle)
        .add("storageClass", storageClass)
        .toString();
  }

}

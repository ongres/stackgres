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
public class AwsS3Storage implements PrefixedStorage {

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

  @JsonProperty("storageClass")
  private String storageClass;

  @JsonProperty("sse")
  private String sse;

  @JsonProperty("sseKmsId")
  private String sseKmsId;

  @JsonProperty("cseKmsId")
  private String cseKmsId;

  @JsonProperty("cseKmsRegion")
  private String cseKmsRegion;

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

  public String getStorageClass() {
    return storageClass;
  }

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  public String getSse() {
    return sse;
  }

  public void setSse(String sse) {
    this.sse = sse;
  }

  public String getSseKmsId() {
    return sseKmsId;
  }

  public void setSseKmsId(String sseKmsId) {
    this.sseKmsId = sseKmsId;
  }

  public String getCseKmsId() {
    return cseKmsId;
  }

  public void setCseKmsId(String cseKmsId) {
    this.cseKmsId = cseKmsId;
  }

  public String getCseKmsRegion() {
    return cseKmsRegion;
  }

  public void setCseKmsRegion(String cseKmsRegion) {
    this.cseKmsRegion = cseKmsRegion;
  }

  @Override
  public int hashCode() {
    return Objects.hash(credentials, cseKmsId, cseKmsRegion, bucket,
        region, sse, sseKmsId, storageClass, path);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof AwsS3Storage)) {
      return false;
    }
    AwsS3Storage other = (AwsS3Storage) obj;
    return Objects.equals(credentials, other.credentials)
        && Objects.equals(cseKmsId, other.cseKmsId)
        && Objects.equals(cseKmsRegion, other.cseKmsRegion)
        && Objects.equals(bucket, other.bucket) && Objects.equals(region, other.region)
        && Objects.equals(sse, other.sse) && Objects.equals(sseKmsId, other.sseKmsId)
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
        .add("storageClass", storageClass)
        .add("sse", sse)
        .add("sseKmsId", sseKmsId)
        .add("cseKmsId", cseKmsId)
        .add("cseKmsRegion", cseKmsRegion)
        .toString();
  }

}

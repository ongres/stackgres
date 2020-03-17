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
public class GoogleCloudStorage implements PrefixedStorage {

  @JsonProperty("bucket")
  @NotNull(message = "The bucket is required")
  private String bucket;

  @JsonProperty("path")
  private String path;

  @JsonProperty("credentials")
  @NotNull(message = "The credentials is required")
  private GoogleCloudCredentials credentials;

  @Override
  public String getSchema() {
    return "gcs";
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

  public String getPrefix() {
    String path = getPath();
    if (path != null) {
      if (!path.startsWith("/")) {
        path = "/" + path;
      }
    } else {
      path = "";
    }
    return "gcs://" + getBucket() + path;
  }

  public void setPrefix(String prefix) {
    int doubleSlashIndex = prefix.indexOf("://");
    final int endBucketIndex;
    if (doubleSlashIndex > 0) {
      endBucketIndex = prefix.indexOf("/", doubleSlashIndex + 3) + 1;
    } else {
      endBucketIndex = prefix.length();
    }
    final int startBucketIndex;
    if (prefix.startsWith("gcs://")) {
      startBucketIndex = 6;
    } else {
      startBucketIndex = 0;
    }
    setBucket(prefix.substring(startBucketIndex, endBucketIndex));
    setPath(prefix.substring(endBucketIndex));
  }

  public GoogleCloudCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(GoogleCloudCredentials credentials) {
    this.credentials = credentials;
  }

  @Override
  public int hashCode() {
    return Objects.hash(credentials, bucket, path);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof GoogleCloudStorage)) {
      return false;
    }
    GoogleCloudStorage other = (GoogleCloudStorage) obj;
    return Objects.equals(credentials, other.credentials) && Objects.equals(bucket, other.bucket)
        && Objects.equals(path, other.path);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("bucket", bucket)
        .add("path", path)
        .add("credentials", credentials)
        .toString();
  }

}

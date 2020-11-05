/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

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
public class GoogleCloudStorage implements PrefixedStorage {

  @JsonProperty("bucket")
  @NotNull(message = "The bucket is required")
  private String bucket;

  @JsonProperty("path")
  private String path;

  @JsonProperty("gcpCredentials")
  @NotNull(message = "The credentials is required")
  @Valid
  private GoogleCloudCredentials credentials;

  @Override
  public String getSchema() {
    return "gcs";
  }

  @Override
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

  public GoogleCloudCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(GoogleCloudCredentials credentials) {
    this.credentials = credentials;
  }

  @Override
  public int hashCode() {
    return Objects.hash(bucket, credentials, path);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof GoogleCloudStorage)) {
      return false;
    }
    GoogleCloudStorage other = (GoogleCloudStorage) obj;
    return Objects.equals(bucket, other.bucket) && Objects.equals(credentials, other.credentials)
        && Objects.equals(path, other.path);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

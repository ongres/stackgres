/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class GoogleCloudStorage implements PrefixedStorage {

  @NotNull(message = "The bucket is required")
  private String bucket;

  @Deprecated(forRemoval = true)
  private String path;

  @NotNull(message = "The credentials is required")
  @Valid
  private GoogleCloudCredentials gcpCredentials;

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
  @Deprecated(forRemoval = true)
  public String getPath() {
    return path;
  }

  @Override
  @Deprecated(forRemoval = true)
  public void setPath(String path) {
    this.path = path;
  }

  public GoogleCloudCredentials getGcpCredentials() {
    return gcpCredentials;
  }

  public void setGcpCredentials(GoogleCloudCredentials gcpCredentials) {
    this.gcpCredentials = gcpCredentials;
  }

  @Override
  public int hashCode() {
    return Objects.hash(bucket, gcpCredentials, path);
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
    return Objects.equals(bucket, other.bucket)
        && Objects.equals(gcpCredentials, other.gcpCredentials)
        && Objects.equals(path, other.path);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

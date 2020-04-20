/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

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
public class AzureBlobStorage implements PrefixedStorage {

  @JsonProperty("bucket")
  @NotNull(message = "The bucket is required")
  private String bucket;

  @JsonProperty("path")
  private String path;

  @JsonProperty("azureCredentials")
  @NotNull(message = "The azureCredentials is required")
  private AzureBlobStorageCredentials azureCredentials;

  @Override
  public String getSchema() {
    return "azure";
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

  public AzureBlobStorageCredentials getAzureCredentials() {
    return azureCredentials;
  }

  public void setAzureCredentials(AzureBlobStorageCredentials azureCredentials) {
    this.azureCredentials = azureCredentials;
  }

  @Override
  public int hashCode() {
    return Objects.hash(azureCredentials, bucket);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof AzureBlobStorage)) {
      return false;
    }
    AzureBlobStorage other = (AzureBlobStorage) obj;
    return Objects.equals(azureCredentials, other.azureCredentials)
        && Objects.equals(bucket, other.bucket)
        && Objects.equals(path, other.path);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("bucket", bucket)
        .add("path", path)
        .add("azureCredentials", azureCredentials)
        .toString();
  }

}

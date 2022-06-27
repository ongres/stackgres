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
public class AzureBlobStorage implements PrefixedStorage {

  @JsonProperty("bucket")
  @NotNull(message = "The bucket is required")
  private String bucket;

  @JsonProperty("path")
  @Deprecated(forRemoval = true)
  private String path;

  @JsonProperty("azureCredentials")
  @NotNull(message = "The azureCredentials is required")
  @Valid
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
  @Deprecated(forRemoval = true)
  public String getPath() {
    return path;
  }

  @Override
  @Deprecated(forRemoval = true)
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
    return Objects.hash(azureCredentials, bucket, path);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AzureBlobStorage)) {
      return false;
    }
    AzureBlobStorage other = (AzureBlobStorage) obj;
    return Objects.equals(azureCredentials, other.azureCredentials)
        && Objects.equals(bucket, other.bucket) && Objects.equals(path, other.path);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

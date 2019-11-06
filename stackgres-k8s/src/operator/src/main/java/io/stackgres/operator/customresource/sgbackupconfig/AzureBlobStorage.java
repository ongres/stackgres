/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.operator.customresource.sgbackupconfig;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;

import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class AzureBlobStorage {

  @JsonProperty("prefix")
  @NotNull(message = "The prefix is required")
  private String prefix;

  @JsonProperty("credentials")
  @NotNull(message = "The credentials is required")
  private AzureBlobStorageCredentials credentials;

  @JsonProperty("bufferSize")
  private long bufferSize;

  @JsonProperty("maxBuffers")
  private int maxBuffers;

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
  }

  public AzureBlobStorageCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(AzureBlobStorageCredentials credentials) {
    this.credentials = credentials;
  }

  public long getBufferSize() {
    return bufferSize;
  }

  public void setBufferSize(long bufferSize) {
    this.bufferSize = bufferSize;
  }

  public int getMaxBuffers() {
    return maxBuffers;
  }

  public void setMaxBuffers(int maxBuffers) {
    this.maxBuffers = maxBuffers;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("prefix", prefix)
        .add("credentials", credentials)
        .add("bufferSize", bufferSize)
        .add("maxBuffers", maxBuffers)
        .toString();
  }

}

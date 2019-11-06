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
public class AwsS3Storage {

  @JsonProperty("prefix")
  @NotNull(message = "The prefix is required")
  private String prefix;

  @JsonProperty("credentials")
  @NotNull(message = "The credentials is required")
  private AwsCredentials credentials;

  @JsonProperty("region")
  private String region;

  @JsonProperty("endpoint")
  private String endpoint;

  @JsonProperty("forcePathStyle")
  private boolean forcePathStyle;

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

  public String getPrefix() {
    return prefix;
  }

  public void setPrefix(String prefix) {
    this.prefix = prefix;
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

  public boolean isForcePathStyle() {
    return forcePathStyle;
  }

  public void setForcePathStyle(boolean forcePathStyle) {
    this.forcePathStyle = forcePathStyle;
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
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("prefix", prefix)
        .add("credentials", credentials)
        .add("region", region)
        .add("endpoint", endpoint)
        .add("forcePathStyle", forcePathStyle)
        .add("storageClass", storageClass)
        .add("sse", sse)
        .add("sseKmsId", sseKmsId)
        .add("cseKmsId", cseKmsId)
        .add("cseKmsRegion", cseKmsRegion)
        .toString();
  }

}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AwsS3CompatibleStorageDto {

  private String bucket;

  private String path;

  private AwsCredentialsDto awsCredentials;

  private String region;

  private String endpoint;

  private Boolean enablePathStyleAddressing;

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

  public AwsCredentialsDto getAwsCredentials() {
    return awsCredentials;
  }

  public void setAwsCredentials(AwsCredentialsDto awsCredentials) {
    this.awsCredentials = awsCredentials;
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

  public Boolean isEnablePathStyleAddressing() {
    return enablePathStyleAddressing;
  }

  public void setEnablePathStyleAddressing(Boolean enablePathStyleAddressing) {
    this.enablePathStyleAddressing = enablePathStyleAddressing;
  }

  public String getStorageClass() {
    return storageClass;
  }

  public void setStorageClass(String storageClass) {
    this.storageClass = storageClass;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

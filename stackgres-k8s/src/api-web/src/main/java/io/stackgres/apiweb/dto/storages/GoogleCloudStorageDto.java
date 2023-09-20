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
public class GoogleCloudStorageDto {

  private String bucket;

  private String path;

  private GoogleCloudCredentialsDto gcpCredentials;

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

  public GoogleCloudCredentialsDto getGcpCredentials() {
    return gcpCredentials;
  }

  public void setGcpCredentials(GoogleCloudCredentialsDto gcpCredentials) {
    this.gcpCredentials = gcpCredentials;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

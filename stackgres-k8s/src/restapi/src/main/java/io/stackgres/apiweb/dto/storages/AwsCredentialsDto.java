/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AwsCredentialsDto {

  private String accessKeyId;

  private String secretAccessKey;

  private AwsSecretKeySelector secretKeySelectors = new AwsSecretKeySelector();

  @JsonProperty("useIAMRole")
  private Boolean useIamRole;

  public String getAccessKeyId() {
    return accessKeyId;
  }

  public void setAccessKeyId(String accessKeyId) {
    this.accessKeyId = accessKeyId;
  }

  public String getSecretAccessKey() {
    return secretAccessKey;
  }

  public void setSecretAccessKey(String secretAccessKey) {
    this.secretAccessKey = secretAccessKey;
  }

  public AwsSecretKeySelector getSecretKeySelectors() {
    return secretKeySelectors;
  }

  public void setSecretKeySelectors(AwsSecretKeySelector secretKeySelectors) {
    this.secretKeySelectors = secretKeySelectors;
  }

  public Boolean getUseIamRole() {
    return useIamRole;
  }

  public void setUseIamRole(Boolean useIamRole) {
    this.useIamRole = useIamRole;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

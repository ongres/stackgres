/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AwsSecretKeySelector {

  @JsonProperty("accessKeyId")
  private SecretKeySelector accessKeyId;

  @JsonProperty("secretAccessKey")
  private SecretKeySelector secretAccessKey;

  @JsonProperty("caCertificate")
  private SecretKeySelector caCertificate;

  public SecretKeySelector getAccessKeyId() {
    return accessKeyId;
  }

  public void setAccessKeyId(SecretKeySelector accessKeyId) {
    this.accessKeyId = accessKeyId;
  }

  public SecretKeySelector getSecretAccessKey() {
    return secretAccessKey;
  }

  public void setSecretAccessKey(SecretKeySelector secretAccessKey) {
    this.secretAccessKey = secretAccessKey;
  }

  public SecretKeySelector getCaCertificate() {
    return caCertificate;
  }

  public void setCaCertificate(SecretKeySelector caCertificate) {
    this.caCertificate = caCertificate;
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

}

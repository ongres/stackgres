/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.storages;

import java.util.Objects;

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

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AwsSecretKeySelector that = (AwsSecretKeySelector) o;
    return Objects.equals(accessKeyId, that.accessKeyId)
        && Objects.equals(secretAccessKey, that.secretAccessKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessKeyId, secretAccessKey);
  }
}

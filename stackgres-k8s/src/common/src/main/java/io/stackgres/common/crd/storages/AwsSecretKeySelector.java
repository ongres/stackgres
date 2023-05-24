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
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.sundr.builder.annotations.Buildable;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class AwsSecretKeySelector {

  @JsonProperty("accessKeyId")
  @NotNull(message = "The accessKey is required")
  @Valid
  private SecretKeySelector accessKeyId;

  @JsonProperty("secretAccessKey")
  @NotNull(message = "The secretKey is required")
  @Valid
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
  public int hashCode() {
    return Objects.hash(accessKeyId, secretAccessKey);
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
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

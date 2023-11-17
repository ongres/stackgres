/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class AwsSecretKeySelector {

  @NotNull(message = "The accessKey is required")
  @Valid
  private SecretKeySelector accessKeyId;

  @NotNull(message = "The secretKey is required")
  @Valid
  private SecretKeySelector secretAccessKey;

  @Valid
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
  public int hashCode() {
    return Objects.hash(accessKeyId, caCertificate, secretAccessKey);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AwsSecretKeySelector)) {
      return false;
    }
    AwsSecretKeySelector other = (AwsSecretKeySelector) obj;
    return Objects.equals(accessKeyId, other.accessKeyId)
        && Objects.equals(caCertificate, other.caCertificate)
        && Objects.equals(secretAccessKey, other.secretAccessKey);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

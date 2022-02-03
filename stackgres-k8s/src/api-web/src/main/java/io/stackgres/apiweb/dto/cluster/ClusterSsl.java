/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.SecretKeySelector;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterSsl {

  @JsonProperty("enabled")
  private Boolean enabled;

  @JsonProperty("certificateSecretKeySelector")
  private SecretKeySelector certificateSecretKeySelector;

  @JsonProperty("privateKeySecretKeySelector")
  private SecretKeySelector privateKeySecretKeySelector;

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public SecretKeySelector getCertificateSecretKeySelector() {
    return certificateSecretKeySelector;
  }

  public void setCertificateSecretKeySelector(SecretKeySelector certificateSecretKeySelector) {
    this.certificateSecretKeySelector = certificateSecretKeySelector;
  }

  public SecretKeySelector getPrivateKeySecretKeySelector() {
    return privateKeySecretKeySelector;
  }

  public void setPrivateKeySecretKeySelector(SecretKeySelector privateKeySecretKeySelector) {
    this.privateKeySecretKeySelector = privateKeySecretKeySelector;
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
    ClusterSsl that = (ClusterSsl) o;
    return Objects.equals(enabled, that.enabled)
        && Objects.equals(certificateSecretKeySelector, that.certificateSecretKeySelector)
        && Objects.equals(privateKeySecretKeySelector, that.privateKeySecretKeySelector);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, certificateSecretKeySelector, privateKeySecretKeySelector);
  }
}

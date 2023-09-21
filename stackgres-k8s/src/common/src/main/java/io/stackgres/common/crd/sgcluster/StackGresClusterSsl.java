/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
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
public class StackGresClusterSsl {

  private Boolean enabled;

  @Valid
  private SecretKeySelector certificateSecretKeySelector;

  @Valid
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
  public int hashCode() {
    return Objects.hash(certificateSecretKeySelector, enabled, privateKeySecretKeySelector);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterSsl)) {
      return false;
    }
    StackGresClusterSsl other = (StackGresClusterSsl) obj;
    return Objects.equals(certificateSecretKeySelector, other.certificateSecretKeySelector)
        && Objects.equals(enabled, other.enabled)
        && Objects.equals(privateKeySecretKeySelector, other.privateKeySecretKeySelector);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

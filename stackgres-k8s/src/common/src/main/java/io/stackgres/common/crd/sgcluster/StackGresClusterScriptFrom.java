/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.fabric8.kubernetes.api.model.ConfigMapKeySelector;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class StackGresClusterScriptFrom {

  @JsonProperty("secretKeyRef")
  @Valid
  private SecretKeySelector secretKeyRef;

  @JsonProperty("configMapKeyRef")
  @Valid
  private ConfigMapKeySelector configMapKeyRef;

  @AssertTrue(message = "secretKeyRef and configMapKeyRef are mutually exclusive and one of them is"
      + " required.")
  public boolean areSecretKeySelectorAndConfigMapKeySelectorMutuallyExclusiveAndOneRequired() {
    return (secretKeyRef != null && configMapKeyRef == null) // NOPMD
        || (secretKeyRef == null && configMapKeyRef != null); //NOPMD
  }

  public SecretKeySelector getSecretKeyRef() {
    return secretKeyRef;
  }

  public void setSecretKeyRef(SecretKeySelector secretKeyRef) {
    this.secretKeyRef = secretKeyRef;
  }

  public ConfigMapKeySelector getConfigMapKeyRef() {
    return configMapKeyRef;
  }

  public void setConfigMapKeyRef(ConfigMapKeySelector configMapKeyRef) {
    this.configMapKeyRef = configMapKeyRef;
  }

  @Override
  public int hashCode() {
    return Objects.hash(configMapKeyRef, secretKeyRef);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterScriptFrom)) {
      return false;
    }
    StackGresClusterScriptFrom other = (StackGresClusterScriptFrom) obj;
    return Objects.equals(configMapKeyRef, other.configMapKeyRef)
        && Objects.equals(secretKeyRef, other.secretKeyRef);
  }

  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("secretKeyRef", secretKeyRef)
        .add("configMapKeyRef", configMapKeyRef)
        .toString();
  }
}

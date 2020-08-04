/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.apiweb.dto.cluster;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.quarkus.runtime.annotations.RegisterForReflection;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterScriptFrom {

  @NotEmpty
  private String secretScript;

  @Valid
  private SecretKeySelectorDto secretKeyRef;

  @NotEmpty
  private String configMapScript;

  @Valid
  private ConfigMapKeySelectorDto configMapKeyRef;

  @JsonIgnore
  @AssertTrue(message = "secretKeyRef and configMapKeyRef are mutually exclusive and one of them is"
      + " required.")
  public boolean areSecretKeySelectorAndConfigMapKeySelectorMutuallyExclusiveAndOneRequired() {
    return (isSecretScriptConfigured() && !isConfigMapScriptConfigured()) // NOPMD
        || (!isSecretScriptConfigured() && isConfigMapScriptConfigured()); //NOPMD
  }

  @JsonIgnore
  public boolean isSecretScriptConfigured() {
    return secretKeyRef != null || secretScript != null;
  }

  @JsonIgnore
  public boolean isConfigMapScriptConfigured() {
    return configMapKeyRef != null || configMapScript != null;
  }

  public String getSecretScript() {
    return secretScript;
  }

  public void setSecretScript(String secretScript) {
    this.secretScript = secretScript;
  }

  public SecretKeySelectorDto getSecretKeyRef() {
    return secretKeyRef;
  }

  public void setSecretKeyRef(SecretKeySelectorDto secretKeyRef) {
    this.secretKeyRef = secretKeyRef;
  }

  public String getConfigMapScript() {
    return configMapScript;
  }

  public void setConfigMapScript(String configMapScript) {
    this.configMapScript = configMapScript;
  }

  public ConfigMapKeySelectorDto getConfigMapKeyRef() {
    return configMapKeyRef;
  }

  public void setConfigMapKeyRef(ConfigMapKeySelectorDto configMapKeyRef) {
    this.configMapKeyRef = configMapKeyRef;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterScriptFrom that = (ClusterScriptFrom) o;
    return Objects.equals(secretScript, that.secretScript)
        && Objects.equals(secretKeyRef, that.secretKeyRef)
        && Objects.equals(configMapScript, that.configMapScript)
        && Objects.equals(configMapKeyRef, that.configMapKeyRef);
  }

  @Override
  public int hashCode() {
    return Objects.hash(secretScript, secretKeyRef, configMapScript, configMapKeyRef);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("secretScript", secretScript)
        .add("secretKeyRef", secretKeyRef)
        .add("configMapScript", configMapScript)
        .add("configMapKeyRef", configMapKeyRef)
        .toString();
  }
}

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
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
public class ClusterScriptFrom {

  @NotEmpty
  private String secretScript;

  @Valid
  private SecretKeySelector secretKeyRef;

  @NotEmpty
  private String configMapScript;

  @Valid
  private ConfigMapKeySelector configMapKeyRef;

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

  public SecretKeySelector getSecretKeyRef() {
    return secretKeyRef;
  }

  public void setSecretKeyRef(SecretKeySelector secretKeyRef) {
    this.secretKeyRef = secretKeyRef;
  }

  public String getConfigMapScript() {
    return configMapScript;
  }

  public void setConfigMapScript(String configMapScript) {
    this.configMapScript = configMapScript;
  }

  public ConfigMapKeySelector getConfigMapKeyRef() {
    return configMapKeyRef;
  }

  public void setConfigMapKeyRef(ConfigMapKeySelector configMapKeyRef) {
    this.configMapKeyRef = configMapKeyRef;
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
}

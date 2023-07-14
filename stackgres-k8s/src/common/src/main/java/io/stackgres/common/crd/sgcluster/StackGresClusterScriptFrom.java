/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.stackgres.common.crd.ConfigMapKeySelector;
import io.stackgres.common.crd.SecretKeySelector;
import io.stackgres.common.validation.FieldReference;
import io.stackgres.common.validation.FieldReference.ReferencedField;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class StackGresClusterScriptFrom {

  @JsonProperty("secretKeyRef")
  @Valid
  private SecretKeySelector secretKeyRef;

  @JsonProperty("configMapKeyRef")
  @Valid
  private ConfigMapKeySelector configMapKeyRef;

  @ReferencedField("secretKeyRef")
  interface SecretKeyRef extends FieldReference { }

  @ReferencedField("configMapKeyRef")
  interface ConfigMapKeyRef extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "secretKeyRef and configMapKeyRef are mutually exclusive and one of them is"
      + " required.",
      payload = { SecretKeyRef.class, ConfigMapKeyRef.class })
  public boolean isSecretKeySelectorAndConfigMapKeySelectorMutuallyExclusiveAndRequired() {
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
    return StackGresUtil.toPrettyYaml(this);
  }
}

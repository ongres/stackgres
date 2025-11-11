/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.time.format.DateTimeParseException;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
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
public class StackGresClusterSsl {

  private Boolean enabled;

  @Valid
  private SecretKeySelector certificateSecretKeySelector;

  @Valid
  private SecretKeySelector privateKeySecretKeySelector;

  private String duration;

  @ReferencedField("duration")
  interface Duration extends FieldReference { }

  @JsonIgnore
  @AssertTrue(message = "duration must be positive and in ISO 8601 duration format:"
      + " `PnDTnHnMn.nS`.",
      payload = Duration.class)
  public boolean isBackupNewerThanValid() {
    try {
      if (duration != null) {
        return !java.time.Duration.parse(duration).isNegative();
      }
      return true;
    } catch (DateTimeParseException ex) {
      return false;
    }
  }

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

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
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

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonIgnoreProperties(ignoreUnknown = true)
@Buildable(editableEnabled = false, validationEnabled = false, generateBuilderPackage = false,
    lazyCollectionInitEnabled = false, lazyMapInitEnabled = false,
    builderPackage = "io.fabric8.kubernetes.api.builder")
public class AwsCredentials {

  @NotNull(message = "The secretKeySelectors are required")
  @Valid
  private AwsSecretKeySelector secretKeySelectors;

  @JsonProperty("useIAMRole")
  private Boolean useIamRole;

  public AwsSecretKeySelector getSecretKeySelectors() {
    return secretKeySelectors;
  }

  public void setSecretKeySelectors(AwsSecretKeySelector secretKeySelectors) {
    this.secretKeySelectors = secretKeySelectors;
  }

  public Boolean getUseIamRole() {
    return useIamRole;
  }

  public void setUseIamRole(Boolean useIamRole) {
    this.useIamRole = useIamRole;
  }

  @Override
  public int hashCode() {
    return Objects.hash(secretKeySelectors, useIamRole);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof AwsCredentials)) {
      return false;
    }
    AwsCredentials other = (AwsCredentials) obj;
    return Objects.equals(secretKeySelectors, other.secretKeySelectors)
        && Objects.equals(useIamRole, other.useIamRole);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

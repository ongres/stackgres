/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.sgcluster;

import java.util.Objects;

import javax.validation.Valid;

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
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresClusterPatroniCredentials {

  @JsonProperty("restApiPassword")
  @Valid
  private SecretKeySelector restApiPassword;

  public SecretKeySelector getRestApiPassword() {
    return restApiPassword;
  }

  public void setRestApiPassword(SecretKeySelector restApiPassword) {
    this.restApiPassword = restApiPassword;
  }

  @Override
  public int hashCode() {
    return Objects.hash(restApiPassword);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof StackGresClusterPatroniCredentials)) {
      return false;
    }
    StackGresClusterPatroniCredentials other = (StackGresClusterPatroniCredentials) obj;
    return Objects.equals(restApiPassword, other.restApiPassword);
  }

  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

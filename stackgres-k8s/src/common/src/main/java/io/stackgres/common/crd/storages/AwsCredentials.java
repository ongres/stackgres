/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.storages;

import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

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

  public AwsSecretKeySelector getSecretKeySelectors() {
    return secretKeySelectors;
  }

  public void setSecretKeySelectors(AwsSecretKeySelector secretKeySelectors) {
    this.secretKeySelectors = secretKeySelectors;
  }

  @Override
  public int hashCode() {
    return Objects.hash(secretKeySelectors);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AwsCredentials that = (AwsCredentials) o;
    return Objects.equals(secretKeySelectors, that.secretKeySelectors);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.postgres.service;

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
public class StackGresPostgresServices<T extends StackGresPostgresService> {

  @Valid
  @NotNull(message = "primary is required")
  protected T primary;

  @Valid
  @NotNull(message = "replicas is required")
  protected T replicas;

  public T getPrimary() {
    return primary;
  }

  public void setPrimary(T primary) {
    this.primary = primary;
  }

  public T getReplicas() {
    return replicas;
  }

  public void setReplicas(T replicas) {
    this.replicas = replicas;
  }

  @Override
  public int hashCode() {
    return Objects.hash(primary, replicas);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StackGresPostgresServices<?> that = (StackGresPostgresServices<?>) o;
    return Objects.equals(primary, that.primary)
        && Objects.equals(replicas, that.replicas);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}

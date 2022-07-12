/*
 * Copyright (C) 2019 OnGres, Inc.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package io.stackgres.common.crd.postgres.service;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.stackgres.common.StackGresUtil;
import io.sundr.builder.annotations.Buildable;

@JsonDeserialize
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@RegisterForReflection
@Buildable(editableEnabled = false, validationEnabled = false, lazyCollectionInitEnabled = false)
public class StackGresPostgresServices {

  protected StackGresPostgresService primary;

  protected StackGresPostgresService replicas;

  public StackGresPostgresServices() {}

  public StackGresPostgresServices(StackGresPostgresService primary,
      StackGresPostgresService replicas) {
    this.primary = primary;
    this.replicas = replicas;
  }

  public StackGresPostgresService getPrimary() {
    return primary;
  }

  public void setPrimary(StackGresPostgresService primary) {
    this.primary = primary;
  }

  public StackGresPostgresService getReplicas() {
    return replicas;
  }

  public void setReplicas(StackGresPostgresService replicas) {
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
    StackGresPostgresServices that = (StackGresPostgresServices) o;
    return Objects.equals(primary, that.primary)
        && Objects.equals(replicas, that.replicas);
  }

  @Override
  public String toString() {
    return StackGresUtil.toPrettyYaml(this);
  }
}
